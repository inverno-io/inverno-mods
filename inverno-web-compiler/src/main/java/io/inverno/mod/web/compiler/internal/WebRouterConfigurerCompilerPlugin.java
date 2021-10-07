/*
 * Copyright 2021 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.web.compiler.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import io.inverno.core.compiler.spi.plugin.CompilerPlugin;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.core.compiler.spi.plugin.PluginExecutionException;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.WebRouterConfigurer;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;
import io.inverno.mod.web.annotation.WebRoutes;
import io.inverno.mod.web.compiler.internal.WebRouterConfigurerClassGenerationContext.GenerationMode;
import io.inverno.mod.web.compiler.spi.WebControllerInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeContextParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebProvidedRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import io.inverno.mod.web.compiler.spi.WebRouterConfigurerQualifiedName;

/**
 * <p>
 * The web Inverno compiler plugin generates a {@link WebRouterConfigurer}
 * implementation that aggregates the routes defined in the
 * {@link WebController @WebController} beans defined in the module as well as
 * the {@link WebRoutes web router configurer} beans provided in the module.
 * </p>
 * 
 * <p>
 * This plugin can also generates an <a href="https://www.openapis.org/">Open
 * API</a> specification for all the web controllers defined in the module. This
 * can be activated with option {@code inverno.web.generateOpenApiDefinition}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class WebRouterConfigurerCompilerPlugin implements CompilerPlugin {

	private static final String OPTION_GENERATE_OPENAPI_DEFINITION = "inverno.web.generateOpenApiDefinition";
	
	private final WebRouterConfigurerOpenApiGenerator openApiGenerator;
	private final WebRouterConfigurerClassGenerator webRouterConfigurerClassGenerator;
	private final WebRouteDuplicateDetector webRouteDuplicateDectector;
	
	private PluginContext pluginContext;
	private TypeHierarchyExtractor typeHierarchyExtractor;
	
	private TypeMirror webControllerAnnotationType;
	private TypeMirror webRoutesAnnotationType;
	private TypeMirror webRouterConfigurerType;
	private TypeMirror exchangeContextType;
	private TypeMirror objectType;
	
	private boolean enabled = true;
	
	public WebRouterConfigurerCompilerPlugin() {
		this.webRouterConfigurerClassGenerator = new WebRouterConfigurerClassGenerator();
		this.openApiGenerator = new WebRouterConfigurerOpenApiGenerator();
		this.webRouteDuplicateDectector = new WebRouteDuplicateDetector();
	}
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(WebRoute.class.getCanonicalName(), WebController.class.getCanonicalName());
	}
	
	@Override
	public Set<String> getSupportedOptions() {
		return Set.of(WebRouterConfigurerCompilerPlugin.OPTION_GENERATE_OPENAPI_DEFINITION);
	}

	@Override
	public void init(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
		
		TypeElement webControllerElement = this.pluginContext.getElementUtils().getTypeElement(WebController.class.getCanonicalName());
		if(webControllerElement == null) {
			this.enabled = false;
			if(pluginContext.getOptions().isDebug()) {
				System.err.println("Plugin " + WebRouterConfigurerCompilerPlugin.class.getCanonicalName() + " disabled due to missing dependencies");
			}
			return;
		}
		this.webControllerAnnotationType = webControllerElement.asType();
		this.webRoutesAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebRoutes.class.getCanonicalName()).asType();
		this.webRouterConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebRouterConfigurer.class.getCanonicalName()).asType());
		this.exchangeContextType = this.pluginContext.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType();
		this.objectType = this.pluginContext.getElementUtils().getTypeElement(Object.class.getCanonicalName()).asType();
		
		this.typeHierarchyExtractor = new TypeHierarchyExtractor(this.pluginContext.getTypeUtils());
	}

	@Override
	public boolean canExecute(ModuleElement moduleElement) {
		return this.enabled && this.pluginContext.getElementUtils().getTypeElement(moduleElement, WebController.class.getCanonicalName()) != null;
	}
	
	@Override
	public void execute(PluginExecution execution) throws PluginExecutionException {
		WebRouteInfoFactory webRouteFactory = new WebRouteInfoFactory(this.pluginContext, execution);
		WebRouterConfigurerQualifiedName webRouterConfigurerQName = new WebRouterConfigurerQualifiedName(execution.getModuleQualifiedName());
		
		this.processWebRoutes(execution, webRouteFactory);
		List<? extends WebControllerInfo> webControllers = this.processWebControllers(execution, webRouteFactory);
		List<? extends WebProvidedRouterConfigurerInfo> webRouters = this.processWebRouters(execution, webRouteFactory);

		Comparator<TypeMirror> contexTypeComparator = (t1,t2) -> {
			if(this.pluginContext.getTypeUtils().isSameType(t1,t2)) {
				return 0;
			}
			return t1.toString().compareTo(t2.toString());
		};
		
		Set<TypeMirror> contextTypes = new TreeSet<>(contexTypeComparator);
		
		webControllers.stream()
			.flatMap(controller -> Arrays.stream(controller.getRoutes())
				.flatMap(route -> Arrays.stream(route.getParameters())
					.flatMap(parameter -> {
						if(parameter instanceof WebExchangeParameterInfo) {
							TypeMirror contextType = ((WebExchangeParameterInfo)parameter).getContextType();
							return Stream.concat(Stream.of(contextType), this.getAllSuperTypes(contextType).stream());
						}
						else if(parameter instanceof WebExchangeContextParameterInfo) {
							TypeMirror contextType = parameter.getType();
							return Stream.concat(Stream.of(contextType), this.getAllSuperTypes(contextType).stream());
						}
						return Stream.of();
					})
				)
			)
			.forEach(contextTypes::add);
		
		webRouters.stream().forEach(router -> {
			contextTypes.add(router.getContextType());
			contextTypes.addAll(this.getAllSuperTypes(router.getContextType()));
		});
		
		GenericWebRouterConfigurerInfo webRouterConfigurerInfo = new GenericWebRouterConfigurerInfo(execution.getModuleElement(), webRouterConfigurerQName, webControllers, webRouters, contextTypes);
		
		for(Map.Entry<WebRouteInfo, Set<WebRouteInfo>> e : this.webRouteDuplicateDectector.findDuplicates(Stream.concat(webControllers.stream().flatMap(controller -> Arrays.stream(controller.getRoutes())), webRouters.stream().flatMap(router -> Arrays.stream(router.getRoutes()))).collect(Collectors.toList())).entrySet()) {
			e.getKey().error("Route " + e.getKey().getQualifiedName() + " is conflicting with route(s):\n" + e.getValue().stream().map(route -> "- " + route.getQualifiedName()).collect(Collectors.joining("\n")));
		}
		
		if(!webRouterConfigurerInfo.hasError() && (webRouterConfigurerInfo.getControllers().length > 0 || webRouterConfigurerInfo.getRouters().length > 0)) {
			try {
				execution.createSourceFile(webRouterConfigurerInfo.getQualifiedName().getClassName(), execution.getElements().stream().toArray(Element[]::new), () -> {
					return webRouterConfigurerInfo.accept(this.webRouterConfigurerClassGenerator, new WebRouterConfigurerClassGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), GenerationMode.CONFIGURER_CLASS)).toString();
				});
			} 
			catch (IOException e) {
				throw new PluginExecutionException("Unable to generate web router configurer class " + webRouterConfigurerInfo.getQualifiedName().getClassName(), e);
			}
			
			if(webRouterConfigurerInfo.getControllers().length > 0 && this.pluginContext.getOptions().isOptionActivated(WebRouterConfigurerCompilerPlugin.OPTION_GENERATE_OPENAPI_DEFINITION, false)) {
				try {
					execution.createResourceFile("META-INF/inverno/web/" + webRouterConfigurerQName.getModuleQName().toString() + "/openapi.yml", execution.getElements().stream().toArray(Element[]::new), () -> {
						return webRouterConfigurerInfo.accept(this.openApiGenerator, new WebRouterConfigurerOpenApiGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), this.pluginContext.getDocUtils(), WebRouterConfigurerOpenApiGenerationContext.GenerationMode.ROUTER_SPEC)).toString();
					});
				} 
				catch (Exception e) {
					System.err.print("\n");
					System.err.println("Error generating OpenApi specification for module : " + execution.getModuleQualifiedName());
					if(this.pluginContext.getOptions().isDebug()) {
						e.printStackTrace();
					}
					System.out.print("... ");
				}
			}
		}
	}
	
	private void processWebRoutes(PluginExecution execution, WebRouteInfoFactory webRouteFactory) {
		for(ExecutableElement routeElement : execution.<ExecutableElement>getElementsAnnotatedWith(WebRoute.class)) {
			webRouteFactory.compileRoute(routeElement);
		}
	}
	
	private List<? extends WebControllerInfo> processWebControllers(PluginExecution execution, WebRouteInfoFactory webRouteFactory) {
		return Arrays.stream(execution.getBeans())
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				return this.typeHierarchyExtractor.extractTypeHierarchy(beanElement).stream()
					.map(element -> this.pluginContext.getElementUtils().getAllAnnotationMirrors(element).stream()
						.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webControllerAnnotationType))
						.findFirst()
					)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.findFirst()
					.map(webControllerAnnotation -> {
						String controllerRootPath = null;
						for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webControllerAnnotation).entrySet()) {
							switch(value.getKey().getSimpleName().toString()) {
								case "path" : controllerRootPath = (String)value.getValue().getValue();
									break;
							}
						}
						
						List<GenericWebRouteInfo> webRoutes = webRouteFactory.compileControllerRoutes(bean);
						
						if(webRoutes.isEmpty()) {
							bean.warning("Ignoring web controller which does not define any route");
							return null;
						}
						return new GenericWebControllerInfo(beanElement, bean.getQualifiedName(), bean, (DeclaredType)bean.getType(), controllerRootPath, webRoutes);
					});
			})
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}
	
	private List<? extends WebProvidedRouterConfigurerInfo> processWebRouters(PluginExecution execution, WebRouteInfoFactory webRouteFactory) {
		// Get component routers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.webRouterConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				Optional<? extends WebProvidedRouterConfigurerInfo> providedRouterConfigurerInfo = this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
					.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRoutesAnnotationType))
					.findFirst()
					.map(webRouterConfigurerAnnotation -> {
						List<ProvidedWebRouteInfo> webRoutes = webRouteFactory.compileRouterRoutes(bean);
						if(webRoutes.isEmpty()) {
							return null;
						}
						
						List<? extends TypeMirror> webRouterConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.webRouterConfigurerType)).getTypeArguments();
						TypeMirror contextType = !webRouterConfigurerTypeArguments.isEmpty() ? webRouterConfigurerTypeArguments.get(0) : this.exchangeContextType;
						
						return new GenericWebProvidedRouterConfigurerInfo(beanElement, new WebRouterConfigurerQualifiedName(bean.getQualifiedName(), this.pluginContext.getTypeUtils().asElement(this.pluginContext.getTypeUtils().erasure(bean.getType())).getSimpleName().toString()), bean, webRoutes, contextType);
					});
				
				if(!providedRouterConfigurerInfo.isPresent()) {
					bean.warning("Ignoring web router configurer which does not define any route");
					return null;
				}
				return providedRouterConfigurerInfo.get();
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Finds the super types matching the specified erased super type.
	 * </p>
	 * 
	 * @param type            the type where to find the super type
	 * @param erasedSuperType the erased super type to find
	 * 
	 * @return a type or null
	 */
	private TypeMirror findSuperType(TypeMirror type, TypeMirror erasedSuperType) {
		for(TypeMirror superType : this.pluginContext.getTypeUtils().directSupertypes(type)) {
			if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(superType), erasedSuperType)) {
				return superType;
			}
			else {
				TypeMirror result = this.findSuperType(superType, erasedSuperType);
				if(result != null) {
					return result;
				}
			}
		}
		return null;
	}
	
	/**
	 * <p>
	 * Returns all the unique super types of the specified type, Object excluded.
	 * </p>
	 * 
	 * @param type a type
	 * 
	 * @return a set of types
	 */
	private Set<TypeMirror> getAllSuperTypes(TypeMirror type) {
		Set<TypeMirror> result = new TreeSet<>( (t1,t2) -> {
			if(this.pluginContext.getTypeUtils().isSameType(t1, t2)) {
				return 0;
			}
			return t1.toString().compareTo(t2.toString());
		});
		
		for(TypeMirror superType : this.pluginContext.getTypeUtils().directSupertypes(type)) {
			if(!this.pluginContext.getTypeUtils().isSameType(superType, this.objectType)) {
				result.add(superType);
				result.addAll(this.getAllSuperTypes(superType));
			}
		}
		
		return result;
	}
}
