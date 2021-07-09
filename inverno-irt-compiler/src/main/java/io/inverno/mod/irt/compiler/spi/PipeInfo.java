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
package io.inverno.mod.irt.compiler.spi;

import java.util.Optional;

import io.inverno.mod.irt.compiler.spi.ApplyInfo.ArgumentInfo;

/**
 * <p>
 * A pipe info specifies a pipe after a value in a value or apply statement.
 * </p>
 * 
 * <p>
 * Pipes are applied sequentially to the value before rendering the value or
 * applying templates.
 * </p>
 * 
 * <p>
 * A pipe info can be specified as a name pointing to a factory method with
 * optional arguments or as a raw Java expression.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface PipeInfo {

	/**
	 * <p>
	 * Returns the raw Java expression that evaluates to a pipe.
	 * </p>
	 * 
	 * @return an optional returning a raw Java expression or an empty optional if
	 *         the pipe is defined as a name instead.
	 */
	Optional<String> getExpression();

	/**
	 * <p>
	 * Returns the name that evaluate to a pipe.
	 * </p>
	 * 
	 * <p>
	 * The name should point to a factory method returning a pipe which may accept
	 * arguments in which case arguments must be provided.
	 * </p>
	 * 
	 * @return an optional returning a name or an empty optional if the pipe is
	 *         specified as an expression instead.
	 */
	Optional<NameInfo> getName();
	
	/**
	 * <p>
	 * Returns the list of arguments to pass to the pipe factory method.
	 * </p>
	 * 
	 * @return an optional returning the list of arguments or an empty optional if
	 *         the pipe has no argument or it has been defined with an expression.
	 */
	Optional<ArgumentInfo[]> getArguments();
}
