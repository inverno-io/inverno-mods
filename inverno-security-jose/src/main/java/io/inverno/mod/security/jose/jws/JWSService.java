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
package io.inverno.mod.security.jose.jws;

import io.inverno.mod.security.jose.JOSEObjectService;
import io.inverno.mod.security.jose.jwk.JWK;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;

/**
 * <p>
 * The JWS Service is the main entry point for creating and reading JSON Web Signature objects.
 * </p>
 * 
 * <p>
 * It allows to fluently create JWS objects using {@link JWSBuilder} for compact serialization or {@link JsonJWSBuilder} for JSON serialization. It also exposes {@link JWSReader} and
 * {@link JsonJWSReader} to read JWS compact representation or JWS JSON representation respectively.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWSService extends JOSEObjectService {
	
	@Override
	default <T> JWSBuilder<T, ?, ?> builder() {
		return this.builder((Type)null, null);
	}
	
	@Override
	default <T> JWSBuilder<T, ?, ?> builder(Class<T> type) {
		return this.builder((Type)type, null);
	}
	
	@Override
	default <T> JWSBuilder<T, ?, ?> builder(Type type) {
		return this.builder(type, null);
	}
	
	@Override
	default <T> JWSBuilder<T, ?, ?> builder(Publisher<? extends JWK> keys) {
		return this.builder((Type)null, keys);
	}
	
	@Override
	default <T> JWSBuilder<T, ?, ?> builder(Class<T> type, Publisher<? extends JWK> keys) {
		return this.builder((Type)type, keys);
	}
	
	@Override
	<T> JWSBuilder<T, ?, ?> builder(Type type, Publisher<? extends JWK> keys);
	
	@Override
	default <T> JWSReader<T, ?> reader(Class<T> type) {
		return this.reader((Type)type, null);
	}
	
	@Override
	default <T> JWSReader<T, ?> reader(Type type) {
		return this.reader(type, null);
	}
	
	@Override
	default <T> JWSReader<T, ?> reader(Class<T> type, Publisher<? extends JWK> keys) {
		return this.reader((Type)type, keys);
	}
	
	@Override
	<T> JWSReader<T, ?> reader(Type type, Publisher<? extends JWK> keys);
	
	@Override
	default <T> JsonJWSBuilder<T, ?, ?> jsonBuilder() {
		return this.jsonBuilder((Type)null);
	}
	
	@Override
	default <T> JsonJWSBuilder<T, ?, ?> jsonBuilder(Class<T> type) {
		return this.jsonBuilder((Type)type);
	}
	
	@Override
	<T> JsonJWSBuilder<T, ?, ?> jsonBuilder(Type type);
	
	@Override
	default <T> JsonJWSReader<T, ?> jsonReader(Class<T> type) {
		return this.jsonReader((Type)type);
	}
	
	@Override
	<T> JsonJWSReader<T, ?> jsonReader(Type type);
}
