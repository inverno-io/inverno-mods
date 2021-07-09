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

import io.inverno.mod.irt.compiler.spi.ApplyInfo;
import io.inverno.mod.irt.compiler.spi.ParameterInfo;
import io.inverno.mod.irt.compiler.spi.ValueInfo;

/**
 * <p>
 * Generic {@link ApplyInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public class GenericApplyInfo extends BaseInfo implements ApplyInfo {

	private final Optional<ValueInfo> value;
	private final TargetParameterInfo[] targetParameters;
	private final TargetInfo[] targets;

	/**
	 * <p>
	 * Creates a generic apply info.
	 * </p>
	 * 
	 * @param range            the range in the IRT source file where the info is
	 *                         defined
	 * @param value            a value
	 * @param targetParameters a list of target parameters
	 * @param targets          a list of targets
	 */
	public GenericApplyInfo(Range range, ValueInfo value, List<TargetParameterInfo> targetParameters, List<TargetInfo> targets) {
		super(range);
		this.value = Optional.ofNullable(value);
		this.targetParameters = targetParameters.stream().toArray(TargetParameterInfo[]::new);
		this.targets = targets.stream().toArray(TargetInfo[]::new);
	}

	@Override
	public Optional<ValueInfo> getValue() {
		return this.value;
	}

	@Override
	public TargetParameterInfo[] getTargetParameters() {
		return this.targetParameters;
	}

	@Override
	public TargetInfo[] getTargets() {
		return this.targets;
	}

	/**
	 * <p>
	 * Generic {@link TargetParameterInfo} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 */
	public static class GenericTargetParameterInfo extends BaseInfo implements TargetParameterInfo {

		private final Optional<String> name;
		private final Optional<ParameterInfo> parameter;

		/**
		 * <p>
		 * Creates a generic target parameter info.
		 * </p>
		 * 
		 * @param range     the range in the IRT source file where the info is defined
		 * @param name      a name
		 * @param parameter a parameter
		 */
		public GenericTargetParameterInfo(Range range, String name, ParameterInfo parameter) {
			super(range);
			this.name = Optional.ofNullable(name);
			this.parameter = Optional.ofNullable(parameter);
		}

		@Override
		public Optional<String> getName() {
			return this.name;
		}

		@Override
		public Optional<ParameterInfo> getParameter() {
			return this.parameter;
		}
	}

	/**
	 * <p>
	 * Generic {@link TargetInfo} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 */
	public static class GenericTargetInfo extends BaseInfo implements TargetInfo {

		private final Optional<String> name;
		private final Optional<ArgumentInfo[]> arguments;
		private final Optional<String> guardExpression;

		/**
		 * <p>
		 * Creates a generic target info.
		 * </p>
		 * 
		 * @param range           the range in the IRT source file where the info is
		 *                        defined
		 * @param name            a name
		 * @param arguments       a list of arguments
		 * @param guardExpression a guard expression
		 */
		public GenericTargetInfo(Range range, String name, List<ArgumentInfo> arguments, String guardExpression) {
			super(range);
			this.name = Optional.ofNullable(name);
			this.arguments = Optional.ofNullable(arguments).map(l -> l.stream().toArray(ArgumentInfo[]::new))
					.filter(arr -> arr.length > 0);
			this.guardExpression = Optional.ofNullable(guardExpression);
		}

		@Override
		public Optional<String> getName() {
			return this.name;
		}

		@Override
		public Optional<ArgumentInfo[]> getArguments() {
			return this.arguments;
		}

		@Override
		public Optional<String> getGuardExpression() {
			return this.guardExpression;
		}
	}

	/**
	 * <p>
	 * Generic {@link ArgumentInfo} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 */
	public static class GenericArgumentInfo extends BaseInfo implements ArgumentInfo {

		private final String value;

		/**
		 * <p>
		 * Creates a generic argument info.
		 * </p>
		 * 
		 * @param range the range in the IRT source file where the info is defined
		 * @param value a value expression
		 */
		public GenericArgumentInfo(Range range, String value) {
			super(range);
			this.value = value;
		}

		@Override
		public String getValue() {
			return this.value;
		}
	}
}
