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
 * @author jkuhn
 *
 */
public final class Types {

	public static Class<?> boxType(Class<?> type) {
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
	
	public static Class<?> unboxType(Class<?> type) {
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
	
	public static TypeBuilder type(Class<?> rawType) {
		return new TypeBuilderImpl(rawType);
	}
	
	public static ArrayTypeBuilder arrayType() {
		return new ArrayTypeBuilderImpl();
	}
	
	public static interface TypeBuilder {
		
		TypeArgumentBuilder<TypeBuilder> type(Class<?> rawType);
		
		WildcardTypeArgumentBuilder<TypeBuilder> wildcardType();
		
		ArrayTypeArgumentBuilder<TypeBuilder> arrayType();
		
		TypeArgumentBuilder<TypeBuilder> ownerType(Class<?> rawType);
		
		Type build();
		
	}
	
	public static interface TypeArgumentBuilder<A> {
		
		TypeArgumentBuilder<TypeArgumentBuilder<A>> type(Class<?> rawType);
		
		WildcardTypeArgumentBuilder<TypeArgumentBuilder<A>> wildcardType();
		
		ArrayTypeArgumentBuilder<TypeArgumentBuilder<A>> arrayType();
		
		TypeArgumentBuilder<TypeArgumentBuilder<A>> ownerType(Class<?> rawType);
		
		A and();
	}
	
	public static interface WildcardTypeArgumentBuilder<A> {
		
		TypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> upperBoundType(Class<?> rawType);
		
		ArrayTypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> upperBoundArrayType();
		
		TypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> lowerBoundType(Class<?> rawType);
		
		ArrayTypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> lowerBoundArrayType();
		
		A and();
	}
	
	public static interface ArrayTypeBuilder {
		
		TypeArgumentBuilder<ArrayTypeBuilder> componentType(Class<?> rawType);
		
		ArrayTypeArgumentBuilder<ArrayTypeBuilder> componentArrayType();
		
		Type build();
	}
	
	public static interface ArrayTypeArgumentBuilder<A> {
		
		TypeArgumentBuilder<A> componentType(Class<?> rawType);
		
		ArrayTypeArgumentBuilder<A> componentArrayType();
		
		A and();
	}
	
}
