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
public class Nested2ConfigurationBuilder {

	private Supplier<String> nested2_string;
	private Supplier<Float> nested2_float;
	
	private Nested2ConfigurationBuilder() {}
	
	private Nested2Configuration build() {
		return new GeneratedNested2Configuration(this.nested2_string, this.nested2_float);
	}

	public static Nested2Configuration build(Consumer<Nested2ConfigurationBuilder> configurator) {
		Nested2ConfigurationBuilder builder = new Nested2ConfigurationBuilder();
		configurator.accept(builder);
		return builder.build();
	}

	public Nested2ConfigurationBuilder nested2_string(String nested2_string) {
		this.nested2_string = () -> nested2_string;
		return this;
	}
	
	public Nested2ConfigurationBuilder nested2_float(float nested2_float) {
		this.nested2_float = () -> nested2_float;
		return this;
	}
}
