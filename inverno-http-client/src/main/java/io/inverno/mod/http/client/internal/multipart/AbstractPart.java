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
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.PartHeaders;
import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public abstract class AbstractPart<A> implements Part<A> {

	protected final GenericPartHeaders headers;
	
	protected String name;
	protected String filename;

	protected AbstractPart(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headers = new GenericPartHeaders(headerService, parameterConverter);
	}

	protected AbstractPart(HeaderService headerService, ObjectConverter<String> parameterConverter, GenericPartHeaders headers) {
		this.headers = headers;
	}
	
	@Override
	public AbstractPart<A> name(String name) {
		this.name = name;
		return this;
	}

	@Override
	public AbstractPart<A> filename(String filename) {
		this.filename = filename;
		return this;
	}

	@Override
	public GenericPartHeaders headers() {
		return this.headers;
	}

	@Override
	public AbstractPart<A> headers(Consumer<PartHeaders> headersConfigurer) {
		headersConfigurer.accept(this.headers);
		return this;
	}
	
	public String getContentDisposition(Charset charset) {
		StringBuilder contentDisposition = new StringBuilder()
			.append(Headers.ContentDisposition.TYPE_FORM_DATA).append(";")
			.append(Headers.ContentDisposition.PART_NAME).append("=\"").append(URIs.encodeQuery(this.name, charset)).append("\"");
		
		if(this.filename != null) {
			contentDisposition.append(";").append(Headers.ContentDisposition.FILENAME).append("=\"").append(URIs.encodeQuery(this.filename, charset)).append("\"");
		}
		
		return contentDisposition.toString();
	}
}
