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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.web.ErrorWebRoute;

/**
 * <p>
 * Base Error Web router class.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
abstract class AbstractErrorWebRouter {

	protected static final ContentTypeCodec CONTENT_TYPE_CODEC = new ContentTypeCodec();
	protected static final AcceptLanguageCodec ACCEPT_LANGUAGE_CODEC = new AcceptLanguageCodec(false);

	/**
	 * <p>
	 * Sets the specified error web route in the router.
	 * </p>
	 *
	 * @param route an error web route
	 */
	abstract void setRoute(ErrorWebRoute route);

	/**
	 * <p>
	 * Enables the specified error web route if it exists.
	 * </p>
	 *
	 * @param route the error web route to enable
	 */
	abstract void enableRoute(ErrorWebRoute route);

	/**
	 * <p>
	 * Disables the specified error web route if it exists.
	 * </p>
	 *
	 * @param route the error web route to disable
	 */
	abstract void disableRoute(ErrorWebRoute route);

	/**
	 * <p>
	 * Removes the specified error web route if it exists.
	 * </p>
	 *
	 * @param route the error web route to remove
	 */
	abstract void removeRoute(ErrorWebRoute route);
}
