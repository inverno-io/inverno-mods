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
package io.winterframework.mod.web.internal.server;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.HttpConstants;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.ResponseHeaders;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.internal.header.GenericHeader;

/**
 * @author jkuhn
 *
 */
public class GenericResponseHeaders implements ResponseHeaders {

	private HeaderService headerService;

	private Charset charset = HttpConstants.DEFAULT_CHARSET;
	
	private LinkedList<Header> headers;

	private int status;
	private GenericHeader statusHeader;
	
	private Long size;
	private GenericHeader contentLengthHeader;
	
	private ContentTypeCodec.ContentType contentTypeHeader;
	
	private boolean written;
	
	public GenericResponseHeaders(HeaderService headerService) {
		this.headerService = headerService;
		this.status = 200;
		this.statusHeader = new GenericHeader(Headers.PSEUDO_STATUS, "200");
		this.headers = new LinkedList<>();
	}
	
	public boolean isWritten() {
		return this.written;
	}
	
	public void setWritten(boolean headersSent) {
		this.written = headersSent;
	}
	
	public int getStatus() {
		return this.status;
	}
	
	public Long getSize() {
		return this.size;
	}
	
	public Headers.ContentType getContentType() {
		return this.contentTypeHeader;
	}
	
	public List<Header> getAllAsList() {
		List<Header> result = new LinkedList<>();
		result.add(this.statusHeader);
		result.add(this.contentTypeHeader);
		result.add(this.contentLengthHeader);
		result.addAll(this.headers);
		
		return result.stream().filter(Objects::nonNull).map(header -> {
			if(!header.getClass().equals(GenericHeader.class)) {
				return new GenericHeader(header.getHeaderName(), this.headerService.encodeValue(header));
			}
			return header;
		}).collect(Collectors.toList());
	}
	
	// TODO This has some rooms for optimization
	private Map<String, List<Header>> getAll() {
		List<Header> result = new LinkedList<>();
		result.add(this.statusHeader);
		result.add(this.contentTypeHeader);
		result.add(this.contentLengthHeader);
		result.addAll(this.headers);
		
		return result.stream().filter(Objects::nonNull).map(header -> {
			if(!header.getClass().equals(GenericHeader.class)) {
				return new GenericHeader(header.getHeaderName(), this.headerService.encodeValue(header));
			}
			return header;
		}).collect(Collectors.groupingBy(Header::getHeaderName));
	}
	
	public Set<String> getNames() {
		return this.getAll().keySet();
	}

	public <T extends Header> Optional<T> get(String name) {
		return this.<T>getAll(name).stream().findFirst();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Header> List<T> getAll(String name) {
		List<Header> all = this.getAll().get(name);
		if(all != null) {
			return all.stream().map(e -> (T)e).collect(Collectors.toList());
		}
		return List.of();
	}

	private void requireNonWritten() {
		if(this.written) {
			throw new IllegalStateException("Headers have been already written");
		}
	}
	
	@Override
	public ResponseHeaders status(int status) {
		this.requireNonWritten();
		this.status = status;
		this.statusHeader.setHeaderValue(Integer.toString(status));
		return this;
	}

	@Override
	public ResponseHeaders contentType(String contentType) {
		this.requireNonWritten();
		// TODO here I know I'll get a ContentTypeCodec.ContentType but that's not guaranteed...
		this.contentTypeHeader = this.headerService.decode(Headers.CONTENT_TYPE, contentType);
		if(this.contentTypeHeader.getCharset() == null) {
			this.contentTypeHeader.setCharset(this.charset);
		}
		else {
			this.charset = this.contentTypeHeader.getCharset();
		}
		return this;
	}

	@Override
	public ResponseHeaders charset(Charset charset) {
		this.requireNonWritten();
		this.charset = charset;
		if(this.contentTypeHeader != null) {
			this.contentTypeHeader.setCharset(charset);
		}
		return this;
	}

	@Override
	public ResponseHeaders size(long size) {
		this.requireNonWritten();
		this.size = size;
		if(this.contentLengthHeader == null) {
			this.contentLengthHeader = new GenericHeader(Headers.CONTENT_LENGTH, Long.toString(size));
		}
		else {
			this.contentLengthHeader.setHeaderValue(Long.toString(size));
		}
		return this;
	}

	@Override
	public ResponseHeaders add(Header... headers) {
		this.requireNonWritten();
		if(this.headers == null) {
			this.headers = new LinkedList<>();
		}
		this.headers.addAll(Arrays.asList(headers));
		return this;
	}

	@Override
	public ResponseHeaders add(String name, String value) {
		this.requireNonWritten();
		this.add(this.headerService.decode(name, value));
		return this;
	}
}
