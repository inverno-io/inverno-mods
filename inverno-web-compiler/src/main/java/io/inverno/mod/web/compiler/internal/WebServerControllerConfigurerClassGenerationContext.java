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

import io.inverno.core.compiler.spi.ModuleQualifiedName;
import io.inverno.core.compiler.spi.support.AbstractSourceGenerationContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.web.server.MissingRequiredParameterException;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.server.annotation.WebSocketRoute;
import io.inverno.mod.web.compiler.spi.WebControllerInfo;
import io.inverno.mod.web.compiler.spi.WebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRequestBodyParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import io.inverno.mod.web.compiler.spi.WebSocketInboundPublisherParameterInfo;
import io.inverno.mod.web.compiler.spi.WebSocketRouteInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents a generation context used by the {@link WebRouterConfigurerClassGenerator} during the generation of a web router configurer in an Inverno module.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class WebServerControllerConfigurerClassGenerationContext extends AbstractSourceGenerationContext<WebServerControllerConfigurerClassGenerationContext, WebServerControllerConfigurerClassGenerationContext.GenerationMode> {

	public static enum GenerationMode {
		CONFIGURER_CLASS,
		CONFIGURER_ANNOTATION,
		CONFIGURER_FIELD,
		CONFIGURER_PARAMETER,
		CONFIGURER_ASSIGNMENT,
		CONFIGURER_INVOKE,
		CONFIGURER_CONTEXT,
		CONFIGURER_CONTEXT_CREATOR,
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
		WEBSOCKET_ROUTE_HANDLER_TYPE;
	}
	
	private final TypeGenerator typeGenerator;
	private WebControllerInfo webController;
	private WebRouteInfo webRoute;
	private Integer parameterIndex;
	
	private TypeMirror collectionType;
	private TypeMirror listType;
	private TypeMirror setType;
	private TypeMirror optionalType;
	private TypeMirror collectorsType;
	private TypeMirror webRouteAnnotationType;
	private TypeMirror webExchangeType;
	private TypeMirror methodType;
	private TypeMirror missingRequiredParameterExceptionType;
	private TypeMirror publisherType;
	private TypeMirror monoType;
	private TypeMirror fluxType;
	private TypeMirror parameterType;
	private TypeMirror unpooledType;
	private TypeMirror typeType;
	private TypeMirror typesType;
	private TypeMirror voidType;
	private TypeMirror byteBufType;
	private TypeMirror webSocketRouteAnnotationType;
	private TypeMirror webSocketMessageKindType;
	private TypeMirror webSocketExchangeInboundType;
	private TypeMirror webSocketExchangeOutboundType;
	private TypeMirror webSocketMessageType;
	private TypeMirror webSocketFrameType;
	
	public WebServerControllerConfigurerClassGenerationContext(Types typeUtils, Elements elementUtils, GenerationMode mode) {
		super(typeUtils, elementUtils, mode);
		
		this.typeGenerator = new TypeGenerator();
		
		this.collectionType = typeUtils.erasure(elementUtils.getTypeElement(Collection.class.getCanonicalName()).asType());
		this.listType = typeUtils.erasure(elementUtils.getTypeElement(List.class.getCanonicalName()).asType());
		this.setType = typeUtils.erasure(elementUtils.getTypeElement(Set.class.getCanonicalName()).asType());
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
		this.typeType = elementUtils.getTypeElement(Type.class.getCanonicalName()).asType();
		this.typesType = elementUtils.getTypeElement(io.inverno.mod.base.reflect.Types.class.getCanonicalName()).asType();
		this.voidType = elementUtils.getTypeElement(Void.class.getCanonicalName()).asType();
		this.byteBufType = elementUtils.getTypeElement(ByteBuf.class.getCanonicalName()).asType();
		this.webSocketRouteAnnotationType = elementUtils.getTypeElement(WebSocketRoute.class.getCanonicalName()).asType();
		this.webSocketMessageKindType = elementUtils.getTypeElement(WebSocketMessage.Kind.class.getCanonicalName()).asType();
		this.webSocketExchangeInboundType = elementUtils.getTypeElement(WebSocketExchange.Inbound.class.getCanonicalName()).asType();
		this.webSocketExchangeOutboundType = elementUtils.getTypeElement(WebSocketExchange.Outbound.class.getCanonicalName()).asType();
		this.webSocketMessageType = elementUtils.getTypeElement(WebSocketMessage.class.getCanonicalName()).asType();
		this.webSocketFrameType = elementUtils.getTypeElement(WebSocketFrame.class.getCanonicalName()).asType();
	}
	
	private WebServerControllerConfigurerClassGenerationContext(WebServerControllerConfigurerClassGenerationContext parentGeneration) {
		super(parentGeneration);
		this.typeGenerator = parentGeneration.typeGenerator;
		this.webController = parentGeneration.webController;
		this.webRoute = parentGeneration.webRoute;
		this.parameterIndex = parentGeneration.parameterIndex;
	}
	
	@Override
	public WebServerControllerConfigurerClassGenerationContext withMode(GenerationMode mode) {
		WebServerControllerConfigurerClassGenerationContext context = new WebServerControllerConfigurerClassGenerationContext(this);
		context.mode = mode;
		return context;
	}

	@Override
	public WebServerControllerConfigurerClassGenerationContext withIndentDepth(int indentDepth) {
		WebServerControllerConfigurerClassGenerationContext context = new WebServerControllerConfigurerClassGenerationContext(this);
		context.indentDepth = indentDepth;
		return context;
	}

	@Override
	public WebServerControllerConfigurerClassGenerationContext withModule(ModuleQualifiedName moduleQualifiedName) {
		WebServerControllerConfigurerClassGenerationContext context = new WebServerControllerConfigurerClassGenerationContext(this);
		context.moduleQualifiedName = moduleQualifiedName;
		return context;
	}
	
	public WebServerControllerConfigurerClassGenerationContext withWebController(WebControllerInfo webController) {
		WebServerControllerConfigurerClassGenerationContext context = new WebServerControllerConfigurerClassGenerationContext(this);
		context.webController = webController;
		return context;
	}
	
	public WebServerControllerConfigurerClassGenerationContext withWebRoute(WebRouteInfo webRoute) {
		WebServerControllerConfigurerClassGenerationContext context = new WebServerControllerConfigurerClassGenerationContext(this);
		context.webRoute = webRoute;
		return context;
	}
	
	public WebServerControllerConfigurerClassGenerationContext withParameterIndex(int parameterIndex) {
		WebServerControllerConfigurerClassGenerationContext context = new WebServerControllerConfigurerClassGenerationContext(this);
		context.parameterIndex = parameterIndex;
		return context;
	}
	
	public WebControllerInfo getWebController() {
		return webController;
	}
	
	public WebRouteInfo getWebRoute() {
		return webRoute;
	}
	
	public int getParameterIndex() {
		return parameterIndex;
	}
	
	public TypeMirror getParameterConverterType(TypeMirror type) {
		if(this.isArrayType(type)) {
			return ((ArrayType)type).getComponentType();
		}
		else if(isCollectionType(type)) {
			return ((DeclaredType)type).getTypeArguments().get(0);
		}
		return type;
	}
	
	public boolean isArrayType(TypeMirror type) {
		return type.getKind() == TypeKind.ARRAY;
	}
	
	public boolean isCollectionType(TypeMirror type) {
		if(type.getKind() == TypeKind.VOID) {
			return false;
		}
		
		TypeMirror erasedType = this.typeUtils.erasure(type);
		return this.typeUtils.isSameType(erasedType, this.getCollectionType()) ||
				this.typeUtils.isSameType(erasedType, this.getListType()) ||
				this.typeUtils.isSameType(erasedType, this.getSetType());
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
	
	public boolean isTypeMode(WebRouteInfo routeInfo) {
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
	
	public boolean isTypeMode(WebSocketRouteInfo routeInfo) {
		return routeInfo.getOutboundPublisher()
				.map(outboundPublisherInfo -> outboundPublisherInfo.getType().getKind() != TypeKind.VOID && !this.isClassType(outboundPublisherInfo.getType()))
				.orElse(false) 
			|| Arrays.stream(routeInfo.getParameters()).anyMatch(parameter -> (parameter instanceof WebSocketInboundPublisherParameterInfo) && !this.isClassType(parameter.getType()));
	}
	
	public boolean isClassType(TypeMirror type) {
		if(type.getKind() == TypeKind.ARRAY) {
			return false;
		}
		else if(type.getKind() == TypeKind.DECLARED) {
			return ((DeclaredType)type).getTypeArguments().isEmpty();
		}
		return true;
	}
	
	public TypeMirror getCollectionType() {
		return collectionType != null ? collectionType : this.parentGeneration.getCollectionType();
	}
	
	public String getCollectionTypeName() {
		return this.getTypeName(this.getCollectionType());
	}
	
	public TypeMirror getListType() {
		return listType != null ? listType : this.parentGeneration.getListType();
	}
	
	public String getListTypeName() {
		return this.getTypeName(this.getListType());
	}
	
	public TypeMirror getSetType() {
		return setType != null ? setType : this.parentGeneration.getSetType();
	}
	
	public String getSetTypeName() {
		return this.getTypeName(this.getSetType());
	}
	
	public TypeMirror getOptionalType() {
		return optionalType != null ? optionalType : this.parentGeneration.getOptionalType();
	}
	
	public String getOptionalTypeName() {
		return this.getTypeName(this.getOptionalType());
	}
	
	public TypeMirror getCollectorsType() {
		return collectorsType != null ? collectorsType : this.parentGeneration.getCollectorsType();
	}
	
	public String getCollectorsTypeName() {
		return this.getTypeName(this.getCollectorsType());
	}
	
	public TypeMirror getWebRouteAnnotationType() {
		return webRouteAnnotationType != null ? webRouteAnnotationType : this.parentGeneration.getWebRouteAnnotationType();
	}
	
	public String getWebRouteAnnotationTypeName() {
		return this.getTypeName(this.getWebRouteAnnotationType());
	}
	
	public TypeMirror getWebExchangeType() {
		return webExchangeType != null ? webExchangeType : this.parentGeneration.getWebExchangeType();
	}
	
	public String getWebExchangeTypeName() {
		return this.getTypeName(this.getWebExchangeType());
	}
	
	public TypeMirror getMethodType() {
		return methodType != null ? methodType : this.parentGeneration.getMethodType();
	}
	
	public String getMethodTypeName() {
		return this.getTypeName(this.getMethodType());
	}
	
	public TypeMirror getMissingRequiredParameterExceptionType() {
		return missingRequiredParameterExceptionType != null ? missingRequiredParameterExceptionType : this.parentGeneration.getMissingRequiredParameterExceptionType();
	}
	
	public String getMissingRequiredParameterExceptionTypeName() {
		return this.getTypeName(this.getMissingRequiredParameterExceptionType());
	}
	
	public TypeMirror getPublisherType() {
		return publisherType != null ? publisherType : this.parentGeneration.getPublisherType();
	}
	
	public String getPublisherTypeName() {
		return this.getTypeName(this.getPublisherType());
	}
	
	public TypeMirror getMonoType() {
		return monoType != null ? monoType : this.parentGeneration.getMonoType();
	}
	
	public String getMonoTypeName() {
		return this.getTypeName(this.getMonoType());
	}
	
	public TypeMirror getFluxType() {
		return fluxType != null ? fluxType : this.parentGeneration.getFluxType();
	}
	
	public String getFluxTypeName() {
		return this.getTypeName(this.getFluxType());
	}
	
	public TypeMirror getParameterType() {
		return parameterType != null ? parameterType : this.parentGeneration.getParameterType();
	}
	
	public String getParameterTypeName() {
		return this.getTypeName(this.getParameterType());
	}
	
	public TypeMirror getUnpooledType() {
		return unpooledType != null ? unpooledType : this.parentGeneration.getUnpooledType();
	}
	
	public String getUnpooledTypeName() {
		return this.getTypeName(this.getUnpooledType());
	}
	
	public TypeMirror getTypeType() {
		return typeType != null ? typeType : this.parentGeneration.getTypeType();
	}
	
	public String getTypeTypeName() {
		return this.getTypeName(this.getTypeType());
	}
	
	public TypeMirror getTypesType() {
		return typesType != null ? typesType : this.parentGeneration.getTypesType();
	}
	
	public String getTypesTypeName() {
		return this.getTypeName(this.getTypesType());
	}
	
	public TypeMirror getVoidType() {
		return voidType != null ? voidType : this.parentGeneration.getVoidType();
	}
	
	public String getVoidTypeName() {
		return this.getTypeName(this.getVoidType());
	}
	
	public TypeMirror getByteBufType() {
		return byteBufType != null ? byteBufType : this.parentGeneration.getByteBufType();
	}
	
	public String getByteBufTypeName() {
		return this.getTypeName(this.getByteBufType());
	}
	
	public TypeMirror getWebSocketRouteAnnotationType() {
		return webSocketRouteAnnotationType != null ? webSocketRouteAnnotationType : this.parentGeneration.getWebSocketRouteAnnotationType();
	}
	
	public String getWebSocketRouteAnnotationTypeName() {
		return this.getTypeName(this.getWebSocketRouteAnnotationType());
	}
	
	public TypeMirror getWebSocketMessageKindType() {
		return webSocketMessageKindType != null ? webSocketMessageKindType : this.parentGeneration.getWebSocketMessageKindType();
	}
	
	public String getWebSocketMessageKindTypeName() {
		return this.getTypeName(this.getWebSocketMessageKindType());
	}
	
	public TypeMirror getWebSocketExchangeInboundType() {
		return webSocketExchangeInboundType != null ? webSocketExchangeInboundType : this.parentGeneration.getWebSocketExchangeInboundType();
	}
	
	public String getWebSocketExchangeInboundTypeName() {
		return this.getTypeName(this.getWebSocketExchangeInboundType());
	}
	
	public TypeMirror getWebSocketExchangeOutboundType() {
		return webSocketExchangeOutboundType != null ? webSocketExchangeOutboundType : this.parentGeneration.getWebSocketExchangeOutboundType();
	}
	
	public String getWebSocketExchangeOutboundTypeName() {
		return this.getTypeName(this.getWebSocketExchangeOutboundType());
	}
	
	public TypeMirror getWebSocketMessageType() {
		return webSocketMessageType != null ? webSocketMessageType : this.parentGeneration.getWebSocketMessageType();
	}
	
	public String getWebSocketMessageTypeName() {
		return this.getTypeName(this.getWebSocketMessageType());
	}
	
	public TypeMirror getWebSocketFrameType() {
		return webSocketFrameType != null ? webSocketFrameType : this.parentGeneration.getWebSocketFrameType();
	}
	
	public String getWebSocketFrameTypeName() {
		return this.getTypeName(this.getWebSocketFrameType());
	}
	
	public StringBuilder getTypeGenerator(TypeMirror type) {
		return this.typeGenerator.visit(type);
	}
	
	private static enum TypeGenerationKind {
		PARAMETERIZED,
		UPPERBOUND,
		LOWERBOUND,
		COMPONENT,
		OWNER
	}
	
	private class TypeGenerator implements TypeVisitor<StringBuilder, TypeGenerationKind> {

		@Override
		public StringBuilder visit(TypeMirror type, TypeGenerationKind kind) {
			Objects.requireNonNull(type, "type");
			if(type instanceof PrimitiveType) {
				return this.visitPrimitive((PrimitiveType)type, kind);
			}
			else if(type instanceof ArrayType) {
				return this.visitArray((ArrayType)type, kind);
			}
			else if(type instanceof DeclaredType) {
				return this.visitDeclared((DeclaredType)type, kind);
			}
			else if(type instanceof WildcardType) {
				return this.visitWildcard((WildcardType)type, kind);
			}
			else if(type instanceof ExecutableType) {
				return this.visitExecutable((ExecutableType)type, kind);
			}
			else if(type instanceof NoType) {
				return this.visitNoType((NoType)type, kind);
			}
			else if(type instanceof IntersectionType) {
				return this.visitIntersection((IntersectionType)type, kind);
			}
			else {
				// The compiler should check that and report the error properly
				throw new IllegalStateException();
			}
		}

		@Override
		public StringBuilder visitPrimitive(PrimitiveType type, TypeGenerationKind kind) {
			// .type(Integer.class).and()
			// Types.type(int.class).build()
			if(kind == null) {
				return new StringBuilder(WebServerControllerConfigurerClassGenerationContext.this.getTypesTypeName()).append(".type(").append(WebServerControllerConfigurerClassGenerationContext.this.getTypeName(type)).append(".class).build()");
			}
			else {
				StringBuilder result = new StringBuilder();
				
				if(kind == TypeGenerationKind.PARAMETERIZED) {
					result.append(".type(");
				}
				else if(kind == TypeGenerationKind.UPPERBOUND){
					result.append(".upperBoundType(");
				}
				else if(kind == TypeGenerationKind.LOWERBOUND){
					result.append(".lowerBoundType(");
				}
				else if(kind == TypeGenerationKind.COMPONENT){
					result.append(".componentType(");
				}
				else {
					throw new IllegalStateException("A primitive type can't appear as a " + kind + " type");
				}
				
				result.append(WebServerControllerConfigurerClassGenerationContext.this.getTypeName(WebServerControllerConfigurerClassGenerationContext.this.typeUtils.boxedClass(type).asType())).append(".class).and()");
				
				return result;
			}
		}

		@Override
		public StringBuilder visitNull(NullType type, TypeGenerationKind kind) {
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitArray(ArrayType type, TypeGenerationKind kind) {
			if(kind == null) {
				return new StringBuilder(WebServerControllerConfigurerClassGenerationContext.this.getTypesTypeName()).append(".arrayType()").append(this.visit(type.getComponentType(), TypeGenerationKind.COMPONENT)).append(".build()");
			}
			else {
				StringBuilder result = new StringBuilder();
				
				if(kind == TypeGenerationKind.PARAMETERIZED) {
					result.append(".arrayType()");
				}
				else if(kind == TypeGenerationKind.UPPERBOUND){
					result.append(".upperBoundArrayType()");
				}
				else if(kind == TypeGenerationKind.LOWERBOUND){
					result.append(".lowerBoundArrayType()");
				}
				else if(kind == TypeGenerationKind.COMPONENT){
					result.append(".componentArrayType()");
				}
				else {
					throw new IllegalStateException("An array type can't appear as a " + kind + " type");
				}
				
				result.append(this.visit(type.getComponentType(), TypeGenerationKind.COMPONENT)).append(".and()");
				
				return result;
			}
		}

		@Override
		public StringBuilder visitDeclared(DeclaredType type, TypeGenerationKind kind) {
			if(kind == null) {
				StringBuilder result = new StringBuilder(WebServerControllerConfigurerClassGenerationContext.this.getTypesTypeName()).append(".type(").append(WebServerControllerConfigurerClassGenerationContext.this.getTypeName(WebServerControllerConfigurerClassGenerationContext.this.typeUtils.erasure(type))).append(".class)");
				if(type.getEnclosingType() instanceof DeclaredType) {
					result.append(this.visit(type.getEnclosingType(), TypeGenerationKind.OWNER));
				}
				
				for(TypeMirror typeArgument : type.getTypeArguments()) {
					result.append(this.visit(typeArgument, TypeGenerationKind.PARAMETERIZED));
				}
				
				result.append(".build()");
				
				return result;
			}
			else {
				StringBuilder result = new StringBuilder();
				
				if(kind == TypeGenerationKind.PARAMETERIZED) {
					result.append(".type(");
				}
				else if(kind == TypeGenerationKind.UPPERBOUND){
					result.append(".upperBoundType(");
				}
				else if(kind == TypeGenerationKind.LOWERBOUND){
					result.append(".lowerBoundType(");
				}
				else if(kind == TypeGenerationKind.COMPONENT){
					result.append(".componentType(");
				}
				else {
					throw new IllegalStateException("A declared type can't appear as a " + kind + " type");
				}
				
				result.append(WebServerControllerConfigurerClassGenerationContext.this.getTypeName(WebServerControllerConfigurerClassGenerationContext.this.typeUtils.erasure(type))).append(".class)");
				if(type.getEnclosingType() instanceof DeclaredType) {
					result.append(this.visit(type.getEnclosingType(), TypeGenerationKind.OWNER));
				}
				for(TypeMirror typeArgument : type.getTypeArguments()) {
					result.append(this.visit(typeArgument, TypeGenerationKind.PARAMETERIZED));
				}
				result.append(".and()");
				
				return result;
			}
		}

		@Override
		public StringBuilder visitError(ErrorType type, TypeGenerationKind kind) {
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitTypeVariable(TypeVariable type, TypeGenerationKind kind) {
			// We should throw an IllegalStateException here, we want to be able to encode or decode stuff here, how can we do that if we have type variables... 
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitWildcard(WildcardType type, TypeGenerationKind kind) {
			if(kind == null) {
				throw new IllegalStateException("WildcardType can't exist on its own");
			}
			else {
				StringBuilder result = new StringBuilder();
				
				if(kind == TypeGenerationKind.PARAMETERIZED) {
					result.append(".wildcardType()");
				}
				else {
					throw new IllegalStateException("A wildcard type can't appear as a " + kind + " type");
				}
				
				if(type.getExtendsBound() != null) {
					result.append(this.visit(type.getExtendsBound(), TypeGenerationKind.UPPERBOUND));
				}
				else if(type.getSuperBound() != null) {
					result.append(this.visit(type.getSuperBound(), TypeGenerationKind.LOWERBOUND));
				}
				result.append(".and()");
				
				return result;
			}
		}

		@Override
		public StringBuilder visitExecutable(ExecutableType type, TypeGenerationKind kind) {
			if(kind == null) {
				throw new IllegalStateException("ExecutableType can't appear as a " + kind + " type");
			}
			else {
				return this.visit(type.getReturnType());
			}
		}

		@Override
		public StringBuilder visitNoType(NoType type, TypeGenerationKind kind) {
			// only support VOID kind
			if(type.getKind() != TypeKind.VOID) {
				throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
			}
			
			if(kind == null) {
				return new StringBuilder(WebServerControllerConfigurerClassGenerationContext.this.getTypesTypeName()).append(".type(").append(WebServerControllerConfigurerClassGenerationContext.this.getVoidTypeName()).append(".class).build()");
			}
			else {
				StringBuilder result = new StringBuilder();
				
				if(kind == TypeGenerationKind.PARAMETERIZED) {
					result.append(".type(");
				}
				else if(kind == TypeGenerationKind.UPPERBOUND){
					result.append(".upperBoundType(");
				}
				else if(kind == TypeGenerationKind.LOWERBOUND){
					result.append(".lowerBoundType(");
				}
				else if(kind == TypeGenerationKind.COMPONENT){
					result.append(".componentType(");
				}
				else {
					throw new IllegalStateException("Void type can't appear as a " + kind + " type");
				}
				
				result.append(WebServerControllerConfigurerClassGenerationContext.this.getVoidTypeName()).append(".class).build()");
				
				return result;
			}
		}

		@Override
		public StringBuilder visitUnknown(TypeMirror type, TypeGenerationKind kind) {
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitUnion(UnionType type, TypeGenerationKind kind) {
			// multi-catch: Type1 | Type2
			// I don't need/want to support that => throw an IllegalStateException
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitIntersection(IntersectionType type, TypeGenerationKind kind) {
			// <T extends Number & Runnable>
			// I don't know in which case this might happen but we never know... 
			if(kind == null) {
				throw new IllegalStateException("IntersectionType can't exist on its own");
			}
			else {
				StringBuilder result = new StringBuilder();
				
				if(kind != TypeGenerationKind.UPPERBOUND && kind != TypeGenerationKind.LOWERBOUND) {
					throw new IllegalStateException("A wildcard type can't appear as a " + kind + " type");
				}
				
				for(TypeMirror boundType : type.getBounds()) {
					result.append(this.visit(boundType, kind));
				}
				
				return result;
			}
		}
	}
}
