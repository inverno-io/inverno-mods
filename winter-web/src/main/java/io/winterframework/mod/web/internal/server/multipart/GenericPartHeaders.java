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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.header.Header;
import io.winterframework.mod.web.internal.server.GenericParameter;
import io.winterframework.mod.web.server.PartHeaders;

/**
 * @author jkuhn
 *
 */
// TODO Relying on Header by default is not performant so this needs to be refactored as well as the MultiparBodyDecoder
class GenericPartHeaders implements PartHeaders {

	private final ObjectConverter<String> parameterConverter;
	
	private String contentType;
	
	private Long contentLength;
	
	private Map<String, List<? extends Header>> headers;
	
	public GenericPartHeaders(Map<String, List<Header>> headers, String contentType, Charset charset, Long contentLength, ObjectConverter<String> parameterConverter) {
		this.parameterConverter = parameterConverter;
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
	public boolean contains(CharSequence name) {
		// TODO case insensitive
		return this.headers.containsKey(name);
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		List<? extends Header> headers = this.headers.get(name);
		if(headers != null) {
			for(Header h : headers) {
				if(h.getHeaderValue().equalsIgnoreCase(value.toString())) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public Set<String> getNames() {
		return this.headers.keySet();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name)).map(headers -> {
			if(!headers.isEmpty()) {
				return headers.get(0).getHeaderValue();
			}
			return null;
		});
	}

	@Override
	public List<String> getAll(CharSequence name) {
		List<? extends Header> headers = this.headers.get(name);
		if(headers != null) {
			return headers.stream().map(h -> h.getHeaderValue()).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public List<Entry<String, String>> getAll() {
		return this.headers.values().stream().flatMap(l -> l.stream().map(h -> Map.entry(h.getHeaderName(), h.getHeaderValue()))).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name)).map(headers -> {
			if(!headers.isEmpty()) {
				return (T)headers.get(0);
			}
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		List<? extends Header> headers = this.headers.get(name);
		if(headers != null) {
			return headers.stream().map(header -> (T)header).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public List<Header> getAllHeader() {
		return this.headers.values().stream().flatMap(l -> l.stream()).collect(Collectors.toList());
	}

	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name)).map(headers -> {
			if(!headers.isEmpty()) {
				return new GenericParameter(this.parameterConverter, headers.get(0).getHeaderName(), headers.get(0).getHeaderValue());
			}
			return null;
		});
	}

	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		List<? extends Header> headers = this.headers.get(name);
		if(headers != null) {
			return headers.stream().map(h -> new GenericParameter(this.parameterConverter, h.getHeaderName(), h.getHeaderValue())).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public List<Parameter> getAllParameter() {
		return this.headers.values().stream().flatMap(l -> l.stream().map(h -> new GenericParameter(this.parameterConverter, h.getHeaderName(), h.getHeaderValue()))).collect(Collectors.toList());
	}

	
	
	/*@Override
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
	}*/
}
