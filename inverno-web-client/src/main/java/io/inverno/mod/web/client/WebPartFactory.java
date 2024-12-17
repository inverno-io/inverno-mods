/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.client;

import io.inverno.mod.http.client.Part;
import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * <p>
 * A part factory supporting payload encoding.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebPartFactory extends Part.Factory {

	/**
	 * <p>
	 * Creates an encoded part.
	 * </p>
	 *
	 * @param <T>            the payload type
	 * @param partConfigurer an encoded part configurer
	 *
	 * @return an encoded Web part
	 */
	<T> WebPart<T> encoded(Consumer<WebPart<T>> partConfigurer);

	/**
	 * <p>
	 * Creates an encoded part.
	 * </p>
	 *
	 * @param <T>            the payload type
	 * @param partConfigurer an encoded part configurer
	 * @param type           the payload type
	 *
	 * @return an encoded Web part
	 */
	<T> WebPart<T> encoded(Consumer<WebPart<T>> partConfigurer, Class<T> type);

	/**
	 * <p>
	 * Creates an encoded part.
	 * </p>
	 *
	 * @param <T>            the payload type
	 * @param partConfigurer an encoded part configurer
	 * @param type           the payload type
	 *
	 * @return an encoded Web part
	 */
	<T> WebPart<T> encoded(Consumer<WebPart<T>> partConfigurer, Type type);
}
