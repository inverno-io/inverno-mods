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
package io.winterframework.mod.web.router.internal.compiler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import io.winterframework.core.compiler.spi.plugin.CompilerPlugin;
import io.winterframework.core.compiler.spi.plugin.PluginContext;
import io.winterframework.core.compiler.spi.plugin.PluginExecution;
import io.winterframework.core.compiler.spi.plugin.PluginExecutionException;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.router.annotation.WebController;
import io.winterframework.mod.web.router.annotation.WebRoute;
import io.winterframework.mod.web.router.annotation.WebRoutes;
import io.winterframework.mod.web.router.internal.compiler.WebRouterConfigurerGenerationContext.GenerationMode;
import io.winterframework.mod.web.router.internal.compiler.spi.WebControllerInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebProvidedRouterConfigurerInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebRouteInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebRouteQualifiedName;
import io.winterframework.mod.web.router.internal.compiler.spi.WebRouterConfigurerQualifiedName;

/**
 * @author jkuhn
 *
 */
public class WebRouterConfigurerCompilerPlugin implements CompilerPlugin {

	private final WebRouterConfigurerClassGenerator webRouterConfigurerClassGenerator;
	private final WebRouteDuplicateDetector webRouteDuplicateDectector;
	
	private PluginContext pluginContext;
	
	private TypeMirror webControllerAnnotationType;
	private TypeMirror webRoutesAnnotationType;
	
	private TypeMirror webRouterConfigurerType;
	
	public WebRouterConfigurerCompilerPlugin() {
		this.webRouterConfigurerClassGenerator = new WebRouterConfigurerClassGenerator();
		this.webRouteDuplicateDectector = new WebRouteDuplicateDetector();
	}
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(WebRoute.class.getCanonicalName(), WebController.class.getCanonicalName());
	}

	@Override
	public void init(PluginContext pluginContext) {
		this.pluginContext = pluginContext;
		
		this.webControllerAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebController.class.getCanonicalName()).asType();
		this.webRoutesAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebRoutes.class.getCanonicalName()).asType();
		
		// WebRouterConfigurer<WebExchange> 
		TypeMirror webExchangeType = this.pluginContext.getElementUtils().getTypeElement("io.winterframework.mod.web.router.WebExchange").asType();
		TypeElement webRouterConfigurerTypeElement = this.pluginContext.getElementUtils().getTypeElement("io.winterframework.mod.web.router.WebRouterConfigurer");
		this.webRouterConfigurerType = this.pluginContext.getTypeUtils().getDeclaredType(webRouterConfigurerTypeElement, webExchangeType);
	}

	@Override
	public void execute(PluginExecution execution) throws PluginExecutionException {
		WebRouteInfoFactory webRouteFactory = new WebRouteInfoFactory(this.pluginContext, execution);
		WebRouterConfigurerQualifiedName webRouterConfigurerQName = new WebRouterConfigurerQualifiedName(execution.getModule());
		
		this.processWebRoutes(execution, webRouteFactory);
		List<? extends WebControllerInfo> webControllers = this.processWebControllers(execution, webRouteFactory);
		List<? extends WebProvidedRouterConfigurerInfo> webRouters = this.processWebRouters(execution);
		
		GenericWebRouterConfigurerInfo webRouterConfigurerInfo = new GenericWebRouterConfigurerInfo(webRouterConfigurerQName, webControllers, webRouters);
		
		for(Map.Entry<WebRouteInfo, Set<WebRouteInfo>> e : this.webRouteDuplicateDectector.findDuplicates(Stream.concat(webControllers.stream().flatMap(controller -> Arrays.stream(controller.getRoutes())), webRouters.stream().flatMap(router -> Arrays.stream(router.getRoutes()))).collect(Collectors.toList())).entrySet()) {
			e.getKey().error("Route " + e.getKey().getQualifiedName() + " is conflicting with route(s):\n" + e.getValue().stream().map(route -> "- " + route.getQualifiedName()).collect(Collectors.joining("\n")));
		}
			
		if(!webRouterConfigurerInfo.hasError() && (webRouterConfigurerInfo.getControllers().length > 0 || webRouterConfigurerInfo.getRouters().length > 0)) {
			try {
				execution.createSourceFile(webRouterConfigurerInfo.getQualifiedName().getClassName(),  execution.getElements().stream().toArray(Element[]::new), () -> {
					return webRouterConfigurerInfo.accept(this.webRouterConfigurerClassGenerator, new WebRouterConfigurerGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), GenerationMode.CONFIGURER_CLASS)).toString();
				});
			} 
			catch (IOException e) {
				throw new PluginExecutionException("Unable to generate web router configurer class " + webRouterConfigurerInfo.getQualifiedName().getClassName(), e);
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
						annotationLoop:
						for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webControllerAnnotation).entrySet()) {
							switch(value.getKey().getSimpleName().toString()) {
								case "value" : controllerRootPath = (String)value.getValue().getValue();
									break annotationLoop;
								case "path" : controllerRootPath = (String)value.getValue().getValue();
									break;
							}
						}
						
						List<? extends WebRouteInfo> webRoutes = webRouteFactory.compileControllerRoutes(bean);
						
						if(webRoutes.isEmpty()) {
							bean.warning("Ignoring web controller which does not define any route");
							return null;
						}
						return new GenericWebControllerInfo(bean.getQualifiedName(), bean, (DeclaredType)bean.getType(), controllerRootPath, webRoutes);
					});
			})
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}
	
	@SuppressWarnings("unchecked")
	private List<? extends WebProvidedRouterConfigurerInfo> processWebRouters(PluginExecution execution) {
		// Get component routers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.webRouterConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				Optional<? extends WebProvidedRouterConfigurerInfo> providedRouterConfigurerInfo = this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
					.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRoutesAnnotationType))
					.findFirst()
					.map(webRouterConfigurerAnnotation -> {
						final AtomicInteger routeIndex = new AtomicInteger();
						List<WebRouteInfo> webRoutes = ((Collection<? extends AnnotationValue>)webRouterConfigurerAnnotation.getElementValues().values().iterator().next().getValue()).stream()
							.map(value -> (AnnotationMirror)value.getValue())
							.map(webRouteAnnotation -> {
								Set<String> paths = new HashSet<>();
								boolean matchTrailingSlash = false;
								Set<Method> methods = new HashSet<>();
								Set<String> consumes = new HashSet<>();
								Set<String> produces = new HashSet<>();
								Set<String> languages = new HashSet<>();
								annotationLoop:
								for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webRouteAnnotation).entrySet()) {
									switch(value.getKey().getSimpleName().toString()) {
										case "value" : paths.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
											break annotationLoop;
										case "path" : paths.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
											break;
										case "matchTrailingSlash": matchTrailingSlash = (boolean)value.getValue().getValue();
											break;
										case "method" : methods.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> Method.valueOf(v.getValue().toString())).collect(Collectors.toSet()));
											break;
										case "consumes" : consumes.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
											break;
										case "produces" : produces.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
											break;
										case "language" : languages.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
											break;
									}
								}
								WebRouteQualifiedName routeQName = new WebRouteQualifiedName(bean.getQualifiedName(), "route_" + routeIndex.getAndIncrement());
								return new ProvidedWebRouteInfo(routeQName, bean, paths, matchTrailingSlash, methods, consumes, produces, languages);
							})
							.collect(Collectors.toList());
						
						if(webRoutes.isEmpty()) {
							return null;
						}
						return new GenericWebProvidedRouterConfigurerInfo(new WebRouterConfigurerQualifiedName(bean.getQualifiedName(), this.pluginContext.getTypeUtils().asElement(this.pluginContext.getTypeUtils().erasure(bean.getType())).getSimpleName().toString()), bean, webRoutes);
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
