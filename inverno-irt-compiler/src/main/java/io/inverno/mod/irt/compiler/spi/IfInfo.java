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

/**
 * <p>
 * An if info corresponds to an if statement in a template declaration in a template set source file.
 * </p>
 *
 * <p>
 * An if statement basically specifies a conditional expression with different cases with guard expressions evaluated in sequence to determine the statements to apply based on the data model.
 * </p>
 *
 * <p>
 * There should be at most one case with no guard expression and it must comes last.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface IfInfo extends StatementInfo {

	/**
	 * <p>
	 * Returns the list of cases.
	 * </p>
	 *
	 * @return an array of cases
	 */
	CaseInfo[] getCases();

	/**
	 * <p>
	 * A case info specifies a guard expression and a corresponding list of statements in an if statement.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	interface CaseInfo {

		/**
		 * <p>
		 * Returns the guard expression.
		 * </p>
		 *
		 * @return an optional returning a guard expression or an empty optional if there's no guard expression (default case).
		 */
		Optional<String> getGuardExpression();

		/**
		 * <p>
		 * Returns the list of statements to evaluate when the guard expression evaluates to true.
		 * </p>
		 *
		 * @return an array of statements
		 */
		StatementInfo[] getStatements();
	}
}
