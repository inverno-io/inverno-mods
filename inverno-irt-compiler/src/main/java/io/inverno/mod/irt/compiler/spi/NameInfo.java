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
 * An name info corresponds to a name in a template set source file.
 * </p>
 *
 * <p>
 * A name info is generic, it can designate a package, an import, an include, a template set name or a value accessor.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface NameInfo {

	/**
	 * <p>
	 * Returns the parts composing the name.
	 * </p>
	 *
	 * @return the name parts
	 */
	String[] getParts();
}
