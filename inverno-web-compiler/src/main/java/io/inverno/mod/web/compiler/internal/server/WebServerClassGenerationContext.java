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
package io.inverno.mod.web.compiler.internal.server;

import io.inverno.core.compiler.spi.ModuleQualifiedName;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.web.compiler.internal.AbstractWebSourceGenerationContext;
import io.inverno.mod.web.compiler.spi.WebBasicParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerControllerInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerModuleInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerInboundPublisherParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebSocketServerRouteInfo;
import io.inverno.mod.web.base.MissingRequiredParameterException;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.server.annotation.WebSocketRoute;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Web server class generation context.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebServerClassGenerationContext extends AbstractWebSourceGenerationContext<WebServerClassGenerationContext, WebServerClassGenerationContext.GenerationMode> {

	public enum GenerationMode {
		BOOT_SERVER_CLASS,
		SERVER_CLASS,
		WEB_ROUTES_ANNOTATION,
		SERVER_CONTEXT,
		SERVER_CONTEXT_IMPL,
		CONTROLLER_FIELD,
		CONTROLLER_PARAMETER,
		CONTROLLER_ASSIGNMENT,
		ROUTE_ANNOTATION,
		ROUTE_DECLARATION,
		ROUTE_HANDLER_CLASS,
		ROUTE_HANDLER_TYPE,
		ROUTE_PARAMETER_REFERENCE_CLASS,
		ROUTE_PARAMETER_REFERENCE_TYPE,
		ROUTE_PARAMETER_REFERENCE_ONE,
		ROUTE_PARAMETER_REFERENCE_MANY,
		WEBSOCKET_ROUTE_ANNOTATION,
		WEBSOCKET_ROUTE_HANDLER_CLASS,
		WEBSOCKET_ROUTE_HANDLER_TYPE
	}

	private final TypeMirror optionalType;
	private final TypeMirror collectorsType;
	private final TypeMirror webRouteAnnotationType;
	private final TypeMirror webExchangeType;
	private final TypeMirror methodType;
	private final TypeMirror missingRequiredParameterExceptionType;
	private final TypeMirror publisherType;
	private final TypeMirror monoType;
	private final TypeMirror fluxType;
	private final TypeMirror parameterType;
	private final TypeMirror unpooledType;
	private final TypeMirror byteBufType;
	private final TypeMirror webSocketRouteAnnotationType;
	private final TypeMirror webSocketMessageKindType;
	private final TypeMirror webSocketExchangeInboundType;
	private final TypeMirror webSocketExchangeOutboundType;
	private final TypeMirror webSocketMessageType;
	private final TypeMirror webSocketFrameType;
	private final TypeMirror charSequenceType;
	private final TypeMirror stringType;

	private WebServerModuleInfo webServerModuleInfo;
	private WebServerControllerInfo webServerControllerInfo;
	private WebServerRouteInfo webServerRouteInfo;
	private Integer parameterIndex;

	/**
	 * <p>
	 * Creates a Web server class generation context.
	 * </p>
	 *
	 * @param typeUtils    the type utils
	 * @param elementUtils the element utils
	 * @param mode         the generation mode
	 */
	public WebServerClassGenerationContext(Types typeUtils, Elements elementUtils, GenerationMode mode) {
		super(typeUtils, elementUtils, mode);

		this.optionalType = typeUtils.erasure(elementUtils.getTypeElement(Optional.class.getCanonicalName()).asType());
		this.collectorsType = elementUtils.getTypeElement(Collectors.class.getCanonicalName()).asType();
		this.webRouteAnnotationType = elementUtils.getTypeElement(WebRoute.class.getCanonicalName()).asType();
		this.webExchangeType = elementUtils.getTypeElement(WebExchange.class.getCanonicalName()).asType();
		this.methodType = elementUtils.getTypeElement(Method.class.getCanonicalName()).asType();
		this.missingRequiredParameterExceptionType = elementUtils.getTypeElement(MissingRequiredParameterException.class.getCanonicalName()).asType();
		this.publisherType = typeUtils.erasure(elementUtils.getTypeElement(Publisher.class.getCanonicalName()).asType());
		this.monoType = typeUtils.erasure(elementUtils.getTypeElement(Mono.class.getCanonicalName()).asType());
		this.fluxType = typeUtils.erasure(elementUtils.getTypeElement(Flux.class.getCanonicalName()).asType());
		this.parameterType = elementUtils.getTypeElement(Parameter.class.getCanonicalName()).asType();
		this.unpooledType = elementUtils.getTypeElement(Unpooled.class.getCanonicalName()).asType();
		this.byteBufType = elementUtils.getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.webSocketRouteAnnotationType = elementUtils.getTypeElement(WebSocketRoute.class.getCanonicalName()).asType();
		this.webSocketMessageKindType = elementUtils.getTypeElement(WebSocketMessage.Kind.class.getCanonicalName()).asType();
		this.webSocketExchangeInboundType = elementUtils.getTypeElement(WebSocketExchange.Inbound.class.getCanonicalName()).asType();
		this.webSocketExchangeOutboundType = elementUtils.getTypeElement(WebSocketExchange.Outbound.class.getCanonicalName()).asType();
		this.webSocketMessageType = elementUtils.getTypeElement(WebSocketMessage.class.getCanonicalName()).asType();
		this.webSocketFrameType = elementUtils.getTypeElement(WebSocketFrame.class.getCanonicalName()).asType();
		this.charSequenceType = elementUtils.getTypeElement(CharSequence.class.getCanonicalName()).asType();
		this.stringType  = elementUtils.getTypeElement(String.class.getCanonicalName()).asType();
	}

	/**
	 * <p>
	 * Creates a Web server class generation context from a parent generation context.
	 * </p>
	 *
	 * @param parentGeneration the parent context
	 */
	private WebServerClassGenerationContext(WebServerClassGenerationContext parentGeneration) {
		super(parentGeneration);

		this.webServerModuleInfo = parentGeneration.webServerModuleInfo;
		this.webServerControllerInfo = parentGeneration.webServerControllerInfo;
		this.webServerRouteInfo = parentGeneration.webServerRouteInfo;
		this.parameterIndex = parentGeneration.parameterIndex;

		this.optionalType = parentGeneration.optionalType;
		this.collectorsType = parentGeneration.collectorsType;
		this.webRouteAnnotationType = parentGeneration.webRouteAnnotationType;
		this.webExchangeType = parentGeneration.webExchangeType;
		this.methodType = parentGeneration.methodType;
		this.missingRequiredParameterExceptionType = parentGeneration.missingRequiredParameterExceptionType;
		this.publisherType = parentGeneration.publisherType;
		this.monoType = parentGeneration.monoType;
		this.fluxType = parentGeneration.fluxType;
		this.parameterType = parentGeneration.parameterType;
		this.unpooledType = parentGeneration.unpooledType;
		this.byteBufType = parentGeneration.byteBufType;
		this.webSocketRouteAnnotationType = parentGeneration.webSocketRouteAnnotationType;
		this.webSocketMessageKindType = parentGeneration.webSocketMessageKindType;
		this.webSocketExchangeInboundType = parentGeneration.webSocketExchangeInboundType;
		this.webSocketExchangeOutboundType = parentGeneration.webSocketExchangeOutboundType;
		this.webSocketMessageType = parentGeneration.webSocketMessageType;
		this.webSocketFrameType = parentGeneration.webSocketFrameType;
		this.charSequenceType = parentGeneration.charSequenceType;
		this.stringType = parentGeneration.stringType;
	}

	@Override
	public WebServerClassGenerationContext withMode(GenerationMode mode) {
		WebServerClassGenerationContext context = new WebServerClassGenerationContext(this);
		context.mode = mode;
		return context;
	}

	@Override
	public WebServerClassGenerationContext withIndentDepth(int indentDepth) {
		WebServerClassGenerationContext context = new WebServerClassGenerationContext(this);
		context.indentDepth = indentDepth;
		return context;
	}

	@Override
	public WebServerClassGenerationContext withModule(ModuleQualifiedName moduleQualifiedName) {
		WebServerClassGenerationContext context = new WebServerClassGenerationContext(this);
		context.moduleQualifiedName = moduleQualifiedName;
		return context;
	}

	public WebServerClassGenerationContext withParameterIndex(int parameterIndex) {
		WebServerClassGenerationContext context = new WebServerClassGenerationContext(this);
		context.parameterIndex = parameterIndex;
		return context;
	}

	public WebServerClassGenerationContext withWebServerModuleInfo(WebServerModuleInfo webServerModuleInfo) {
		WebServerClassGenerationContext context = new WebServerClassGenerationContext(this);
		context.webServerModuleInfo = webServerModuleInfo;
		return context;
	}

	public WebServerClassGenerationContext withWebServerControllerInfo(WebServerControllerInfo webServerControllerInfo) {
		WebServerClassGenerationContext context = new WebServerClassGenerationContext(this);
		context.webServerControllerInfo = webServerControllerInfo;
		return context;
	}

	public WebServerClassGenerationContext withWebServerRouteInfo(WebServerRouteInfo webServerRouteInfo) {
		WebServerClassGenerationContext context = new WebServerClassGenerationContext(this);
		context.webServerRouteInfo = webServerRouteInfo;
		return context;
	}

	public WebServerModuleInfo getWebServerModuleInfo() {
		return webServerModuleInfo;
	}

	public WebServerControllerInfo getWebServerControllerInfo() {
		return webServerControllerInfo;
	}

	public WebServerRouteInfo getWebServerRouteInfo() {
		return webServerRouteInfo;
	}

	public int getTypeIndex(TypeMirror type) {
		for(int i=0;i<this.webServerModuleInfo.getTypesRegistry().length;i++) {
			if(this.typeUtils.isSameType(type, this.webServerModuleInfo.getTypesRegistry()[i])) {
				return i;
			}
		}
		throw new IllegalArgumentException();
	}

	public String getCollectionTypeName() {
		return this.getTypeName(this.getCollectionType());
	}

	public String getListTypeName() {
		return this.getTypeName(this.getListType());
	}

	public String getSetTypeName() {
		return this.getTypeName(this.getSetType());
	}

	public TypeMirror getOptionalType() {
		return optionalType;
	}

	public String getOptionalTypeName() {
		return this.getTypeName(this.optionalType);
	}

	public TypeMirror getCollectorsType() {
		return collectorsType;
	}

	public String getCollectorsTypeName() {
		return this.getTypeName(this.collectorsType);
	}

	public TypeMirror getWebRouteAnnotationType() {
		return webRouteAnnotationType;
	}

	public String getWebRouteAnnotationTypeName() {
		return this.getTypeName(this.webRouteAnnotationType);
	}

	public TypeMirror getWebExchangeType() {
		return webExchangeType;
	}

	public String getWebExchangeTypeName() {
		return this.getTypeName(this.webExchangeType);
	}

	public TypeMirror getMethodType() {
		return methodType;
	}

	public String getMethodTypeName() {
		return this.getTypeName(this.methodType);
	}

	public TypeMirror getMissingRequiredParameterExceptionType() {
		return missingRequiredParameterExceptionType;
	}

	public String getMissingRequiredParameterExceptionTypeName() {
		return this.getTypeName(this.missingRequiredParameterExceptionType);
	}

	public TypeMirror getPublisherType() {
		return publisherType;
	}

	public String getPublisherTypeName() {
		return this.getTypeName(this.publisherType);
	}

	public TypeMirror getMonoType() {
		return monoType;
	}

	public String getMonoTypeName() {
		return this.getTypeName(this.monoType);
	}

	public TypeMirror getFluxType() {
		return fluxType;
	}

	public String getFluxTypeName() {
		return this.getTypeName(this.fluxType);
	}

	public TypeMirror getParameterType() {
		return parameterType;
	}

	public String getParameterTypeName() {
		return this.getTypeName(this.parameterType);
	}

	public TypeMirror getUnpooledType() {
		return unpooledType;
	}

	public String getUnpooledTypeName() {
		return this.getTypeName(this.unpooledType);
	}

	public String getTypeTypeName() {
		return this.getTypeName(this.getTypeType());
	}

	public String getTypesTypeName() {
		return this.getTypeName(this.getTypesType());
	}

	public String getVoidTypeName() {
		return this.getTypeName(this.getVoidType());
	}

	public TypeMirror getByteBufType() {
		return byteBufType;
	}

	public String getByteBufTypeName() {
		return this.getTypeName(this.byteBufType);
	}

	public TypeMirror getWebSocketRouteAnnotationType() {
		return webSocketRouteAnnotationType;
	}

	public String getWebSocketRouteAnnotationTypeName() {
		return this.getTypeName(this.webSocketRouteAnnotationType);
	}

	public TypeMirror getWebSocketMessageKindType() {
		return webSocketMessageKindType;
	}

	public String getWebSocketMessageKindTypeName() {
		return this.getTypeName(this.webSocketMessageKindType);
	}

	public TypeMirror getWebSocketExchangeInboundType() {
		return webSocketExchangeInboundType;
	}

	public String getWebSocketExchangeInboundTypeName() {
		return this.getTypeName(this.webSocketExchangeInboundType);
	}

	public TypeMirror getWebSocketExchangeOutboundType() {
		return webSocketExchangeOutboundType;
	}

	public String getWebSocketExchangeOutboundTypeName() {
		return this.getTypeName(this.webSocketExchangeOutboundType);
	}

	public TypeMirror getWebSocketMessageType() {
		return webSocketMessageType;
	}

	public String getWebSocketMessageTypeName() {
		return this.getTypeName(this.webSocketMessageType);
	}

	public TypeMirror getWebSocketFrameType() {
		return webSocketFrameType;
	}

	public String getWebSocketFrameTypeName() {
		return this.getTypeName(this.webSocketFrameType);
	}

	public TypeMirror getCharSequenceType() {
		return charSequenceType;
	}

	public String getCharSequenceTypeName() {
		return this.getTypeName(this.charSequenceType);
	}

	public TypeMirror getStringType() {
		return stringType;
	}

	public String getStringTypeName() {
		return this.getTypeName(this.stringType);
	}

	public Integer getParameterIndex() {
		return parameterIndex;
	}

	public TypeMirror getParameterConverterType(TypeMirror type) {
		if(this.isArrayType(type)) {
			return ((ArrayType)type).getComponentType();
		}
		else if(isCollectionType(type)) {
			return ((DeclaredType)type).getTypeArguments().getFirst();
		}
		return type;
	}

	public boolean isReactiveType(TypeMirror type) {
		if(type.getKind() == TypeKind.VOID) {
			return false;
		}

		TypeMirror erasedType = this.typeUtils.erasure(type);
		return this.typeUtils.isSameType(erasedType, this.getPublisherType()) ||
			this.typeUtils.isSameType(erasedType, this.getMonoType()) ||
			this.typeUtils.isSameType(erasedType, this.getFluxType());
	}

	public boolean isTypeMode(WebServerRouteInfo routeInfo) {
		return (routeInfo.getResponseBody().getType().getKind() != TypeKind.VOID && !this.isClassType(routeInfo.getResponseBody().getType()))
			|| Arrays.stream(routeInfo.getParameters())
			.filter(parameter -> !(parameter instanceof WebExchangeParameterInfo))
			.anyMatch(parameter -> {
				if(parameter instanceof WebRequestBodyParameterInfo) {
					return !this.isClassType(parameter.getType());
				}
				else {
					return !this.isClassType(this.getParameterConverterType(parameter.getType()));
				}
			});
	}

	public boolean isTypeMode(WebSocketServerRouteInfo routeInfo) {
		return routeInfo.getOutboundPublisher()
			.map(outboundPublisherInfo -> outboundPublisherInfo.getType().getKind() != TypeKind.VOID && !this.isClassType(outboundPublisherInfo.getType()))
			.orElse(false)
			|| Arrays.stream(routeInfo.getParameters()).anyMatch(parameter -> {
				if(parameter instanceof WebSocketServerInboundPublisherParameterInfo) {
					return !this.isClassType(parameter.getType());
				}
				else if(parameter instanceof WebBasicParameterInfo) {
					return !this.isClassType(this.getParameterConverterType(parameter.getType()));
				}
				return false;
			});
	}
}
