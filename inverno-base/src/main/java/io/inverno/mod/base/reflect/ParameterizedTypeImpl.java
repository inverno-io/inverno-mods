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
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;


/**
 * <p>{@link ParameterizedType} implementation.</p>
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

	public ParameterizedTypeImpl(Class<?> rawType, Type[] actualTypeArguments, Type ownerType) {
		this.actualTypeArguments = actualTypeArguments;
		this.rawType = rawType;
		this.ownerType = (ownerType != null) ? ownerType : rawType.getDeclaringClass();
		validateConstructorArguments();
	}

	private void validateConstructorArguments() {
		TypeVariable<?>[] formals = rawType.getTypeParameters();
		// check correct arity of actual type args
		if (formals.length != actualTypeArguments.length) {
			throw new MalformedParameterizedTypeException();
		}
		for (int i = 0; i < actualTypeArguments.length; i++) {
			// check actuals against formals' bounds
		}
	}

	/**
	 * <p>
	 * Returns an array of {@code Type} objects representing the actual type
	 * arguments to this type.
	 * </p>
	 *
	 * <p>
	 * Note that in some cases, the returned array be empty. This can occur if this
	 * type represents a non-parameterized type nested within a parameterized type.
	 * </p>
	 *
	 * @return an array of {@code Type} objects representing the actual type
	 *         arguments to this type
	 * @throws TypeNotPresentException             if any of the actual type
	 *                                             arguments refers to a
	 *                                             non-existent type declaration
	 * @throws MalformedParameterizedTypeException if any of the actual type
	 *                                             parameters refer to a
	 *                                             parameterized type that cannot be
	 *                                             instantiated for any reason
	 */
	public Type[] getActualTypeArguments() {
		return actualTypeArguments.clone();
	}

	/**
	 * <p>
	 * Returns the {@code Type} object representing the class or interface that
	 * declared this type.
	 * </p>
	 *
	 * @return the {@code Type} object representing the class or interface that
	 *         declared this type
	 */
	public Class<?> getRawType() {
		return rawType;
	}

	/**
	 * <p>
	 * Returns a {@code Type} object representing the type that this type is a
	 * member of. For example, if this type is {@code O<T>.I<S>}, return a
	 * representation of {@code O<T>}.
	 * </p>
	 * 
	 * <p>
	 * If this type is a top-level type, {@code null} is returned.
	 * </p>
	 *
	 * @return a {@code Type} object representing the type that this type is a
	 *         member of. If this type is a top-level type, {@code null} is returned
	 * @throws TypeNotPresentException             if the owner type refers to a
	 *                                             non-existent type declaration
	 * @throws MalformedParameterizedTypeException if the owner type refers to a
	 *                                             parameterized type that cannot be
	 *                                             instantiated for any reason
	 *
	 */
	public Type getOwnerType() {
		return ownerType;
	}

	/*
	 * From the JavaDoc for java.lang.reflect.ParameterizedType "Instances of
	 * classes that implement this interface must implement an equals() method that
	 * equates any two instances that share the same generic type declaration and
	 * have equal type parameters."
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ParameterizedType) {
			// Check that information is equivalent
			ParameterizedType that = (ParameterizedType) o;

			if (this == that)
				return true;

			Type thatOwner = that.getOwnerType();
			Type thatRawType = that.getRawType();

			return Objects.equals(ownerType, thatOwner) && Objects.equals(rawType, thatRawType)
					&& Arrays.equals(actualTypeArguments, // avoid clone
							that.getActualTypeArguments());
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(actualTypeArguments) ^ Objects.hashCode(ownerType) ^ Objects.hashCode(rawType);
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
			sb.append(sj.toString());
		}

		return sb.toString();
	}
}
