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
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author jkuhn
 *
 */
public class GeneratedDummyConfiguration implements DummyConfiguration {

	private URL some_url;
	private String string_with_default = DummyConfiguration.super.string_with_default();
	private Integer some_int;
	private String undefined_string;
	private NestedConfiguration nested = NestedConfigurationBuilder.build(builder -> {});
	private String[] some_array;
	private Collection<String> some_collection;
	private List<String> some_list;
	private Set<String> some_set;
	private Nested2Configuration nested2 = Nested2ConfigurationBuilder.build(builder -> {});
	
	public GeneratedDummyConfiguration(Supplier<URL> some_url, 
			Supplier<String> string_with_default, 
			Supplier<Integer> some_int, 
			Supplier<String> undefined_string, 
			Supplier<NestedConfiguration> nested,
			Supplier<String[]> some_array,
			Supplier<Collection<String>> some_collection,
			Supplier<List<String>> some_list,
			Supplier<Set<String>> some_set,
			Supplier<Nested2Configuration> nested2) {
		Optional.ofNullable(some_url).ifPresent(s -> this.some_url = s.get());
		Optional.ofNullable(string_with_default).ifPresent(s -> this.string_with_default = s.get());
		Optional.ofNullable(some_int).ifPresent(s -> this.some_int = s.get());
		Optional.ofNullable(undefined_string).ifPresent(s -> this.undefined_string = s.get());
		Optional.ofNullable(nested).ifPresent(s -> this.nested = s.get());
		Optional.ofNullable(some_array).ifPresent(s -> this.some_array = s.get());
		Optional.ofNullable(some_collection).ifPresent(s -> this.some_collection = s.get());
		Optional.ofNullable(some_list).ifPresent(s -> this.some_list = s.get());
		Optional.ofNullable(some_set).ifPresent(s -> this.some_set = s.get());
		Optional.ofNullable(nested2).ifPresent(s -> this.nested2 = s.get());
	}
	
	@Override
	public String string_with_default() {
		return string_with_default;
	}
	
	@Override
	public URL some_url() {
		return some_url;
	}

	@Override
	public int some_int() {
		return some_int;
	}

	@Override
	public String undefined_string() {
		return undefined_string;
	}

	@Override
	public NestedConfiguration nested() {
		return nested;
	}
	
	@Override
	public String[] some_array() {
		return some_array;
	}
	
	@Override
	public Collection<String> some_collection() {
		return some_collection;
	}
	
	@Override
	public List<String> some_list() {
		return some_list;
	}
	
	@Override
	public Set<String> some_set() {
		return some_set;
	}
	
	@Override
	public Nested2Configuration nested2() {
		return nested2;
	}
}
