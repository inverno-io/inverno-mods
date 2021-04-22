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
package io.winterframework.mod.web;

import io.winterframework.mod.http.server.Exchange;

/**
 * <p>
 * A route that specifies criteria used to determine whether the resource served
 * by the route can produce a response which is acceptable to the client.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Route
 * 
 * @param <A> the type of web exchange handled by the route
 */
public interface AcceptAwareRoute<A extends Exchange> extends Route<A> {

	/**
	 * <p>
	 * Returns the media type of the resource served by the route.
	 * </p>
	 * 
	 * <p>
	 * This criteria should match the request {@code accept} header field.
	 * </p>
	 * 
	 * @return a media type or null
	 */
	String getProduce();
	
	/**
	 * <p>
	 * Returns the language of the resource served by the route.
	 * </p>
	 * 
	 * <p>
	 * This criteria should match the request {@code accept-language} header field.
	 * </p>
	 * 
	 * @return a language tag or null
	 */
	String getLanguage();
}
