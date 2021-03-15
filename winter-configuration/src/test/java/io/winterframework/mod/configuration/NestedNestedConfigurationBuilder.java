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

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class NestedNestedConfigurationBuilder {

	private Supplier<String> nested_nested_string;
	
	private NestedNestedConfigurationBuilder() {}
	
	private NestedNestedConfiguration build() {
		return new GeneratedNestedNestedConfiguration(this.nested_nested_string);
	}

	public static NestedNestedConfiguration build(Consumer<NestedNestedConfigurationBuilder> configurator) {
		NestedNestedConfigurationBuilder builder = new NestedNestedConfigurationBuilder();
		configurator.accept(builder);
		return builder.build();
	}

	public NestedNestedConfigurationBuilder nested_nested_string(String nested_nested_string) {
		this.nested_nested_string = () -> nested_nested_string;
		return this;
	}
}
