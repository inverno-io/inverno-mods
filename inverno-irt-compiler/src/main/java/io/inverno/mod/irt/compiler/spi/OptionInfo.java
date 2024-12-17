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
 * An option info corresponds to an option statement in a template set source file.
 * </p>
 *
 * <p>
 * An option statement allows to specify option to the template set compiler such as the generation modes or the charset.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface OptionInfo {

	/**
	 * <p>
	 * Returns the option name.
	 * </p>
	 *
	 * @return a name
	 */
	String getName();

	/**
	 * <p>
	 * Returns the option value.
	 * </p>
	 *
	 * @return a value
	 */
	Object getValue();
}
