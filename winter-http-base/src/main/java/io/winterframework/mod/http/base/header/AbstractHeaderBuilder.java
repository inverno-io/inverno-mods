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
package io.winterframework.mod.http.base.header;

/**
 * <p>
 * Base implementation for {@link HeaderBuilder}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see HeaderBuilder
 * 
 * @param <A> the header type
 * @param <B> the header builder type
 */
public abstract class AbstractHeaderBuilder<A extends Header, B extends AbstractHeaderBuilder<A, B>> implements HeaderBuilder<A, B> {

	/**
	 * The header name.
	 */
	protected String headerName;
	
	/**
	 * The header value.
	 */
	protected String headerValue;

	@SuppressWarnings("unchecked")
	@Override
	public B headerName(String headerName) {
		this.headerName = headerName;
		return (B)this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public B headerValue(String headerValue) {
		this.headerValue= headerValue;
		return (B)this;
	}
}
