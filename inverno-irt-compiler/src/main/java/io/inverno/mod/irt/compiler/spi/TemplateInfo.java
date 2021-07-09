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

import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * An option info corresponds to a template declaration in a template set source
 * file.
 * </p>
 * 
 * <p>
 * A template specifies the rendering rules of one or more input data. It can
 * have a name to be able to define multiple rendering mode for the same type of
 * input data. It is composed of a list of {@link StatementInfo statements}
 * which specify how the input data should be rendered.
 * </p>
 * 
 * <p>
 * When multiple template sets are included in the template set it is also
 * possible to resolve conflicting templates using a {@link SelectInfo} which
 * allows to select the template set to use when a particular template is
 * applied. A selector might also be used to defined template aliases.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public interface TemplateInfo {

	/**
	 * <p>
	 * Returns the name of the template.
	 * </p>
	 * 
	 * @return an optional returning a name or an empty optional if the template has no name.
	 */
	Optional<String> getName();
	
	/**
	 * <p>
	 * Returns the template parameters specifying the input data rendered in the
	 * template.
	 * </p>
	 * 
	 * @return an ordered map of parameter info.
	 */
	Map<String, ParameterInfo> getParameters();
	
	/**
	 * <p>
	 * Returns the list of statements defining the rendering rules.
	 * </p>
	 * 
	 * @return an optional returning the list of statements or an empty optional if
	 *         this template defines a selector info instead.
	 */
	Optional<StatementInfo[]> getStatements();
	
	/**
	 * <p>
	 * Returns the template selector.
	 * </p>
	 * 
	 * @return an optional returning a template selector or an empty optional if
	 *         this template defines a list of statements instead.
	 */
	Optional<SelectInfo> getSelect();
	
	/**
	 * <p>
	 * A template selector used to select a particular template set in case of
	 * templates conflict or to define a template alias.
	 * </p>
	 * 
	 * <p>
	 * A conflict may occur when two included templates define the same template
	 * (ie. same name and same parameters). Such conflict can be resolved by
	 * overriding the template and explicitly select the included template set.
	 * </p>
	 * 
	 * <p>
	 * A template alias can be defined by defining a template and make it point to
	 * another named template.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 */
	interface SelectInfo {
		
		/**
		 * <p>
		 * Returns the name of the template set to select to resolve a conflict.
		 * </p>
		 * 
		 * @return an optional returning the template set name or an empty optional if
		 *         an alias is defined instead.
		 */
		Optional<NameInfo> getTemplateSetName();

		/**
		 * <p>
		 * The name of the template to apply when this template is applied.
		 * </p>
		 * 
		 * @return an optional returning a templat ename or an empty optional if a
		 *         template set is defined instead.
		 */
		Optional<String> getTemplateName();
	}
}
