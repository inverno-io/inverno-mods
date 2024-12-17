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

import java.lang.reflect.Type;

/**
 * <p>
 * A type argument builder is used to specify the arguments of a parameterized type.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * 
 * @param <A> the type of the parent builder
 */
public interface TypeArgumentBuilder<A> {

	/**
	 * <p>
	 * Specifies a type as the next argument of the parameterized type.
	 * </p>
	 * 
	 * @param rawType an erased type
	 *
	 * @return a type argument builder with this builder as parent
	 */
	TypeArgumentBuilder<TypeArgumentBuilder<A>> type(Class<?> rawType);

	/**
	 * <p>
	 * Specifies a type as the next argument of the parameterized type.
	 * </p>
	 * 
	 * @param type a type
	 *
	 * @return this builder
	 */
	TypeArgumentBuilder<A> type(Type type);
	
	/**
	 * <p>
	 * Specifies a variable type as the next argument of the parameterized type.
	 * </p>
	 * 
	 * @param name The name of the variable
	 *
	 * @return a type variable builder with this builder as parent
	 */
	TypeVariableBuilder<TypeArgumentBuilder<A>> variableType(String name);
	
	/**
	 * <p>
	 * Specifies a wildcard type as the next argument of the parameterized type.
	 * </p>
	 * 
	 * @return a wildcard type argument builder with this builder as parent
	 */
	WildcardTypeArgumentBuilder<TypeArgumentBuilder<A>> wildcardType();
	
	/**
	 * <p>
	 * Specifies an array type as the next argument of the parameterized type.
	 * </p>
	 * 
	 * @return an array type argument builder with this builder as parent
	 */
	ArrayTypeArgumentBuilder<TypeArgumentBuilder<A>> arrayType();
	
	/**
	 * <p>
	 * Specifies the owner type of the parameterized type.
	 * </p>
	 * 
	 * @param rawType the owner type to set
	 *
	 * @return a type argument builder with this builder as parent
	 */
	TypeArgumentBuilder<TypeArgumentBuilder<A>> ownerType(Class<?> rawType);
	
	/**
	 * <p>
	 * Finalizes this builder and returns the parent builder.
	 * </p>
	 * 
	 * @return the parent builder
	 *
	 * @throws IllegalStateException if the builder is not in a proper state to be finalized
	 */
	A and() throws IllegalStateException;
}
