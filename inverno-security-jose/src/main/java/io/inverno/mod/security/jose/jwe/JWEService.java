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
package io.inverno.mod.security.jose.jwe;

import io.inverno.mod.security.jose.JOSEObjectService;
import io.inverno.mod.security.jose.jwk.JWK;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;

/**
 * <p>
 * The JWE Service is the main entry point for creating and reading JSON Web Encryption objects.
 * </p>
 * 
 * <p>
 * It allows to fluently create JWE objects using {@link JWEBuilder} for compact serialization or {@link JsonJWEBuilder} for JSON serialization. It also exposes {@link JWEReader} and
 * {@link JsonJWEReader} to read JWE compact representation or JWE JSON representation respectively.
 * </p>
 * 
 * <p>
 * Please refer to <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-7.1">RFC7516 Section 7.1</a> for compact serialization and to
 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-7.2">RFC7516 Section 7.2</a> for JSON serialization.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWEService extends JOSEObjectService {
	
	@Override
	default <T> JWEBuilder<T, ?, ?> builder() {
		return this.builder((Type)null, null);
	}
	
	@Override
	default <T> JWEBuilder<T, ?, ?> builder(Class<T> type) {
		return this.builder((Type)type, null);
	}
	
	@Override
	default <T> JWEBuilder<T, ?, ?> builder(Type type) {
		return this.builder(type, null);
	}
	
	@Override
	default <T> JWEBuilder<T, ?, ?> builder(Publisher<? extends JWK> keys) {
		return this.builder((Type)null, keys);
	}
	
	@Override
	default <T> JWEBuilder<T, ?, ?> builder(Class<T> type, Publisher<? extends JWK> keys) {
		return this.builder((Type)type, keys);
	}
	
	@Override
	<T> JWEBuilder<T, ?, ?> builder(Type type, Publisher<? extends JWK> keys);
	
	@Override
	default <T> JWEReader<T, ?> reader(Class<T> type) {
		return this.reader((Type)type, null);
	}
	
	@Override
	default <T> JWEReader<T, ?> reader(Type type) {
		return this.reader(type, null);
	}
	
	@Override
	default <T> JWEReader<T, ?> reader(Class<T> type, Publisher<? extends JWK> keys) {
		return this.reader((Type)type, keys);
	}
	
	@Override
	<T> JWEReader<T, ?> reader(Type type, Publisher<? extends JWK> keys);
	
	@Override
	default <T> JsonJWEBuilder<T, ?, ?> jsonBuilder() {
		return this.jsonBuilder((Type)null);
	}
	
	@Override
	default <T> JsonJWEBuilder<T, ?, ?> jsonBuilder(Class<T> type) {
		return this.jsonBuilder((Type)type);
	}
	
	@Override
	<T> JsonJWEBuilder<T, ?, ?> jsonBuilder(Type type);
	
	@Override
	default <T> JsonJWEReader<T, ?> jsonReader(Class<T> type) {
		return this.jsonReader((Type)type);
	}
	
	@Override
	<T> JsonJWEReader<T, ?> jsonReader(Type type);
}
