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
package io.inverno.mod.irt.compiler.internal;

import java.util.List;
import java.util.Optional;

import io.inverno.mod.irt.compiler.spi.NameInfo;
import io.inverno.mod.irt.compiler.spi.PipeInfo;
import io.inverno.mod.irt.compiler.spi.ApplyInfo.ArgumentInfo;

/**
 * <p>
 * Generic {@link PipeInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public class GenericPipeInfo extends BaseInfo implements PipeInfo {

	private final Optional<String> expression;
	
	private final Optional<NameInfo> name;

	private final Optional<ArgumentInfo[]> arguments;

	/**
	 * <p>
	 * Creates a generic pipe info.
	 * </p>
	 * 
	 * @param range      the range in the IRT source file where the info is defined
	 * @param name       a name
	 * @param arguments  a list of arguments
	 * @param expression an expression
	 */
	public GenericPipeInfo(Range range, NameInfo name, List<ArgumentInfo> arguments, String expression) {
		super(range);
		this.expression = Optional.ofNullable(expression);
		this.name = Optional.ofNullable(name);
		this.arguments = Optional.ofNullable(arguments).map(l -> l.stream().toArray(ArgumentInfo[]::new))
				.filter(arr -> arr.length > 0);
	}
	
	@Override
	public Optional<String> getExpression() {
		return this.expression;
	}

	@Override
	public Optional<NameInfo> getName() {
		return this.name;
	}

	@Override
	public Optional<ArgumentInfo[]> getArguments() {
		return this.arguments;
	}

	
}
