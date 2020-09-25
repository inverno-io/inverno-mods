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
package io.winterframework.mod.configuration;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author jkuhn
 *
 */
public class GeneratedNestedConfiguration implements NestedConfiguration {

	private NestedNestedConfiguration nested_nested = NestedNestedConfigurationBuilder.build(builder -> {});
	
	private String nested_string;
	
	public GeneratedNestedConfiguration(Supplier<NestedNestedConfiguration> nested_nested, Supplier<String> nested_string) {
		Optional.ofNullable(nested_nested).ifPresent(s -> this.nested_nested = s.get());
		Optional.ofNullable(nested_string).ifPresent(s -> this.nested_string = s.get());
	}
	
	@Override
	public NestedNestedConfiguration nested_nested() {
		return nested_nested;
	}
	
	@Override
	public String nested_string() {
		return nested_string;
	}

}
