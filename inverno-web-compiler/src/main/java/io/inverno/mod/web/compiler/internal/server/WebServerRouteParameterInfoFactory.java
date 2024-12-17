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

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.base.annotation.FormParam;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.internal.GenericWebExchangeParameterInfo;
import io.inverno.mod.web.compiler.internal.GenericWebFormParameterInfo;
import io.inverno.mod.web.compiler.internal.GenericWebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.internal.GenericWebSseEventFactoryParameterInfo;
import io.inverno.mod.web.compiler.spi.RequestBodyKind;
import io.inverno.mod.web.compiler.spi.RequestBodyReactiveKind;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import io.inverno.mod.web.compiler.spi.server.WebSseEventFactoryParameterInfo.SseEventFactoryKind;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebPart;
import io.inverno.mod.web.server.WebResponseBody;
import io.inverno.mod.web.server.annotation.SseEventFactory;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;
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
 * Factory used to create Web server route parameter info.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebParameterInfo
 */
public class WebServerRouteParameterInfoFactory extends AbstractWebServerRouteParameterInfoFactory {

	/* Web annotations */
	private final TypeMirror formParameterAnnotationType;
	private final TypeMirror bodyAnnotationType;
	private final TypeMirror sseEventFactoryAnnotationType;
	
	/* Contextual */
	private final TypeMirror webExchangeType;
	private final TypeMirror sseEventFactoryType;
	private final TypeMirror sseEncoderEventFactoryType;

	/* Types */
	private final TypeMirror monoType;
	private final TypeMirror fluxType;
	private final TypeMirror voidType;
	private final TypeMirror publisherType;
	private final TypeMirror byteBufType;
	private final TypeMirror charSequenceType;
	private final TypeMirror stringType;
	private final TypeMirror webPartType;
	private final TypeMirror parameterType;

	/**
	 * <p>
	 * Creates a Web server route parameter info factory.
	 * </p>
	 * 
	 * @param pluginContext   the Web compiler plugin context
	 * @param pluginExecution the Web compiler plugin execution
	 */
	public WebServerRouteParameterInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		super(pluginContext, pluginExecution);

		this.formParameterAnnotationType = this.pluginContext.getElementUtils().getTypeElement(FormParam.class.getCanonicalName()).asType();
		this.bodyAnnotationType = this.pluginContext.getElementUtils().getTypeElement(Body.class.getCanonicalName()).asType();
		this.sseEventFactoryAnnotationType = this.pluginContext.getElementUtils().getTypeElement(SseEventFactory.class.getCanonicalName()).asType();
		
		this.monoType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Mono.class.getCanonicalName()).asType());
		this.fluxType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Flux.class.getCanonicalName()).asType());
		this.voidType = this.pluginContext.getElementUtils().getTypeElement(Void.class.getCanonicalName()).asType();
		this.publisherType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Publisher.class.getCanonicalName()).asType());
		this.byteBufType = this.pluginContext.getElementUtils().getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.charSequenceType = this.pluginContext.getElementUtils().getTypeElement(CharSequence.class.getCanonicalName()).asType();
		this.stringType = this.pluginContext.getElementUtils().getTypeElement(String.class.getCanonicalName()).asType();
		this.webPartType = this.pluginContext.getElementUtils().getTypeElement(WebPart.class.getCanonicalName()).asType();
		this.parameterType = this.pluginContext.getElementUtils().getTypeElement(Parameter.class.getCanonicalName()).asType();
		
		this.webExchangeType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebExchange.class.getCanonicalName()).asType());
		this.sseEventFactoryType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(ResponseBody.Sse.EventFactory.class.getCanonicalName()).asType());
		this.sseEncoderEventFactoryType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebResponseBody.SseEncoder.EventFactory.class.getCanonicalName()).asType());
	}

	@Override
	protected AbstractWebParameterInfo createContextualParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType) {
		if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.webExchangeType)) {
			return this.createExchangeParameter(reporter, parameterQName, parameterElement);
		}
		return super.createContextualParameter(reporter, parameterQName, parameterElement, parameterType);
	}

	@Override
	protected AbstractWebParameterInfo createParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, AnnotationMirror annotation, Set<String> consumes, Set<String> produces, boolean required) {
		if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.formParameterAnnotationType)) {
			return this.createFormParameter(reporter, parameterQName, parameterElement, parameterType, required);
		}
		else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.bodyAnnotationType)) {
			if (!required) {
				reporter.error("Request body parameter can't be optional");
			}
			else {
				return this.createRequestBodyParameter(reporter, parameterQName, parameterElement, parameterType, consumes, produces);
			}
		}
		else if(this.pluginContext.getTypeUtils().isSameType(annotation.getAnnotationType(), this.sseEventFactoryAnnotationType)) {
			if (!required) {
				reporter.error("SSE event factory parameter can't be optional");
			} else {
				return this.createSseEventFactoryParameter(reporter, parameterQName, parameterElement);
			}
		}
		return null;
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
	 * @return a Web exchange parameter info
	 */
	private GenericWebExchangeParameterInfo createExchangeParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		TypeMirror contextType = this.exchangeContextType;
		List<? extends TypeMirror> typeArguments = ((DeclaredType)parameterElement.asType()).getTypeArguments();
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
			reporter.error("Web exchange context must be an interface");
		}
		return new GenericWebExchangeParameterInfo(parameterQName, reporter, parameterElement, parameterElement.asType(), contextType);
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
	 * @return a Web form parameter info
	 */
	private GenericWebFormParameterInfo createFormParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, boolean required) {
		return new GenericWebFormParameterInfo(parameterQName, reporter, parameterElement, parameterType, required);
	}

	/**
	 * <p>
	 * Creates a Web request body parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 * @param parameterType    the parameter type
	 *
	 * @return a request body parameter info
	 */
	private GenericWebRequestBodyParameterInfo createRequestBodyParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, Set<String> consumes, Set<String> produces) {
		TypeMirror requestBodyType = parameterType;
		TypeMirror erasedRequestBodyType = this.pluginContext.getTypeUtils().erasure(requestBodyType);
		
		RequestBodyReactiveKind requestBodyReactiveKind;
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
			requestBodyType = ((DeclaredType)requestBodyType).getTypeArguments().getFirst();
			if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.byteBufType)) {
				requestBodyKind = RequestBodyKind.RAW;
			}
			else if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.charSequenceType) || this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.stringType)) {
				if(consumes.isEmpty()) {
					requestBodyKind = RequestBodyKind.CHARSEQUENCE;
				}
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
		else if(this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.charSequenceType) || this.pluginContext.getTypeUtils().isSameType(requestBodyType, this.stringType)) {
			if(consumes.isEmpty()) {
				requestBodyKind = RequestBodyKind.CHARSEQUENCE;
			}
		}
		return new GenericWebRequestBodyParameterInfo(parameterQName, reporter, parameterElement, requestBodyType, requestBodyKind, requestBodyReactiveKind);
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
	 * @return a Web server-sent event factory parameter info
	 */
	private GenericWebSseEventFactoryParameterInfo createSseEventFactoryParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		if(this.pluginContext.getTypeUtils().isSameType(this.sseEncoderEventFactoryType, this.pluginContext.getTypeUtils().erasure(parameterElement.asType()))) {
			return new GenericWebSseEventFactoryParameterInfo(parameterQName, reporter, parameterElement, ((DeclaredType)parameterElement.asType()).getTypeArguments().getFirst(), SseEventFactoryKind.ENCODED, parameterElement.getAnnotation(SseEventFactory.class).value());
		}
		else if(this.pluginContext.getTypeUtils().isSameType(this.sseEventFactoryType, this.pluginContext.getTypeUtils().erasure(parameterElement.asType()))) {
			TypeMirror sseEventType = ((DeclaredType)parameterElement.asType()).getTypeArguments().getFirst();

			SseEventFactoryKind sseFactoryType = SseEventFactoryKind.RAW;
			if(this.pluginContext.getTypeUtils().isAssignable(sseEventType, this.charSequenceType)) {
				sseFactoryType = SseEventFactoryKind.CHARSEQUENCE;
			}
			else if(!this.pluginContext.getTypeUtils().isSameType(sseEventType, this.byteBufType)) {
				reporter.error("Unsupported server-sent event type: " + sseEventType);
			}
			return new GenericWebSseEventFactoryParameterInfo(parameterQName, reporter, parameterElement, sseEventType, sseFactoryType, parameterElement.getAnnotation(SseEventFactory.class).value());
		}
		else {
			reporter.error("Invalid SSE event factory parameter which must be of type " + this.sseEventFactoryType + " or " + this.sseEncoderEventFactoryType);
			return new GenericWebSseEventFactoryParameterInfo(parameterQName, reporter, parameterElement, this.byteBufType, SseEventFactoryKind.RAW);
		}
	}
}
