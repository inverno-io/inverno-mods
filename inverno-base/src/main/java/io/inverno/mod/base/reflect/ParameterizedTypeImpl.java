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
package io.inverno.mod.base.reflect;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * <p>
 * {@link ParameterizedType} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ParameterizedType
 */
class ParameterizedTypeImpl implements ParameterizedType {

	private final Type[] actualTypeArguments;
	private final Class<?> rawType;
	private final Type ownerType;

	/**
	 * <p>
	 * Creates a parameterized type.
	 * </p>
	 *
	 * @param rawType             the raw type
	 * @param actualTypeArguments the type arguments
	 * @param ownerType           the owner type
	 */
	public ParameterizedTypeImpl(Class<?> rawType, Type[] actualTypeArguments, Type ownerType) {
		this.actualTypeArguments = actualTypeArguments;
		this.rawType = rawType;
		if(rawType.getEnclosingClass() == null) {
			if(ownerType != null) {
				throw new MalformedParameterizedTypeException(/*"You can't specify an owner type on " + this.rawType*/);
			}
			this.ownerType = null;
		}
		else if(ownerType == null) {
			this.ownerType = rawType.getEnclosingClass();
		}
		else if(Types.isAssignable(ownerType, rawType.getEnclosingClass())) {
			this.ownerType = ownerType;
		}
		else {
			throw new MalformedParameterizedTypeException(/*"Owner type " + this.ownerType + " is not assignable to " + rawType.getEnclosingClass()*/);
		}
		
		TypeVariable<?>[] formals = rawType.getTypeParameters();
		// check correct arity of actual type args
		if (formals.length != this.actualTypeArguments.length) {
			throw new MalformedParameterizedTypeException(/*"Actual arguments does not match type parameters arity"*/);
		}
		
		Set<String> variables = null;
		for (int i = 0; i < this.actualTypeArguments.length; i++) {
			if(this.actualTypeArguments[i] instanceof TypeVariable<?>) {
				if(variables == null) {
					variables = new HashSet<>();
				}
				if(!variables.add(((TypeVariable<?>)this.actualTypeArguments[i]).getName())) {
					throw new MalformedParameterizedTypeException(/*"Duplicate type parameter " + ((TypeVariable<?>)this.actualTypeArguments[i]).getName()*/); 
				}
			}
			
			// check actual against formal bounds
			Map<TypeVariable<?>, Type> typeArguments = Map.of();
			if(this.ownerType instanceof ParameterizedType) {
				typeArguments = TypeUtils.getTypeArguments((ParameterizedType)this.ownerType);
			}
			
			for(Type formalBound : formals[i].getBounds()) {
				Type actualBound = formalBound instanceof TypeVariable<?> ? typeArguments.get((TypeVariable<?>)formalBound) : formalBound;
				if(this.actualTypeArguments[i] instanceof WildcardType) {
					// Wildcard types don't allow for multiple bounds which is why we only consider the first bound
					// see WildcardType comments...
					if(!this.actualTypeArguments[i].equals(WildcardTypeImpl.WILDCARD_ALL)) {
						if(((WildcardType)this.actualTypeArguments[i]).getLowerBounds().length > 0) {
							// ? super ...
							// we must check that the super bound is assignable to the formal bound
							if(!Types.isAssignable(((WildcardType)this.actualTypeArguments[i]).getLowerBounds()[0], actualBound)) {
								throw new MalformedParameterizedTypeException(/*"Type " + ((WildcardType)this.actualTypeArguments[i]).getLowerBounds()[0] + " can't be assigned to type " + actualBound*/);
							}
						}
						else {
							// ? extends ...
							// we must check that the extends bound is assignable to the formal bound
							if(!Types.isAssignable(((WildcardType)this.actualTypeArguments[i]).getUpperBounds()[0], actualBound)) {
								throw new MalformedParameterizedTypeException(/*"Type " + ((WildcardType)this.actualTypeArguments[i]).getUpperBounds()[0] + " can't be assigned to type " + actualBound*/);
							}
						}
					}
				}
				else if(this.actualTypeArguments[i] instanceof TypeVariable<?>) {
					if(((TypeVariable<?>)this.actualTypeArguments[i]).getBounds().length > 0) {
						// T extends ...
						// we must check that the bound is assignable to the formal bound
						if(!Types.isAssignable(((TypeVariable<?>)this.actualTypeArguments[i]).getBounds()[0], actualBound)) {
							throw new MalformedParameterizedTypeException(/*"Type " + ((TypeVariable<?>)this.actualTypeArguments[i]).getBounds()[0] + " can't be assigned to type " + actualBound*/);
						}
					}
				}
				else if(!TypeUtils.isAssignable(this.actualTypeArguments[i], actualBound)) {
					throw new MalformedParameterizedTypeException(/*"Type " + this.actualTypeArguments[i] + " can't be assigned to type " + actualBound*/);
				}
			}
		}
	}

	/**
	 * <p>
	 * Returns an array of {@code Type} objects representing the actual type arguments to this type.
	 * </p>
	 *
	 * <p>
	 * Note that in some cases, the returned array be empty. This can occur if this type represents a non-parameterized type nested within a parameterized type.
	 * </p>
	 *
	 * @return an array of {@code Type} objects representing the actual type arguments to this type
	 *
	 * @throws TypeNotPresentException             if any of the actual type arguments refers to a non-existent type declaration
	 * @throws MalformedParameterizedTypeException if any of the actual type parameters refer to a parameterized type that cannot be instantiated for any reason
	 */
	@Override
	public Type[] getActualTypeArguments() {
		return actualTypeArguments.clone();
	}

	/**
	 * <p>
	 * Returns the {@code Type} object representing the class or interface that declared this type.
	 * </p>
	 *
	 * @return the {@code Type} object representing the class or interface that declared this type
	 */
	@Override
	public Class<?> getRawType() {
		return rawType;
	}

	/**
	 * <p>
	 * Returns a {@code Type} object representing the type that this type is a member of. For example, if this type is {@code O<T>.I<S>}, return a representation of {@code O<T>}.
	 * </p>
	 * 
	 * <p>
	 * If this type is a top-level type, {@code null} is returned.
	 * </p>
	 *
	 * @return a {@code Type} object representing the type that this type is a member of. If this type is a top-level type, {@code null} is returned
	 *
	 * @throws TypeNotPresentException             if the owner type refers to a non-existent type declaration
	 * @throws MalformedParameterizedTypeException if the owner type refers to a parameterized type that cannot be instantiated for any reason
	 */
	@Override
	public Type getOwnerType() {
		return ownerType;
	}

	/*
	 * From the JavaDoc for java.lang.reflect.ParameterizedType "Instances of classes that implement this interface must implement an equals() method that equates any two instances that share the same
	 * generic type declaration and have equal type parameters."
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ParameterizedTypeImpl that = (ParameterizedTypeImpl) o;
		return Objects.deepEquals(actualTypeArguments, that.actualTypeArguments) && Objects.equals(rawType, that.rawType) && Objects.equals(ownerType, that.ownerType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(Arrays.hashCode(actualTypeArguments), rawType, ownerType);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (ownerType != null) {
			sb.append(ownerType.getTypeName());

			sb.append("$");

			if (ownerType instanceof ParameterizedTypeImpl) {
				// Find simple name of nested type by removing the
				// shared prefix with owner.
				sb.append(rawType.getName().replace(((ParameterizedTypeImpl) ownerType).rawType.getName() + "$", ""));
			} else
				sb.append(rawType.getSimpleName());
		} else
			sb.append(rawType.getName());

		if (actualTypeArguments != null) {
			StringJoiner sj = new StringJoiner(", ", "<", ">");
			sj.setEmptyValue("");
			for (Type t : actualTypeArguments) {
				sj.add(t.getTypeName());
			}
			sb.append(sj);
		}
		return sb.toString();
	}
}
