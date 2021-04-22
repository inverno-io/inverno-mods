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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.winterframework.core.compiler.spi.ReporterInfo;
import io.winterframework.core.compiler.spi.plugin.PluginContext;
import io.winterframework.core.compiler.spi.plugin.PluginExecution;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.server.ResponseBody;
import io.winterframework.mod.web.WebExchange;
import io.winterframework.mod.web.WebPart;
import io.winterframework.mod.web.WebResponseBody;
import io.winterframework.mod.web.annotation.Body;
import io.winterframework.mod.web.annotation.CookieParam;
import io.winterframework.mod.web.annotation.FormParam;
import io.winterframework.mod.web.annotation.HeaderParam;
import io.winterframework.mod.web.annotation.PathParam;
import io.winterframework.mod.web.annotation.QueryParam;
import io.winterframework.mod.web.annotation.SseEventFactory;
import io.winterframework.mod.web.annotation.WebRoute;
import io.winterframework.mod.web.compiler.spi.WebParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebParameterQualifiedName;
import io.winterframework.mod.web.compiler.spi.WebRouteQualifiedName;
import io.winterframework.mod.web.compiler.spi.WebRequestBodyParameterInfo.RequestBodyKind;
import io.winterframework.mod.web.compiler.spi.WebRequestBodyParameterInfo.RequestBodyReactiveKind;
import io.winterframework.mod.web.compiler.spi.WebSseEventFactoryParameterInfo.SseEventFactoryKind;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A web parameter info factory is used to create web parameter info.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
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
	private final TypeMirror rawSseEventFactoryType;
	private final TypeMirror sseEncoderEventFactoryType;
	
	/* Types */
	private final TypeMirror optionalType;
	private final TypeMirror monoType;
	private final TypeMirror fluxType;
	private final TypeMirror publisherType;
	private final TypeMirror byteBufType;
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
		this.publisherType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Publisher.class.getCanonicalName()).asType());
		this.byteBufType = this.pluginContext.getElementUtils().getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.webPartType = this.pluginContext.getElementUtils().getTypeElement(WebPart.class.getCanonicalName()).asType();
		this.parameterType = this.pluginContext.getElementUtils().getTypeElement(Parameter.class.getCanonicalName()).asType();
		
		this.webExchangeType = this.pluginContext.getElementUtils().getTypeElement(WebExchange.class.getCanonicalName()).asType();
		TypeMirror rawSseEventType = this.pluginContext.getTypeUtils().getDeclaredType(this.pluginContext.getElementUtils().getTypeElement(ResponseBody.Sse.Event.class.getCanonicalName()), this.byteBufType);
		this.rawSseEventFactoryType = this.pluginContext.getTypeUtils().getDeclaredType(this.pluginContext.getElementUtils().getTypeElement(ResponseBody.Sse.EventFactory.class.getCanonicalName()), this.byteBufType, rawSseEventType);
		this.sseEncoderEventFactoryType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebResponseBody.SseEncoder.EventFactory.class.getCanonicalName()).asType());
	}
	
	/**
	 * <p>
	 * Creates a web parameter from a parameter element.
	 * </p>
	 * 
	 * @param routeQName                the qualified name of the route for which
	 *                                  the parameter is defined
	 * @param parameterElement          the variable element of the parameter
	 * @param annotatedParameterElement the variable element of the parameter in the
	 *                                  method actually annotated with
	 *                                  {@link WebRoute @WebRoute} annotation
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

		ReporterInfo parameterReporter = this.pluginExecution.getReporter(parameterElement);
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
			if(this.pluginContext.getTypeUtils().isAssignable(this.webExchangeType, parameterElement.asType())) {
				result = this.createExchangeParameter(parameterReporter, parameterQName, parameterElement);
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
	 * <p>Creates an exchange parameter info.</p>
	 * 
	 * @param reporter the parameter reporter
	 * @param parameterQName the parameter qualified name
	 * @param parameterElement the parameter element
	 * 
	 * @return a web exchange parameter info
	 */
	private GenericWebExchangeParameterInfo createExchangeParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		return new GenericWebExchangeParameterInfo(parameterQName, reporter, parameterElement);
	}
	
	/**
	 * <p>Creates a server-sent event factory parameter info.</p>
	 * 
	 * @param reporter the parameter reporter
	 * @param parameterQName the parameter qualified name
	 * @param parameterElement the parameter element
	 * 
	 * @return a web server-sent event factory parameter info
	 */
	private GenericWebSseEventFactoryParameterInfo createSseEventFactoryParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		if(this.pluginContext.getTypeUtils().isSameType(this.sseEncoderEventFactoryType, this.pluginContext.getTypeUtils().erasure(parameterElement.asType()))) {
			return new GenericWebSseEventFactoryParameterInfo(parameterQName, reporter, parameterElement, ((DeclaredType)parameterElement.asType()).getTypeArguments().get(0), SseEventFactoryKind.ENCODED, parameterElement.getAnnotation(SseEventFactory.class).value());
		}
		else if(this.pluginContext.getTypeUtils().isSameType(this.rawSseEventFactoryType, parameterElement.asType())) {
			return new GenericWebSseEventFactoryParameterInfo(parameterQName, reporter, parameterElement, this.byteBufType, SseEventFactoryKind.RAW, parameterElement.getAnnotation(SseEventFactory.class).value());
		}
		else {
			reporter.error("Invalid SSE event factory parameter which must be of type " + this.rawSseEventFactoryType + " or " + this.sseEncoderEventFactoryType);
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
	 * @param required         true to indicate a required parameter, false
	 *                         otherwise
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
	 * @param required         true to indicate a required parameter, false
	 *                         otherwise
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
	 * @param required         true to indicate a required parameter, false
	 *                         otherwise
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
	 * @param reporter            the parameter reporter
	 * @param parameterQName      the parameter qualified name
	 * @param parameterElementthe parameter element
	 * @param parameterType       the parameter type
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
		}
		else if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.byteBufType)) {
			requestBodyKind = RequestBodyKind.RAW;
		}
		return new GenericWebRequestBodyParameterInfo(parameterQName, reporter, parameterElement, requestBodyType, requestBodyKind, requestBodyReactiveKind);
	}
}
