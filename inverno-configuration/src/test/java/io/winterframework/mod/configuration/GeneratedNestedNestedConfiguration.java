/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.configuration;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class GeneratedNestedNestedConfiguration implements NestedNestedConfiguration {

	private String nested_nested_string;
	
	public GeneratedNestedNestedConfiguration(Supplier<String> nested_nested_string) {
		Optional.ofNullable(nested_nested_string).ifPresent(s -> this.nested_nested_string = s.get());
	}
	
	@Override
	public String nested_nested_string() {
		return this.nested_nested_string;
	}

}
