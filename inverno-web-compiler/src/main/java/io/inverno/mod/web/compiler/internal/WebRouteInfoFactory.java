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

import io.inverno.core.compiler.spi.BeanInfo;
import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.web.server.WebResponseBody;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.server.annotation.WebRoutes;
import io.inverno.mod.web.server.annotation.WebSocketRoute;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebFormParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.WebResponseBodyInfo;
import io.inverno.mod.web.compiler.spi.WebResponseBodyInfo.ResponseBodyKind;
import io.inverno.mod.web.compiler.spi.WebResponseBodyInfo.ResponseBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import io.inverno.mod.web.compiler.spi.WebRouteQualifiedName;
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSocketExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketInboundParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketInboundPublisherParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketOutboundParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSseEventFactoryParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSseEventFactoryParameterInfo.SseEventFactoryKind;
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
 * A factory is used to create web route info.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebRouteInfo
 */
class WebRouteInfoFactory {

	private final PluginContext pluginContext;
	private final PluginExecution pluginExecution;
	private final WebParameterInfoFactory parameterFactory;
	private final Map<ExecutableElement, GenericWebRouteInfo> routes;
	private final TypeHierarchyExtractor typeHierarchyExtractor;
	
	/* Web annotations */
	private final TypeMirror webControllerAnnotationType;
	private final TypeMirror webRoutesAnnotationType;
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
	 * Creates a web route info factory.
	 * </p>
	 * 
	 * @param pluginContext   the web compiler plugin context
	 * @param pluginExecution the web compiler plugin execution
	 */
	public WebRouteInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		this.pluginContext = Objects.requireNonNull(pluginContext);
		this.pluginExecution = Objects.requireNonNull(pluginExecution);
		this.parameterFactory = new WebParameterInfoFactory(this.pluginContext , this.pluginExecution);
		this.routes = new HashMap<>();
		this.typeHierarchyExtractor = new TypeHierarchyExtractor(this.pluginContext.getTypeUtils());
		
		this.webControllerAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebController.class.getCanonicalName()).asType();
		this.webRoutesAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebRoutes.class.getCanonicalName()).asType();
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
	 * Compiles the specified executable element to create a web route info.
	 * </p>
	 *
	 * @param routeElement the executable element to compile
	 *
	 * @return an optional returning a web route info or an empty optional if the specified element doesn't designate a web route
	 */
	public Optional<GenericWebRouteInfo> compileRoute(ExecutableElement routeElement) {
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
				// end route compilation here
				return Optional.empty();
			}
			
			GenericWebRouteInfo routeInfo = this.routes.get(routeElement);
			if(routeInfo != null) {
				return Optional.of(routeInfo);
			}
			else {
				return Optional.ofNullable(this.createRoute(webRouteAnnotation, null, routeElement, routeElement, (ExecutableType)routeElement.asType(), null));
			}
		}
		else if(webSocketRouteAnnotation != null) {
			GenericWebRouteInfo routeInfo = this.routes.get(routeElement);
			if(routeInfo != null) {
				return Optional.of(routeInfo);
			}
			else {
				return Optional.ofNullable(this.createWebSocketRoute(webSocketRouteAnnotation, null, routeElement, routeElement, (ExecutableType)routeElement.asType(), null));
			}
		}
		else {
			return Optional.empty();
		}
	}
	
	/**
	 * <p>
	 * Compiles the specified provided web router configurer bean and extracts the web routes it defines.
	 * </p>
	 *
	 * @param bean a provided web router configurer bean
	 *
	 * @return the list of web routes defined in the web router configurer
	 *
	 * @throws IllegalArgumentException if the specified bean is not a provided web router configurer
	 */
	@SuppressWarnings("unchecked")
	public List<ProvidedWebRouteInfo> compileRouterRoutes(BeanInfo bean) throws IllegalArgumentException {
		TypeElement beanElement = (TypeElement) this.pluginContext.getTypeUtils().asElement(bean.getType());
		
		AnnotationMirror webRoutesAnnotation = this.pluginContext.getElementUtils().getAllAnnotationMirrors(beanElement).stream()
			.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webRoutesAnnotationType))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("bean " + bean + " is not annotated with " + WebRoutes.class));
		
		if(webRoutesAnnotation.getElementValues().isEmpty()) {
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
								WebRouteQualifiedName routeQName = new WebRouteQualifiedName(bean.getQualifiedName(), "route_" + routeIndex.getAndIncrement());
								return new ProvidedWebRouteInfo(routeQName, bean, paths, matchTrailingSlash, methods, consumes, produces, languages);
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
								WebRouteQualifiedName routeQName = new WebRouteQualifiedName(bean.getQualifiedName(), "route_" + routeIndex.getAndIncrement());
								return new ProvidedWebSocketRouteInfo(routeQName, bean, paths, matchTrailingSlash, languages, subprotocols, messageType);
							});
					}
				}
				
				return null;
			})
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>
	 * Compiles the specified web controller bean and extracts the web routes it defines.
	 * </p>
	 *
	 * @param bean a web controller bean
	 *
	 * @return the list of web routes defined in the web controller
	 *
	 * @throws IllegalArgumentException if the specified bean is not a web controller
	 */
	public List<GenericWebRouteInfo> compileControllerRoutes(BeanInfo bean) throws IllegalArgumentException {
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
		
		Map<String, List<GenericWebRouteInfo>> routesByMethodName = new HashMap<>();
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
					List<GenericWebRouteInfo> currentRoutes = routesByMethodName.get(routeMethodName);
					if(currentRoutes == null) {
						currentRoutes = new LinkedList<>();
						routesByMethodName.put(routeMethodName, currentRoutes);
					}
					
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
	private GenericWebRouteInfo createRoute(AnnotationMirror webRouteAnnotation, BeanQualifiedName controllerQName, ExecutableElement routeElement, ExecutableElement routeAnnotatedElement, ExecutableType routeType, Integer discriminator) {
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
		WebRouteQualifiedName routeQName;
		if(controllerQName != null) {
			routeQName = new WebRouteQualifiedName(controllerQName, routeName);
		}
		else {
			routeQName = new WebRouteQualifiedName(routeName);
		}
		
		// Get Response body info
		TypeMirror responseBodyType = routeElement.getReturnType();
		TypeMirror erasedResponseBodyType = this.pluginContext.getTypeUtils().erasure(responseBodyType);
		
		ResponseBodyReactiveKind responseBodyReactiveKind = null;
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
			responseBodyType = ((DeclaredType)responseBodyType).getTypeArguments().get(0);
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
				responseBodyType = ((DeclaredType)responseBodyType).getTypeArguments().get(0);
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
				responseBodyType = ((DeclaredType)responseBodyType).getTypeArguments().get(0);
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
		
		WebResponseBodyInfo responseBodyInfo  = new GenericWebResponseBodyInfo(responseBodyType, responseBodyKind, responseBodyReactiveKind);
		
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
			AbstractWebParameterInfo parameterInfo = this.parameterFactory.createParameter(routeQName, routeParameterElement, routeAnnotatedParameterElement, routeParameterType);
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

		if(hasFormParameters && bodyParameters.size() > 0) {
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

		GenericWebRouteInfo routeInfo = new GenericWebRouteInfo(routeElement, routeQName, routeReporter, paths, matchTrailingSlash, methods, consumes, produces, languages, parameters, responseBodyInfo);
		this.routes.putIfAbsent(routeElement, routeInfo);
		return routeInfo;
	}
	
	@SuppressWarnings("unchecked")
	private GenericWebSocketRouteInfo createWebSocketRoute(AnnotationMirror webSocketRouteAnnotation, BeanQualifiedName controllerQName, ExecutableElement routeElement, ExecutableElement routeAnnotatedElement, ExecutableType routeType, Integer discriminator) {
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
				case "messageType" : messageType = WebSocketMessage.Kind.valueOf(((AnnotationValue)value.getValue()).getValue().toString());
					break;
			}
		}
		
		String routeName = routeElement.getSimpleName().toString();
		if(discriminator != null) {
			routeName += "_" + discriminator; 
		}
		WebRouteQualifiedName routeQName;
		if(controllerQName != null) {
			routeQName = new WebRouteQualifiedName(controllerQName, routeName);
		}
		else {
			routeQName = new WebRouteQualifiedName(routeName);
		}
		
		// Determine whether we have an outbound publisher as return type
		GenericWebSocketOutboundPublisherInfo outboundPublisherInfo = null;
		
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
		
		if(outboundReactiveKind != null) {
			// We have an outbound publisher as return type
			WebSocketBoundPublisherInfo.BoundKind outboundKind = WebSocketBoundPublisherInfo.BoundKind.ENCODED;
			TypeMirror boundType = ((DeclaredType)returnType).getTypeArguments().get(0);
			if(this.pluginContext.getTypeUtils().isSameType(boundType, this.voidType)) {
				outboundKind = WebSocketBoundPublisherInfo.BoundKind.EMPTY;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(boundType, this.byteBufType)) {
				outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED;
			}
			else if(this.pluginContext.getTypeUtils().isAssignable(boundType, this.charSequenceType)) {
				outboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED;
			}
			else if(boundType instanceof DeclaredType && ((DeclaredType)boundType).getTypeArguments().size() == 1) {
				// maybe we have a reactive message payload
				TypeMirror erasedBoundType = this.pluginContext.getTypeUtils().erasure(boundType);
				TypeMirror nextBoundType = ((DeclaredType)boundType).getTypeArguments().get(0);
				if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.publisherType)) {
					boundType = nextBoundType;
					if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_PUBLISHER;
					}
					else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_PUBLISHER;
					}
				}
				else if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.fluxType)) {
					boundType = nextBoundType;
					if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_MANY;
					}
					else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_MANY;
					}
				}
				else if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.monoType)) {
					boundType = nextBoundType;
					if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED_ONE;
					}
					else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
						outboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED_ONE;
					}
				}
			}
			outboundPublisherInfo = new GenericWebSocketOutboundPublisherInfo(boundType, outboundKind, outboundReactiveKind);
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
			AbstractWebParameterInfo parameterInfo = this.parameterFactory.createWebSocketParameter(routeQName, routeParameterElement, routeAnnotatedParameterElement, routeParameterType);
			if(parameterInfo instanceof WebSocketExchangeParameterInfo) {
				if(hasExchangeParameter) {
					routeReporter.error("Multiple WebSocket exchange parameters");
				}
				hasExchangeParameter = true;
			}
			else if(parameterInfo instanceof WebSocketInboundParameterInfo) {
				if(hasInboundParameter) {
					routeReporter.error("Multiple WebSocket inbound parameters");
				}
				hasInboundParameter = true;
			}
			else if(parameterInfo instanceof WebSocketInboundPublisherParameterInfo) {
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
		GenericWebSocketRouteInfo routeInfo = new GenericWebSocketRouteInfo(routeElement, routeQName, routeReporter, paths, matchTrailingSlash, languages, subprotocols, messageType, parameters, outboundPublisherInfo);
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
