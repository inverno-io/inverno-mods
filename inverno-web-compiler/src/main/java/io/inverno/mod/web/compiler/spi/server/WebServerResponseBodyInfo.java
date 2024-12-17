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
package io.inverno.mod.web.compiler.spi.server;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.mod.web.compiler.spi.ResponseBodyKind;
import io.inverno.mod.web.compiler.spi.ResponseBodyReactiveKind;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Describes the response body of a route.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface WebServerResponseBodyInfo extends ReporterInfo {

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
	 * @return the response body kind
	 */
	ResponseBodyKind getBodyKind();
	
	/**
	 * <p>
	 * Returns the response body reactive kind.
	 * </p>
	 * 
	 * @return the response body reactive kind
	 */
	ResponseBodyReactiveKind getBodyReactiveKind();

}
