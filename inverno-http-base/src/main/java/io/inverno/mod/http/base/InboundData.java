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
package io.inverno.mod.http.base;

import org.reactivestreams.Publisher;

/**
 * <p>
 * A generic inbound data consumer.
 * </p>
 * 
 * <p>
 * It is used to consume the payload received by a client or a server either in a response or a request.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the type of data
 */
@FunctionalInterface
public interface InboundData<A> {

	/**
	 * <p>
	 * Returns the inbound data publisher.
	 * </p>
	 * 
	 * @return a data publisher
	 */
	Publisher<A> stream();
}
