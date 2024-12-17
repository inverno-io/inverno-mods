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
package io.inverno.mod.web.compiler.internal.server;

import io.inverno.core.annotation.Bean;
import io.inverno.core.compiler.spi.BeanInfo;
import io.inverno.core.compiler.spi.ModuleBeanInfo;
import io.inverno.core.compiler.spi.ModuleInfo;
import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.core.compiler.spi.plugin.PluginExecutionException;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.server.annotation.WebSocketRoute;
import io.inverno.mod.web.compiler.internal.AbstractWebCompilerPlugin;
import io.inverno.mod.web.compiler.spi.WebBasicParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.server.ErrorWebServerRouteInterceptorConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.ErrorWebServerRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerControllerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeContextParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerModuleQualifiedName;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInterceptorConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerInboundPublisherParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerRouteInfo;
import io.inverno.mod.web.server.ErrorWebRouteInterceptor;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.WebRouteInterceptor;
import io.inverno.mod.web.server.WebRouter;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.server.annotation.WebRoutes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

/**
 * <p>
 * The Inverno Web compiler plugin generates {@link WebServer} beans aggregating the Web server configuration defined in the module.
 * </p>
 *
 * <p>
 * The resulting bean basically configures interceptors and routes defined in Web interceptor configurers, Web route configurers and Web server configurers beans and then defines the Web routes
 * defined in Web controller beans. A global {@link ExchangeContext} type is generated in order to aggregate all context types declared in interceptors configurers, routes configurers, server
 * configurers or Web controllers.
 * </p>
 *
 * <p>
 * When the compiled module includes the Web server module, the aggregation is done in a wrapper bean that uses the {@link io.inverno.mod.web.server.WebServer.Boot} to create the root Web server using
 * the generated {@code ExchangeContext}. The resulting {@code WebClient} instance can then be injected in sub modules.
 * </p>
 *
 * <p>
 * When the compiled module does not include the Web server module (i.e. the Web server module is not started by the module), the aggregation is done in a mutator socket bean that exposes the expected
 * exchange context. The mutator socket bean defines the {@code ExchangeContext} type required by the module, the compiler can then include it when generating its own {@code ExchangeContext}.
 * </p>
 *
 * <p>
 * This plugin can also generates an <a href="https://www.openapis.org/">Open API</a> specification for all the Web controllers defined in the module. This can be activated with option
 * {@code inverno.web.generateOpenApiDefinition}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class WebServerCompilerPlugin extends AbstractWebCompilerPlugin {

	private static final String WEB_SERVER_MODULE_NAME = "io.inverno.mod.web.server";
	private static final String OPTION_GENERATE_OPENAPI_DEFINITION = "inverno.web.generateOpenApiDefinition";

	private final WebServerClassGenerator webServerClassGenerator;
	private final WebServerOpenApiGenerator webServerOpenApiGenerator;
	private final WebServerRouteClashDetector webServerRouteClashDetector;
	
	private TypeMirror webControllerAnnotationType;
	private TypeMirror webRoutesAnnotationType;
	private TypeMirror webRouteInterceptorConfigurerType;
	private TypeMirror webRouterConfigurerType;
	private TypeMirror errorWebRouteInterceptorConfigurerType;
	private TypeMirror errorWebRouterConfigurerType;
	private TypeMirror webServerConfigurerType;
	private TypeMirror webServerType;
	private TypeMirror exchangeContextType;

	private boolean enabled = true;

	/**
	 * <p>
	 * Creates a Web server compiler plugin.
	 * </p>
	 */
	public WebServerCompilerPlugin() {
		this.webServerClassGenerator = new WebServerClassGenerator();
		this.webServerOpenApiGenerator = new WebServerOpenApiGenerator();
		this.webServerRouteClashDetector = new WebServerRouteClashDetector();
	}
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(WebRoute.class.getCanonicalName(), WebSocketRoute.class.getCanonicalName(), WebController.class.getCanonicalName());
	}
	
	@Override
	public Set<String> getSupportedOptions() {
		return Set.of(WebServerCompilerPlugin.OPTION_GENERATE_OPENAPI_DEFINITION);
	}

	@Override
	public void init(PluginContext pluginContext) {
		super.init(pluginContext);

		TypeElement webControllerAnnotationElement = this.pluginContext.getElementUtils().getTypeElement(WebController.class.getCanonicalName());
		if(webControllerAnnotationElement == null) {
			this.enabled = false;
			if(pluginContext.getOptions().isDebug()) {
				System.err.println("Plugin " + WebServerCompilerPlugin.class.getCanonicalName() + " disabled due to missing dependencies");
			}
			return;
		}
		this.webControllerAnnotationType = webControllerAnnotationElement.asType();
		this.webRoutesAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebRoutes.class.getCanonicalName()).asType();
		
		this.webRouteInterceptorConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebRouteInterceptor.Configurer.class.getCanonicalName()).asType());
		this.webRouterConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebRouter.Configurer.class.getCanonicalName()).asType());

		this.errorWebRouteInterceptorConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(ErrorWebRouteInterceptor.Configurer.class.getCanonicalName()).asType());
		this.errorWebRouterConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(ErrorWebRouter.Configurer.class.getCanonicalName()).asType());

		this.webServerConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebServer.Configurer.class.getCanonicalName()).asType());

		this.webServerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebServer.class.getCanonicalName()).asType());
		this.exchangeContextType = this.pluginContext.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType();
	}

	@Override
	public boolean canExecute(ModuleElement moduleElement) {
		return this.enabled && moduleElement.getDirectives().stream()
			.filter(directive -> directive.getKind().equals(ModuleElement.DirectiveKind.REQUIRES))
			.anyMatch(requires -> WEB_SERVER_MODULE_NAME.equals(((ModuleElement.RequiresDirective)requires).getDependency().getQualifiedName().toString()));
	}

	@Override
	public void execute(PluginExecution execution) throws PluginExecutionException {
		WebServerRouteInfoFactory webRouteFactory = new WebServerRouteInfoFactory(this.pluginContext, execution);
		WebServerModuleQualifiedName webModuleQName = new WebServerModuleQualifiedName(execution.getModuleQualifiedName());
		
		this.processWebRoutes(execution, webRouteFactory);
		List<? extends WebServerControllerInfo> webControllers = this.processWebControllers(execution, webRouteFactory);
		
		List<? extends WebServerRouteInterceptorConfigurerInfo> webRouteInterceptorConfigurers = this.processWebRouteInterceptorConfigurers(execution);
		List<? extends WebServerRouterConfigurerInfo> webRouterConfigurers = this.processWebRouterConfigurers(execution, webRouteFactory);

		List<? extends ErrorWebServerRouteInterceptorConfigurerInfo> errorWebRouteInterceptorConfigurers = this.processErrorWebRouteInterceptorConfigurers(execution);
		List<? extends ErrorWebServerRouterConfigurerInfo> errorWebRouterConfigurers = this.processErrorWebRouterConfigurers(execution);

		List<? extends WebServerConfigurerInfo> webServerConfigurers = this.processWebServerConfigurers(execution, webRouteFactory);

		Set<TypeMirror> contextTypes = Stream.of(
				webControllers.stream()
					.flatMap(controller -> Arrays.stream(controller.getRoutes()))
					.flatMap(route -> Arrays.stream(route.getParameters())),
				webRouteInterceptorConfigurers.stream(),
				webRouterConfigurers.stream(),
				errorWebRouteInterceptorConfigurers.stream(),
				errorWebRouterConfigurers.stream(),
				webServerConfigurers.stream()
			)
			.flatMap(Function.identity())
			.flatMap(info -> {
				TypeMirror contextType;
				if(info instanceof WebExchangeParameterInfo) {
					contextType = ((WebExchangeParameterInfo)info).getContextType();
				}
				else if(info instanceof WebExchangeContextParameterInfo) {
					contextType = ((WebExchangeContextParameterInfo)info).getContextType();
				}
				else if(info instanceof WebServerRouteInterceptorConfigurerInfo) {
					contextType = ((WebServerRouteInterceptorConfigurerInfo)info).getContextType();
				}
				else if(info instanceof WebServerRouterConfigurerInfo) {
					contextType = ((WebServerRouterConfigurerInfo)info).getContextType();
				}
				else if(info instanceof ErrorWebServerRouteInterceptorConfigurerInfo) {
					contextType = ((ErrorWebServerRouteInterceptorConfigurerInfo)info).getContextType();
				}
				else if(info instanceof ErrorWebServerRouterConfigurerInfo) {
					contextType = ((ErrorWebServerRouterConfigurerInfo)info).getContextType();
				}
				else if(info instanceof WebServerConfigurerInfo) {
					contextType = ((WebServerConfigurerInfo)info).getContextType();
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
						// this means we could have compiled errors if the user specified a non-public context type explicitly
						.flatMap(type -> this.getAllSuperTypes(contextType).stream().filter(superContextType -> ((DeclaredType)superContextType).asElement().getModifiers().contains(Modifier.PUBLIC)))
				)
				.map(type -> new Object[] {type, info});
			})
			.collect(Collectors.groupingBy(typeAndInfo -> this.pluginContext.getTypeUtils().erasure((TypeMirror)typeAndInfo[0])))
			// We must retain one type per map entry
			.values().stream().map(typeAndInfos -> {
				// We must retain the type that can be assigned to all others, if it doesn't exist we shall report an error: we have defined incompatible context types
				// n^2 complexity... Can we do better?
				Set<ReporterInfo> infos = new HashSet<>();
				main:
				for (Object[] typeAndInfo1 : typeAndInfos) {
					TypeMirror t1 = (TypeMirror) typeAndInfo1[0];
					ReporterInfo info = (ReporterInfo) typeAndInfo1[1];
					infos.add(info);
					for (Object[] typeAndInfo2 : typeAndInfos) {
						TypeMirror t2 = (TypeMirror) typeAndInfo2[0];
						if (!this.pluginContext.getTypeUtils().isAssignable(t1, t2)) {
							continue main;
						}
					}
					// t1 is assignable to all other types
					return this.unwildContextType(info, (DeclaredType) t1);
				}

				// report the error and ignore context type: compilation will fail
				infos.forEach(reporter -> reporter.error("Inconsistent context types"));
				return null;
			})
			.collect(Collectors.toSet());
		
		// Filter types to remove duplicates: same type but with different parameters
		// SecurityContext<Identity, RoleBasedAccessController> can be cast to SecurityContext<? extends Identity, ? extends RoleBasedAccessController>
		// => we need to remove wildcard because it simply doesn't compile: <? extends A> => <A>

		// Report route clash
		List<WebServerRouteInfo> routes = new ArrayList<>();
		webControllers.stream().flatMap(controller -> Arrays.stream(controller.getRoutes())).forEach(routes::add);
		webRouterConfigurers.stream().flatMap(configurer -> Arrays.stream(configurer.getRoutes())).forEach(routes::add);
		webServerConfigurers.stream().flatMap(configurer -> Arrays.stream(configurer.getRoutes())).forEach(routes::add);

		for(Map.Entry<WebServerRouteInfo, Set<WebServerRouteInfo>> e : this.webServerRouteClashDetector.findDuplicates(routes).entrySet()) {
			e.getKey().error("Route " + e.getKey().getQualifiedName() + " is clashing with route(s):\n" + e.getValue().stream().map(route -> "- " + route.getQualifiedName()).collect(Collectors.joining("\n")));
		}

		Set<TypeMirror> typesRegistry = new TreeSet<>((o1, o2) -> {
			if(this.pluginContext.getTypeUtils().isSameType(o1, o2)) {
				return 0;
			}
			return o1.toString().compareTo(o2.toString());
		});
		webControllers.stream().flatMap(controller -> Arrays.stream(controller.getTypesRegistry())).forEach(typesRegistry::add);

		GenericWebServerModuleInfo webServerModuleInfo = new GenericWebServerModuleInfo(
			execution.getModuleElement(),
			webModuleQName,
			webRouteInterceptorConfigurers,
			webRouterConfigurers,
			errorWebRouteInterceptorConfigurers,
			errorWebRouterConfigurers,
			webServerConfigurers,
			webControllers,
			contextTypes,
			typesRegistry
		);

		// TODO check that below comment is not an issue => we must always create at least the boot server class
		// test: create a module that includes the Web server module but doesn't configure anything => the boot server class must be generated
		if(!webServerModuleInfo.hasError() /*&& (webServerModuleInfo.getControllers().length > 0 || webServerModuleInfo.getRouterConfigurers().length > 0 || webServerModuleInfo.getServerConfigurers().length > 0)*/) {
			WebServerClassGenerationContext.GenerationMode mode;
			if(Arrays.stream(execution.getModules()).anyMatch(m -> m.getQualifiedName().getValue().equals(WEB_SERVER_MODULE_NAME))) {
				mode = WebServerClassGenerationContext.GenerationMode.BOOT_SERVER_CLASS;
			}
			else {
				mode = WebServerClassGenerationContext.GenerationMode.SERVER_CLASS;
				if(webServerModuleInfo.getControllers().length == 0 && webServerModuleInfo.getRouterConfigurers().length == 0 && webServerModuleInfo.getServerConfigurers().length == 0) {
					// the module doesn't configure the server so we don't need to create the server class
					return;
				}
			}

			try {
				execution.createSourceFile(
					webServerModuleInfo.getQualifiedName().getClassName(),
					execution.getElements().toArray(Element[]::new),
					() -> webServerModuleInfo.accept(
							this.webServerClassGenerator,
							new WebServerClassGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), mode)
						).toString()
				);
			}
			catch (IOException e) {
				throw new PluginExecutionException("Unable to generate Web server class " + webServerModuleInfo.getQualifiedName().getClassName(), e);
			}

			if(webServerModuleInfo.getControllers().length > 0 && this.pluginContext.getOptions().isOptionActivated(WebServerCompilerPlugin.OPTION_GENERATE_OPENAPI_DEFINITION, false)) {
				try {
					execution.createResourceFile(
						"META-INF/inverno/web/server/" + webModuleQName.getModuleQName().toString() + "/openapi.yml",
						execution.getElements().toArray(Element[]::new),
						() -> webServerModuleInfo.accept(
								this.webServerOpenApiGenerator,
								new WebServerOpenApiGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), this.pluginContext.getDocUtils(), WebServerOpenApiGenerationContext.GenerationMode.ROUTER_SPEC)
							).toString()
					);
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
	 * @param execution       the plugin execution
	 * @param webRouteFactory the Web route factory
	 */
	private void processWebRoutes(PluginExecution execution, WebServerRouteInfoFactory webRouteFactory) {
		for(ExecutableElement routeElement : execution.<ExecutableElement>getElementsAnnotatedWith(WebRoute.class)) {
			webRouteFactory.preCompileRoute(routeElement);
		}
		for(ExecutableElement routeElement : execution.<ExecutableElement>getElementsAnnotatedWith(WebSocketRoute.class)) {
			webRouteFactory.preCompileRoute(routeElement);
		}
	}
	
	/**
	 * <p>
	 * Scans the module for {@link WebController} annotated beans.
	 * </p>
	 * 
	 * @param execution       the plugin execution
	 * @param webRouteFactory the Web route factory
	 *
	 * @return a list of Web controller info
	 */
	private List<? extends WebServerControllerInfo> processWebControllers(PluginExecution execution, WebServerRouteInfoFactory webRouteFactory) {
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
							if(value.getKey().getSimpleName().toString().equals("path")) {
								controllerRootPath = (String)value.getValue().getValue();
							}
						}
						
						List<GenericWebServerRouteInfo> webRoutes = webRouteFactory.compileControllerRoutes(bean);
						
						if(webRoutes.isEmpty()) {
							bean.warning("Ignoring Web controller which does not define any route");
							return null;
						}

						Set<TypeMirror> typesRegistry = new TreeSet<>((o1, o2) -> {
							if(this.pluginContext.getTypeUtils().isSameType(o1, o2)) {
								return 0;
							}
							return o1.toString().compareTo(o2.toString());
						});
						for(WebServerRouteInfo route : webRoutes) {
							if(route.getResponseBody() != null) {
								typesRegistry.add(route.getResponseBody().getType());
							}
							else if(route instanceof WebSocketServerRouteInfo) {
								((WebSocketServerRouteInfo) route).getOutboundPublisher().ifPresent(outboundPublisherInfo -> typesRegistry.add(outboundPublisherInfo.getType()));
							}

							for(WebParameterInfo parameter : route.getParameters()) {
								if(parameter instanceof WebBasicParameterInfo || parameter instanceof WebRequestBodyParameterInfo || parameter instanceof WebSocketServerInboundPublisherParameterInfo) {
									typesRegistry.add(parameter.getType());
								}
							}
						}
						return new GenericWebServerControllerInfo(beanElement, bean.getQualifiedName(), bean, (DeclaredType)bean.getType(), controllerRootPath, webRoutes, typesRegistry);
					});
			})
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Scans the module for {@link WebRouteInterceptor.Configurer} beans.
	 * </p>
	 * 
	 * @param execution the plugin execution
	 *
	 * @return a list of interceptors configurers
	 */
	private List<? extends WebServerRouteInterceptorConfigurerInfo> processWebRouteInterceptorConfigurers(PluginExecution execution) {
		// Get Web interceptors configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.webRouteInterceptorConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include al Web interceptors configurer
				// - raise a warning if it is defined in the module and not private
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Web interceptors configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				List<? extends TypeMirror> webRouteInterceptorConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.webRouteInterceptorConfigurerType)).getTypeArguments();
				TypeMirror contextType = !webRouteInterceptorConfigurerTypeArguments.isEmpty() ? webRouteInterceptorConfigurerTypeArguments.getFirst() : this.exchangeContextType;
				
				return new GenericWebServerRouteInterceptorConfigurerInfo(beanElement, bean.getQualifiedName(), bean, contextType);
			})
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Scans the module for {@link WebRouter.Configurer} beans.
	 * </p>
	 * 
	 * @param execution       the plugin execution
	 * @param webRouteFactory the Web route factory
	 *
	 * @return a list of routes configurers
	 */
	private List<? extends WebServerRouterConfigurerInfo> processWebRouterConfigurers(PluginExecution execution, WebServerRouteInfoFactory webRouteFactory) {
		// Get Web interceptors configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.webRouterConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include al Web router configurer
				// - raise a warning if it is defined in the module and not private
				// - raise a warning if it is defined without @WebRoutes
				Optional<List<ProvidedWebServerRouteInfo>> webRoutes = this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
					.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRoutesAnnotationType))
					.findFirst()
					.map(annotation -> webRouteFactory.compileRoutes(bean, annotation));
				
				if(webRoutes.isEmpty()) {
					bean.warning("Unable to determine route clashes since no route is specified in @WebRoutes");
				}
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Web routes configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				List<? extends TypeMirror> webRouterConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.webRouterConfigurerType)).getTypeArguments();
				TypeMirror contextType = !webRouterConfigurerTypeArguments.isEmpty() ? webRouterConfigurerTypeArguments.getFirst() : this.exchangeContextType;
				
				return new GenericWebServerRouterConfigurerInfo(beanElement, bean.getQualifiedName(), bean, webRoutes.orElse(List.of()), contextType);
			})
			.collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Scans the module for {@link ErrorWebRouteInterceptor.Configurer} beans.
	 * </p>
	 * 
	 * @param execution the plugin execution
	 *
	 * @return a list of error interceptors configurers
	 */
	private List<? extends ErrorWebServerRouteInterceptorConfigurerInfo> processErrorWebRouteInterceptorConfigurers(PluginExecution execution) {
		// Get Web interceptors configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.errorWebRouteInterceptorConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include all error Web interceptors configurer
				// - raise a warning if it is defined in the module and not private
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Error Web interceptors configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				
				List<? extends TypeMirror> errorWebRouteInterceptorConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.errorWebRouteInterceptorConfigurerType)).getTypeArguments();
				TypeMirror contextType = !errorWebRouteInterceptorConfigurerTypeArguments.isEmpty() ? errorWebRouteInterceptorConfigurerTypeArguments.getFirst() : this.exchangeContextType;
				
				return new GenericErrorWebServerRouteInterceptorConfigurerInfo(beanElement, bean.getQualifiedName(), bean, contextType);
			})
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Scans the module for {@link ErrorWebRouter.Configurer} beans.
	 * </p>
	 * 
	 * @param execution the plugin execution
	 *
	 * @return a list of error routes configurers
	 */
	private List<? extends ErrorWebServerRouterConfigurerInfo> processErrorWebRouterConfigurers(PluginExecution execution) {
		// Get Web interceptors configurers
		return Arrays.stream(execution.getBeans())
			.filter(bean -> this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.errorWebRouterConfigurerType))
			.map(bean -> {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
				
				// We want to include all error Web routes configurer
				// - raise a warning if it is defined in the module and not private
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Error Web routes configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}
				
				List<? extends TypeMirror> errorWebRouterConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.errorWebRouterConfigurerType)).getTypeArguments();
				TypeMirror contextType = !errorWebRouterConfigurerTypeArguments.isEmpty() ? errorWebRouterConfigurerTypeArguments.getFirst() : this.exchangeContextType;
				
				return new GenericErrorWebServerRouterConfigurerInfo(beanElement, bean.getQualifiedName(), bean, contextType);
			})
			.collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Scans the module for {@link WebServer.Configurer} beans.
	 * </p>
	 *
	 * @param execution the plugin execution
	 *
	 * @return a list of Web server configurers
	 */
	private List<? extends WebServerConfigurerInfo> processWebServerConfigurers(PluginExecution execution, WebServerRouteInfoFactory webRouteFactory) {

		List<WebServerConfigurerInfo> result = new ArrayList<>();

		for(BeanInfo bean : execution.getBeans()) {
			if(this.pluginContext.getTypeUtils().isAssignable(bean.getType(), this.webServerConfigurerType)) {
				TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());

				// We want to include al Web router configurer
				// - raise a warning if it is defined in the module and not private
				// - raise a warning if it is defined without @WebRoutes
				Optional<List<ProvidedWebServerRouteInfo>> webRoutes = this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
					.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRoutesAnnotationType))
					.findFirst()
					.map(annotation -> webRouteFactory.compileRoutes(bean, annotation));

				if(webRoutes.isEmpty()) {
					bean.warning("Can't determine route clashes since no route is specified in @WebRoutes on " + bean.getQualifiedName());
				}
				if(bean.getQualifiedName().getModuleQName().equals(execution.getModuleQualifiedName()) && bean instanceof ModuleBeanInfo && ((ModuleBeanInfo)bean).getVisibility().equals(Bean.Visibility.PUBLIC)) {
					bean.warning("Web Router configurer bean should be declared with Visibility.PRIVATE to prevent side effects when the module is composed");
				}

				List<? extends TypeMirror> webRouterConfigurerTypeArguments = ((DeclaredType)this.findSuperType(bean.getType(), this.webServerConfigurerType)).getTypeArguments();
				TypeMirror contextType = !webRouterConfigurerTypeArguments.isEmpty() ? webRouterConfigurerTypeArguments.getFirst() : this.exchangeContextType;

				result.add(new GenericWebServerConfigurerInfo(beanElement, bean.getQualifiedName(), bean, webRoutes.orElse(List.of()), contextType));
			}
		}

		for(ModuleInfo module : execution.getModules()) {
			Arrays.stream(module.getSockets())
				.filter(socketBean -> this.pluginContext.getTypeUtils().isSubtype(this.pluginContext.getTypeUtils().erasure(socketBean.getType()), this.webServerType))
				.forEach(socketBean -> {
					// extract context type
					TypeMirror contextType = ((DeclaredType) socketBean.getType()).getTypeArguments().getFirst();
					if(contextType.getKind() == TypeKind.WILDCARD) {
						contextType = ((WildcardType)contextType).getExtendsBound();
					}

					Optional<List<ProvidedWebServerRouteInfo>> webRoutes = Optional.of(this.pluginContext.getTypeUtils().asElement(contextType).getEnclosingElement())
						.filter(webServerSocketElement -> webServerSocketElement.getKind() == ElementKind.CLASS)
						.flatMap(webServerSocketElement -> this.pluginContext.getElementUtils().getAllAnnotationMirrors(webServerSocketElement).stream()
							.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRoutesAnnotationType))
							.findFirst()
						)
						.map(annotation -> webRouteFactory.compileRoutes(socketBean, annotation));

					if(webRoutes.isEmpty()) {
						socketBean.warning("Can't determine route clashes since no route is specified in @WebRoutes on " + socketBean.getQualifiedName());
					}
					result.add(new GenericWebServerConfigurerInfo(socketBean.getSocketElement().orElse(null), socketBean.getQualifiedName(), socketBean, webRoutes.orElse(List.of()), contextType));
				});
		}
		return result;
	}
}
