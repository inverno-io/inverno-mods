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

/**
 * <p>
 * An array type argument builder is used to specify the component type of an
 * array type.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ArrayTypeBuilder
 * @see Types
 * 
 * @param <A> the type of the parent builder
 */
public interface ArrayTypeArgumentBuilder<A> {

	/**
	 * <p>
	 * Specifies the component type of the array type.
	 * </p>
	 * 
	 * @param rawType an erased type
	 * @return a type argument builder with this builder as parent
	 */
	TypeArgumentBuilder<ArrayTypeArgumentBuilder<A>> componentType(Class<?> rawType);
	
	/**
	 * <p>
	 * Specifies an array type as component type of the array type.
	 * </p>
	 * 
	 * @return an array type argument builder with this builder as parent
	 */
	ArrayTypeArgumentBuilder<ArrayTypeArgumentBuilder<A>> componentArrayType();
	
	/**
	 * <p>
	 * Finalizes this builder and returns the parent builder.
	 * </p>
	 * 
	 * @return the parent builder
	 * @throws IllegalStateException if the builder is not in a proper state to
	 *                               be finalized
	 */
	A and() throws IllegalStateException;
}
