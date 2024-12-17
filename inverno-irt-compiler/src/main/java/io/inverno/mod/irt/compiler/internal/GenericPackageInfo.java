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

import io.inverno.mod.irt.compiler.spi.NameInfo;
import io.inverno.mod.irt.compiler.spi.PackageInfo;

/**
 * <p>
 * Generic {@link PackageInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericPackageInfo extends BaseInfo implements PackageInfo {

	private final NameInfo name;
	
	/**
	 * <p>
	 * Creates a generic package info.
	 * </p>
	 * 
	 * @param range the range in the IRT source file where the info is defined
	 * @param name  a name
	 */
	public GenericPackageInfo(Range range, NameInfo name) {
		super(range);
		this.name = name;
	}

	@Override
	public NameInfo getName() {
		return this.name;
	}

}
