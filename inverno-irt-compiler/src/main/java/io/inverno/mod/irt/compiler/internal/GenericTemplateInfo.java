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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.inverno.mod.irt.compiler.spi.NameInfo;
import io.inverno.mod.irt.compiler.spi.ParameterInfo;
import io.inverno.mod.irt.compiler.spi.StatementInfo;
import io.inverno.mod.irt.compiler.spi.TemplateInfo;

/**
 * <p>
 * Generic {@link TemplateInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericTemplateInfo extends BaseInfo implements TemplateInfo {

	private final Optional<String> name;
	private final Map<String, ParameterInfo> parameters;
	private final Optional<StatementInfo[]> statements;
	private final Optional<SelectInfo> select;
	
	/**
	 * <p>
	 * Creates a generic template info.
	 * </p>
	 * 
	 * @param range      the range in the IRT source file where the info is defined
	 * @param name       a template name
	 * @param parameters a list of parameters
	 * @param statements a list of statements
	 * @param select     a template selector
	 */
	public GenericTemplateInfo(Range range, String name, List<ParameterInfo> parameters, List<StatementInfo> statements, SelectInfo select) {
		super(range);
		this.name = Optional.ofNullable(name);
		this.parameters = parameters.stream().collect(Collectors.toMap(ParameterInfo::getName, Function.identity(), (p1,p2) -> p1, LinkedHashMap::new));
		this.statements = Optional.ofNullable(statements).map(l -> l.toArray(StatementInfo[]::new)).filter(arr -> arr.length > 0);
		this.select = Optional.ofNullable(select);
	}

	@Override
	public Optional<String> getName() {
		return this.name;
	}

	@Override
	public Map<String, ParameterInfo> getParameters() {
		return this.parameters;
	}

	@Override
	public Optional<StatementInfo[]> getStatements() {
		return this.statements;
	}
	
	@Override
	public Optional<SelectInfo> getSelect() {
		return this.select;
	}
	
	/**
	 * <p>
	 * Generic {@link SelectInfo} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 */
	public static class GenericSelectInfo extends BaseInfo implements SelectInfo {

		private final Optional<NameInfo> templateSetName;
		private final Optional<String> templateName;
		
		/**
		 * <p>
		 * Creates a generic template info.
		 * </p>
		 * 
		 * @param range           the range in the IRT source file where the info is
		 *                        defined
		 * @param templateSetName a template set name
		 * @param templateName    a template name
		 */
		public GenericSelectInfo(Range range, NameInfo templateSetName, String templateName) {
			super(range);
			this.templateSetName = Optional.ofNullable(templateSetName);
			this.templateName = Optional.ofNullable(templateName);
		}

		@Override
		public Optional<NameInfo> getTemplateSetName() {
			return this.templateSetName;
		}

		@Override
		public Optional<String> getTemplateName() {
			return this.templateName;
		}
	}
}
