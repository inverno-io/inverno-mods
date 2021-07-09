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
 * A value info corresponds to value selector in a template declaration in a
 * template set source file.
 * </p>
 * 
 * <p>
 * A value info specifies how a value is extracted from a data model in a
 * generated template set class. It can be defined as a raw Java expression, it
 * is then possible to invoke arbitrary code to get a value in a template, or as
 * a name which allows shorter value accessor declaration. For instance,
 * expression <code>(person.getAddress().getCity())</code> can be written as
 * <code>person.address.city</code> using the name notation.
 * </p>
 * 
 * <p>
 * A value info can contains a sequence of pipes used to tranform (eg. format,
 * escape...) the value before rendering.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public interface ValueInfo extends StatementInfo {

	/**
	 * <p>
	 * Returns the raw Java expression that evaluates to a value.
	 * </p>
	 * 
	 * @return an optional returning a raw Java expression or an empty optional if
	 *         the value is defined as a name instead.
	 */
	Optional<String> getExpression();

	/**
	 * <p>
	 * Returns the name that evaluate to a value.
	 * </p>
	 * 
	 * <p>
	 * The name notation for values follows the following rules:
	 * </p>
	 * <ol>
	 * <li>it first tries to access the getter method: <code>get[PART]()</code></li>
	 * <li>it then tries to access the named method: <code>[PART]()</code></li>
	 * <li>it finally tries to access the field: <code>[PART]</code></li>
	 * </ol>
	 * 
	 * @return an optional returning a name or an empty optional if the value is
	 *         specified as an expression instead.
	 */
	Optional<NameInfo> getName();
	
	/**
	 * <p>
	 * Returns the list of pipes to apply to the value before rendering.
	 * </p>
	 * 
	 * @return A list of pipe info
	 */
	PipeInfo[] getPipes();
}
