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
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Mutator;
import io.inverno.core.annotation.Wrapper;
import io.inverno.core.compiler.spi.ModuleQualifiedName;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.web.client.WebClient;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.client.WebRouteInterceptor;
import io.inverno.mod.web.client.ws.Web2SocketExchange;
import io.inverno.mod.web.compiler.internal.AbstractWebSourceGenerationContext;
import io.inverno.mod.web.compiler.spi.client.WebClientModuleInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientStubInfo;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Web client class generation context.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebClientClassGenerationContext extends AbstractWebSourceGenerationContext<WebClientClassGenerationContext, WebClientClassGenerationContext.GenerationMode> {

	/**
	 * <p>
	 * The Web client class generation mode.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public enum GenerationMode {
		// WebClient classes
		BOOT_CLIENT_CLASS,
		CLIENT_CLASS,
		CLIENT_CONTEXT,
		CLIENT_CONTEXT_IMPL,
		// WebClient stub class
		CLIENT_STUB_CLASS,
		EXCHANGE_BUILDER_PARAMETER,
		EXCHANGE,
		EXCHANGE_REQUEST_HEADERS,
		EXCHANGE_REQUEST_BODY,
		EXCHANGE_REQUEST_BODY_URL_ENCODED,
		EXCHANGE_REQUEST_BODY_MULTIPART,
		EXCHANGE_RESPONSE_BODY,
		WS_EXCHANGE,
		WS_OUTBOUND,
		WS_OUTBOUND_PUBLISHER,
		WS_INBOUND_PUBLISHER
	}

	private WebClientModuleInfo webClientModuleInfo;
	private WebClientStubInfo webClientStubInfo;
	private WebClientRouteInfo webClientRouteInfo;

	private final TypeMirror generatedType;
	private final TypeMirror beanAnnotationType;
	private final TypeMirror wrapperAnnotationType;
	private final TypeMirror supplierType;
	private final TypeMirror initType;
	private final TypeMirror webClientType;
	private final TypeMirror webRouteInterceptorType;
	private final TypeMirror listType;
	private final TypeMirror mutatorAnnotationType;
	private final TypeMirror functionType;
	private final TypeMirror methodType;
	private final TypeMirror webExchangeType;
	private final TypeMirror web2SocketExchangeType;
	private final TypeMirror webSocketFrameType;
	private final TypeMirror webSocketMessageType;
	private final TypeMirror headersType;
	private final TypeMirror fluxType;
	private final TypeMirror monoType;
	private final TypeMirror unpooledType;
	private final TypeMirror collectorsType;
	private final TypeMirror stringType;
	private final TypeMirror charSequenceType;

	/**
	 * <p>
	 * Creates a Web client class generation context.
	 * </p>
	 *
	 * @param typeUtils    the type utils
	 * @param elementUtils the element utils
	 * @param mode         the generation mode
	 */
	public WebClientClassGenerationContext(Types typeUtils, Elements elementUtils, GenerationMode mode) {
		super(typeUtils, elementUtils, mode);

		this.generatedType = elementUtils.getTypeElement(elementUtils.getModuleElement("java.compiler"), "javax.annotation.processing.Generated").asType();
		this.beanAnnotationType = elementUtils.getTypeElement(Bean.class.getCanonicalName()).asType();
		this.wrapperAnnotationType = elementUtils.getTypeElement(Wrapper.class.getCanonicalName()).asType();
		this.supplierType = typeUtils.erasure(elementUtils.getTypeElement(Supplier.class.getCanonicalName()).asType());
		this.initType = elementUtils.getTypeElement(Init.class.getCanonicalName()).asType();
		this.webClientType = typeUtils.erasure(elementUtils.getTypeElement(WebClient.class.getCanonicalName()).asType());
		this.webRouteInterceptorType = typeUtils.erasure(elementUtils.getTypeElement(WebRouteInterceptor.class.getCanonicalName()).asType());
		this.listType = typeUtils.erasure(elementUtils.getTypeElement(List.class.getCanonicalName()).asType());
		this.mutatorAnnotationType = elementUtils.getTypeElement(Mutator.class.getCanonicalName()).asType();
		this.functionType = typeUtils.erasure(elementUtils.getTypeElement(Function.class.getCanonicalName()).asType());
		this.methodType = elementUtils.getTypeElement(Method.class.getCanonicalName()).asType();
		this.webExchangeType = typeUtils.erasure(elementUtils.getTypeElement(WebExchange.class.getCanonicalName()).asType());
		this.web2SocketExchangeType = typeUtils.erasure(elementUtils.getTypeElement(Web2SocketExchange.class.getCanonicalName()).asType());
		this.webSocketFrameType = elementUtils.getTypeElement(WebSocketFrame.class.getCanonicalName()).asType();
		this.webSocketMessageType = elementUtils.getTypeElement(WebSocketMessage.class.getCanonicalName()).asType();
		this.headersType = elementUtils.getTypeElement(Headers.class.getCanonicalName()).asType();
		this.fluxType = typeUtils.erasure(elementUtils.getTypeElement(Flux.class.getCanonicalName()).asType());
		this.monoType = typeUtils.erasure(elementUtils.getTypeElement(Mono.class.getCanonicalName()).asType());
		this.unpooledType = elementUtils.getTypeElement(Unpooled.class.getCanonicalName()).asType();
		this.collectorsType = elementUtils.getTypeElement(Collectors.class.getCanonicalName()).asType();
		this.stringType = elementUtils.getTypeElement(String.class.getCanonicalName()).asType();
		this.charSequenceType = elementUtils.getTypeElement(CharSequence.class.getCanonicalName()).asType();
	}

	/**
	 * <p>
	 * Creates a Web client class generation context from a parent generation context.
	 * </p>
	 *
	 * @param parentGeneration the parent context
	 */
	private WebClientClassGenerationContext(WebClientClassGenerationContext parentGeneration) {
		super(parentGeneration);

		this.webClientModuleInfo = parentGeneration.webClientModuleInfo;
		this.webClientStubInfo = parentGeneration.webClientStubInfo;
		this.webClientRouteInfo = parentGeneration.webClientRouteInfo;

		this.generatedType = parentGeneration.generatedType;
		this.beanAnnotationType = parentGeneration.beanAnnotationType;
		this.wrapperAnnotationType = parentGeneration.wrapperAnnotationType;
		this.supplierType = parentGeneration.supplierType;
		this.initType = parentGeneration.initType;
		this.webClientType = parentGeneration.webClientType;
		this.webRouteInterceptorType = parentGeneration.webRouteInterceptorType;
		this.listType = parentGeneration.listType;
		this.mutatorAnnotationType = parentGeneration.mutatorAnnotationType;
		this.functionType = parentGeneration.functionType;
		this.methodType = parentGeneration.methodType;
		this.webExchangeType = parentGeneration.webExchangeType;
		this.webSocketFrameType = parentGeneration.webSocketFrameType;
		this.webSocketMessageType = parentGeneration.webSocketMessageType;
		this.web2SocketExchangeType = parentGeneration.web2SocketExchangeType;
		this.headersType = parentGeneration.headersType;
		this.fluxType = parentGeneration.fluxType;
		this.monoType = parentGeneration.monoType;
		this.unpooledType = parentGeneration.unpooledType;
		this.collectorsType = parentGeneration.collectorsType;
		this.stringType = parentGeneration.stringType;
		this.charSequenceType = parentGeneration.charSequenceType;
	}

	@Override
	public WebClientClassGenerationContext withMode(GenerationMode mode) {
		WebClientClassGenerationContext context = new WebClientClassGenerationContext(this);
		context.mode = mode;
		return context;
	}

	@Override
	public WebClientClassGenerationContext withIndentDepth(int indentDepth) {
		WebClientClassGenerationContext context = new WebClientClassGenerationContext(this);
		context.indentDepth = indentDepth;
		return context;
	}

	@Override
	public WebClientClassGenerationContext withModule(ModuleQualifiedName moduleQualifiedName) {
		WebClientClassGenerationContext context = new WebClientClassGenerationContext(this);
		context.moduleQualifiedName = moduleQualifiedName;
		return context;
	}

	public WebClientClassGenerationContext withWebClientModuleInfo(WebClientModuleInfo webClientModuleInfo) {
		WebClientClassGenerationContext context = new WebClientClassGenerationContext(this);
		context.webClientModuleInfo = webClientModuleInfo;
		return context;
	}

	public WebClientClassGenerationContext withWebClientStubInfo(WebClientStubInfo webClientStubInfo) {
		WebClientClassGenerationContext context = new WebClientClassGenerationContext(this);
		context.webClientStubInfo = webClientStubInfo;
		return context;
	}

	public WebClientClassGenerationContext withWebClientRouteInfo(WebClientRouteInfo webClientRouteInfo) {
		WebClientClassGenerationContext context = new WebClientClassGenerationContext(this);
		context.webClientRouteInfo = webClientRouteInfo;
		return context;
	}

	public WebClientModuleInfo getWebClientModuleInfo() {
		return this.webClientModuleInfo;
	}

	public WebClientStubInfo getWebClientStubInfo() {
		return webClientStubInfo;
	}

	public WebClientRouteInfo getWebClientRouteInfo() {
		return webClientRouteInfo;
	}

	public int getTypeIndex(TypeMirror type) {
		for(int i=0;i<this.webClientStubInfo.getTypesRegistry().length;i++) {
			if(this.typeUtils.isSameType(type, this.webClientStubInfo.getTypesRegistry()[i])) {
				return i;
			}
		}
		throw new IllegalArgumentException();
	}

	public TypeMirror getGeneratedType() {
		return generatedType;
	}

	public TypeMirror getBeanAnnotationType() {
		return beanAnnotationType;
	}

	public TypeMirror getWrapperAnnotationType() {
		return wrapperAnnotationType;
	}

	public TypeMirror getSupplierType() {
		return supplierType;
	}

	public TypeMirror getInitType() {
		return initType;
	}

	public TypeMirror getWebClientType() {
		return webClientType;
	}

	public TypeMirror getWebRouteInterceptorType() {
		return webRouteInterceptorType;
	}

	public TypeMirror getListType() {
		return listType;
	}

	public TypeMirror getMutatorAnnotationType() {
		return mutatorAnnotationType;
	}

	public TypeMirror getFunctionType() {
		return functionType;
	}

	public TypeMirror getMethodType() {
		return methodType;
	}

	public TypeMirror getWebExchangeType() {
		return webExchangeType;
	}

	public TypeMirror getWeb2SocketExchangeType() {
		return web2SocketExchangeType;
	}

	public TypeMirror getWebSocketFrameType() {
		return webSocketFrameType;
	}

	public TypeMirror getWebSocketMessageType() {
		return webSocketMessageType;
	}

	public TypeMirror getHeadersType() {
		return headersType;
	}

	public TypeMirror getFluxType() {
		return fluxType;
	}

	public TypeMirror getMonoType() {
		return monoType;
	}

	public TypeMirror getUnpooledType() {
		return unpooledType;
	}

	public TypeMirror getCollectorsType() {
		return collectorsType;
	}

	public TypeMirror getStringType() {
		return stringType;
	}

	public TypeMirror getCharSequenceType() {
		return charSequenceType;
	}
}
