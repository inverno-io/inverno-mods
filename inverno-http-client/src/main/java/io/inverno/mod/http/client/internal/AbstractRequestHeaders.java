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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.http.base.OutboundRequestHeaders;

/**
 * <p>
 * Base {@link OutboundRequestHeaders} implementation representing the headers to send to the connected remote endpoint.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.11
 * 
 * @param <A> the type of the headers sent by the connection as part of the HTTP request
 */
public abstract class AbstractRequestHeaders<A> implements OutboundRequestHeaders {

	private boolean written;

	/**
	 * <p>
	 * Returns the headers to send as part of the HTTP request.
	 * </p>
	 * 
	 * @return the wrapped headers
	 */
	protected abstract A unwrap();
	
	/**
	 * <p>
	 * Flags the headers to have been written.
	 * </p>
	 */
	public final void setWritten() {
		this.written = true;
	}

	@Override
	public boolean isWritten() {
		return this.written;
	}
}
