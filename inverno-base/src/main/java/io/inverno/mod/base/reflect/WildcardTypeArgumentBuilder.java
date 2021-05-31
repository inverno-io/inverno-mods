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
 * A wildcard type argument builder is used to specify the bounds of a
 * parameterized type argument.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * 
 * @param <A> the type of the parent builder
 */
public interface WildcardTypeArgumentBuilder<A> {

	/**
	 * <p>
	 * Specifies the upper bound type of the parameterized type argument.
	 * </p>
	 * 
	 * @param rawType an erased type
	 * 
	 * @return a type argument builder with this builder as parent
	 */
	TypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> upperBoundType(Class<?> rawType);
	
	/**
	 * <p>
	 * Specifies an array type as upper bound type of the parameterized type
	 * argument.
	 * </p>
	 * 
	 * @return an array type argument builder with this builder as parent
	 */
	ArrayTypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> upperBoundArrayType();
	
	/**
	 * <p>
	 * Specifies the lower bound type of the parameterized type argument.
	 * </p>
	 * 
	 * @param rawType an erased type
	 * 
	 * @return a type argument builder with this builder as parent
	 */
	TypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> lowerBoundType(Class<?> rawType);
	
	/**
	 * <p>
	 * Specifies an array type as lower bound type of the parameterized type
	 * argument.
	 * </p>
	 * 
	 * @return an array type argument builder with this builder as parent
	 */
	ArrayTypeArgumentBuilder<WildcardTypeArgumentBuilder<A>> lowerBoundArrayType();
	
	/**
	 * <p>
	 * Finalizes this builder and returns the parent builder.
	 * </p>
	 * 
	 * @return the parent builder
	 * @throws IllegalStateException if the builder is not in a proper state to
	 *                               be finalized
	 */
	A and();
}
