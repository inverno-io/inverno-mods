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

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class DummyConfigurationBuilder {

	private Supplier<URL> some_url;
	private Supplier<String> string_with_default;
	private Supplier<Integer> some_int;
	private Supplier<String> undefined_string;
	private Supplier<NestedConfiguration> nested;
	private Supplier<String[]> some_array;
	private Supplier<Collection<String>> some_collection;
	private Supplier<List<String>> some_list;
	private Supplier<Set<String>> some_set;
	private Supplier<Nested2Configuration> nested2;
	
	private DummyConfigurationBuilder() {}

	private DummyConfiguration build() {
		return new GeneratedDummyConfiguration(this.some_url, this.string_with_default, this.some_int, this.undefined_string, this.nested, this.some_array, this.some_collection, this.some_list, this.some_set, this.nested2);
	}

	public static DummyConfiguration build(Consumer<DummyConfigurationBuilder> configurator) {
		DummyConfigurationBuilder builder = new DummyConfigurationBuilder();
		configurator.accept(builder);
		return builder.build();
	}

	public DummyConfigurationBuilder some_url(URL some_url) {
		this.some_url = () -> some_url;
		return this;
	}
	
	public DummyConfigurationBuilder string_with_default(String string_with_default) {
		this.string_with_default = () -> string_with_default;
		return this;
	}
	
	public DummyConfigurationBuilder some_int(int some_int) {
		this.some_int = () -> some_int;
		return this;
	}
	
	public DummyConfigurationBuilder undefined_string(String undefined_string) {
		this.undefined_string = () -> undefined_string;
		return this;
	}
	
	public DummyConfigurationBuilder nested(Consumer<NestedConfigurationBuilder> nested_configurator) {
		this.nested = () -> NestedConfigurationBuilder.build(nested_configurator);
		return this;
	}
	
	public DummyConfigurationBuilder some_array(String[] some_array) {
		this.some_array = () -> some_array;
		return this;
	}
	
	public DummyConfigurationBuilder some_collection(Collection<String> some_collection) {
		this.some_collection = () -> some_collection;
		return this;
	}
	
	public DummyConfigurationBuilder some_list(List<String> some_list) {
		this.some_list = () -> some_list;
		return this;
	}
	
	public DummyConfigurationBuilder some_set(Set<String> some_set) {
		this.some_set = () -> some_set;
		return this;
	}
	
	public DummyConfigurationBuilder nested2(Consumer<Nested2ConfigurationBuilder> nested2_configurator) {
		this.nested2 = () -> Nested2ConfigurationBuilder.build(nested2_configurator);
		return this;
	}
}
