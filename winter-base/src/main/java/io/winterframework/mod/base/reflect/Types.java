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
package io.winterframework.mod.base.reflect;

import java.lang.reflect.Type;

/**
 * <p>
 * Utility methods for Types manipulation.
 * </p>
 * 
 * <p>
 * {@link Type} allows to represent parameterized type at runtime which is not
 * possible with a regular {@link Class} due to type erasure. This can be useful
 * especially when one needs to perform reflective operation at runtime.
 * </p>
 * 
 * <p>A {@link TypeBuilder} makes it easy to create types, for instance:</p>
 * 
 * <blockquote><pre>
 * Type listOfStringType = Types.type(List.class).type(String.class).and().build();
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see TypeBuilder
 * @see ArrayTypeBuilder
 */
public final class Types {

	/**
	 * <p>
	 * Returns the boxed type corresponding to the specified primitive type.
	 * </p>
	 * 
	 * @param type a primitive type
	 *
	 * @return a boxed type
	 * @throws IllegalArgumentException if the specified type is not a primitive
	 *                                  type
	 */
	public static Class<?> boxType(Class<?> type) throws IllegalArgumentException {
		if(!type.isPrimitive()) {
			throw new IllegalArgumentException(type + " is not a primitive type");
		}
		
		if(type.equals(boolean.class)) {
			return Boolean.class;
		}
		else if(type.equals(byte.class)) {
			return Byte.class;
		}
		else if(type.equals(char.class)) {
			return Character.class;
		}
		else if(type.equals(short.class)) {
			return Short.class;
		}
		else if(type.equals(int.class)) {
			return Integer.class;
		}
		else if(type.equals(long.class)) {
			return Long.class;
		}
		else if(type.equals(float.class)) {
			return Float.class;
		}
		else if(type.equals(double.class)) {
			return Double.class;
		}
		else if(type.equals(void.class)) {
			return Void.class;
		}
		throw new IllegalArgumentException("Unknown primitive type: " + type);
	}

	/**
	 * <p>
	 * Returns the primitive type corresponding to the specified boxed type.
	 * </p>
	 * 
	 * @param type a boxed type
	 * 
	 * @return a primitive type
	 * @throws IllegalArgumentException if the specified type is not a boxed type
	 */
	public static Class<?> unboxType(Class<?> type) throws IllegalArgumentException {
		if(type.equals(Boolean.class)) {
			return Boolean.TYPE;
		}
		else if(type.equals(Byte.class)) {
			return Byte.TYPE;
		}
		else if(type.equals(Character.class)) {
			return Character.TYPE;
		}
		else if(type.equals(Short.class)) {
			return Short.TYPE;
		}
		else if(type.equals(Integer.class)) {
			return Integer.TYPE;
		}
		else if(type.equals(Long.class)) {
			return Long.TYPE;
		}
		else if(type.equals(Float.class)) {
			return Float.TYPE;
		}
		else if(type.equals(Double.class)) {
			return Double.TYPE;
		}
		else if(type.equals(Void.class)) {
			return Void.TYPE;
		}
		throw new IllegalArgumentException(type + " is not a boxed type");
	}
	
	/**
	 * <p>
	 * Creates a type builder with the specified raw type.
	 * </p>
	 * 
	 * @param rawType an erased type
	 * 
	 * @return a type builder
	 */
	public static TypeBuilder type(Class<?> rawType) {
		return new GenericTypeBuilder(rawType);
	}
	
	/**
	 * <p>
	 * Creates an array type builder.
	 * </p>
	 * 
	 * @return an array type builder
	 */
	public static ArrayTypeBuilder arrayType() {
		return new GenericArrayTypeBuilder();
	}
}
