/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.security.jose.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import java.util.function.Supplier;

/**
 * <p>
 * {@link com.fasterxml.jackson.databind.ObjectMapper} used to serialize/deserialize JSON JOSE objects and JSON Web Keys.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Overridable
@Wrapper
@Bean( name = "objectMapper", visibility = Bean.Visibility.PRIVATE )
public class JOSEObjectMapper implements Supplier<ObjectMapper> {

	private com.fasterxml.jackson.databind.ObjectMapper instance;
	
	@Override
	public ObjectMapper get() {
		if(this.instance == null) {
			this.instance = new ObjectMapper();
			this.instance.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
		}
		return this.instance;
	}
}
