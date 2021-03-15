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
package io.winterframework.mod.web.compiler.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import io.winterframework.core.compiler.spi.plugin.CompilerPlugin;
import io.winterframework.core.compiler.spi.plugin.PluginContext;
import io.winterframework.core.compiler.spi.plugin.PluginExecution;
import io.winterframework.core.compiler.spi.plugin.PluginExecutionException;
import io.winterframework.mod.web.WebExchange;
import io.winterframework.mod.web.WebRouterConfigurer;
import io.winterframework.mod.web.annotation.WebController;
import io.winterframework.mod.web.annotation.WebRoute;
import io.winterframework.mod.web.annotation.WebRoutes;
import io.winterframework.mod.web.compiler.internal.WebRouterConfigurerClassGenerationContext.GenerationMode;
import io.winterframework.mod.web.compiler.spi.WebControllerInfo;
import io.winterframework.mod.web.compiler.spi.WebProvidedRouterConfigurerInfo;
import io.winterframework.mod.web.compiler.spi.WebRouteInfo;
import io.winterframework.mod.web.compiler.spi.WebRouterConfigurerQualifiedName;

/**
 * <p>
 * The web Winter compiler plugin generates a {@link WebRouterConfigurer}
 * implementation that aggregates the routes defined in the
 * {@link WebController @WebController} beans defined in the module as well as
 * the {@link WebRoutes web router configurer} beans provided in the module.
 * </p>
 * 
 * <p>
 * This plugin can also generates an <a href="https://www.openapis.org/">Open
 * API</a> specification for all the web controllers defined in the module. This
 * can be activated with option {@code winter.web.generateOpenApiDefinition}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class WebRouterConfigurerCompilerPlugin implements CompilerPlugin {

	private static final String OPTION_GENERATE_OPENAPI_DEFINITION = "winter.web.generateOpenApiDefinition";
	
	private final WebRouterConfigurerOpenApiGenerator openApiGenrator;
	private final WebRouterConfigurerClassGenerator webRouterConfigurerClassGenerator;
	private final WebRouteDuplicateDetector webRouteDuplicateDectector;
	
	private PluginContext pluginContext;
	
	private TypeMirror webControllerAnnotationType;
	private TypeMirror webRoutesAnnotationType;
	private TypeMirror webRouterConfigurerType;
	
	private boolean enabled = true;
	
	public WebRouterConfigurerCompilerPlugin() {
		this.webRouterConfigurerClassGenerator = new WebRouterConfigurerClassGenerator();
		this.openApiGenrator = new WebRouterConfigurerOpenApiGenerator();
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
		
		// WebRouterConfigurer<WebExchange> 
		TypeMirror webExchangeType = this.pluginContext.getElementUtils().getTypeElement(WebExchange.class.getCanonicalName()).asType();
		TypeElement webRouterConfigurerTypeElement = this.pluginContext.getElementUtils().getTypeElement(WebRouterConfigurer.class.getCanonicalName());
		this.webRouterConfigurerType = this.pluginContext.getTypeUtils().getDeclaredType(webRouterConfigurerTypeElement, webExchangeType);
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

		GenericWebRouterConfigurerInfo webRouterConfigurerInfo = new GenericWebRouterConfigurerInfo(execution.getModuleElement(), webRouterConfigurerQName, webControllers, webRouters);
		
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
			
			if(this.pluginContext.getOptions().isOptionActivated(WebRouterConfigurerCompilerPlugin.OPTION_GENERATE_OPENAPI_DEFINITION, false)) {
				try {
					execution.createResourceFile("META-INF/winter/web/openapi.yml", execution.getElements().stream().toArray(Element[]::new), () -> {
						return webRouterConfigurerInfo.accept(this.openApiGenrator, new WebRouterConfigurerOpenApiGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), this.pluginContext.getDocUtils(), WebRouterConfigurerOpenApiGenerationContext.GenerationMode.ROUTER_SPEC)).toString();
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
				return this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
					.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webControllerAnnotationType))
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
						return new GenericWebProvidedRouterConfigurerInfo(beanElement, new WebRouterConfigurerQualifiedName(bean.getQualifiedName(), this.pluginContext.getTypeUtils().asElement(this.pluginContext.getTypeUtils().erasure(bean.getType())).getSimpleName().toString()), bean, webRoutes);
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
}
