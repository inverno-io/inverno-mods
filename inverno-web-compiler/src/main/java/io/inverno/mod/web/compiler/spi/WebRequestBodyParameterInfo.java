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
package io.inverno.mod.web.compiler.spi;

import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Describes the request body parameter in a route.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface WebRequestBodyParameterInfo extends WebParameterInfo {

	/**
	 * <p>
	 * Returns the request body kind.
	 * </p>
	 * 
	 * @return the request body kind
	 */
	RequestBodyKind getBodyKind();
	
	/**
	 * <p>
	 * Returns the request body reactive kind.
	 * </p>
	 * 
	 * @return the request body reactive kind
	 */
	RequestBodyReactiveKind getBodyReactiveKind();
	
	/**
	 * <p>
	 * Returns the actual type of the request body.
	 * </p>
	 *
	 * <p>
	 * When the request body is reactive, this corresponds to the type argument of the reactive type.
	 * </p>
	 *
	 * @return the actual body type
	 */
	@Override
	TypeMirror getType();
}
