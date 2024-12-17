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

import io.inverno.mod.irt.TemplateSet;

/**
 * <p>
 * An apply info corresponds to an apply template statement in a template declaration in a template set source file.
 * </p>
 *
 * <p>
 * An apply template statement is used to apply templates to a data model, it is composed of two sections: the value section and the target section.
 * </p>
 *
 * <p>
 * The value section is itself composed of a value selector which specifies the data model onto which templates should be applied and a sequence of pipes to apply to the value before applying the
 * templates as defined in the {@link ValueInfo}. The value section can be missing, in which case the apply statement is used to invoke no-args templates or templates with user defined arguments.
 * </p>
 *
 * <p>
 * The target section specifies the templates to apply to the value. If the target section is missing, the unnamed template corresponding to the value should be applied. An apply target consists of an
 * optional template name, the list of arguments to apply to the template and an optional guard expression which allows to select a target based on the value. If the template name is missing, the
 * unnamed template is invoked. If the arguments are missing, the value is the only argument passed to the template. If multiple apply target are specified, there must be at most one target with no
 * guard expression: the default target.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface ApplyInfo extends StatementInfo {

	/**
	 * <p>
	 * Returns the apply statement value.
	 * </p>
	 * 
	 * @return an optional returning the value or an empty optional if the apply statements has no value (in which case no pipes should be allowed).
	 */
	Optional<ValueInfo> getValue();

	/**
	 * <p>
	 * Returns the list of target parameters to expose in the apply targets and which can then be referenced in apply target's arguments and guard expressions.
	 * </p>
	 *
	 * <p>
	 * There should usually be only one parameter corresponding to the value, however depending on the type of {@link TemplateSet.Renderable} considered, it might be possible to define extra
	 * parameters such as an index in case of an Indexable {@link TemplateSet.IndexableRenderable}.
	 * </p>
	 *
	 * @return an array of target parameter info
	 */
	TargetParameterInfo[] getTargetParameters();

	/**
	 * <p>
	 * Returns the list of apply targets.
	 * </p>
	 * 
	 * <p>
	 * If no target is defined then the unnamed template is applied on the value.
	 * </p>
	 * 
	 * @return an array of target info
	 */
	TargetInfo[] getTargets();

	/**
	 * <p>
	 * A target parameter info corresponds to the declaration of a parameter in the target section of the apply statement.
	 * </p>
	 *
	 * <p>
	 * A target parameter info can be specified as a simple name with an implicit type or as a parameter info with a name and type. Note that in some situation it might be necessary to specify a type
	 * to avoid compilation errors.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	interface TargetParameterInfo {

		/**
		 * <p>
		 * Returns the parameter name.
		 * </p>
		 *
		 * @return an optional returning the parameter name or an empty optional if it is defined as a parameter info with a type instead.
		 */
		Optional<String> getName();

		/**
		 * <p>
		 * Returns the parameter.
		 * </p>
		 *
		 * @return an optional returning the parameter or an empty optional if it is defined as simple name with implicit type instead.
		 */
		Optional<ParameterInfo> getParameter();
	}

	/**
	 * <p>
	 * A target info corresponds specifies a template to apply.
	 * </p>
	 *
	 * <p>
	 * If arguments are missing, the named template when a name is specified or the unnamed template is applied on the value, otherwise the specified arguments are passed to the template.
	 * </p>
	 *
	 * <p>
	 * When a guard expression is defined, it is used to determine whether the template should be applied to a value. There must be at most one target info with no guard expression in an apply
	 * statement.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	interface TargetInfo {

		/**
		 * <p>
		 * Returns the name of the template to apply.
		 * </p>
		 *
		 * @return an optional returning a template name or an empty optional if the unnamed template must be invoked.
		 */
		Optional<String> getName();

		/**
		 * <p>
		 * Returns the list of arguments to pass to the template.
		 * </p>
		 *
		 * @return an optional returning a list of arguments or an empty optional if the template should be applied on the value only.
		 */
		Optional<ArgumentInfo[]> getArguments();

		/**
		 * <p>
		 * Returns the guard expression used to determine whether the template of this target should be applied.
		 * </p>
		 *
		 * @return an optional returning a guard expression or an empty optional if there is no guard expression
		 */
		Optional<String> getGuardExpression();
	}

	/**
	 * <p>
	 * An argument info corresponds to an argument to pass to a pipe or a template in an apply statement.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	interface ArgumentInfo {

		/**
		 * <p>
		 * Returns the value of the argument which is a raw Java expression.
		 * </p>
		 *
		 * @return a raw Java expression
		 */
		String getValue();
	}
}
