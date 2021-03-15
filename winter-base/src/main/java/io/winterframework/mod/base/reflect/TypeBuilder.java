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
 * A type builder is used to build parameterized types.
 * </p>
 * 
 * <p>A typical usage is:</p>
 * 
 * <blockquote><pre>
 * Type listOfStringType = Types.type(List.class).type(String.class).and().build();
 * </pre></blockquote>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Types
 */
public interface TypeBuilder {

	/**
	 * <p>
	 * Specifies a type as the next argument of the parameterized type.
	 * </p>
	 * 
	 * @param rawType an erased type
	 * @return a type argument builder with this builder as parent
	 */
	TypeArgumentBuilder<TypeBuilder> type(Class<?> rawType);
	
	/**
	 * <p>
	 * Specifies a wildcard type as the next argument of the parameterized type.
	 * </p>
	 * 
	 * @return a wildcard type argument builder with this builder as parent
	 */
	WildcardTypeArgumentBuilder<TypeBuilder> wildcardType();
	
	/**
	 * <p>
	 * Specifies an array type as the next argument of the parameterized type.
	 * </p>
	 * 
	 * @return an array type argument builder with this builder as parent
	 */
	ArrayTypeArgumentBuilder<TypeBuilder> arrayType();
	
	/**
	 * <p>
	 * Specifies the owner type of the parameterized type.
	 * </p>
	 * 
	 * @param rawType the owner type to set
	 * @return a type argument builder with this builder as parent
	 */
	TypeArgumentBuilder<TypeBuilder> ownerType(Class<?> rawType);
	
	/**
	 * <p>Builds the type.</p>
	 * 
	 * @return a parameterized type
	 * @throws IllegalStateException if the builder is not in a proper state to
	 *                               build a type
	 */
	Type build();
}
