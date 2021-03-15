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
package io.winterframework.mod.web.compiler.spi;

import java.util.Optional;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * Describes the server-sent event factory route parameter.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public interface WebSseEventFactoryParameterInfo extends WebParameterInfo {

	/**
	 * <p>
	 * Indicates the kind of a server-sent event factory.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static enum SseEventFactoryKind {
		/**
		 * The event data type is {@link ByteBuf}.
		 */
		RAW,
		/**
		 * The event data type is any type other than {@link ByteBuf}
		 */
		ENCODED
	}
	
	/**
	 * <p>
	 * Returns the server-sent event factory kind.
	 * </p>
	 * 
	 * @return the server-sent event factory kind
	 */
	SseEventFactoryKind getEventFactoryKind();
	
	/**
	 * <p>
	 * Returns the media type specified in the server-sent event factory parameter.
	 * </p>
	 * 
	 * @return a media type
	 */
	Optional<String> getDataMediaType();
}
