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

/**
 * <p>
 * An import info corresponds to an import statement in a template set source file.
 * </p>
 *
 * <p>
 * An import statement is like a regular java imports, it is used to declare the classes and methods (static imports) to consider when compiling the template and the generated template class. These
 * imports statements will be output in the generated template class.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface ImportInfo {

	/**
	 * <p>
	 * Determines whether this import statement is static.
	 * </p>
	 *
	 * @return true if this is a static import, false otherwise
	 */
	boolean isStatic();

	/**
	 * <p>
	 * Returns the name of the import which may end with a wildcard.
	 * </p>
	 *
	 * @return a name info
	 */
	NameInfo getName();
}
