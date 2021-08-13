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
package io.inverno.mod.boot.internal;

import java.util.function.Supplier;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;

/**
 * <p>
 * General {@link com.fasterxml.jackson.databind.ObjectMapper} used to encode/decode JSON data.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean
@Wrapper
@Overridable
public class ObjectMapper implements Supplier<com.fasterxml.jackson.databind.ObjectMapper> {

	@Override
	public com.fasterxml.jackson.databind.ObjectMapper get() {
		return new com.fasterxml.jackson.databind.ObjectMapper();
	}
}
