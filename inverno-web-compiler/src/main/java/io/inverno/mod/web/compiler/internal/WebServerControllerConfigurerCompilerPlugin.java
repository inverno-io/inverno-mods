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

import io.inverno.core.annotation.Bean;
import io.inverno.core.compiler.spi.ModuleBeanInfo;
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
import io.inverno.mod.web.ErrorWebInterceptorsConfigurer;
import io.inverno.mod.web.ErrorWebRouterConfigurer;
import io.inverno.mod.web.ErrorWebRoutesConfigurer;
import io.inverno.mod.web.WebInterceptorsConfigurer;
import io.inverno.mod.web.WebRouterConfigurer;
import io.inverno.mod.web.WebRoutesConfigurer;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;
import io.inverno.mod.web.annotation.WebRoutes;
import io.inverno.mod.web.compiler.internal.WebServerControllerConfigurerClassGenerationContext.GenerationMode;
import io.inverno.mod.web.compiler.spi.ErrorWebInterceptorsConfigurerInfo;
import io.inverno.mod.web.compiler.spi.ErrorWebRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.ErrorWebRoutesConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebConfigurerQualifiedName;
import io.inverno.mod.web.compiler.spi.WebControllerInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeContextParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebInterceptorsConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import io.inverno.mod.web.compiler.spi.WebServerControllerConfigurerQualifiedName;
import io.inverno.mod.web.compiler.spi.WebRoutesConfigurerInfo;
import java.util.ArrayList;
import java.util.function.Function;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import io.inverno.mod.web.compiler.spi.WebRouterConfigurerInfo;
import javax.lang.model.element.Modifier;

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
public class WebServerControllerConfigurerCompilerPlugin implements CompilerPlugin {

	private static final String OPTION_GENERATE_OPENAPI_DEFINITION = "inverno.web.generateOpenApiDefinition";
	
	private final WebServerControllerConfigurerOpenApiGenerator openApiGenerator;
	private final WebServerControllerConfigurerClassGenerator webRouterConfigurerClassGenerator;
	private final WebRouteClashDetector webRouteClashDetector;
	
	private PluginContext pluginContext;
	private TypeHierarchyExtractor typeHierarchyExtractor;
	
	private TypeMirror webControllerAnnotationType;
	private TypeMirror webRoutesAnnotationType;
	
	private TypeMirror webInterceptorsConfigurerType;
	private TypeMirror webRoutesConfigurerType;
	private TypeMirror webRouterConfigurerType;
	
	private TypeMirror errorWebInterceptorsConfigurerType;
	private TypeMirror errorWebRoutesConfigurerType;
	private TypeMirror errorWebRouterConfigurerType;
	
	private TypeMirror exchangeContextType;
	private TypeMirror objectType;
	
	private boolean enabled = true;
	
	public WebServerControllerConfigurerCompilerPlugin() {
		this.webRouterConfigurerClassGenerator = new WebServerControllerConfigurerClassGenerator();
		this.openApiGenerator = new WebServerControllerConfigurerOpenApiGenerator();
		this.webRouteClashDetector = new WebRouteClashDetector();
	}
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(WebRoute.class.getCanonicalName(), WebController.class.getCanonicalName());
	}
	
	@Override
	public Set<String> getSupportedOptions() {
		return Set.of(WebServerControllerConfigurerCompilerPlugin.OPTION_GENERATE_OPENAPI_DEFINITION);
	}

	@Override
	public void init(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
		
		TypeElement webControllerElement = this.pluginContext.getElementUtils().getTypeElement(WebController.class.getCanonicalName());
		if(webControllerElement == null) {
			this.enabled = false;
			if(pluginContext.getOptions().isDebug()) {
				System.err.println("Plugin " + WebServerControllerConfigurerCompilerPlugin.class.getCanonicalName() + " disabled due to missing dependencies");
			}
			return;
		}
		this.webControllerAnnotationType = webControllerElement.asType();
		this.webRoutesAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebRoutes.class.getCanonicalName()).asType();
		
		this.webInterceptorsConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebInterceptorsConfigurer.class.getCanonicalName()).asType());
		this.webRoutesConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebRoutesConfigurer.class.getCanonicalName()).asType());
		this.webRouterConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebRouterConfigurer.class.getCanonicalName()).asType());
		
		this.errorWebInterceptorsConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(ErrorWebInterceptorsConfigurer.class.getCanonicalName()).asType());
		this.errorWebRoutesConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(ErrorWebRoutesConfigurer.class.getCanonicalName()).asType());
		this.errorWebRouterConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(ErrorWebRouterConfigurer.class.getCanonicalName()).asType());
		
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
		WebServerControllerConfigurerQualifiedName webRouterConfigurerQName = new WebServerControllerConfigurerQualifiedName(execution.getModuleQualifiedName());
		
		this.processWebRoutes(execution, webRouteFactory);
		List<? extends WebControllerInfo> webControllers = this.processWebControllers(execution, webRouteFactory);
		
		List<? extends WebInterceptorsConfigurerInfo> webInterceptorsConfigurers = this.processWebInterceptorsConfigurers(execution);
		List<? extends WebRoutesConfigurerInfo> webRoutesConfigurers = this.processWebRoutesConfigurers(execution, webRouteFactory);
		List<? extends WebRouterConfigurerInfo> webRouterConfigurers = this.processWebRouterConfigurers(execution, webRouteFactory);

		List<? extends ErrorWebInterceptorsConfigurerInfo> errorWebInterceptorsConfigurers = this.processErrorWebInterceptorsConfigurers(execution);
		List<? extends ErrorWebRoutesConfigurerInfo> errorWebRoutesConfigurers = this.processErrorWebRoutesConfigurers(execution);
		List<? extends ErrorWebRouterConfigurerInfo> errorWebRouterConfigurers = this.processErrorWebRouterConfigurers(execution);
		
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
						TypeMirror contextType;
						if(parameter instanceof WebExchangeParameterInfo) {
							contextType = ((WebExchangeParameterInfo)parameter).getContextType();
						}
						else if(parameter instanceof WebExchangeContextParameterInfo) {
							contextType = parameter.getType();
						}
						else {
							return Stream.of();
						}
						
						List<? extends TypeMirror> actualTypes;
						if(contextType.getKind() == TypeKind.INTERSECTION) {
							actualTypes = ((IntersectionType)contextType).getBounds();
						}
						else {
							actualTypes = List.of(contextType);
						}
						return Stream.concat(
							actualTypes.stream(), 
							actualTypes.stream()
								// we only keep public super types, hidden super types should be included
								// this means we could have compile errors if the user specified a non-public context type explicitly
								.flatMap(type -> this.getAllSuperTypes(contextType).stream().filter(superContextType -> ((DeclaredType)superContextType).asElement().getModifiers().contains(Modifier.PUBLIC)))
						);
					})
				)
			)
			.forEach(contextTypes::add);
		
		Stream.of(
				webInterceptorsConfigurers.stream().map(WebInterceptorsConfigurerInfo::getContextType), 
				webRoutesConfigurers.stream().map(WebRoutesConfigurerInfo::getContextType),
				webRouterConfigurers.stream().map(WebRouterConfigurerInfo::getContextType),
				errorWebInterceptorsConfigurers.stream().map(ErrorWebInterceptorsConfigurerInfo::getContextType), 
				errorWebRoutesConfigurers.stream().map(ErrorWebRoutesConfigurerInfo::getContextType),
				errorWebRouterConfigurers.stream().map(ErrorWebRouterConfigurerInfo::getContextType)
			)
			.flatMap(Function.identity())
			.flatMap(contextType -> {
				List<? extends TypeMirror> actualTypes;
				if(contextType.getKind() == TypeKind.INTERSECTION) {
					actualTypes = ((IntersectionType)contextType).getBounds();
				}
				else {
					actualTypes = List.of(contextType);
				}
				return Stream.concat(
					actualTypes.stream(), 
					actualTypes.stream()
						// we only keep public super types, hidden super types should be included
						// this means we could have compile errors if the user specified a non-public context type explicitly
						.flatMap(type -> this.getAllSuperTypes(contextType).stream().filter(superContextType -> ((DeclaredType)superContextType).asElement().getModifiers().contains(Modifier.PUBLIC)))
				);
			})
			.forEach(contextTypes::add);
		
		GenericWebServerControllerConfigurerInfo webRouterConfigurerInfo = new GenericWebServerControllerConfigurerInfo(
			execution.getModuleElement(), 
			webRouterConfigurerQName, 
			webControllers, 
			webInterceptorsConfigurers, 
			webRoutesConfigurers, 
			webRouterConfigurers, 
			errorWebInterceptorsConfigurers, 
			errorWebRoutesConfigurers, 
			errorWebRouterConfigurers, 
			contextTypes
		);
		
		// Report route clash
		List<WebRouteInfo> routes = new ArrayList<>();
		webControllers.stream().flatMap(controller -> Arrays.stream(controller.getRoutes())).forEach(routes::add);
		webRoutesConfigurers.stream().flatMap(configurer -> Arrays.stream(configurer.getRoutes())).forEach(routes::add);
		webRouterConfigurers.stream().flatMap(configurer -> Arrays.stream(configurer.getRoutes())).forEach(routes::add);
		
		for(Map.Entry<WebRouteInfo, Set<WebRouteInfo>> e : this.webRouteClashDetector.findDuplicates(routes).entrySet()) {
			e.getKey().error("Route " + e.getKey().getQualifiedName() + " is clashing with route(s):\n" + e.getValue().stream().map(route -> "- " + route.getQualifiedName()).collect(Collectors.joining("\n")));
		}
		
		if(!webRouterConfigurerInfo.hasError() && (webRouterConfigurerInfo.getControllers().length > 0 || webRouterConfigurerInfo.getRoutesConfigurers().length > 0 || webRouterConfigurerInfo.getRouterConfigurers().length > 0)) {
			try {
				execution.createSourceFile(webRouterConfigurerInfo.getQualifiedName().getClassName(), execution.getElements().stream().toArray(Element[]::new), () -> {
					return webRouterConfigurerInfo.accept(this.webRouterConfigurerClassGenerator, new WebServerControllerConfigurerClassGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), GenerationMode.CONFIGURER_CLASS)).toString();
				});
			} 
			catch (IOException e) {
				throw new PluginExecutionException("Unable to generate web router configurer class " + webRouterConfigurerInfo.getQualifiedName().getClassName(), e);
			}
			
			if(webRouterConfigurerInfo.getControllers().length > 0 && this.pluginContext.getOptions().isOptionActivated(WebServerControllerConfigurerCompilerPlugin.OPTION_GENERATE_OPENAPI_DEFINITION, false)) {
				try {
					execution.createResourceFile("META-INF/inverno/web/" + webRouterConfigurerQName.getModuleQName().toString() + "/openapi.yml", execution.getElements().stream().toArray(Element[]::new), () -> {
						return webRouterConfigurerInfo.accept(this.openApiGenerator, new WebServerControllerConfigurerOpenApiGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), this.pluginContext.getDocUtils(), WebServerControllerConfigurerOpenApiGenerationContext.GenerationMode.ROUTER_SPEC)).toString();
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
	
	/**
	 * <p>
	 * Scans the module for {@link WebRoute} annotated methods.
	 * </p>
	 * 
	 * @param execution
	 * @param webRouteFactory 
	 */
	private void processWebRoutes(PluginExecution execution, WebRouteInfoFactory webRouteFactory) {
		for(ExecutableElement routeElement : execution.<ExecutableElement>getElementsAnnotatedWith(WebRoute.class)) {
			webRouteFactory.compileRoute(routeElement);
		}
	}
	
	/**
	 * <p>
	 * Scans the module for {@link WebController} annotated beans.
	 * </p>
	 * 
	 * @param execution
	 * @param webRouteFactory
	 * @return 
	 */
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
						return new GenericServerWebControllerInfo(beanElement, bean.getQualifiedName(), bean, (DeclaredType)bean.getType(), controllerRootPath, webRoutes);
					});
			})
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Scans the module for {@link WebInterceptorsConfigurer} beans.
	 * </p>
	 * 
	 * @param execution
	 * @param webRouteFactory
	 * @return 
	 */
	private List<? extends WebInterceptorsConfigurerInfo> processWebInterceptorsConfigurers(PluginExecution execution) {
		// Get web interceptors configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.webInterceptorsConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include al web interceptors configurer
				// - raise a warning if it is defined in the module and not private
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Web interceptors configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				
				List<? extends TypeMirror> webInterceptorsConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.webInterceptorsConfigurerType)).getTypeArguments();
				TypeMirror contextType = !webInterceptorsConfigurerTypeArguments.isEmpty() ? webInterceptorsConfigurerTypeArguments.get(0) : this.exchangeContextType;
				
				return new GenericWebInterceptorsConfigurerInfo(beanElement, new WebConfigurerQualifiedName(bean.getQualifiedName(), this.pluginContext.getTypeUtils().asElement(this.pluginContext.getTypeUtils().erasure(bean.getType())).getSimpleName().toString()), bean, contextType);
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Scans the module for {@link WebRoutesConfigurer} beans.
	 * </p>
	 * 
	 * @param execution
	 * @param webRouteFactory
	 * @return 
	 */
	private List<? extends WebRoutesConfigurerInfo> processWebRoutesConfigurers(PluginExecution execution, WebRouteInfoFactory webRouteFactory) {
		// Get web interceptors configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.webRoutesConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include al web router configurer
				// - raise a warning if it is defined in the module and not private
				// - raise a warning if it is defined without @WebRoutes
				Optional<List<ProvidedWebRouteInfo>> webRoutes = this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
					.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRoutesAnnotationType))
					.findFirst()
					.map(webRouterConfigurerAnnotation -> webRouteFactory.compileRouterRoutes(bean));
				
				if(!webRoutes.isPresent()) {
					bean.warning("Unable to determine route clashes since no route is specified in @WebRoutes");
				}
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Web routes configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				
				List<? extends TypeMirror> webRouterConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.webRoutesConfigurerType)).getTypeArguments();
				TypeMirror contextType = !webRouterConfigurerTypeArguments.isEmpty() ? webRouterConfigurerTypeArguments.get(0) : this.exchangeContextType;
				
				return new GenericWebRoutesConfigurerInfo(beanElement, new WebConfigurerQualifiedName(bean.getQualifiedName(), this.pluginContext.getTypeUtils().asElement(this.pluginContext.getTypeUtils().erasure(bean.getType())).getSimpleName().toString()), bean, webRoutes.orElse(List.of()), contextType);
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Scans the module for {@link WebRouterConfigurer} beans.
	 * </p>
	 * 
	 * @param execution
	 * @param webRouteFactory
	 * @return 
	 */
	private List<? extends WebRouterConfigurerInfo> processWebRouterConfigurers(PluginExecution execution, WebRouteInfoFactory webRouteFactory) {
		// Get web router configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.webRouterConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include al web router configurer
				// - raise a warning if it is defined in the module and not private
				// - raise a warning if it is defined without @WebRoutes
				Optional<List<ProvidedWebRouteInfo>> webRoutes = this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
					.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRoutesAnnotationType))
					.findFirst()
					.map(webRouterConfigurerAnnotation -> webRouteFactory.compileRouterRoutes(bean));
				
				if(!webRoutes.isPresent()) {
					bean.warning("Can't determine route clashes since no route is specified in @WebRoutes");
				}
				
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Web Router configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				
				List<? extends TypeMirror> webRouterConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.webRouterConfigurerType)).getTypeArguments();
				TypeMirror contextType = !webRouterConfigurerTypeArguments.isEmpty() ? webRouterConfigurerTypeArguments.get(0) : this.exchangeContextType;
				
				return new GenericWebRouterConfigurerInfo(beanElement, new WebConfigurerQualifiedName(bean.getQualifiedName(), this.pluginContext.getTypeUtils().asElement(this.pluginContext.getTypeUtils().erasure(bean.getType())).getSimpleName().toString()), bean, webRoutes.orElse(List.of()), contextType);
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Scans the module for {@link ErrorWebInterceptorsConfigurer} beans.
	 * </p>
	 * 
	 * @param execution
	 * @param webRouteFactory
	 * @return 
	 */
	private List<? extends ErrorWebInterceptorsConfigurerInfo> processErrorWebInterceptorsConfigurers(PluginExecution execution) {
		// Get web interceptors configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.errorWebInterceptorsConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include all error web interceptors configurer
				// - raise a warning if it is defined in the module and not private
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Error web interceptors configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				
				List<? extends TypeMirror> errorWebInterceptorsConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.errorWebInterceptorsConfigurerType)).getTypeArguments();
				TypeMirror contextType = !errorWebInterceptorsConfigurerTypeArguments.isEmpty() ? errorWebInterceptorsConfigurerTypeArguments.get(0) : this.exchangeContextType;
				
				return new GenericErrorWebInterceptorsConfigurerInfo(beanElement, new WebConfigurerQualifiedName(bean.getQualifiedName(), this.pluginContext.getTypeUtils().asElement(this.pluginContext.getTypeUtils().erasure(bean.getType())).getSimpleName().toString()), bean, contextType);
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Scans the module for {@link ErrorWebRoutesConfigurer} beans.
	 * </p>
	 * 
	 * @param execution
	 * @param webRouteFactory
	 * @return 
	 */
	private List<? extends ErrorWebRoutesConfigurerInfo> processErrorWebRoutesConfigurers(PluginExecution execution) {
		// Get web interceptors configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.errorWebRoutesConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include all error web routes configurer
				// - raise a warning if it is defined in the module and not private
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Error web routes configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				
				List<? extends TypeMirror> errorWebRouterConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.errorWebRoutesConfigurerType)).getTypeArguments();
				TypeMirror contextType = !errorWebRouterConfigurerTypeArguments.isEmpty() ? errorWebRouterConfigurerTypeArguments.get(0) : this.exchangeContextType;
				
				return new GenericErrorWebRoutesConfigurerInfo(beanElement, new WebConfigurerQualifiedName(bean.getQualifiedName(), this.pluginContext.getTypeUtils().asElement(this.pluginContext.getTypeUtils().erasure(bean.getType())).getSimpleName().toString()), bean, contextType);
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Scans the module for {@link ErrorWebRouterConfigurer} beans.
	 * </p>
	 * 
	 * @param execution
	 * @param webRouteFactory
	 * @return 
	 */
	private List<? extends ErrorWebRouterConfigurerInfo> processErrorWebRouterConfigurers(PluginExecution execution) {
		// Get web router configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.errorWebRouterConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include all error web router configurer
				// - raise a warning if it is defined in the module and not private
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Error web router configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				
				List<? extends TypeMirror> errorWebRouterConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.errorWebRouterConfigurerType)).getTypeArguments();
				TypeMirror contextType = !errorWebRouterConfigurerTypeArguments.isEmpty() ? errorWebRouterConfigurerTypeArguments.get(0) : this.exchangeContextType;
				
				return new GenericErrorWebRouterConfigurerInfo(beanElement, new WebConfigurerQualifiedName(bean.getQualifiedName(), this.pluginContext.getTypeUtils().asElement(this.pluginContext.getTypeUtils().erasure(bean.getType())).getSimpleName().toString()), bean, contextType);
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
