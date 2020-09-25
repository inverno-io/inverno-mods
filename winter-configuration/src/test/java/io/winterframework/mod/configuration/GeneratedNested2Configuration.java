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
public class GeneratedNested2Configuration implements Nested2Configuration {

	private String nested2_string = Nested2Configuration.super.nested2_string();
	
	private float nested2_float;
	
	public GeneratedNested2Configuration(Supplier<String> nested2_string, Supplier<Float> nested2_float) {
		Optional.ofNullable(nested2_string).ifPresent(s -> this.nested2_string = s.get());
		Optional.ofNullable(nested2_float).ifPresent(s -> this.nested2_float = s.get());
	}
	
	@Override
	public String nested2_string() {
		return nested2_string;
	}

	@Override
	public float nested2_float() {
		return nested2_float;
	}
}
