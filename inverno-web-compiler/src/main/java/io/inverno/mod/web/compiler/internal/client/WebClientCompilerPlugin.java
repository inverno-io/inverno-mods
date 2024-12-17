/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.compiler.internal.client;

import io.inverno.core.annotation.Bean;
import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.ModuleBeanInfo;
import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.core.compiler.spi.plugin.PluginExecutionException;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.client.WebClient;
import io.inverno.mod.web.client.WebRouteInterceptor;
import io.inverno.mod.web.client.annotation.WebRoute;
import io.inverno.mod.web.client.annotation.WebSocketRoute;
import io.inverno.mod.web.compiler.internal.AbstractWebCompilerPlugin;
import io.inverno.mod.web.compiler.spi.WebBasicParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientExchangeReturnInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientModuleQualifiedName;
import io.inverno.mod.web.compiler.spi.client.WebClientPartParameterInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientResponseBodyInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteInterceptorConfigurerInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientSocketInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientStubInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientInboundPublisherInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientOutboundPublisherParameterInfo;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * <p>
 * The Inverno Web compiler plugin generates {@link WebClient} beans aggregating the Web client configuration defined in the module.
 * </p>
 *
 * <p>
 * The resulting bean basically configures interceptors defined in Web interceptor configurers beans and generates Web client stubs beans. A global {@link ExchangeContext} type is generated in order
 * to aggregate all context types declared in interceptors configurers or Web client stubs.
 * </p>
 *
 * <p>
 * When the compiled module includes the Web client module, the aggregation is done in a wrapper bean that uses the {@link io.inverno.mod.web.client.WebClient.Boot} to create the root Web client using
 * the generated {@code ExchangeContext}. The resulting {@code WebClient} instance can then be injected in modules beans or sub modules.
 * </p>
 *
 * <p>
 * When the compiled module does not include the Web client module (i.e. the Web client module is not started by the module), the aggregation is done in a mutator socket bean that exposes the expected
 * exchange context. The mutator socket bean defines the {@code ExchangeContext} type required by the module, the compiler can then include it when generating its own {@code ExchangeContext}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebClientCompilerPlugin extends AbstractWebCompilerPlugin {

	private static final String WEB_CLIENT_MODULE_NAME = "io.inverno.mod.web.client";

	private final WebClientClassGenerator webClientClassGenerator;

	private TypeMirror webClientType;
	private TypeMirror webClientAnnotationType;
	private TypeMirror webRouteInterceptorConfigurerType;
	private TypeMirror exchangeContextType;

	private boolean enabled = true;

	/**
	 * <p>
	 * Creates a Web client compiler plugin.
	 * </p>
	 */
	public WebClientCompilerPlugin() {
		this.webClientClassGenerator = new WebClientClassGenerator();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Set.of(WebRoute.class.getCanonicalName(), WebSocketRoute.class.getCanonicalName(), io.inverno.mod.web.client.annotation.WebClient.class.getCanonicalName());
	}

	@Override
	public void init(PluginContext pluginContext) {
		super.init(pluginContext);

		TypeElement webClientElement = this.pluginContext.getElementUtils().getTypeElement(WebClient.class.getCanonicalName());
		if(webClientElement == null) {
			this.enabled = false;
			if(pluginContext.getOptions().isDebug()) {
				System.err.println("Plugin " + WebClientCompilerPlugin.class.getCanonicalName() + " disabled due to missing dependencies");
			}
			return;
		}
		this.webClientType = this.pluginContext.getTypeUtils().erasure(webClientElement.asType());
		this.webClientAnnotationType = this.pluginContext.getElementUtils().getTypeElement(io.inverno.mod.web.client.annotation.WebClient.class.getCanonicalName()).asType();
		this.webRouteInterceptorConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebRouteInterceptor.Configurer.class.getCanonicalName()).asType());
		this.exchangeContextType = this.pluginContext.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType();
	}

	@Override
	public boolean canExecute(ModuleElement moduleElement) {
		return this.enabled && moduleElement.getDirectives().stream()
			.filter(directive -> directive.getKind().equals(ModuleElement.DirectiveKind.REQUIRES))
			.anyMatch(requires -> WEB_CLIENT_MODULE_NAME.equals(((ModuleElement.RequiresDirective)requires).getDependency().getQualifiedName().toString()));
	}

	@Override
	public void execute(PluginExecution execution) throws PluginExecutionException {
		WebClientRouteInfoFactory webRouteFactory = new WebClientRouteInfoFactory(this.pluginContext, execution);
		WebClientModuleQualifiedName webModuleQName = new WebClientModuleQualifiedName(execution.getModuleQualifiedName());

		this.processWebRoutes(execution, webRouteFactory);
		List<? extends WebClientStubInfo> webClientStubs = this.processWebClientStubs(execution, webRouteFactory);

		List<WebClientSocketInfo> webClientSockets = this.processWebClientSockets(execution);
		List<WebClientRouteInterceptorConfigurerInfo> webRouteInterceptorConfigurers = this.processWebRouteInterceptorConfigurers(execution);

		Set<TypeMirror> contextTypes = Stream.of(
				webClientStubs.stream()
					.flatMap(stub -> Arrays.stream(stub.getRoutes()))
					.flatMap(route -> Stream.concat(Stream.of(route.getReturn()), Arrays.stream(route.getParameters()))),
				webClientSockets.stream(),
				webRouteInterceptorConfigurers.stream()
			)
			.flatMap(Function.identity())
			.flatMap(info -> {
				TypeMirror contextType;
				if(info instanceof WebClientSocketInfo) {
					contextType = ((WebClientSocketInfo)info).getContextType();
				}
				else if(info instanceof WebClientRouteInterceptorConfigurerInfo) {
					contextType = ((WebClientRouteInterceptorConfigurerInfo)info).getContextType();
				}
				else if(info instanceof WebExchangeParameterInfo) {
					contextType = ((WebExchangeParameterInfo)info).getContextType();
				}
				else if(info instanceof WebClientExchangeReturnInfo) {
					contextType = ((WebClientExchangeReturnInfo)info).getContextType();
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
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		GenericWebClientModuleInfo webClientModuleInfo = new GenericWebClientModuleInfo(
			execution.getModuleElement(),
			webModuleQName,
			webClientSockets,
			webRouteInterceptorConfigurers,
			webClientStubs,
			contextTypes
		);

		if(!webClientModuleInfo.hasError()) {
			WebClientClassGenerationContext.GenerationMode mode;
			if(Arrays.stream(execution.getModules()).anyMatch(m -> WEB_CLIENT_MODULE_NAME.equals(m.getQualifiedName().getValue()))) {
				mode = WebClientClassGenerationContext.GenerationMode.BOOT_CLIENT_CLASS;
			}
			else {
				mode = WebClientClassGenerationContext.GenerationMode.CLIENT_CLASS;
			}

			try {
				execution.createSourceFile(
					webClientModuleInfo.getQualifiedName().getClassName(),
					execution.getElements().toArray(Element[]::new),
					() -> webClientModuleInfo.accept(
						this.webClientClassGenerator,
						new WebClientClassGenerationContext(this.pluginContext.getTypeUtils(), this.pluginContext.getElementUtils(), mode)
					).toString()
				);
			}
			catch (IOException e) {
				throw new PluginExecutionException("Unable to generate Web client class " + webClientModuleInfo.getQualifiedName().getClassName(), e);
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
	private void processWebRoutes(PluginExecution execution, WebClientRouteInfoFactory webRouteFactory) {
		for(ExecutableElement routeElement : execution.<ExecutableElement>getElementsAnnotatedWith(WebRoute.class)) {
			webRouteFactory.preCompileRoute(routeElement);
		}
		for(ExecutableElement routeElement : execution.<ExecutableElement>getElementsAnnotatedWith(WebSocketRoute.class)) {
			webRouteFactory.preCompileRoute(routeElement);
		}
	}

	/**
	 * <p>
	 * Scans the module for {@link io.inverno.mod.web.client.annotation.WebClient} annotated beans.
	 * </p>
	 *
	 * @param execution       the plugin execution
	 * @param webRouteFactory the Web route factory
	 *
	 * @return a list of Web client stub info
	 */
	private List<? extends WebClientStubInfo> processWebClientStubs(PluginExecution execution, WebClientRouteInfoFactory webRouteFactory) {
		return execution.<TypeElement>getElementsAnnotatedWith(io.inverno.mod.web.client.annotation.WebClient.class).stream()
			.map(clientStubElement -> clientStubElement.getAnnotationMirrors().stream()
				.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webClientAnnotationType))
				.findFirst()
				.map(webClientAnnotation -> {
					ReporterInfo reporter = execution.getReporter(clientStubElement);
					if(clientStubElement.getKind() != ElementKind.INTERFACE) {
						reporter.error("Web client must be an interface");
						return null;
					}

					String name = null;
					String uri = null;
					Bean.Visibility visibility = Bean.Visibility.PUBLIC;
					for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> value : webClientAnnotation.getElementValues().entrySet()) {
						switch (value.getKey().getSimpleName().toString()) {
							case "name":
								name = (String) value.getValue().getValue();
								break;
							case "uri":
								uri = (String) value.getValue().getValue();
								break;
							case "visibility":
								visibility = Bean.Visibility.valueOf(value.getValue().getValue().toString());
								break;
						}
					}

					try {
						ServiceID.of(uri);
					}
					catch(IllegalArgumentException e) {
						reporter.error("Invalid destination URI: " + e.getMessage());
						return null;
					}

					if (name == null) {
						name = clientStubElement.getSimpleName().toString();
						name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
					}
					BeanQualifiedName clientStubQName = new BeanQualifiedName(execution.getModuleQualifiedName(), name);
					List<GenericWebClientRouteInfo> webRoutes = webRouteFactory.compileClientStubRoutes(clientStubElement, clientStubQName);

					if(webRoutes.isEmpty()) {
						reporter.warning("Ignoring Web client which does not define any route");
						return null;
					}

					Set<TypeMirror> typesRegistry = new TreeSet<>((o1, o2) -> {
						if(this.pluginContext.getTypeUtils().isSameType(o1, o2)) {
							return 0;
						}
						return o1.toString().compareTo(o2.toString());
					});
					for(WebClientRouteInfo route : webRoutes) {
						if(route.getReturn() instanceof WebClientResponseBodyInfo) {
							typesRegistry.add(((WebClientResponseBodyInfo) route.getReturn()).getType());
						}
						else if(route.getReturn() instanceof WebSocketClientInboundPublisherInfo) {
							typesRegistry.add(((WebSocketClientInboundPublisherInfo) route.getReturn()).getType());
						}

						for(WebParameterInfo parameter : route.getParameters()) {
							if(parameter instanceof WebBasicParameterInfo || parameter instanceof WebClientPartParameterInfo || parameter instanceof WebRequestBodyParameterInfo || parameter instanceof WebSocketClientOutboundPublisherParameterInfo) {
								typesRegistry.add(parameter.getType());
							}
						}
					}
					return new GenericWebClientStubInfo(clientStubElement, clientStubQName, reporter, (DeclaredType)clientStubElement.asType(), uri, visibility, webRoutes, typesRegistry);
				})
			)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Scans module bean sockets and sub module bean sockets for {@link WebClient} sockets.
	 * </p>
	 *
	 * @param execution the plugin execution
	 *
	 * @return a list of Web client sockets
	 */
	private List<WebClientSocketInfo> processWebClientSockets(PluginExecution execution) {
		// I need to scan any socket where a WebClient is injected to determine the context, can I do that?
		// I also need to get module sockets
		return Stream.concat(
				Arrays.stream(execution.getBeans())
					.filter(bean -> bean instanceof ModuleBeanInfo)
					.flatMap(moduleBean -> Arrays.stream(((ModuleBeanInfo)moduleBean).getSockets())),
				Arrays.stream(execution.getModules())
					.flatMap(module -> Arrays.stream(module.getSockets()))
			)
			.filter(socket -> this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(socket.getType()), this.webClientType))
			.map(webClientSocket -> {
				TypeMirror contextType = this.exchangeContextType;

				List<? extends TypeMirror> typeArguments = ((DeclaredType)webClientSocket.getType()).getTypeArguments();
				if(!typeArguments.isEmpty()) {
					contextType = typeArguments.getFirst();
					if(contextType.getKind() == TypeKind.WILDCARD) {
						TypeMirror extendsBound = ((WildcardType)contextType).getExtendsBound();
						if(extendsBound != null) {
							contextType = extendsBound;
						}
						else {
							contextType = this.exchangeContextType;
						}
					}
					else if(contextType.getKind() == TypeKind.TYPEVAR) {
						contextType = ((TypeVariable)contextType).getUpperBound();
					}
				}

				List<? extends TypeMirror> actualTypes;
				if(contextType.getKind() == TypeKind.INTERSECTION) {
					actualTypes = ((IntersectionType)contextType).getBounds();
				}
				else {
					actualTypes = List.of(contextType);
				}

				if(actualTypes.stream().anyMatch(type -> this.pluginContext.getTypeUtils().asElement(type).getKind() != ElementKind.INTERFACE)) {
					webClientSocket.error("Web exchange context must be an interface");
				}

				return new GenericWebClientSocketInfo(webClientSocket.getQualifiedName(), webClientSocket, contextType);
			})
			.collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Scans the module for {@link io.inverno.mod.web.client.WebRouteInterceptor.Configurer} beans.
	 * </p>
	 *
	 * @param execution the plugin execution
	 *
	 * @return a list of interceptors configurers
	 */
	private List<WebClientRouteInterceptorConfigurerInfo> processWebRouteInterceptorConfigurers(PluginExecution execution) {
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

				return new GenericWebClientRouteInterceptorConfigurerInfo(beanElement, bean.getQualifiedName(), bean, contextType);
			})
			.collect(Collectors.toList());
	}
}
