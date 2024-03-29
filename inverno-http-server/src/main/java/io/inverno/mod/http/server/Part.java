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
package io.inverno.mod.http.server;

import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.netty.buffer.ByteBuf;
import java.util.Optional;

/**
 * <p>
 * Represents a part in a multipart/form-data request body as defined by <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see RequestBody.Multipart
 */
public interface Part {

	/**
	 * <p>
	 * Returns the part's name.
	 * </p>
	 * 
	 * @return the name
	 */
	String getName();

	/**
	 * <p>
	 * Returns the part's file name.
	 * </p>
	 *
	 * @return an optional returning the file name or an empty optional if the part is not a file
	 */
	Optional<String> getFilename();
	
	/**
	 * <p>
	 * Returns the part's headers.
	 * </p>
	 * 
	 * @return the headers
	 */
	InboundRequestHeaders headers();
	
	/**
	 * <p>
	 * Returns the part's raw data.
	 * </p>
	 * 
	 * @return the raw data
	 */
	InboundData<ByteBuf> raw();
	
	/**
	 * <p>
	 * Returns the part's data as string.
	 * </p>
	 * 
	 * @return the string data
	 */
	InboundData<CharSequence> string();
}
