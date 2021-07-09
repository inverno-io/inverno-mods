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
import io.inverno.mod.irt.compiler.spi.ValueInfo;

/**
 * <p>
 * Generic {@link ValueInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public class GenericValueInfo extends BaseInfo implements ValueInfo {

	private final Optional<NameInfo> name;
	private final Optional<String> expression;
	private final PipeInfo[] pipes;
	
	/**
	 * <p>
	 * Creates a generic value info.
	 * </p>
	 * 
	 * @param range      the range in the IRT source file where the info is defined
	 * @param name       a name
	 * @param expression a raw Java expression
	 * @param pipes a list of pipes
	 */
	public GenericValueInfo(Range range, NameInfo name, String expression, List<PipeInfo> pipes) {
		super(range);
		this.name = Optional.ofNullable(name);
		this.expression = Optional.ofNullable(expression);
		this.pipes = pipes.stream().toArray(PipeInfo[]::new);
	}

	@Override
	public Optional<NameInfo> getName() {
		return this.name;
	}
	
	@Override
	public Optional<String> getExpression() {
		return this.expression;
	}
	
	@Override
	public PipeInfo[] getPipes() {
		return this.pipes;
	}
}
