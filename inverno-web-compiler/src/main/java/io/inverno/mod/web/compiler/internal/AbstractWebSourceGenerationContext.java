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
package io.inverno.mod.web.compiler.internal;

import io.inverno.core.compiler.spi.support.AbstractSourceGenerationContext;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

/**
 * <p>
 * Base Web {@link AbstractSourceGenerationContext} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public abstract class AbstractWebSourceGenerationContext<A extends AbstractWebSourceGenerationContext<A, B>, B extends Enum<B>> extends AbstractSourceGenerationContext<A, B> {

	private final TypeGenerator typeGenerator;

	private final TypeMirror typeType;
	private final TypeMirror typesType;
	private final TypeMirror voidType;
	private final TypeMirror collectionType;
	private final TypeMirror listType;
	private final TypeMirror setType;

	/**
	 * <p>
	 * Creates a Web source generation context.
	 * </p>
	 *
	 * @param typeUtils    the type utils
	 * @param elementUtils the element utlils
	 * @param mode         the generation mode
	 */
	public AbstractWebSourceGenerationContext(Types typeUtils, Elements elementUtils, B mode) {
		super(typeUtils, elementUtils, mode);
		this.typeGenerator = new TypeGenerator();

		this.typeType = elementUtils.getTypeElement(Type.class.getCanonicalName()).asType();
		this.typesType = elementUtils.getTypeElement(io.inverno.mod.base.reflect.Types.class.getCanonicalName()).asType();
		this.voidType =  elementUtils.getTypeElement(Void.class.getCanonicalName()).asType();
		this.collectionType = typeUtils.erasure(elementUtils.getTypeElement(Collection.class.getCanonicalName()).asType());
		this.listType = typeUtils.erasure(elementUtils.getTypeElement(List.class.getCanonicalName()).asType());
		this.setType = typeUtils.erasure(elementUtils.getTypeElement(Set.class.getCanonicalName()).asType());
	}

	/**
	 * <p>
	 * Creates a Web source generation context from a parent context.
	 * </p>
	 *
	 * @param parentGeneration the parent generation context
	 */
	public AbstractWebSourceGenerationContext(A parentGeneration) {
		super(parentGeneration);
		this.typeGenerator = ((AbstractWebSourceGenerationContext<A, B>)parentGeneration).typeGenerator;

		this.typeType = ((AbstractWebSourceGenerationContext<A, B>)parentGeneration).typeType;
		this.typesType = ((AbstractWebSourceGenerationContext<A, B>)parentGeneration).typesType;
		this.voidType = ((AbstractWebSourceGenerationContext<A, B>)parentGeneration).voidType;
		this.collectionType = ((AbstractWebSourceGenerationContext<A, B>)parentGeneration).collectionType;
		this.listType = ((AbstractWebSourceGenerationContext<A, B>)parentGeneration).listType;
		this.setType = ((AbstractWebSourceGenerationContext<A, B>)parentGeneration).setType;
	}

	public TypeMirror getTypeType() {
		return typeType;
	}

	public TypeMirror getTypesType() {
		return typesType;
	}

	public TypeMirror getVoidType() {
		return voidType;
	}

	public TypeMirror getCollectionType() {
		return collectionType;
	}

	public TypeMirror getListType() {
		return listType;
	}

	public TypeMirror getSetType() {
		return setType;
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

	public StringBuilder getTypeGenerator(TypeMirror type) {
		return this.typeGenerator.visit(type);
	}

	private enum TypeGenerationKind {
		PARAMETERIZED,
		UPPERBOUND,
		LOWERBOUND,
		COMPONENT,
		OWNER
	}

	private class TypeGenerator implements TypeVisitor<StringBuilder, AbstractWebSourceGenerationContext.TypeGenerationKind> {

		@Override
		public StringBuilder visit(TypeMirror type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
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
		public StringBuilder visitPrimitive(PrimitiveType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			// .type(Integer.class).and()
			// Types.type(int.class).build()
			if(kind == null) {
				return new StringBuilder(AbstractWebSourceGenerationContext.this.getTypeName(AbstractWebSourceGenerationContext.this.getTypesType())).append(".type(").append(AbstractWebSourceGenerationContext.this.getTypeName(type)).append(".class).build()");
			}
			else {
				StringBuilder result = new StringBuilder();

				if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.PARAMETERIZED) {
					result.append(".type(");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.UPPERBOUND){
					result.append(".upperBoundType(");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.LOWERBOUND){
					result.append(".lowerBoundType(");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.COMPONENT){
					result.append(".componentType(");
				}
				else {
					throw new IllegalStateException("A primitive type can't appear as a " + kind + " type");
				}

				result.append(AbstractWebSourceGenerationContext.this.getTypeName(AbstractWebSourceGenerationContext.this.typeUtils.boxedClass(type).asType())).append(".class).and()");

				return result;
			}
		}

		@Override
		public StringBuilder visitNull(NullType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitArray(ArrayType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			if(kind == null) {
				return new StringBuilder(AbstractWebSourceGenerationContext.this.getTypeName(AbstractWebSourceGenerationContext.this.getTypesType())).append(".arrayType()").append(this.visit(type.getComponentType(), AbstractWebSourceGenerationContext.TypeGenerationKind.COMPONENT)).append(".build()");
			}
			else {
				StringBuilder result = new StringBuilder();

				if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.PARAMETERIZED) {
					result.append(".arrayType()");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.UPPERBOUND){
					result.append(".upperBoundArrayType()");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.LOWERBOUND){
					result.append(".lowerBoundArrayType()");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.COMPONENT){
					result.append(".componentArrayType()");
				}
				else {
					throw new IllegalStateException("An array type can't appear as a " + kind + " type");
				}

				result.append(this.visit(type.getComponentType(), AbstractWebSourceGenerationContext.TypeGenerationKind.COMPONENT)).append(".and()");

				return result;
			}
		}

		@Override
		public StringBuilder visitDeclared(DeclaredType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			StringBuilder result;
			if(kind == null) {
				result = new StringBuilder(AbstractWebSourceGenerationContext.this.getTypeName(AbstractWebSourceGenerationContext.this.getTypesType())).append(".type(").append(AbstractWebSourceGenerationContext.this.getTypeName(AbstractWebSourceGenerationContext.this.typeUtils.erasure(type))).append(".class)");
				if(type.getEnclosingType() instanceof DeclaredType) {
					result.append(this.visit(type.getEnclosingType(), AbstractWebSourceGenerationContext.TypeGenerationKind.OWNER));
				}

				for(TypeMirror typeArgument : type.getTypeArguments()) {
					result.append(this.visit(typeArgument, AbstractWebSourceGenerationContext.TypeGenerationKind.PARAMETERIZED));
				}
				result.append(".build()");
			}
			else {
				result = new StringBuilder();

				if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.PARAMETERIZED) {
					result.append(".type(");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.UPPERBOUND){
					result.append(".upperBoundType(");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.LOWERBOUND){
					result.append(".lowerBoundType(");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.COMPONENT){
					result.append(".componentType(");
				}
				else {
					throw new IllegalStateException("A declared type can't appear as a " + kind + " type");
				}

				result.append(AbstractWebSourceGenerationContext.this.getTypeName(AbstractWebSourceGenerationContext.this.typeUtils.erasure(type))).append(".class)");
				if(type.getEnclosingType() instanceof DeclaredType) {
					result.append(this.visit(type.getEnclosingType(), AbstractWebSourceGenerationContext.TypeGenerationKind.OWNER));
				}
				for(TypeMirror typeArgument : type.getTypeArguments()) {
					result.append(this.visit(typeArgument, AbstractWebSourceGenerationContext.TypeGenerationKind.PARAMETERIZED));
				}
				result.append(".and()");
			}
			return result;
		}

		@Override
		public StringBuilder visitError(ErrorType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitTypeVariable(TypeVariable type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			// We should throw an IllegalStateException here, we want to be able to encode or decode stuff here, how can we do that if we have type variables...
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitWildcard(WildcardType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			if(kind == null) {
				throw new IllegalStateException("WildcardType can't exist on its own");
			}
			else {
				StringBuilder result = new StringBuilder();

				if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.PARAMETERIZED) {
					result.append(".wildcardType()");
				}
				else {
					throw new IllegalStateException("A wildcard type can't appear as a " + kind + " type");
				}

				if(type.getExtendsBound() != null) {
					result.append(this.visit(type.getExtendsBound(), AbstractWebSourceGenerationContext.TypeGenerationKind.UPPERBOUND));
				}
				else if(type.getSuperBound() != null) {
					result.append(this.visit(type.getSuperBound(), AbstractWebSourceGenerationContext.TypeGenerationKind.LOWERBOUND));
				}
				result.append(".and()");

				return result;
			}
		}

		@Override
		public StringBuilder visitExecutable(ExecutableType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			if(kind == null) {
				throw new IllegalStateException("ExecutableType can't appear as a null type");
			}
			else {
				return this.visit(type.getReturnType());
			}
		}

		@Override
		public StringBuilder visitNoType(NoType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			// only support VOID kind
			if(type.getKind() != TypeKind.VOID) {
				throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
			}

			if(kind == null) {
				return new StringBuilder(AbstractWebSourceGenerationContext.this.getTypeName(AbstractWebSourceGenerationContext.this.getTypesType())).append(".type(").append(AbstractWebSourceGenerationContext.this.getTypeName(AbstractWebSourceGenerationContext.this.getVoidType())).append(".class).build()");
			}
			else {
				StringBuilder result = new StringBuilder();

				if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.PARAMETERIZED) {
					result.append(".type(");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.UPPERBOUND){
					result.append(".upperBoundType(");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.LOWERBOUND){
					result.append(".lowerBoundType(");
				}
				else if(kind == AbstractWebSourceGenerationContext.TypeGenerationKind.COMPONENT){
					result.append(".componentType(");
				}
				else {
					throw new IllegalStateException("Void type can't appear as a " + kind + " type");
				}

				result.append(AbstractWebSourceGenerationContext.this.getTypeName(AbstractWebSourceGenerationContext.this.getVoidType())).append(".class).build()");

				return result;
			}
		}

		@Override
		public StringBuilder visitUnknown(TypeMirror type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitUnion(UnionType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			// multi-catch: Type1 | Type2
			// I don't need/want to support that => throw an IllegalStateException
			throw new IllegalStateException(type.getKind() + " can't be converted to a viable Type");
		}

		@Override
		public StringBuilder visitIntersection(IntersectionType type, AbstractWebSourceGenerationContext.TypeGenerationKind kind) {
			// <T extends Number & Runnable>
			// I don't know in which case this might happen but we never know...
			if(kind == null) {
				throw new IllegalStateException("IntersectionType can't exist on its own");
			}
			else {
				StringBuilder result = new StringBuilder();

				if(kind != AbstractWebSourceGenerationContext.TypeGenerationKind.UPPERBOUND && kind != AbstractWebSourceGenerationContext.TypeGenerationKind.LOWERBOUND) {
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
