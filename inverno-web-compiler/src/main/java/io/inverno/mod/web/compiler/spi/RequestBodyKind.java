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
package io.inverno.mod.web.compiler.spi;

import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.web.server.WebPart;
import io.netty.buffer.ByteBuf;

/**
 * <p>
 * Indicates the kind of a request body.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public enum RequestBodyKind {
	/**
	 * The actual request body type is {@link ByteBuf}.
	 */
	RAW,
	/**
	 * The actual request body type is a {@link CharSequence}.
	 */
	CHARSEQUENCE,
	/**
	 * The actual request body type is {@link Resource}.
	 */
	RESOURCE,
	/**
	 * The actual request body type is a super type of {@link WebPart}.
	 */
	MULTIPART,
	/**
	 * The actual request body type is {@link Parameter}.
	 */
	URLENCODED,
	/**
	 * The actual request body type is none of the above.
	 */
	ENCODED;
}