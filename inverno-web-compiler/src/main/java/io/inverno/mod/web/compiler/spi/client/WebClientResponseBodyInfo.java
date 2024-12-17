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
package io.inverno.mod.web.compiler.spi.client;

import io.inverno.mod.web.compiler.spi.ResponseBodyKind;
import io.inverno.mod.web.compiler.spi.ResponseBodyReactiveKind;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Describes a Web client response body return.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebClientResponseBodyInfo extends WebClientRouteReturnInfo {

	/**
	 * <p>
	 * Returns the actual type of the response body.
	 * </p>
	 *
	 * <p>
	 * When the response body is reactive, this corresponds to the type argument of the reactive type.
	 * </p>
	 *
	 * @return the actual body type
	 */
	TypeMirror getType();

	/**
	 * <p>
	 * Returns the response body kind.
	 * </p>
	 *
	 * <p>
	 * SSE is not supported by Web client.
	 * </p>
	 *
	 * @return the response body kind
	 */
	ResponseBodyKind getBodyKind();

	/**
	 * <p>
	 * Returns the response body reactive kind.
	 * </p>
	 *
	 * <p>
	 * NONE is not supported by Web client: response must be reactive.
	 * </p>
	 *
	 * @return the response body reactive kind
	 */
	ResponseBodyReactiveKind getBodyReactiveKind();
}
