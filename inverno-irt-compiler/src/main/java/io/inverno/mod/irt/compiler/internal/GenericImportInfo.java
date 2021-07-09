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

import io.inverno.mod.irt.compiler.spi.ImportInfo;
import io.inverno.mod.irt.compiler.spi.NameInfo;

/**
 * <p>
 * Generic {@link ImportInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public class GenericImportInfo extends BaseInfo implements ImportInfo {

	private final NameInfo name;
	
	private final boolean staticImport;

	/**
	 * <p>
	 * Creates a generic import info.
	 * </p>
	 * 
	 * @param range        the range in the IRT source file where the info is
	 *                     defined
	 * @param name         a name
	 * @param staticImport true to indicate a static import, false otherwise
	 */
	public GenericImportInfo(Range range, NameInfo name, boolean staticImport) {
		super(range);
		this.name = name;
		this.staticImport = staticImport;
	}

	@Override
	public boolean isStatic() {
		return this.staticImport;
	}

	@Override
	public NameInfo getName() {
		return name;
	}
}
