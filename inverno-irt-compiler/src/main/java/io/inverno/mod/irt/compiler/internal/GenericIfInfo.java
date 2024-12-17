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

import io.inverno.mod.irt.compiler.spi.IfInfo;
import io.inverno.mod.irt.compiler.spi.StatementInfo;

/**
 * <p>
 * Generic {@link IfInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericIfInfo extends BaseInfo implements IfInfo {

	private final CaseInfo[] cases;
	
	/**
	 * <p>
	 * Creates a generic if info.
	 * </p>
	 * 
	 * @param range the range in the IRT source file where the info is defined
	 * @param cases a list of cases
	 */
	public GenericIfInfo(Range range, List<CaseInfo> cases) {
		super(range);
		this.cases = cases.toArray(CaseInfo[]::new);
	}

	@Override
	public CaseInfo[] getCases() {
		return this.cases;
	}

	/**
	 * <p>
	 * Generic {@link IfInfo.CaseInfo} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 */
	public static class GenericCaseInfo extends BaseInfo implements IfInfo.CaseInfo {

		private final Optional<String> guardExpression;
		private final StatementInfo[] statements;
		
		/**
		 * <p>
		 * Creates a generic case info.
		 * </p>
		 * 
		 * @param range           the range in the IRT source file where the info is defined
		 * @param guardExpression a guard expression
		 * @param statements      a list of statements
		 */
		public GenericCaseInfo(Range range, String guardExpression, List<StatementInfo> statements) {
			super(range);
			this.guardExpression = Optional.ofNullable(guardExpression);
			this.statements = statements.toArray(StatementInfo[]::new);
		}
		
		@Override
		public Optional<String> getGuardExpression() {
			return this.guardExpression;
		}

		@Override
		public StatementInfo[] getStatements() {
			return this.statements;
		}
	}
}
