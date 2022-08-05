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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.reflect.TypeUtils;

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
 * <pre>{@code
 * Type listOfStringType = Types.type(List.class).type(String.class).and().build();
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
	
	/**
	 * <p>
	 * Determines the raw type of the specified type.
	 * </p>
	 * 
	 * @param type a type
	 * 
	 * @return a raw type
	 * @throws IllegalArgumentException if it is not possible to determine a raw
	 *                                  type from the specified type
	 */
	public static Class<?> toClass(Type type) throws IllegalArgumentException {
		if(type instanceof GenericArrayType) {
			return Array.newInstance(toClass(((GenericArrayType)type).getGenericComponentType()), 0).getClass();
		}
		else if(type instanceof ParameterizedType) {
			return getRawType((ParameterizedType)type);
		}
		else if(type instanceof TypeVariable) {
			throw new IllegalArgumentException("Can't deduce Class from a type variable");
		}
		else if(type instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType)type;
			
			if(wildcardType.getUpperBounds().length == 1) {
				return toClass(wildcardType.getUpperBounds()[0]);
			}
			else if(wildcardType.getUpperBounds().length > 1) {
				throw new IllegalArgumentException("Can't deduce a unique Class from a type with multiple upper bounds");
			}
			else {
				return Object.class;
			}
		}
		else if(type instanceof Class){
			return (Class<?>)type;
		}
		else {
			// let's crash for now
			throw new IllegalArgumentException("Unsupported type: " + type.getClass());
		}
	}
	
	/**
	 * <p>
	 * Determines the actual argument type of a method type variable based on a map
	 * of type arguments extracted from an enclosing type and a set of method
	 * parameters.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	private static class MethodTypeArgumentsSupplier implements Function<TypeVariable<?>, Type> {

		private final Set<TypeVariable<?>> methodTypeParameters;
		
		private final Map<TypeVariable<?>, Type> typeArguments;
		
		/**
		 * <p>
		 * Creates a method type arguments supplier.
		 * </p>
		 * 
		 * @param methodTypeParameters an array of method type parameters
		 * @param typeArguments        a map of type arguments
		 */
		public MethodTypeArgumentsSupplier(TypeVariable<Method>[] methodTypeParameters, Map<TypeVariable<?>, Type> typeArguments) {
			this.methodTypeParameters = new HashSet<>(Arrays.asList(methodTypeParameters));
			this.typeArguments = typeArguments;
		}
		
		@Override
		public Type apply(TypeVariable<?> typeVar) {
			Type typeArg = this.typeArguments.get(typeVar);
			if(typeArg != null) {
				return typeArg;
			}
			else if(typeArg == null && methodTypeParameters.remove(typeVar)) {
				// Let's look for this one
				if(typeVar.getBounds().length > 1) {
					// Let's only consider first bound for now, the API doesn't support this anyway
					throw new IllegalArgumentException("Method type parameters with multiple bounds is not supported");
				}
				Type[] typeArgs = Arrays.stream(typeVar.getBounds()).map(boundType -> parameterizeType(boundType, this)).toArray(Type[]::new);
				this.typeArguments.put(typeVar, typeArgs[0]);
				return typeArgs[0];
			}
			else {
				throw new IllegalArgumentException("Missing type argument for " + typeVar.getName());
			}
		}
	}
	
	/**
	 * <p>
	 * Returns the type of an accessible object when viewed as a member of, or
	 * otherwise directly contained by, a given type.
	 * </p>
	 * 
	 * @param containing the containing type
	 * @param accessor   the accessible object
	 * 
	 * @return a type
	 * @throws IllegalArgumentException if the specified accessor is neither a field
	 *                                  nor a method or if there are missing type
	 *                                  arguments in the containing type
	 */
	public static Type typeAsMemberOf(Type containing, AccessibleObject accessor) throws IllegalArgumentException {
		if(accessor instanceof Field) {
			return typeAsMemberOf(containing, (Field)accessor);
		}
		else if(accessor instanceof Method) {
			return typeAsMemberOf(containing, (Method)accessor);
		}
		else {
			throw new IllegalArgumentException("Unsupported constructor accessor");
		}
	}
	
	/**
	 * <p>
	 * Returns the type of a generic method return type when viewed as a member of,
	 * or otherwise directly contained by, a given type.
	 * </p>
	 * 
	 * @param containing the containing type
	 * @param method     the method
	 * 
	 * @return a type
	 * @throws IllegalArgumentException if there are missing type arguments in the
	 *                                  containing type
	 */
	public static Type typeAsMemberOf(Type containing, Method method) {
		Type genericType = method.getGenericReturnType();
		Class<?> containingClass = extractContainingClass(containing, method);
		
		Map<TypeVariable<?>, Type> typeArguments = new HashMap<>(TypeUtils.getTypeArguments(containing, containingClass));
		
		if(method.getTypeParameters().length > 0) {
			MethodTypeArgumentsSupplier methodTypeArgumentsSupplier = new MethodTypeArgumentsSupplier(method.getTypeParameters(), typeArguments);
			// Since we have a method we shouldn't have any cycles and things should work
			for(TypeVariable<?> typeVar : method.getTypeParameters()) {
				if(!typeArguments.containsKey(typeVar)) {
					methodTypeArgumentsSupplier.apply(typeVar);
				}
			}
		}
		
		return parameterizeType(genericType, typeVar -> {
			Type typeArg = typeArguments.get(typeVar);
			if(typeArg == null) {
				throw new IllegalArgumentException("Missing type argument for " + ((TypeVariable<?>)genericType).getName());
			}
			return typeArg;
		});
	}
	
	/**
	 * <p>
	 * Returns the type of a field when viewed as a member of, or otherwise directly
	 * contained by, a given type.
	 * </p>
	 * 
	 * @param containing the containing type
	 * @param field      the field
	 * 
	 * @return a type
	 * @throws IllegalArgumentException if there are missing type arguments in the
	 *                                  containing type
	 */
	public static Type typeAsMemberOf(Type containing, Field field) {
		Type genericType = field.getGenericType();
		Class<?> containingClass = extractContainingClass(containing, field);
		Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(containing, containingClass);
		
		return parameterizeType(genericType, typeVar -> {
			Type typeArg = typeArguments.get(typeVar);
			if(typeArg == null) {
				throw new IllegalArgumentException("Missing type argument for " + ((TypeVariable<?>)genericType).getName());
			}
			return typeArg;
		});
	}
	
	/**
	 * <p>
	 * Returns the raw type of a containing type and checks that the specified
	 * member is actually a member of that class.
	 * </p>
	 * 
	 * @param containing the containing type
	 * @param member     a member
	 * 
	 * @return a raw type
	 * @throws IllegalArgumentException if the specified member is not a member of
	 *                                  the containing type or the containing type
	 *                                  is not an actual type
	 */
	private static Class<?> extractContainingClass(Type containing, Member member) throws IllegalArgumentException {
		Class<?> memberDeclaringClass = member.getDeclaringClass();
		Class<?> containingRawType;
		if(containing instanceof Class) {
			if(!memberDeclaringClass.isAssignableFrom((Class<?>)containing)) {
				throw new IllegalArgumentException(member.getName() + " is not a member of " + containing);
			}
			containingRawType = (Class<?>)containing;
		}
		else if(containing instanceof ParameterizedType) {
			containingRawType = getRawType((ParameterizedType)containing);
			
			if(!memberDeclaringClass.isAssignableFrom((Class<?>)containingRawType)) {
				throw new IllegalArgumentException(member.getName() + " is not a member of " + containing);
			}
		}
		else {
			throw new IllegalArgumentException("Containing type must be a Class or a parameterized type");
		}
		return containingRawType;
	}
	
	/**
	 * <p>
	 * Returns the raw type of the specified parameterized type.
	 * </p>
	 * 
	 * @param type a parameterized type
	 * @return a raw type
	 */
	private static Class<?> getRawType(ParameterizedType type) {
		Type rawType = type.getRawType();
		if(!(rawType instanceof Class)) {
			throw new IllegalArgumentException("Raw type " + rawType + " of " + type + " is not a class");
		}
		return (Class<?>)rawType;
	}
	
	/**
	 * <p>
	 * Returns a type parameterized using the specified supplier of type arguments.
	 * </p>
	 * 
	 * @param genericType a generic type
	 * @param typeArgumentSupplier a type arguments supplier
	 * @return a type
	 */
	private static Type parameterizeType(Type genericType, Function<TypeVariable<?>, Type> typeArgumentSupplier) {
		return parameterizeType(genericType, null, typeArgumentSupplier);
	}
	
	/**
	 * <p>
	 * Returns a type with the specified owner type parameterized using the
	 * specified supplier of type arguments.
	 * </p>
	 * 
	 * @param genericType          a generic type
	 * @param ownerType            a owner type
	 * @param typeArgumentSupplier a type arguments supplier
	 * @return a type
	 */
	private static Type parameterizeType(Type genericType, Type ownerType, Function<TypeVariable<?>, Type> typeArgumentSupplier) {
		if(genericType == null) {
			return null;
		}
		else if(genericType instanceof GenericArrayType) {
			Type componentType = parameterizeType(((GenericArrayType)genericType).getGenericComponentType(), typeArgumentSupplier);
			if(componentType instanceof Class) {
				return Array.newInstance((Class<?>)componentType, 0).getClass();
			}
			else {
				return new GenericArrayTypeImpl(componentType);
			}
		}
		else if(genericType instanceof ParameterizedType) {
			Class<?> rawType = getRawType((ParameterizedType)genericType);
			Type actualOwnerType = ownerType != null ? ownerType : extractOwnerType(rawType, typeArgumentSupplier);
			// Owner type and bounds are validated in the ParameterizedTypeImpl constructor
			return new ParameterizedTypeImpl(rawType, Arrays.stream(((ParameterizedType)genericType).getActualTypeArguments()).map(typeArg -> parameterizeType(typeArg, typeArgumentSupplier)).toArray(Type[]::new), actualOwnerType);
		}
		else if(genericType instanceof TypeVariable) {
			return typeArgumentSupplier.apply((TypeVariable<?>)genericType);
		}
		else if(genericType instanceof WildcardType) {
			Type[] upperBounds = Arrays.stream(((WildcardType)genericType).getUpperBounds())
				.map(t -> {
					Type boundType = parameterizeType(t, typeArgumentSupplier);
					if(boundType instanceof WildcardType) {
						return null;
					}
					return boundType;
				})
				.filter(Objects::nonNull)
				.toArray(Type[]::new);
			
			Type[] lowerBounds = Arrays.stream(((WildcardType)genericType).getLowerBounds())
				.map(t -> {
					Type boundType = parameterizeType(t, typeArgumentSupplier);
					if(boundType instanceof WildcardType) {
						return null;
					}
					return boundType;
				})
				.filter(Objects::nonNull)
				.toArray(Type[]::new);
			
			return new WildcardTypeImpl(upperBounds, lowerBounds);
		}
		else if(genericType instanceof Class){
			Class<?> rawType = (Class<?>)genericType;
			Type actualOwnerType = ownerType != null ? ownerType : extractOwnerType(rawType, typeArgumentSupplier);
			TypeBuilder builder = Types.type(rawType).ownerType(actualOwnerType);
			// We don't need to consider owner type's bound here, they should only be considered when extracting type arguments
			for(TypeVariable<?> typeVar : rawType.getTypeParameters()) {
				builder.type(typeArgumentSupplier.apply(typeVar));
			}
			return builder.build();
		}
		else {
			// let's crash for now
			throw new IllegalArgumentException("Unsupported type: " + genericType);
		}
	}
	
	/**
	 * <p>
	 * Returns the owner type of the specified raw type parameterized using the
	 * specified supplier of type arguments.
	 * </p>
	 * 
	 * @param rawType              a raw type
	 * @param typeArgumentSupplier a type arguments supplier
	 * @return a type
	 */
	private static Type extractOwnerType(Class<?> rawType, Function<TypeVariable<?>, Type> typeArgumentSupplier) {
		if(!Modifier.isStatic(rawType.getModifiers())) {
			return parameterizeType(rawType.getEnclosingClass(), typeArgumentSupplier);
		}
		else {
			return rawType.getEnclosingClass();
		}
	}
	
	/**
	 * <p>
	 * Determines whether a type can be assignable to another.
	 * </p>
	 * 
	 * <p>When a type is assignable to another, it can be implicitly casted to the other one following the Java generics rules.</p>
	 * 
	 * @param source a source type
	 * @param target a target type
	 * @return true if the source type is assignable to the target type
	 */
	public static boolean isAssignable(Type source, Type target) {
		return TypeUtils.isAssignable(source, target);
	}
}
