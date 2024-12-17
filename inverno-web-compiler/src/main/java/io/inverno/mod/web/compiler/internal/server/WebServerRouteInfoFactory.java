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

import io.inverno.core.compiler.spi.BeanInfo;
import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.server.annotation.WebSocketRoute;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.internal.NoOpReporterInfo;
import io.inverno.mod.web.compiler.internal.TypeHierarchyExtractor;
import io.inverno.mod.web.compiler.spi.ResponseBodyKind;
import io.inverno.mod.web.compiler.spi.ResponseBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebFormParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSocketExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerInboundParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerInboundPublisherParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketOutboundParameterInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerResponseBodyInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteQualifiedName;
import io.inverno.mod.web.compiler.spi.server.WebSseEventFactoryParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebSseEventFactoryParameterInfo.SseEventFactoryKind;
import io.inverno.mod.web.server.WebResponseBody;
import io.inverno.mod.web.server.annotation.WebController;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Factory used to create Web server route info.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebServerRouteInfo
 */
public class WebServerRouteInfoFactory {

	private final PluginContext pluginContext;
	private final PluginExecution pluginExecution;
	private final WebServerRouteParameterInfoFactory webRouteParameterFactory;
	private final WebSocketServerRouteParameterInfoFactory webSocketRouteParameterFactory;
	private final Map<ExecutableElement, GenericWebServerRouteInfo> routes;
	private final TypeHierarchyExtractor typeHierarchyExtractor;
	
	/* Web annotations */
	private final TypeMirror webControllerAnnotationType;
	private final TypeMirror webRouteAnnotationType;
	private final TypeMirror webSocketRouteAnnotationType;
	
	/* Types */
	private final TypeMirror publisherType;
	private final TypeMirror monoType;
	private final TypeMirror fluxType;
	private final TypeMirror voidType;
	private final TypeMirror byteBufType;
	private final TypeMirror charSequenceType;
	private final TypeMirror sseRawEventType;
	private final TypeMirror sseEncoderEventType;
	private final TypeMirror resourceType;
	
	/**
	 * <p>
	 * Creates a Web server route info factory.
	 * </p>
	 * 
	 * @param pluginContext   the Web compiler plugin context
	 * @param pluginExecution the Web compiler plugin execution
	 */
	public WebServerRouteInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		this.pluginContext = Objects.requireNonNull(pluginContext);
		this.pluginExecution = Objects.requireNonNull(pluginExecution);
		this.webRouteParameterFactory = new WebServerRouteParameterInfoFactory(this.pluginContext , this.pluginExecution);
		this.webSocketRouteParameterFactory = new WebSocketServerRouteParameterInfoFactory(this.pluginContext, this.pluginExecution);
		this.routes = new HashMap<>();
		this.typeHierarchyExtractor = new TypeHierarchyExtractor(this.pluginContext.getTypeUtils());
		
		this.webControllerAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebController.class.getCanonicalName()).asType();
		this.webRouteAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebRoute.class.getCanonicalName()).asType();
		this.webSocketRouteAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebSocketRoute.class.getCanonicalName()).asType();
		
		this.publisherType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Publisher.class.getCanonicalName()).asType());
		this.monoType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Mono.class.getCanonicalName()).asType());
		this.fluxType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Flux.class.getCanonicalName()).asType());
		this.voidType = this.pluginContext.getElementUtils().getTypeElement(Void.class.getCanonicalName()).asType();
		this.byteBufType = this.pluginContext.getElementUtils().getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.charSequenceType = this.pluginContext.getElementUtils().getTypeElement(CharSequence.class.getCanonicalName()).asType();
		this.sseRawEventType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(ResponseBody.Sse.Event.class.getCanonicalName()).asType());
		this.sseEncoderEventType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebResponseBody.SseEncoder.Event.class.getCanonicalName()).asType());
		this.resourceType = this.pluginContext.getElementUtils().getTypeElement(Resource.class.getCanonicalName()).asType();
	}

	/**
	 * <p>
	 * Pre-compiles the specified executable element and pre-populates the internal Web server routes map.
	 * </p>
	 *
	 * @param routeElement the executable element to compile
	 */
	public void preCompileRoute(ExecutableElement routeElement) {
		AnnotationMirror webRouteAnnotation = null;
		AnnotationMirror webSocketRouteAnnotation = null;
		for(AnnotationMirror annotation : this.pluginContext.getElementUtils().getAllAnnotationMirrors(routeElement)) {
			if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRouteAnnotationType)) {
				webRouteAnnotation = annotation;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webSocketRouteAnnotationType)) {
				webSocketRouteAnnotation = annotation;
			}
		}

		if(webRouteAnnotation != null) {
			if(webSocketRouteAnnotation != null) {
				if(!this.routes.containsKey(routeElement)) {
					// We don't want to report error multiple times on the same element
					this.pluginExecution.getReporter(routeElement, webSocketRouteAnnotation).error("Can't specify both WebRoute and WebSocketRoute");
					this.routes.put(routeElement, null);
				}
			}
			else if(!this.routes.containsKey(routeElement)) {
				this.createRoute(webRouteAnnotation, null, routeElement, routeElement, (ExecutableType)routeElement.asType(), null);
			}
		}
		else if(webSocketRouteAnnotation != null) {
			if(!this.routes.containsKey(routeElement)) {
				this.createWebSocketRoute(webSocketRouteAnnotation, null, routeElement, routeElement, (ExecutableType)routeElement.asType(), null);
			}
		}
	}

	/**
	 * <p>
	 * Compiles routes from a configurer bean annotated with {@link io.inverno.mod.web.server.annotation.WebRoutes @WebRoutes}.
	 * </p>
	 *
	 * @param bean                the configurer bean
	 * @param webRoutesAnnotation the @WebRoutes annotation
	 *
	 * @return a list of Web routes
	 */
	@SuppressWarnings("unchecked")
	public List<ProvidedWebServerRouteInfo> compileRoutes(BeanInfo bean, AnnotationMirror webRoutesAnnotation) {
		if(webRoutesAnnotation == null || webRoutesAnnotation.getElementValues().isEmpty()) {
			return List.of();
		}

		final AtomicInteger routeIndex = new AtomicInteger();
		return this.pluginContext.getElementUtils().getElementValuesWithDefaults(webRoutesAnnotation).entrySet().stream()
			.flatMap(webRoutesEntry -> {
				switch(webRoutesEntry.getKey().getSimpleName().toString()) {
					case "value": {
						return ((Collection<? extends AnnotationValue>)webRoutesEntry.getValue().getValue()).stream()
							.map(value -> (AnnotationMirror)value.getValue())
							.map(webRouteAnnotation -> {
								Set<String> paths = new HashSet<>();
								boolean matchTrailingSlash = false;
								Set<Method> methods = new HashSet<>();
								Set<String> consumes = new HashSet<>();
								Set<String> produces = new HashSet<>();
								Set<String> languages = new HashSet<>();
								for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webRouteAnnotation).entrySet()) {
									switch(value.getKey().getSimpleName().toString()) {
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
								WebServerRouteQualifiedName routeQName = new WebServerRouteQualifiedName(bean.getQualifiedName(), "route_" + routeIndex.getAndIncrement());
								return new ProvidedWebServerRouteInfo(routeQName, bean, paths, matchTrailingSlash, methods, consumes, produces, languages);
							});
					}
					case "webSockets": {
						return ((Collection<? extends AnnotationValue>)webRoutesEntry.getValue().getValue()).stream()
							.map(value -> (AnnotationMirror)value.getValue())
							.map(webSocketRouteAnnotation -> {
								Set<String> paths = new HashSet<>();
								boolean matchTrailingSlash = false;
								Set<String> languages = new HashSet<>();
								Set<String> subprotocols = new HashSet<>();
								WebSocketMessage.Kind messageType = WebSocketMessage.Kind.TEXT;
								for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webSocketRouteAnnotation).entrySet()) {
									switch(value.getKey().getSimpleName().toString()) {
										case "path" : paths.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
											break;
										case "matchTrailingSlash": matchTrailingSlash = (boolean)value.getValue().getValue();
											break;
										case "language" : languages.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
											break;
										case "subprotocol" : subprotocols.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
											break;
										case "messageType" : messageType = WebSocketMessage.Kind.valueOf(((AnnotationValue)value.getValue().getValue()).getValue().toString());
											break;
									}
								}
								WebServerRouteQualifiedName routeQName = new WebServerRouteQualifiedName(bean.getQualifiedName(), "route_" + routeIndex.getAndIncrement());
								return new ProvidedWebSocketServerRouteInfo(routeQName, bean, paths, matchTrailingSlash, languages, subprotocols, messageType);
							});
					}
				}

				return null;
			})
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Compiles the specified Web controller bean and extracts the Web routes it defines.
	 * </p>
	 *
	 * @param bean a Web controller bean
	 *
	 * @return the list of Web routes defined in the Web controller
	 *
	 * @throws IllegalArgumentException if the specified bean is not a Web controller
	 */
	public List<GenericWebServerRouteInfo> compileControllerRoutes(BeanInfo bean) throws IllegalArgumentException {
		TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
		
		this.typeHierarchyExtractor.extractTypeHierarchy(beanElement).stream()
			.map(element -> this.pluginContext.getElementUtils().getAllAnnotationMirrors(element).stream()
				.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webControllerAnnotationType))
				.findFirst()
			)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("bean " + bean + " is not annotated with " + WebController.class));
		
		Map<String, List<GenericWebServerRouteInfo>> routesByMethodName = new HashMap<>();
		for(ExecutableElement routeElement : ElementFilter.methodsIn(this.pluginContext.getElementUtils().getAllMembers(beanElement))) {
			this.getWebRouteAnnotatedElement(routeElement, beanElement)
				.ifPresent(routeAnnotatedElement -> {
					AnnotationMirror webRouteAnnotation = null;
					AnnotationMirror webSocketRouteAnnotation = null;
					for(AnnotationMirror annotation : this.pluginContext.getElementUtils().getAllAnnotationMirrors(routeAnnotatedElement)) {
						if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRouteAnnotationType)) {
							webRouteAnnotation = annotation;
						}
						else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webSocketRouteAnnotationType)) {
							webSocketRouteAnnotation = annotation;
						}
					}
					
					ExecutableType routeElementType = (ExecutableType)this.pluginContext.getTypeUtils().asMemberOf((DeclaredType)bean.getType(), routeElement);
					
					// We have to create the route no matter what since annotations and/or types
					// might be different from what the compiler found when analyzing WebRoute
					// annotated elements.
					// eg. we can imagine an interface A external to the module exposing a WebRoute
					// annotated method, and a class B in the module overriding that particular
					// interface and implementing that particular method overriding the WebRoute
					// annotation. If a class C extends B and implements A, then we'll found the
					// route coming from B in the list of routes but we'll have to consider WebRoute
					// annotation from A and therefore we have to recreate the route for that
					// particular configuration but we must not report errors already reported on B.
					// That being said, overriding WebRoute annotation is a bad idea, it would be
					// interesting to see how JAX-RS and Spring handle this.
					
					String routeMethodName = routeElement.getSimpleName().toString();
					List<GenericWebServerRouteInfo> currentRoutes = routesByMethodName.computeIfAbsent(routeMethodName, k -> new LinkedList<>());

					// getWebRouteAnnotatedElement() makes sure we have one of webRouteAnnotation or webSocketRouteAnnotation
					if(webRouteAnnotation != null) {
						currentRoutes.add(this.createRoute(webRouteAnnotation, bean.getQualifiedName(), routeElement, routeAnnotatedElement, routeElementType, currentRoutes.isEmpty() ? null : currentRoutes.size()));
					}
					else if(webSocketRouteAnnotation != null) {
						currentRoutes.add(this.createWebSocketRoute(webSocketRouteAnnotation, bean.getQualifiedName(), routeElement, routeAnnotatedElement, routeElementType, currentRoutes.isEmpty() ? null : currentRoutes.size()));
					}
				});
		}
		return routesByMethodName.values().stream().flatMap(List::stream).collect(Collectors.toList());
	}
	
	@SuppressWarnings("unchecked")
	private GenericWebServerRouteInfo createRoute(AnnotationMirror webRouteAnnotation, BeanQualifiedName controllerQName, ExecutableElement routeElement, ExecutableElement routeAnnotatedElement, ExecutableType routeType, Integer discriminator) {
		ReporterInfo routeReporter;
		if(this.routes.containsKey(routeElement)) {
			// We don't want to report error multiple times on the same element
			routeReporter = new NoOpReporterInfo(this.routes.get(routeElement));
		}
		else if(routeElement == routeAnnotatedElement) {
			routeReporter = this.pluginExecution.getReporter(routeElement, webRouteAnnotation);
		}
		else {
			routeReporter = this.pluginExecution.getReporter(routeElement);
		}
		
		// Get route metadata
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
		
		String routeName = routeElement.getSimpleName().toString();
		if(discriminator != null) {
			routeName += "_" + discriminator; 
		}
		WebServerRouteQualifiedName routeQName;
		if(controllerQName != null) {
			routeQName = new WebServerRouteQualifiedName(controllerQName, routeName);
		}
		else {
			routeQName = new WebServerRouteQualifiedName(routeName);
		}
		
		// Get Response body info
		TypeMirror responseBodyType = routeType.getReturnType();
		TypeMirror erasedResponseBodyType = this.pluginContext.getTypeUtils().erasure(responseBodyType);
		
		ResponseBodyReactiveKind responseBodyReactiveKind;
		if(this.pluginContext.getTypeUtils().isSameType(erasedResponseBodyType, this.publisherType)) {
			responseBodyReactiveKind = ResponseBodyReactiveKind.PUBLISHER;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedResponseBodyType, this.monoType)) {
			responseBodyReactiveKind = ResponseBodyReactiveKind.ONE;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedResponseBodyType, this.fluxType)) {
			responseBodyReactiveKind = ResponseBodyReactiveKind.MANY;
		}
		else {
			responseBodyReactiveKind = ResponseBodyReactiveKind.NONE;
		}
		
		ResponseBodyKind responseBodyKind = ResponseBodyKind.ENCODED;
		if(responseBodyReactiveKind != ResponseBodyReactiveKind.NONE) {
			responseBodyType = ((DeclaredType)responseBodyType).getTypeArguments().getFirst();
			if(this.pluginContext.getTypeUtils().isSameType(responseBodyType, this.voidType)) {
				responseBodyKind = ResponseBodyKind.EMPTY;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(responseBodyType, this.byteBufType)) {
				responseBodyKind = ResponseBodyKind.RAW;
			}
			else if(this.pluginContext.getTypeUtils().isAssignable(responseBodyType, this.charSequenceType)) {
				if(produces.isEmpty()) {
					responseBodyKind = ResponseBodyKind.CHARSEQUENCE;
				}
			}
			else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(responseBodyType), this.sseRawEventType)) {
				responseBodyType = ((DeclaredType)responseBodyType).getTypeArguments().getFirst();
				if(this.pluginContext.getTypeUtils().isSameType(responseBodyType, this.byteBufType)) {
					responseBodyKind = ResponseBodyKind.SSE_RAW;
				}
				else if(this.pluginContext.getTypeUtils().isAssignable(responseBodyType, this.charSequenceType)) {
					responseBodyKind = ResponseBodyKind.SSE_CHARSEQUENCE;
				}
				else {
					routeReporter.error("Unsupported server-sent event type: " + responseBodyType);
				}
			}
			else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(responseBodyType), this.sseEncoderEventType)) {
				responseBodyKind = ResponseBodyKind.SSE_ENCODED;
				responseBodyType = ((DeclaredType)responseBodyType).getTypeArguments().getFirst();
			}
		}
		else if(responseBodyType.getKind() == TypeKind.VOID) {
			responseBodyKind = ResponseBodyKind.EMPTY;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(responseBodyType, this.byteBufType)) {
			responseBodyKind = ResponseBodyKind.RAW;
		}
		else if(this.pluginContext.getTypeUtils().isAssignable(responseBodyType, this.charSequenceType)) {
			if(produces.isEmpty()) {
				responseBodyKind = ResponseBodyKind.CHARSEQUENCE;
			}
		}
		else if(this.pluginContext.getTypeUtils().isSameType(responseBodyType, this.resourceType)) {
			responseBodyKind = ResponseBodyKind.RESOURCE;
		}
		
		WebServerResponseBodyInfo responseBodyInfo  = new GenericWebServerResponseBodyInfo(routeReporter, responseBodyType, responseBodyKind, responseBodyReactiveKind);
		
		// Get route parameters
		List<AbstractWebParameterInfo> parameters = new ArrayList<>();
		boolean hasFormParameters = false;
		boolean hasExchangeParameter = false;
		List<WebParameterInfo> bodyParameters = new ArrayList<>();
		WebSseEventFactoryParameterInfo sseFactoryParameter = null;
		List<? extends VariableElement> routeParameters = routeElement.getParameters();
		List<? extends VariableElement> routeAnnotatedParameters = routeAnnotatedElement.getParameters();
		List<? extends TypeMirror> routeParameterTypes = routeType.getParameterTypes();
		for(int i=0;i<routeParameters.size();i++) {
			VariableElement routeParameterElement = routeParameters.get(i);
			VariableElement routeAnnotatedParameterElement = routeAnnotatedParameters.get(i);
			TypeMirror routeParameterType = routeParameterTypes.get(i);
			AbstractWebParameterInfo parameterInfo = this.webRouteParameterFactory.createParameter(routeQName, routeParameterElement, routeAnnotatedParameterElement, routeParameterType, consumes, produces);
			if(parameterInfo instanceof WebRequestBodyParameterInfo) {
				bodyParameters.add(parameterInfo);
			}
			else if(parameterInfo instanceof WebFormParameterInfo) {
				hasFormParameters = true;
			}
			else if(parameterInfo instanceof WebSseEventFactoryParameterInfo) {
				sseFactoryParameter = (WebSseEventFactoryParameterInfo) parameterInfo;
			}
			else if(parameterInfo instanceof WebExchangeParameterInfo) {
				if(hasExchangeParameter) {
					routeReporter.error("Multiple Web exchange parameters");
				}
				hasExchangeParameter = true;
			}
			parameters.add(parameterInfo);
		}

		if(hasFormParameters && !bodyParameters.isEmpty()) {
			routeReporter.error("Can't mix Body and Form parameters");
		}
		if(bodyParameters.size() > 1) {
			routeReporter.error("Multiple Body parameters");
		}
		if(sseFactoryParameter != null) {
			if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_RAW) {
				if(sseFactoryParameter.getEventFactoryKind() != SseEventFactoryKind.RAW) {
					routeReporter.error("SSE event factory " + sseFactoryParameter.getType() + " doesn't match sse response " + responseBodyInfo.getType());
				}
			}
			else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_CHARSEQUENCE) {
				if(sseFactoryParameter.getEventFactoryKind() != SseEventFactoryKind.CHARSEQUENCE) {
					routeReporter.error("SSE event factory " + sseFactoryParameter.getType() + " doesn't match sse response " + responseBodyInfo.getType());
				}
			}
			else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_ENCODED) {
				if(sseFactoryParameter.getEventFactoryKind() != SseEventFactoryKind.ENCODED || !this.pluginContext.getTypeUtils().isSameType(responseBodyInfo.getType(), sseFactoryParameter.getType())) {
					routeReporter.error("SSE event factory " + sseFactoryParameter.getType() + " doesn't match sse response " + responseBodyInfo.getType());
				}
			}
			else {
				routeReporter.error("Invalid SSE route declaration which must return a publisher of SSE events and define a corresponding SSE event factory parameter");
			}
		}
		else if(responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_RAW || responseBodyInfo.getBodyKind() == ResponseBodyKind.SSE_ENCODED) {
			routeReporter.error("Invalid SSE route declaration which must return a publisher of SSE events and define a corresponding SSE event factory parameter");
		}

		GenericWebServerRouteInfo routeInfo = new GenericWebServerRouteInfo(routeElement, routeType, routeQName, routeReporter, paths, matchTrailingSlash, methods, consumes, produces, languages, parameters, responseBodyInfo);
		this.routes.putIfAbsent(routeElement, routeInfo);
		return routeInfo;
	}
	
	@SuppressWarnings("unchecked")
	private GenericWebSocketServerRouteInfo createWebSocketRoute(AnnotationMirror webSocketRouteAnnotation, BeanQualifiedName controllerQName, ExecutableElement routeElement, ExecutableElement routeAnnotatedElement, ExecutableType routeType, Integer discriminator) {
		ReporterInfo routeReporter;
		if(this.routes.containsKey(routeElement)) {
			// We don't want to report error multiple times on the same element
			routeReporter = new NoOpReporterInfo(this.routes.get(routeElement));
		}
		else if(routeElement == routeAnnotatedElement) {
			routeReporter = this.pluginExecution.getReporter(routeElement, webSocketRouteAnnotation);
		}
		else {
			routeReporter = this.pluginExecution.getReporter(routeElement);
		}
		
		// Get route metadata
		Set<String> paths = new HashSet<>();
		boolean matchTrailingSlash = false;
		Set<String> languages = new HashSet<>();
		Set<String> subprotocols = new HashSet<>();
		WebSocketMessage.Kind messageType = WebSocketMessage.Kind.TEXT;
		boolean closeOnComplete = true;
		for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webSocketRouteAnnotation).entrySet()) {
			switch(value.getKey().getSimpleName().toString()) {
				case "path" : paths.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
				case "matchTrailingSlash": matchTrailingSlash = (boolean)value.getValue().getValue();
					break;
				case "language" : languages.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
				case "subprotocol" : subprotocols.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
				case "messageType" : messageType = WebSocketMessage.Kind.valueOf(value.getValue().getValue().toString());
					break;
				case "closeOnComplete": closeOnComplete = (boolean)value.getValue().getValue();
					break;
			}
		}
		
		String routeName = routeElement.getSimpleName().toString();
		if(discriminator != null) {
			routeName += "_" + discriminator; 
		}
		WebServerRouteQualifiedName routeQName;
		if(controllerQName != null) {
			routeQName = new WebServerRouteQualifiedName(controllerQName, routeName);
		}
		else {
			routeQName = new WebServerRouteQualifiedName(routeName);
		}
		
		// Determine whether we have an outbound publisher as return type
		WebSocketBoundPublisherInfo.BoundReactiveKind outboundReactiveKind = null;
		TypeMirror returnType = routeElement.getReturnType();
		if(returnType.getKind() != TypeKind.VOID) {
			TypeMirror erasedReturnType = this.pluginContext.getTypeUtils().erasure(returnType);
			if(this.pluginContext.getTypeUtils().isSameType(erasedReturnType, this.publisherType)) {
				outboundReactiveKind = WebSocketBoundPublisherInfo.BoundReactiveKind.PUBLISHER;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(erasedReturnType, this.monoType)) {
				outboundReactiveKind = WebSocketBoundPublisherInfo.BoundReactiveKind.ONE;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(erasedReturnType, this.fluxType)) {
				outboundReactiveKind = WebSocketBoundPublisherInfo.BoundReactiveKind.MANY;
			}
			else {
				// This is not allowed
				routeReporter.error("Must return void or an outbound publisher");
			}
		}

		GenericWebSocketServerOutboundPublisherInfo outboundPublisherInfo = null;
		if(outboundReactiveKind != null) {
			// We have an outbound publisher as return type
			WebSocketBoundPublisherInfo.BoundKind outboundKind = WebSocketBoundPublisherInfo.BoundKind.ENCODED;
			TypeMirror type = ((DeclaredType)returnType).getTypeArguments().getFirst();
			if(this.pluginContext.getTypeUtils().isSameType(type, this.voidType)) {
				outboundKind = WebSocketBoundPublisherInfo.BoundKind.EMPTY;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(type, this.byteBufType)) {
				outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED;
			}
			else if(this.pluginContext.getTypeUtils().isAssignable(type, this.charSequenceType)) {
				outboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED;
			}
			else if(type instanceof DeclaredType && ((DeclaredType)type).getTypeArguments().size() == 1) {
				// maybe we have a reactive message payload
				TypeMirror erasedBoundType = this.pluginContext.getTypeUtils().erasure(type);
				TypeMirror nextBoundType = ((DeclaredType)type).getTypeArguments().getFirst();
				if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.publisherType)) {
					if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
						type = nextBoundType;
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_PUBLISHER;
					}
					else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
						type = nextBoundType;
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_PUBLISHER;
					}
				}
				else if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.fluxType)) {
					if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
						type = nextBoundType;
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_MANY;
					}
					else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
						type = nextBoundType;
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_MANY;
					}
				}
				else if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.monoType)) {
					if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
						type = nextBoundType;
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED_ONE;
					}
					else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
						type = nextBoundType;
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED_ONE;
					}
				}
			}
			outboundPublisherInfo = new GenericWebSocketServerOutboundPublisherInfo(routeReporter, type, outboundKind, outboundReactiveKind);
		}
		
		// Get route parameters
		List<AbstractWebParameterInfo> parameters = new ArrayList<>();
		boolean hasExchangeParameter = false;
		boolean hasInboundParameter = false;
		boolean hasOutboundParameter = false;
		List<? extends VariableElement> routeParameters = routeElement.getParameters();
		List<? extends VariableElement> routeAnnotatedParameters = routeAnnotatedElement.getParameters();
		List<? extends TypeMirror> routeParameterTypes = routeType.getParameterTypes();
		for(int i=0;i<routeParameters.size();i++) {
			VariableElement routeParameterElement = routeParameters.get(i);
			VariableElement routeAnnotatedParameterElement = routeAnnotatedParameters.get(i);
			TypeMirror routeParameterType = routeParameterTypes.get(i);
			AbstractWebParameterInfo parameterInfo = this.webSocketRouteParameterFactory.createParameter(routeQName, routeParameterElement, routeAnnotatedParameterElement, routeParameterType, Set.of(), Set.of());
			if(parameterInfo instanceof WebSocketExchangeParameterInfo) {
				if(hasExchangeParameter) {
					routeReporter.error("Multiple WebSocket exchange parameters");
				}
				hasExchangeParameter = true;
			}
			else if(parameterInfo instanceof WebSocketServerInboundParameterInfo) {
				if(hasInboundParameter) {
					routeReporter.error("Multiple WebSocket inbound parameters");
				}
				hasInboundParameter = true;
			}
			else if(parameterInfo instanceof WebSocketServerInboundPublisherParameterInfo) {
				if(hasInboundParameter) {
					routeReporter.error("Multiple WebSocket inbound parameters");
				}
				hasInboundParameter = true;
			}
			else if(parameterInfo instanceof WebSocketOutboundParameterInfo) {
				if(hasOutboundParameter) {
					routeReporter.error("Multiple WebSocket outbound parameters");
				}
				else if(outboundPublisherInfo != null) {
					routeReporter.error("Can't specify WebSocket outbound publisher as return type with WebSocket outbound parameter");
				}
				hasOutboundParameter = true;
			}
			parameters.add(parameterInfo);
		}
		GenericWebSocketServerRouteInfo routeInfo = new GenericWebSocketServerRouteInfo(routeElement, routeType, routeQName, routeReporter, paths, matchTrailingSlash, languages, subprotocols, messageType, parameters, outboundPublisherInfo, closeOnComplete);
		this.routes.putIfAbsent(routeElement, routeInfo);
		return routeInfo;
	}
	
	private Optional<ExecutableElement> getWebRouteAnnotatedElement(ExecutableElement executableElement, TypeElement typeElement) {
		List<ExecutableElement> overriddenElements = this.typeHierarchyExtractor.extractTypeHierarchy(typeElement).stream()
			.map(element -> ElementFilter.methodsIn(element.getEnclosedElements()).stream()
					.filter(methodElement -> methodElement.equals(executableElement) || this.pluginContext.getElementUtils().overrides(executableElement, methodElement, typeElement))
					.findFirst()
			)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toList());
		
		// We must make sure we have either @WebRoute or @WebSocketRoute but not both defined anywhere in the chain
		Optional<ExecutableElement> webRouteAnnotatedElement = overriddenElements.stream().filter(overriddenElement -> overriddenElement.getAnnotationMirrors().stream().anyMatch(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRouteAnnotationType))).findFirst();
		if(webRouteAnnotatedElement.isPresent()) {
			if(overriddenElements.stream().anyMatch(overriddenElement -> overriddenElement.getAnnotationMirrors().stream().anyMatch(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webSocketRouteAnnotationType)))) {
				if(!this.routes.containsKey(webRouteAnnotatedElement.get())) {
					this.pluginExecution.getReporter(webRouteAnnotatedElement.get()).error("Can't specify both WebRoute and WebSocketRoute");
					this.routes.put(webRouteAnnotatedElement.get(), null);
				}
				// end route compilation here
				return Optional.empty();
			}
			return webRouteAnnotatedElement;
		}
		return overriddenElements.stream().filter(overriddenElement -> overriddenElement.getAnnotationMirrors().stream().anyMatch(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webSocketRouteAnnotationType))).findFirst();
	}
}
