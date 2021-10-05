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
package io.inverno.mod.web;

/**
 * <p>
 * Wraps a Throwable thrown by in
 * {@link WebRouter#handle(io.inverno.mod.http.server.Exchange)} in order to
 * propagate the Web exchange context to the {@link ErrorWebRouter}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
public class WebRouterException extends RuntimeException {

	private static final long serialVersionUID = -8003756739063840057L;
	
	/**
	 * The Web exchange context.
	 */
	private WebExchange.Context context;

	/**
	 * <p>
	 * Creates a Web router exception with the specified throwable and context.
	 * </p>
	 * 
	 * @param t       the wrapped throwable
	 * @param context the Web exchange context
	 */
	public WebRouterException(Throwable t, WebExchange.Context context) {
		super(t);
		this.context = context;
	}
	
	/**
	 * <p>
	 * Returns the Web exchange context associated to the Web exchange that caused
	 * the error.
	 * </p>
	 * 
	 * @return the Web exchange context or null
	 */
	public WebExchange.Context getContext() {
		return this.context;
	}
}
