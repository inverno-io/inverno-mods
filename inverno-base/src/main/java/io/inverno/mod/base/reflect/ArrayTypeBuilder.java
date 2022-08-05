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
 * An array type builder is used to build array types.
 * </p>
 * 
 * <p>A typical usage is:</p>
 * 
 * <pre>{@code
 * Type arrayOfStringType = Types.arrayType().componentType(String.class).and().build();
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Types
 */
public interface ArrayTypeBuilder {

	/**
	 * <p>
	 * Specifies the component type of the array type.
	 * </p>
	 * 
	 * @param rawType an erased type
	 * @return a type argument builder with this builder as parent
	 */
	TypeArgumentBuilder<ArrayTypeBuilder> componentType(Class<?> rawType);
	
	/**
	 * <p>
	 * Specifies the component type of the array type.
	 * </p>
	 * 
	 * @param type a type
	 * @return this builder
	 */
	ArrayTypeBuilder componentType(Type type);
	
	/**
	 * <p>
	 * Specifies an array type as component type of the array type.
	 * </p>
	 * 
	 * @return an array type argument builder with this builder as parent
	 */
	ArrayTypeArgumentBuilder<ArrayTypeBuilder> componentArrayType();
	
	/**
	 * <p>Builds the array type.</p>
	 * 
	 * @return a generic array type
	 * @throws IllegalStateException if the builder is not in a proper state to
	 *                               build a type
	 */
	Type build() throws IllegalStateException;
}
