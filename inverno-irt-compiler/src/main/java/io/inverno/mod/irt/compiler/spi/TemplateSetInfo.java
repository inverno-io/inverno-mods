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
 * A template set info holds the data of a template set source file.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface TemplateSetInfo {

	/**
	 * <p>
	 * Returns the template set declared package.
	 * </p>
	 *
	 * @return an optional returning the package info or an empty optional for the default package
	 */
	Optional<PackageInfo> getPackage();

	/**
	 * <p>
	 * Returns the list of import statements defined in the template set.
	 * </p>
	 *
	 * @return an array of import info
	 */
	ImportInfo[] getImports();

	/**
	 * <p>
	 * Returns the list of include statements defined in the template set.
	 * </p>
	 *
	 * <p>
	 * An include statement is used to specify a template to include in the template set.
	 * </p>
	 *
	 * @return an array of include info
	 */
	IncludeInfo[] getIncludes();

	/**
	 * <p>
	 * Returns the list of option statements defined in the template set.
	 * </p>
	 *
	 * @return an array of option info
	 */
	OptionInfo[] getOptions();

	/**
	 * <p>
	 * Returns the list of templates defined in the template set.
	 * </p>
	 *
	 * @return an array of template info
	 */
	TemplateInfo[] getTemplates();

	/**
	 * <p>
	 * Accepts the specified template set info visitor.
	 * </p>
	 *
	 * @param <R>     the type of the visitor result
	 * @param <P>     the type of the visitor parameter
	 * @param visitor the visitor to invoke
	 * @param p       the parameter
	 *
	 * @return the visitor result
	 */
	<R, P> R accept(TemplateSetInfoVisitor<R, P> visitor, P p);
}
