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

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.web.server.Web2SocketExchange;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebPart;
import io.inverno.mod.web.server.WebResponseBody;
import io.inverno.mod.web.server.annotation.Body;
import io.inverno.mod.web.server.annotation.CookieParam;
import io.inverno.mod.web.server.annotation.FormParam;
import io.inverno.mod.web.server.annotation.HeaderParam;
import io.inverno.mod.web.server.annotation.PathParam;
import io.inverno.mod.web.server.annotation.QueryParam;
import io.inverno.mod.web.server.annotation.SseEventFactory;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo.RequestBodyKind;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo.RequestBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebRouteQualifiedName;
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.compiler.spi.WebSseEventFactoryParameterInfo.SseEventFactoryKind;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A web parameter info factory is used to create web parameter info.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebParameterInfo
 */
class WebParameterInfoFactory {

	private final PluginContext pluginContext;
	private final PluginExecution pluginExecution;
	private final Map<VariableElement, AbstractWebParameterInfo> processedParameterElements;
	
	/* Web annotations */
	private final TypeMirror bodyAnnotationType;
	private final TypeMirror cookieParameterAnnotationType;
	private final TypeMirror formParameterAnnotationType;
	private final TypeMirror headerParameterAnnotationType;
	private final TypeMirror pathParameterAnnotationType;
	private final TypeMirror queryParameterAnnotationType;
	private final TypeMirror sseEventFactoryAnnotationType;
	
	/* Contextual */
	private final TypeMirror webExchangeType;
	private final TypeMirror exchangeContextType;
	private final TypeMirror sseEventFactoryType;
	private final TypeMirror sseEncoderEventFactoryType;
	private final TypeMirror web2SocketExchangeType;
	private final TypeMirror web2SocketExchangeInboundType;
	private final TypeMirror web2SocketExchangeOutboundType;
	
	/* Types */
	private final TypeMirror optionalType;
	private final TypeMirror monoType;
	private final TypeMirror fluxType;
	private final TypeMirror voidType;
	private final TypeMirror publisherType;
	private final TypeMirror byteBufType;
	private final TypeMirror charSequenceType;
	private final TypeMirror webPartType;
	private final TypeMirror parameterType;

	/**
	 * <p>
	 * Creates a web parameter info factory.
	 * </p>
	 * 
	 * @param pluginContext   the web compiler plugin context
	 * @param pluginExecution the web compiler plugin execution
	 */
	public WebParameterInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		this.pluginContext = Objects.requireNonNull(pluginContext);
		this.pluginExecution = Objects.requireNonNull(pluginExecution);
		this.processedParameterElements = new HashMap<>();
		
		this.bodyAnnotationType = this.pluginContext.getElementUtils().getTypeElement(Body.class.getCanonicalName()).asType();
		this.cookieParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(CookieParam.class.getCanonicalName()).asType();
		this.formParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(FormParam.class.getCanonicalName()).asType();
		this.headerParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(HeaderParam.class.getCanonicalName()).asType();
		this.pathParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(PathParam.class.getCanonicalName()).asType();
		this.queryParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(QueryParam.class.getCanonicalName()).asType();
		this.sseEventFactoryAnnotationType = this.pluginContext.getElementUtils().getTypeElement(SseEventFactory.class.getCanonicalName()).asType();
		
		this.optionalType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Optional.class.getCanonicalName()).asType());
		this.monoType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Mono.class.getCanonicalName()).asType());
		this.fluxType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Flux.class.getCanonicalName()).asType());
		this.voidType = this.pluginContext.getElementUtils().getTypeElement(Void.class.getCanonicalName()).asType();
		this.publisherType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Publisher.class.getCanonicalName()).asType());
		this.byteBufType = this.pluginContext.getElementUtils().getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.charSequenceType = this.pluginContext.getElementUtils().getTypeElement(CharSequence.class.getCanonicalName()).asType();
		this.webPartType = this.pluginContext.getElementUtils().getTypeElement(WebPart.class.getCanonicalName()).asType();
		this.parameterType = this.pluginContext.getElementUtils().getTypeElement(Parameter.class.getCanonicalName()).asType();
		
		this.webExchangeType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebExchange.class.getCanonicalName()).asType());
		this.exchangeContextType = this.pluginContext.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType();
		this.sseEventFactoryType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(ResponseBody.Sse.EventFactory.class.getCanonicalName()).asType());
		this.sseEncoderEventFactoryType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebResponseBody.SseEncoder.EventFactory.class.getCanonicalName()).asType());
		this.web2SocketExchangeType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Web2SocketExchange.class.getCanonicalName()).asType());
		this.web2SocketExchangeInboundType = this.pluginContext.getElementUtils().getTypeElement(Web2SocketExchange.Inbound.class.getCanonicalName()).asType();
		this.web2SocketExchangeOutboundType = this.pluginContext.getElementUtils().getTypeElement(Web2SocketExchange.Outbound.class.getCanonicalName()).asType();
	}
	
	/**
	 * <p>
	 * Creates a web parameter from a parameter element.
	 * </p>
	 *
	 * @param routeQName                the qualified name of the route for which the parameter is defined
	 * @param parameterElement          the variable element of the parameter
	 * @param annotatedParameterElement the variable element of the parameter in the method actually annotated with {@link WebRoute @WebRoute} annotation
	 * @param parameterType             the parameter type
	 *
	 * @return an abstract web parameter info
	 */
	public AbstractWebParameterInfo createParameter(WebRouteQualifiedName routeQName, VariableElement parameterElement, VariableElement annotatedParameterElement, TypeMirror parameterType) {
		AbstractWebParameterInfo result = null;
		
		WebParameterQualifiedName parameterQName = new WebParameterQualifiedName(routeQName, annotatedParameterElement.getSimpleName().toString());
		boolean required = !this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterType), this.optionalType);
		if(!required) {
			// For optional parameter consider the Optional<> argument
			parameterType = ((DeclaredType)parameterType).getTypeArguments().get(0);
		}

		ReporterInfo parameterReporter;
		if(this.processedParameterElements.containsKey(parameterElement)) {
			parameterReporter = new NoOpReporterInfo(this.processedParameterElements.get(parameterElement));
		}
		else {
			parameterReporter = this.pluginExecution.getReporter(parameterElement);
		}
		
		// A web parameter can't be annotated with multiple web parameter annotations
		for(AnnotationMirror annotation : annotatedParameterElement.getAnnotationMirrors()) {
			AbstractWebParameterInfo currentParameterInfo = null;
			if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.bodyAnnotationType)) {
				if(!required) {
					parameterReporter.error("Request body parameter can't be optional");
				}
				else {
					currentParameterInfo = this.createRequestBodyParameter(parameterReporter, parameterQName, parameterElement, parameterType);
				}
			}
			else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.cookieParameterAnnotationType)) {
				currentParameterInfo = this.createCookieParameter(parameterReporter, parameterQName, parameterElement, parameterType, required);
			}
			else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.formParameterAnnotationType)) {
				currentParameterInfo = this.createFormParameter(parameterReporter, parameterQName, parameterElement, parameterType, required);
			}
			else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.headerParameterAnnotationType)) {
				currentParameterInfo = this.createHeaderParameter(parameterReporter, parameterQName, parameterElement, parameterType, required);
			}
			else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.pathParameterAnnotationType)) {
				currentParameterInfo = this.createPathParameter(parameterReporter, parameterQName, parameterElement, parameterType, required);
			}
			else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.queryParameterAnnotationType)) {
				currentParameterInfo = this.createQueryParameter(parameterReporter, parameterQName, parameterElement, parameterType, required);
			}
			else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.sseEventFactoryAnnotationType)) {
				if(!required) {
					parameterReporter.error("SSE event factory parameter can't be optional");
				}
				else {
					currentParameterInfo = this.createSseEventFactoryParameter(parameterReporter, parameterQName, parameterElement);
				}
			}
			
			if(currentParameterInfo != null) {
				if(result != null) {
					parameterReporter.error("Too many web parameter annotations specified, only one is allowed");
					break;
				}
				result = currentParameterInfo;
			}
		}
		
		// Contextual
		if(result == null) {
			if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.webExchangeType)) {
				result = this.createExchangeParameter(parameterReporter, parameterQName, parameterElement);
			}
			else if(this.pluginContext.getTypeUtils().isAssignable(parameterElement.asType(), this.exchangeContextType)) {
				result = this.createExchangeContextParameter(parameterReporter, parameterQName, parameterElement);
			}
		}
		
		if(result == null) {
			if(!parameterReporter.hasError()) {
				parameterReporter.error("Invalid parameter which is neither a web parameter, nor a valid contextual parameter");
			}
			result = new InvalidWebParameterInfo(parameterQName, parameterReporter, parameterElement, required);
		}
		this.processedParameterElements.putIfAbsent(parameterElement, result);
		return result;
	}
	
	/**
	 * <p>
	 * Creates a WebSocket parameter from a parameter element.
	 * </p>
	 *
	 * @param routeQName                the qualified name of the route for which the parameter is defined
	 * @param parameterElement          the variable element of the parameter
	 * @param annotatedParameterElement the variable element of the parameter in the method actually annotated with {@link WebRoute @WebRoute} annotation
	 * @param parameterType             the parameter type
	 *
	 * @return an abstract web parameter info
	 */
	public AbstractWebParameterInfo createWebSocketParameter(WebRouteQualifiedName routeQName, VariableElement parameterElement, VariableElement annotatedParameterElement, TypeMirror parameterType) {
		AbstractWebParameterInfo result;
		
		WebParameterQualifiedName parameterQName = new WebParameterQualifiedName(routeQName, annotatedParameterElement.getSimpleName().toString());
		ReporterInfo parameterReporter;
		if(this.processedParameterElements.containsKey(parameterElement)) {
			parameterReporter = new NoOpReporterInfo(this.processedParameterElements.get(parameterElement));
		}
		else {
			parameterReporter = this.pluginExecution.getReporter(parameterElement);
		}
		
		// only contextual, no annotation: 
		// - Web2SocketExchange.Inbound
		// - Web2SocketExchange.Outbound
		// - Web2SockerExchange<? extends Context>
		// - Publiser<T>, Flux<T>, Mono<T>
		// - T extends Context
		
		// Contextual
		if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.web2SocketExchangeType)) {
			result = this.createWebSocketExchangeParameter(parameterReporter, parameterQName, parameterElement);
		}
		else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.web2SocketExchangeInboundType)) {
			result = this.createWebSocketInboundParameter(parameterReporter, parameterQName, parameterElement);
		}
		else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.web2SocketExchangeOutboundType)) {
			result = this.createWebSocketOutboundParameter(parameterReporter, parameterQName, parameterElement);
		}
		else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.publisherType) 
			|| this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.fluxType) 
			|| this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.monoType)) {
			result = this.createWebSocketInboundPublisherParameter(parameterReporter, parameterQName, parameterElement, parameterType);
		}
		else if(this.pluginContext.getTypeUtils().isAssignable(parameterElement.asType(), this.exchangeContextType)) {
			result = this.createExchangeContextParameter(parameterReporter, parameterQName, parameterElement);
		}
		else {
			parameterReporter.error("Invalid parameter which is not a WebSocket parameter");
			result = new InvalidWebParameterInfo(parameterQName, parameterReporter, parameterElement, false);
		}
		this.processedParameterElements.putIfAbsent(parameterElement, result);
		return result;
	}
	
	/**
	 * <p>
	 * Creates a cookie parameter info.
	 * </p>
	 * 
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false
	 *                         otherwise
	 * 
	 * @return a web cookie parameter info
	 */
	private GenericWebCookieParameterInfo createCookieParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebCookieParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}
	
	/**
	 * <p>
	 * Creates an exchange parameter info.
	 * </p>
	 * 
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * 
	 * @return a web exchange parameter info
	 */
	private GenericWebExchangeParameterInfo createExchangeParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		TypeMirror contextType = this.exchangeContextType;
		List<? extends TypeMirror> typeArguments = ((DeclaredType)parameterElement.asType()).getTypeArguments();
		if(!typeArguments.isEmpty()) {
			contextType = typeArguments.get(0);
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
			reporter.error("Web exchange context must be an interface");
		}
		return new GenericWebExchangeParameterInfo(parameterQName, reporter, parameterElement, contextType);
	}
	
	/**
	 * <p>
	 * Creates an exchange context parameter info.
	 * </p>
	 * 
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * 
	 * @return a web exchange context parameter info
	 */
	private GenericWebExchangeContextParameterInfo createExchangeContextParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		TypeMirror contextType = parameterElement.asType();
		if(contextType.getKind() == TypeKind.TYPEVAR) {
			contextType = ((TypeVariable)contextType).getUpperBound();
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
		return new GenericWebExchangeContextParameterInfo(parameterQName, reporter, parameterElement, contextType);
	}
	
	/**
	 * <p>
	 * Creates a server-sent event factory parameter info.
	 * </p>
	 * 
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * 
	 * @return a web server-sent event factory parameter info
	 */
	private GenericWebSseEventFactoryParameterInfo createSseEventFactoryParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		if(this.pluginContext.getTypeUtils().isSameType(this.sseEncoderEventFactoryType, this.pluginContext.getTypeUtils().erasure(parameterElement.asType()))) {
			return new GenericWebSseEventFactoryParameterInfo(parameterQName, reporter, parameterElement, ((DeclaredType)parameterElement.asType()).getTypeArguments().get(0), SseEventFactoryKind.ENCODED, parameterElement.getAnnotation(SseEventFactory.class).value());
		}
		else if(this.pluginContext.getTypeUtils().isSameType(this.sseEventFactoryType, this.pluginContext.getTypeUtils().erasure(parameterElement.asType()))) {
			TypeMirror sseEventType = ((DeclaredType)parameterElement.asType()).getTypeArguments().get(0);
			
			SseEventFactoryKind sseFactoryType = SseEventFactoryKind.RAW;
			if(this.pluginContext.getTypeUtils().isSameType(sseEventType, this.byteBufType)) {
				sseFactoryType = SseEventFactoryKind.RAW;
			}
			else if(this.pluginContext.getTypeUtils().isAssignable(sseEventType, this.charSequenceType)) {
				sseFactoryType = SseEventFactoryKind.CHARSEQUENCE;
			}
			else {
				reporter.error("Unsupported server-sent event type: " + sseEventType);
			}
			return new GenericWebSseEventFactoryParameterInfo(parameterQName, reporter, parameterElement, sseEventType, sseFactoryType, parameterElement.getAnnotation(SseEventFactory.class).value());
		}
		else {
			reporter.error("Invalid SSE event factory parameter which must be of type " + this.sseEventFactoryType + " or " + this.sseEncoderEventFactoryType);
			return new GenericWebSseEventFactoryParameterInfo(parameterQName, reporter, parameterElement, this.byteBufType, SseEventFactoryKind.RAW);
		}
	}
	
	/**
	 * <p>
	 * Creates a form parameter info.
	 * </p>
	 * 
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false
	 *                         otherwise
	 * 
	 * @return a web form parameter info
	 */
	private GenericWebFormParameterInfo createFormParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebFormParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}
	
	/**
	 * <p>
	 * Creates a header parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false otherwise
	 *
	 * @return a web header parameter info
	 */
	private GenericWebHeaderParameterInfo createHeaderParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebHeaderParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}
	
	/**
	 * <p>
	 * Creates a path parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false otherwise
	 *
	 * @return a web path parameter info
	 */
	private GenericWebPathParameterInfo createPathParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebPathParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}
	
	/**
	 * <p>
	 * Creates a query parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 * @param required         true to indicate a required parameter, false otherwise
	 *
	 * @return a web query parameter info
	 */
	private GenericWebQueryParameterInfo createQueryParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebQueryParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}
	
	/**
	 * <p>
	 * Creates a web request body parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 *
	 * @return a request body parameter info
	 */
	private GenericWebRequestBodyParameterInfo createRequestBodyParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType) {
		TypeMirror requestBodyType = parameterType;
		TypeMirror erasedRequestBodyType = this.pluginContext.getTypeUtils().erasure(requestBodyType);
		
		RequestBodyReactiveKind requestBodyReactiveKind = null;
		if(this.pluginContext.getTypeUtils().isSameType(erasedRequestBodyType, this.publisherType)) {
			requestBodyReactiveKind = RequestBodyReactiveKind.PUBLISHER;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedRequestBodyType, this.monoType)) {
			requestBodyReactiveKind = RequestBodyReactiveKind.ONE;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedRequestBodyType, this.fluxType)) {
			requestBodyReactiveKind = RequestBodyReactiveKind.MANY;
		}
		else {
			requestBodyReactiveKind = RequestBodyReactiveKind.NONE;
		}
		
		RequestBodyKind requestBodyKind = RequestBodyKind.ENCODED;
		if(requestBodyReactiveKind != RequestBodyReactiveKind.NONE) {
			requestBodyType = ((DeclaredType)requestBodyType).getTypeArguments().get(0);
			if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.byteBufType)) {
				requestBodyKind = RequestBodyKind.RAW;
			}
			else if(this.pluginContext.getTypeUtils().isAssignable(this.webPartType, requestBodyType)) {
				requestBodyKind = RequestBodyKind.MULTIPART;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.parameterType)) {
				requestBodyKind = RequestBodyKind.URLENCODED;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.voidType)) {
				reporter.error("Request body publisher can't have type argument void");
			}
		}
		else if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.byteBufType)) {
			requestBodyKind = RequestBodyKind.RAW;
		}
		return new GenericWebRequestBodyParameterInfo(parameterQName, reporter, parameterElement, requestBodyType, requestBodyKind, requestBodyReactiveKind);
	}
	
	/**
	 * <p>
	 * Creates a WebSocket exchange parameter info.
	 * </p>
	 * 
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * 
	 * @return a WebSocket exchange parameter info
	 */
	private GenericWebSocketExchangeParameterInfo createWebSocketExchangeParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		TypeMirror contextType = this.exchangeContextType;
		List<? extends TypeMirror> typeArguments = ((DeclaredType)parameterElement.asType()).getTypeArguments();
		if(!typeArguments.isEmpty()) {
			contextType = typeArguments.get(0);
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
			reporter.error("Web exchange context must be an interface");
		}
		return new GenericWebSocketExchangeParameterInfo(parameterQName, reporter, parameterElement, contextType);
	}
	
	/**
	 * <p>
	 * Creates a WebSocket inbound parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 *
	 * @return a WebSocket inbound parameter info
	 */
	private GenericWebSocketInboundParameterInfo createWebSocketInboundParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		return new GenericWebSocketInboundParameterInfo(parameterQName, reporter, parameterElement, this.web2SocketExchangeInboundType);
	}
	
	/**
	 * <p>
	 * Creates a WebSocket inbound publisher parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 *
	 * @return a WebSocket inbound publisher parameter info
	 */
	private GenericWebSocketInboundPublisherParameterInfo createWebSocketInboundPublisherParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType) {
		
		// 1. determine reactive kind
		// 2. determine kind
		
		WebSocketBoundPublisherInfo.BoundReactiveKind inboundReactiveKind = null;
		TypeMirror erasedParameterType = this.pluginContext.getTypeUtils().erasure(parameterType);
		if(this.pluginContext.getTypeUtils().isSameType(erasedParameterType, this.publisherType)) {
			inboundReactiveKind = WebSocketBoundPublisherInfo.BoundReactiveKind.PUBLISHER;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedParameterType, this.monoType)) {
			inboundReactiveKind = WebSocketBoundPublisherInfo.BoundReactiveKind.ONE;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(erasedParameterType, this.fluxType)) {
			inboundReactiveKind = WebSocketBoundPublisherInfo.BoundReactiveKind.MANY;
		}
		else {
			// should never happen, this is checked in createWebSocketRoute()
			throw new IllegalStateException("Unexpected inbound publisher type");
		}
		
		// We have an outbound publisher as return type
		WebSocketBoundPublisherInfo.BoundKind inboundKind = WebSocketBoundPublisherInfo.BoundKind.ENCODED;
		TypeMirror boundType = ((DeclaredType)parameterType).getTypeArguments().get(0);
		if(this.pluginContext.getTypeUtils().isSameType(boundType, this.voidType)) {
			inboundKind = WebSocketBoundPublisherInfo.BoundKind.EMPTY;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(boundType, this.byteBufType)) {
			inboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED;
		}
		else if(this.pluginContext.getTypeUtils().isAssignable(boundType, this.charSequenceType)) {
			inboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED;
		}
		else if(boundType instanceof DeclaredType && ((DeclaredType)boundType).getTypeArguments().size() == 1) {
			// maybe we have a reactive message payload
			TypeMirror erasedBoundType = this.pluginContext.getTypeUtils().erasure(boundType);
			TypeMirror nextBoundType = ((DeclaredType)boundType).getTypeArguments().get(0);
			if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.publisherType)) {
				if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_PUBLISHER;
				}
				else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_PUBLISHER;
				}
			}
			else if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.fluxType)) {
				if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_MANY;
				}
				else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_MANY;
				}
			}
			else if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.monoType)) {
				if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED_ONE;
				}
				else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED_ONE;
				}
			}
		}
		return new GenericWebSocketInboundPublisherParameterInfo(parameterQName, reporter, parameterElement, boundType, inboundKind, inboundReactiveKind);
	}
	
	/**
	 * <p>
	 * Creates a WebSocket outbound parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 *
	 * @return a WebSocket outbound parameter info
	 */
	private GenericWebSocketOutboundParameterInfo createWebSocketOutboundParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		return new GenericWebSocketOutboundParameterInfo(parameterQName, reporter, parameterElement, this.web2SocketExchangeOutboundType);
	}
}
