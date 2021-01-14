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
package io.winterframework.mod.web.internal.server.multipart;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.web.header.Header;
import io.winterframework.mod.web.server.PartHeaders;

/**
 * @author jkuhn
 *
 */
class GenericPartHeaders implements PartHeaders {

	private String contentType;
	
	private Long contentLength;
	
	private Map<String, List<? extends Header>> headers;
	
	public GenericPartHeaders(Map<String, List<Header>> headers, String contentType, Charset charset, Long contentLength) {
		this.headers = headers != null ? Collections.unmodifiableMap(headers) : Map.of();
		this.contentType = contentType;
		this.contentLength = contentLength;
	}
	
	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public Long getContentLength() {
		return this.contentLength;
	}

	@Override
	public Set<String> getNames() {
		return this.headers.keySet();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Header> Optional<T> getHeader(String name) {
		return Optional.ofNullable(this.headers.get(name)).map(headers -> {
			if(!headers.isEmpty()) {
				return (T)headers.get(0);
			}
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> List<T> getAllHeader(String name) {
		if(this.headers.containsKey(name)) {
			return this.headers.get(name).stream().map(header -> (T)header).collect(Collectors.toList());
		}
		return null;
	}

	@Override
	public Map<String, List<? extends Header>> getAllHeader() {
		return this.headers;
	}
}
