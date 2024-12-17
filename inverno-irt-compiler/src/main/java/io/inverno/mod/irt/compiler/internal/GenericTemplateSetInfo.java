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

import java.util.List;
import java.util.Optional;

import io.inverno.mod.irt.compiler.spi.ImportInfo;
import io.inverno.mod.irt.compiler.spi.IncludeInfo;
import io.inverno.mod.irt.compiler.spi.OptionInfo;
import io.inverno.mod.irt.compiler.spi.PackageInfo;
import io.inverno.mod.irt.compiler.spi.TemplateInfo;
import io.inverno.mod.irt.compiler.spi.TemplateSetInfo;
import io.inverno.mod.irt.compiler.spi.TemplateSetInfoVisitor;

/**
 * <p>
 * Generic {@link TemplateSetInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericTemplateSetInfo extends BaseInfo implements TemplateSetInfo {

	private final Optional<PackageInfo> templatePackage;
	private final ImportInfo[] imports;
	private final IncludeInfo[] includes;
	private final OptionInfo[] options;
	private final TemplateInfo[] templates;
	
	/**
	 * <p>
	 * Creates a generic template set info.
	 * </p>
	 * 
	 * @param range           the range in the IRT source file where the info is
	 *                        defined
	 * @param templatePackage the template set declared package
	 * @param imports         a list of imports
	 * @param includes        a list of includes
	 * @param options         a list of options
	 * @param templates       a list of template declarations
	 */
	public GenericTemplateSetInfo(Range range, PackageInfo templatePackage, List<ImportInfo> imports, List<IncludeInfo> includes, List<OptionInfo> options, List<TemplateInfo> templates) {
		super(range);
		this.templatePackage = Optional.ofNullable(templatePackage);
		this.imports = imports.toArray(ImportInfo[]::new);
		this.includes = includes.toArray(IncludeInfo[]::new);
		this.options = options.toArray(OptionInfo[]::new);
		this.templates = templates.toArray(TemplateInfo[]::new);
	}
	
	@Override
	public Optional<PackageInfo> getPackage() {
		return this.templatePackage;
	}

	@Override
	public ImportInfo[] getImports() {
		return this.imports;
	}

	@Override
	public IncludeInfo[] getIncludes() {
		return this.includes;
	}
	
	@Override
	public OptionInfo[] getOptions() {
		return this.options;
	}

	@Override
	public TemplateInfo[] getTemplates() {
		return this.templates;
	}
	
	@Override
	public <R, P> R accept(TemplateSetInfoVisitor<R, P> visitor, P p) {
		return visitor.visit(this, p);
	}
}
