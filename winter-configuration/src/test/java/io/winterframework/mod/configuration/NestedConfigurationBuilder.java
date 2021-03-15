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
public class NestedConfigurationBuilder {

	private Supplier<NestedNestedConfiguration> nested_nested;
	
	private Supplier<String> nested_string;
	
	private NestedConfigurationBuilder() {}
	
	private NestedConfiguration build() {
		return new GeneratedNestedConfiguration(this.nested_nested, this.nested_string);
	}

	public static NestedConfiguration build(Consumer<NestedConfigurationBuilder> configurator) {
		NestedConfigurationBuilder builder = new NestedConfigurationBuilder();
		configurator.accept(builder);
		return builder.build();
	}

	public NestedConfigurationBuilder nested_nested(Consumer<NestedNestedConfigurationBuilder> nested_nested_configurator) {
		this.nested_nested = () -> NestedNestedConfigurationBuilder.build(nested_nested_configurator);
		return this;
	}
	
	public NestedConfigurationBuilder nested_string(String nested_string) {
		this.nested_string = () -> nested_string;
		return this;
	}
}
