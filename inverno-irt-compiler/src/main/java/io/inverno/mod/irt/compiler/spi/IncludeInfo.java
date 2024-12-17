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
 * An include info corresponds to an include statement in a template set source file.
 * </p>
 *
 * <p>
 * An include statement is used to combine multiple external template sets within the current template. The generated template set class will basically extends the included template set classes and as
 * such can apply their templates. It shall possible to override an included template and resolve ambiguities when two included template set define the same template.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface IncludeInfo {

	/**
	 * <p>
	 * Returns the name of the included template set.
	 * </p>
	 *
	 * @return a name info
	 */
	NameInfo getName();
}
