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

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.internal.GenericWebSocketExchangeParameterInfo;
import io.inverno.mod.web.compiler.internal.GenericWebSocketOutboundParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import io.inverno.mod.web.compiler.spi.WebSocketBoundPublisherInfo;
import io.inverno.mod.web.server.ws.Web2SocketExchange;
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
 * Factory used to create WebSocket server route parameter info.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class WebSocketServerRouteParameterInfoFactory extends AbstractWebServerRouteParameterInfoFactory {

	private final TypeMirror web2SocketExchangeType;
	private final TypeMirror web2SocketExchangeInboundType;
	private final TypeMirror web2SocketExchangeOutboundType;

	private final TypeMirror monoType;
	private final TypeMirror fluxType;
	private final TypeMirror voidType;
	private final TypeMirror publisherType;
	private final TypeMirror byteBufType;
	private final TypeMirror charSequenceType;

	/**
	 * <p>
	 * Creates a WebSocket server route parameter info factory.
	 * </p>
	 *
	 * @param pluginContext   the Web compiler plugin context
	 * @param pluginExecution the Web compiler plugin execution
	 */
	public WebSocketServerRouteParameterInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		super(pluginContext, pluginExecution);

		this.web2SocketExchangeType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Web2SocketExchange.class.getCanonicalName()).asType());
		this.web2SocketExchangeInboundType = this.pluginContext.getElementUtils().getTypeElement(BaseWeb2SocketExchange.Inbound.class.getCanonicalName()).asType();
		this.web2SocketExchangeOutboundType = this.pluginContext.getElementUtils().getTypeElement(BaseWeb2SocketExchange.Outbound.class.getCanonicalName()).asType();

		this.monoType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Mono.class.getCanonicalName()).asType());
		this.fluxType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Flux.class.getCanonicalName()).asType());
		this.voidType = this.pluginContext.getElementUtils().getTypeElement(Void.class.getCanonicalName()).asType();
		this.publisherType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(Publisher.class.getCanonicalName()).asType());
		this.byteBufType = this.pluginContext.getElementUtils().getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.charSequenceType = this.pluginContext.getElementUtils().getTypeElement(CharSequence.class.getCanonicalName()).asType();
	}

	@Override
	protected AbstractWebParameterInfo createContextualParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType) {
		if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.web2SocketExchangeType)) {
			return this.createWebSocketExchangeParameter(reporter, parameterQName, parameterElement);
		}
		else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.web2SocketExchangeInboundType)) {
			return this.createWebSocketInboundParameter(reporter, parameterQName, parameterElement);
		}
		else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.web2SocketExchangeOutboundType)) {
			return this.createWebSocketOutboundParameter(reporter, parameterQName, parameterElement);
		}
		else if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.publisherType)
			|| this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.fluxType)
			|| this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterElement.asType()), this.monoType)) {
			return this.createWebSocketInboundPublisherParameter(reporter, parameterQName, parameterElement, parameterType);
		}
		return super.createContextualParameter(reporter, parameterQName, parameterElement, parameterType);
	}

	@Override
	protected AbstractWebParameterInfo createParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType, AnnotationMirror annotation, Set<String> consumes, Set<String> produces, boolean required) {
		return null;
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
		return new GenericWebSocketExchangeParameterInfo(parameterQName, reporter, parameterElement, ((DeclaredType)parameterElement.asType()).getTypeArguments().getFirst(), contextType);
	}

	/**
	 * <p>
	 * Creates a WebSocket inbound parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 *
	 * @return a WebSocket inbound parameter info
	 */
	private GenericWebSocketServerInboundParameterInfo createWebSocketInboundParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		return new GenericWebSocketServerInboundParameterInfo(parameterQName, reporter, parameterElement, this.web2SocketExchangeInboundType);
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
	private GenericWebSocketServerInboundPublisherParameterInfo createWebSocketInboundPublisherParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType) {
		WebSocketBoundPublisherInfo.BoundReactiveKind inboundReactiveKind;
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
		TypeMirror type = ((DeclaredType)parameterType).getTypeArguments().getFirst();
		if(this.pluginContext.getTypeUtils().isSameType(type, this.voidType)) {
			inboundKind = WebSocketBoundPublisherInfo.BoundKind.EMPTY;
		}
		else if(this.pluginContext.getTypeUtils().isSameType(type, this.byteBufType)) {
			inboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED;
		}
		else if(this.pluginContext.getTypeUtils().isAssignable(type, this.charSequenceType)) {
			inboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED;
		}
		else if(type instanceof DeclaredType && ((DeclaredType)type).getTypeArguments().size() == 1) {
			// maybe we have a reactive message payload
			TypeMirror erasedBoundType = this.pluginContext.getTypeUtils().erasure(type);
			TypeMirror nextBoundType = ((DeclaredType)type).getTypeArguments().getFirst();
			if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.publisherType)) {
				if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
					type = nextBoundType;
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_PUBLISHER;
				}
				else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
					type = nextBoundType;
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_PUBLISHER;
				}
			}
			else if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.fluxType)) {
				if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
					type = nextBoundType;
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_MANY;
				}
				else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
					type = nextBoundType;
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_MANY;
				}
			}
			else if(this.pluginContext.getTypeUtils().isSameType(erasedBoundType, this.monoType)) {
				if(this.pluginContext.getTypeUtils().isSameType(nextBoundType, this.byteBufType)) {
					type = nextBoundType;
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.RAW_REDUCED_ONE;
				}
				else if(this.pluginContext.getTypeUtils().isAssignable(nextBoundType, this.charSequenceType)) {
					type = nextBoundType;
					inboundKind = WebSocketBoundPublisherInfo.BoundKind.CHARSEQUENCE_REDUCED_ONE;
				}
			}
		}
		return new GenericWebSocketServerInboundPublisherParameterInfo(parameterQName, reporter, parameterElement, type, inboundKind, inboundReactiveKind);
	}

	/**
	 * <p>
	 * Creates a WebSocket outbound parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 *
	 * @return a WebSocket outbound parameter info
	 */
	private GenericWebSocketOutboundParameterInfo createWebSocketOutboundParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		return new GenericWebSocketOutboundParameterInfo(parameterQName, reporter, parameterElement, this.web2SocketExchangeOutboundType);
	}
}
