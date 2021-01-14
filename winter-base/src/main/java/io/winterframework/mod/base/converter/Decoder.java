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
package io.winterframework.mod.base.converter;

import java.util.List;
import java.util.Set;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public interface Decoder<From, To> {

	<T extends To> Mono<T> decodeOne(Publisher<From> data, Class<T> type);
	<T extends To> Flux<T> decodeMany(Publisher<From> data, Class<T> type);
	
	<T extends To> T decode(From data, Class<T> type);
	<T extends To> List<T> decodeToList(From data, Class<T> type);
	<T extends To> Set<T> decodeToSet(From data, Class<T> type);
	<T extends To> T[] decodeToArray(From data, Class<T> type);
}
