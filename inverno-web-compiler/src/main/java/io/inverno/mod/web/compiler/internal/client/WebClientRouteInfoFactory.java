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

import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.client.WebResponse;
import io.inverno.mod.web.client.annotation.WebClient;
import io.inverno.mod.web.client.annotation.WebRoute;
import io.inverno.mod.web.client.annotation.WebSocketRoute;
import io.inverno.mod.web.client.ws.Web2SocketExchange;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.internal.NoOpReporterInfo;
import io.inverno.mod.web.compiler.internal.TypeHierarchyExtractor;
import io.inverno.mod.web.compiler.spi.ResponseBodyKind;
import io.inverno.mod.web.compiler.spi.ResponseBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebFormParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteQualifiedName;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteReturnInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientOutboundPublisherParameterInfo;
import io.inverno.mod.web.compiler.spi.client.WebSocketClientRouteReturnInfo;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Factory used to create Web client route info.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebClientRouteInfoFactory {

	private final PluginContext pluginContext;
	private final PluginExecution pluginExecution;
	private final WebClientRouteParameterInfoFactory webRouteParameterFactory;
	private final WebSocketClientRouteParameterInfoFactory webSocketRouteParameterFactory;
	private final Map<ExecutableElement, GenericWebClientRouteInfo> routes;
	private final TypeHierarchyExtractor typeHierarchyExtractor;

	/* Web annotations */
	private final TypeMirror webClientAnnotationType;
	private final TypeMirror webRouteAnnotationType;
	private final TypeMirror webSocketRouteAnnotationType;

	/* Types */
	private final TypeMirror publisherType;
	private final TypeMirror monoType;
	private final TypeMirror fluxType;
	private final TypeMirror byteBufType;
	private final TypeMirror charSequenceType;
	private final TypeMirror stringType;
	private final TypeMirror voidType;
	private final TypeMirror webExchangeType;
	private final TypeMirror webResponseType;
	private final TypeMirror exchangeContextType;
	private final TypeMirror web2SocketExchangeType;
	private final TypeMirror web2SocketExchangeInboundType;

	/**
	 * <p>
	 * Creates a Web client route info factory.
	 * </p>
	 *
	 * @param pluginContext   the Web compiler plugin context
	 * @param pluginExecution the Web compiler plugin execution
	 */
	public WebClientRouteInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		this.pluginContext = Objects.requireNonNull(pluginContext);
		this.pluginExecution = Objects.requireNonNull(pluginExecution);
		this.webRouteParameterFactory = new WebClientRouteParameterInfoFactory(this.pluginContext , this.pluginExecution);
		this.webSocketRouteParameterFactory = new WebSocketClientRouteParameterInfoFactory(this.pluginContext, this.pluginExecution);
		this.routes = new HashMap<>();
		this.typeHierarchyExtractor = new TypeHierarchyExtractor(this.pluginContext.getTypeUtils());

		this.webClientAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebClient.class.getCanonicalName()).asType();
		this.webRouteAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebRoute.class.getCanonicalName()).asType();
		this.webSocketRouteAnnotationType = this.pluginContext.getElementUtils().getTypeElement(WebSocketRoute.class.getCanonicalName()).asType();

		this.publisherType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Publisher.class.getCanonicalName()).asType());
		this.monoType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Mono.class.getCanonicalName()).asType());
		this.fluxType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Flux.class.getCanonicalName()).asType());
		this.byteBufType = this.pluginContext.getElementUtils().getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.charSequenceType = this.pluginContext.getElementUtils().getTypeElement(CharSequence.class.getCanonicalName()).asType();
		this.stringType = this.pluginContext.getElementUtils().getTypeElement(String.class.getCanonicalName()).asType();
		this.voidType = this.pluginContext.getElementUtils().getTypeElement(Void.class.getCanonicalName()).asType();
		this.webExchangeType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebExchange.class.getCanonicalName()).asType());
		this.webResponseType = this.pluginContext.getElementUtils().getTypeElement(WebResponse.class.getCanonicalName()).asType();
		this.exchangeContextType = this.pluginContext.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType();
		this.web2SocketExchangeType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Web2SocketExchange.class.getCanonicalName()).asType());
		this.web2SocketExchangeInboundType =  this.pluginContext.getElementUtils().getTypeElement(BaseWeb2SocketExchange.Inbound.class.getCanonicalName()).asType();
	}

	/**
	 * <p>
	 * Pre-compiles the specified executable element and pre-populates the internal Web routes map.
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
	 * Compiles the specified Web client and extracts the Web routes it defines.
	 * </p>
	 *
	 * @param clientStubElement a Web client element
	 * @param clientStubQName   client stub qualified name
	 *
	 * @return the list of Web routes defined in the Web client
	 *
	 * @throws IllegalArgumentException if the specified element is not a Web client
	 */
	public List<GenericWebClientRouteInfo> compileClientStubRoutes(TypeElement clientStubElement, BeanQualifiedName clientStubQName) throws IllegalArgumentException {
		this.typeHierarchyExtractor.extractTypeHierarchy(clientStubElement).stream()
			.map(element -> this.pluginContext.getElementUtils().getAllAnnotationMirrors(element).stream()
				.filter(annotation -> this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.webClientAnnotationType))
				.findFirst()
			)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(clientStubElement + " is not annotated with " + WebClient.class));

		Map<String, List<GenericWebClientRouteInfo>> routesByMethodName = new HashMap<>();
		for(ExecutableElement routeElement : ElementFilter.methodsIn(this.pluginContext.getElementUtils().getAllMembers(clientStubElement))) {
			this.getWebRouteAnnotatedElement(routeElement, clientStubElement)
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
					ExecutableType routeElementType = (ExecutableType)this.pluginContext.getTypeUtils().asMemberOf((DeclaredType)clientStubElement.asType(), routeElement);

					String routeMethodName = routeElement.getSimpleName().toString();
					List<GenericWebClientRouteInfo> currentRoutes = routesByMethodName.computeIfAbsent(routeMethodName, k -> new LinkedList<>());

					if(webRouteAnnotation != null) {
						currentRoutes.add(this.createRoute(webRouteAnnotation, clientStubQName, routeElement, routeAnnotatedElement, routeElementType, currentRoutes.isEmpty() ? null : currentRoutes.size()));
					}
					else if(webSocketRouteAnnotation != null) {
						currentRoutes.add(this.createWebSocketRoute(webSocketRouteAnnotation, clientStubQName, routeElement, routeAnnotatedElement, routeElementType, currentRoutes.isEmpty() ? null : currentRoutes.size()));
					}
				});
		}
		return routesByMethodName.values().stream().flatMap(List::stream).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private GenericWebClientRouteInfo createRoute(AnnotationMirror webRouteAnnotation, BeanQualifiedName clientQName, ExecutableElement routeElement, ExecutableElement routeAnnotatedElement, ExecutableType routeType, Integer discriminator) {
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
		String path = null;
		Method method = null;
		Set<Method> methods = new HashSet<>();
		Set<String> consumes = new HashSet<>();
		String produces = null;
		Set<String> languages = new HashSet<>();
		for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webRouteAnnotation).entrySet()) {
			switch(value.getKey().getSimpleName().toString()) {
				case "path" : path = (String)value.getValue().getValue();
					break;
				case "method" : method = Method.valueOf(value.getValue().getValue().toString());
					break;
				case "consumes" : consumes.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
				case "produces" : produces = (String)value.getValue().getValue();
					break;
				case "language" : languages.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
			}
		}

		if(StringUtils.isBlank(path)) {
			path = null;
		}
		if(StringUtils.isBlank(produces)) {
			produces = null;
		}

		String routeName = routeElement.getSimpleName().toString();
		if(discriminator != null) {
			routeName += "_" + discriminator;
		}
		WebClientRouteQualifiedName routeQName;
		if(clientQName != null) {
			routeQName = new WebClientRouteQualifiedName(clientQName, routeName);
		}
		else {
			routeQName = new WebClientRouteQualifiedName(routeName);
		}

		TypeMirror returnType = routeType.getReturnType();
		TypeMirror erasedReturnType = this.pluginContext.getTypeUtils().erasure(returnType);

		ResponseBodyReactiveKind returnReactiveKind;
		if(this.pluginContext.getTypeUtils().isSameType(erasedReturnType, this.publisherType)) {
			returnReactiveKind = ResponseBodyReactiveKind.PUBLISHER;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedReturnType, this.monoType)) {
			returnReactiveKind = ResponseBodyReactiveKind.ONE;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedReturnType, this.fluxType)) {
			returnReactiveKind = ResponseBodyReactiveKind.MANY;
		}
		else {
			routeReporter.error("Must return a publisher: " +  this.publisherType + ", " +  this.fluxType + " or " + this.monoType);
			returnReactiveKind = null;
		}

		WebClientRouteReturnInfo returnInfo = null;
		if(returnReactiveKind  != null) {
			if(((DeclaredType)returnType).getTypeArguments().isEmpty()) {
				routeReporter.error("Missing return type");
			}
			else {
				returnType = ((DeclaredType) returnType).getTypeArguments().getFirst();
				if(returnType.getKind() == TypeKind.WILDCARD) {
					if(((WildcardType)returnType).getExtendsBound() != null) {
						returnType = ((WildcardType)returnType).getExtendsBound();
					}
					else if(((WildcardType)returnType).getSuperBound() != null) {
						returnType = ((WildcardType)returnType).getSuperBound();
					}
					else {
						routeReporter.error("Missing return type");
						returnType = null;
					}
				}
				if(returnType != null) {
					if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(returnType), this.webExchangeType)) {
						if(returnReactiveKind != ResponseBodyReactiveKind.ONE) {
							routeReporter.error("Exchange can only be returned in a " + Mono.class.getSimpleName());
							returnInfo = new GenericWebClientExchangeReturnInfo(routeReporter, this.exchangeContextType, this.exchangeContextType);
						}
						else {
							returnInfo = this.createExchangeReturnInfo(routeReporter, (DeclaredType) returnType);
						}
					}
					else if(this.pluginContext.getTypeUtils().isSameType(returnType, this.webResponseType)) {
						if(returnReactiveKind != ResponseBodyReactiveKind.ONE) {
							routeReporter.error("Response can only be returned in a " + Mono.class.getSimpleName());
						}
						returnInfo = new GenericWebClientResponseReturnInfo(routeReporter);
					}
					else {
						ResponseBodyKind responseBodyKind = ResponseBodyKind.ENCODED;
						if(this.pluginContext.getTypeUtils().isSameType(returnType, this.byteBufType)) {
							responseBodyKind = ResponseBodyKind.RAW;
						}
						else if(this.pluginContext.getTypeUtils().isSameType(returnType, this.charSequenceType) || this.pluginContext.getTypeUtils().isSameType(returnType, this.stringType)) {
							if(consumes.isEmpty()) {
								responseBodyKind = ResponseBodyKind.CHARSEQUENCE;
							}
						}
						if(this.pluginContext.getTypeUtils().isSameType(returnType, this.voidType)) {
							responseBodyKind = ResponseBodyKind.EMPTY;
						}
						returnInfo = new GenericWebClientResponseBodyInfo(routeReporter, returnType, responseBodyKind, returnReactiveKind);
					}
				}
				else {
					returnInfo = new GenericWebClientExchangeReturnInfo(routeReporter, this.exchangeContextType, this.exchangeContextType);
				}
			}
		}

		// Get route parameters
		List<AbstractWebParameterInfo> parameters = new ArrayList<>();
		boolean hasFormParameters = false;
		List<WebParameterInfo> bodyParameters = new ArrayList<>();
		List<? extends VariableElement> routeParameters = routeElement.getParameters();
		List<? extends VariableElement> routeAnnotatedParameters = routeAnnotatedElement.getParameters();
		List<? extends TypeMirror> routeParameterTypes = routeType.getParameterTypes();
		for(int i=0;i<routeParameters.size();i++) {
			VariableElement routeParameterElement = routeParameters.get(i);
			VariableElement routeAnnotatedParameterElement = routeAnnotatedParameters.get(i);
			TypeMirror routeParameterType = routeParameterTypes.get(i);
			AbstractWebParameterInfo parameterInfo = this.webRouteParameterFactory.createParameter(routeQName, routeParameterElement, routeAnnotatedParameterElement, routeParameterType, consumes, produces == null ? Set.of() : Set.of(produces));
			if(parameterInfo instanceof WebRequestBodyParameterInfo) {
				bodyParameters.add(parameterInfo);
			}
			else if(parameterInfo instanceof WebFormParameterInfo) {
				hasFormParameters = true;
			}
			parameters.add(parameterInfo);
		}

		if(hasFormParameters && !bodyParameters.isEmpty()) {
			routeReporter.error("Can't mix Body and Form parameters");
		}
		if(bodyParameters.size() > 1) {
			routeReporter.error("Multiple Body parameters");
		}

		GenericWebClientRouteInfo routeInfo = new GenericWebClientRouteInfo(routeElement, routeType, routeQName, routeReporter, path, method, consumes, produces, languages, parameters, returnInfo);
		this.routes.putIfAbsent(routeElement, routeInfo);
		return routeInfo;
	}

	@SuppressWarnings("unchecked")
	private GenericWebSocketClientRouteInfo createWebSocketRoute(AnnotationMirror webSocketRouteAnnotation, BeanQualifiedName clientQName, ExecutableElement routeElement, ExecutableElement routeAnnotatedElement, ExecutableType routeType, Integer discriminator) {
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
		String path = null;
		Set<String> languages = new HashSet<>();
		String subprotocol = null;
		WebSocketMessage.Kind messageType = WebSocketMessage.Kind.TEXT;
		boolean closeOnComplete = true;
		for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.pluginContext.getElementUtils().getElementValuesWithDefaults(webSocketRouteAnnotation).entrySet()) {
			switch(value.getKey().getSimpleName().toString()) {
				case "path" : path = (String)value.getValue().getValue();
					break;
				case "language" : languages.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
					break;
				case "subprotocol" : subprotocol = (String)value.getValue().getValue();
					break;
				case "messageType" : messageType = WebSocketMessage.Kind.valueOf(value.getValue().getValue().toString());
					break;
				case "closeOnComplete": closeOnComplete = (boolean)value.getValue().getValue();
					break;
			}
		}
		if(StringUtils.isBlank(path)) {
			path = null;
		}
		if(StringUtils.isBlank(subprotocol)) {
			subprotocol = null;
		}

		String routeName = routeElement.getSimpleName().toString();
		if(discriminator != null) {
			routeName += "_" + discriminator;
		}
		WebClientRouteQualifiedName routeQName;
		if(clientQName != null) {
			routeQName = new WebClientRouteQualifiedName(clientQName, routeName);
		}
		else {
			routeQName = new WebClientRouteQualifiedName(routeName);
		}

		TypeMirror returnType = routeType.getReturnType();
		TypeMirror erasedReturnType = this.pluginContext.getTypeUtils().erasure(returnType);
		WebSocketBoundPublisherInfo.BoundReactiveKind returnReactiveKind;
		if(this.pluginContext.getTypeUtils().isSameType(erasedReturnType, this.publisherType)) {
			returnReactiveKind = WebSocketBoundPublisherInfo.BoundReactiveKind.PUBLISHER;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedReturnType, this.monoType)) {
			returnReactiveKind = WebSocketBoundPublisherInfo.BoundReactiveKind.ONE;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedReturnType, this.fluxType)) {
			returnReactiveKind = WebSocketBoundPublisherInfo.BoundReactiveKind.MANY;
		}
		else {
			routeReporter.error("Must return a publisher: " +  this.publisherType + ", " +  this.fluxType + " or " + this.monoType);
			returnReactiveKind = null;
		}

		WebSocketClientRouteReturnInfo returnInfo = null;
		if(returnReactiveKind  != null) {
			if(((DeclaredType) returnType).getTypeArguments().isEmpty()) {
				routeReporter.error("Missing return type");
			}
			else {
				returnType = ((DeclaredType) returnType).getTypeArguments().getFirst();
				if(returnType.getKind() == TypeKind.WILDCARD) {
					if(((WildcardType) returnType).getExtendsBound() != null) {
						returnType = ((WildcardType) returnType).getExtendsBound();
					}
					else if(((WildcardType) returnType).getSuperBound() != null) {
						returnType = ((WildcardType) returnType).getSuperBound();
					}
					else {
						routeReporter.error("Missing return type");
						returnType = null;
					}
				}
				if(returnType != null) {
					if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(returnType), this.web2SocketExchangeType)) {
						if (returnReactiveKind != WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
							routeReporter.error("Exchange can only be returned in a " + Mono.class.getSimpleName());
							returnInfo = new GenericWebSocketClientExchangeReturnInfo(routeReporter, this.exchangeContextType, this.exchangeContextType);
						} else {
							returnInfo = this.createWebSocketExchangeReturnInfo(routeReporter, (DeclaredType) returnType);
						}
					}
					else if(this.pluginContext.getTypeUtils().isSameType(returnType, this.web2SocketExchangeInboundType)) {
						if (returnReactiveKind != WebSocketBoundPublisherInfo.BoundReactiveKind.ONE) {
							routeReporter.error("Inbound can only be returned in a " + Mono.class.getSimpleName());
						}
						returnInfo = new GenericWebSocketClientInboundReturnInfo(routeReporter);
					}
					else {
						WebSocketBoundPublisherInfo.BoundKind outboundKind = WebSocketBoundPublisherInfo.BoundKind.ENCODED;
						TypeMirror type = returnType;
						if(this.pluginContext.getTypeUtils().isSameType(type, this.voidType)) {
							outboundKind = WebSocketBoundPublisherInfo.BoundKind.EMPTY;
						}
						else if(this.pluginContext.getTypeUtils().isSameType(type, this.byteBufType)) {
							outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED;
						}
						else if(this.pluginContext.getTypeUtils().isAssignable(type, this.charSequenceType)) {
							outboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED;
						}
						else if (type instanceof DeclaredType && ((DeclaredType) type).getTypeArguments().size() == 1) {
							// maybe we have a reactive message payload
							TypeMirror erasedBoundType = this.pluginContext.getTypeUtils().erasure(type);
							TypeMirror nextBoundType = ((DeclaredType) type).getTypeArguments().getFirst();
							if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.publisherType)) {
								if (this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
									type = nextBoundType;
									outboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_PUBLISHER;
								}
								else if (this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
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
						returnInfo = new GenericWebSocketClientInboundPublisherInfo(routeReporter, type, outboundKind, returnReactiveKind);
					}
				}
				else {
					returnInfo = new GenericWebSocketClientInboundPublisherInfo(routeReporter, this.voidType, WebSocketBoundPublisherInfo.BoundKind.EMPTY, returnReactiveKind);
				}
			}
		}
		// Get route parameters
		List<AbstractWebParameterInfo> parameters = new ArrayList<>();
		boolean hasOutboundParameter = false;
		List<? extends VariableElement> routeParameters = routeElement.getParameters();
		List<? extends VariableElement> routeAnnotatedParameters = routeAnnotatedElement.getParameters();
		List<? extends TypeMirror> routeParameterTypes = routeType.getParameterTypes();
		for(int i=0;i<routeParameters.size();i++) {
			VariableElement routeParameterElement = routeParameters.get(i);
			VariableElement routeAnnotatedParameterElement = routeAnnotatedParameters.get(i);
			TypeMirror routeParameterType = routeParameterTypes.get(i);
			AbstractWebParameterInfo parameterInfo = this.webSocketRouteParameterFactory.createParameter(routeQName, routeParameterElement, routeAnnotatedParameterElement, routeParameterType, Set.of(), Set.of());
			if(parameterInfo instanceof WebSocketClientOutboundPublisherParameterInfo) {
				if(hasOutboundParameter) {
					routeReporter.error("Multiple WebSocket outbound parameters");
				}
				hasOutboundParameter = true;
			}
			parameters.add(parameterInfo);
		}
		GenericWebSocketClientRouteInfo routeInfo = new GenericWebSocketClientRouteInfo(routeElement, routeType, routeQName, routeReporter, path, languages, subprotocol, messageType, parameters, returnInfo, closeOnComplete);
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

	private GenericWebClientExchangeReturnInfo createExchangeReturnInfo(ReporterInfo reporter, DeclaredType webExchangeType) {
		TypeMirror contextType = this.exchangeContextType;
		List<? extends TypeMirror> typeArguments = webExchangeType.getTypeArguments();
		if(!typeArguments.isEmpty()) {
			contextType = typeArguments.getFirst();
			if(contextType.getKind() == TypeKind.WILDCARD) {
				TypeMirror extendsBound = ((WildcardType)contextType).getExtendsBound();
				if(extendsBound != null) {
					contextType = extendsBound;
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
			reporter.error("Web exchange context must be an interface");
		}
		return new GenericWebClientExchangeReturnInfo(reporter, webExchangeType, contextType);
	}

	private GenericWebSocketClientExchangeReturnInfo createWebSocketExchangeReturnInfo(ReporterInfo reporter, DeclaredType webExchangeType) {
		TypeMirror contextType = this.exchangeContextType;
		List<? extends TypeMirror> typeArguments = webExchangeType.getTypeArguments();
		if(!typeArguments.isEmpty()) {
			contextType = typeArguments.getFirst();
			if(contextType.getKind() == TypeKind.WILDCARD) {
				TypeMirror extendsBound = ((WildcardType)contextType).getExtendsBound();
				if(extendsBound != null) {
					contextType = extendsBound;
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
			reporter.error("Web exchange context must be an interface");
		}
		return new GenericWebSocketClientExchangeReturnInfo(reporter, webExchangeType, contextType);
	}
}
