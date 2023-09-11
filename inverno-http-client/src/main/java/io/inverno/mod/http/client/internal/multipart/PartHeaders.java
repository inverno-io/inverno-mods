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
package io.inverno.mod.http.client.internal.multipart;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.internal.GenericRequestHeaders;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A {@link OutboundRequestHeaders} implementation used for representing part's headers in a multipart form data body.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class PartHeaders extends GenericRequestHeaders {

	/**
	 * <p>
	 * Creates blank part's headers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public PartHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(headerService, parameterConverter);
	}

	/**
	 * <p>
	 * Creates part's headers populated with specified header entries.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param entries            a list of HTTP header entries
	 */
	public PartHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter, List<Map.Entry<String, String>> entries) {
		super(headerService, parameterConverter, entries);
	}
	
	/**
	 * <p>
	 * Encodes the specified buffer into the headers.
	 * </p>
	 * 
	 * @param buf a byte buf
	 */
	public void encode(ByteBuf buf) {
		this.underlyingHeaders.encode(buf);
	}
}
