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

import io.inverno.mod.irt.compiler.spi.NameInfo;

/**
 * <p>
 * Generic {@link NameInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericNameInfo extends BaseInfo implements NameInfo {

	private final String[] parts;
	
	/**
	 * <p>
	 * Creates a generic name info.
	 * </p>
	 * 
	 * @param range the range in the IRT source file where the info is defined
	 * @param parts a list of parts composing the name
	 */
	public GenericNameInfo(Range range, List<String> parts) {
		super(range);
		this.parts = parts.toArray(String[]::new);
	}

	@Override
	public String[] getParts() {
		return this.parts;
	}

}
