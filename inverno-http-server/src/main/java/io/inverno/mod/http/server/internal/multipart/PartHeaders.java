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
package io.inverno.mod.http.server.internal.multipart;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link InboundRequestHeaders} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
// TODO Relying on Header by default is not performant so this needs to be refactored as well as the MultipartBodyDecoder
class PartHeaders implements InboundRequestHeaders {

	private final ObjectConverter<String> parameterConverter;
	
	private final Map<String, List<? extends Header>> headers;
	
	/**
	 * <p>
	 * Creates part headers.
	 * </p>
	 * 
	 * @param headers            the map of headers
	 * @param parameterConverter a string object converter
	 */
	public PartHeaders(Map<String, List<Header>> headers, ObjectConverter<String> parameterConverter) {
		this.parameterConverter = parameterConverter;
		this.headers = headers != null ? Collections.unmodifiableMap(headers) : Map.of();
	}
	
	@Override
	public String getContentType() {
		return Optional.ofNullable(this.headers.get(Headers.NAME_CONTENT_TYPE)).filter(l -> !l.isEmpty()).map(l -> l.getFirst().getHeaderValue()).orElse(null);
	}
	
	@Override
	public Headers.ContentType getContentTypeHeader() {
		return (Headers.ContentType)Optional.ofNullable(this.headers.get(Headers.NAME_CONTENT_TYPE)).filter(l -> !l.isEmpty()).map(List::getFirst).orElse(null);
	}

	@Override
	public String getAccept() {
		return Optional.ofNullable(this.headers.get(Headers.NAME_ACCEPT)).filter(l -> !l.isEmpty()).map(l -> l.getFirst().getHeaderValue()).orElse(null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Headers.Accept getAcceptHeader() {
		return Optional.ofNullable(this.headers.get(Headers.NAME_ACCEPT)).filter(l -> !l.isEmpty())
			.flatMap(l -> Headers.Accept.merge((List<Headers.Accept>) l))
			.orElse(null);
	}

	@Override
	public Long getContentLength() {
		return Optional.ofNullable(this.headers.get(Headers.NAME_CONTENT_LENGTH)).filter(l -> !l.isEmpty()).map(l -> l.getFirst().getHeaderValue()).map(Long::parseLong).orElse(null);
	}

	@Override
	public InboundCookies cookies() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean contains(CharSequence name) {
		// TODO case insensitive
		return this.headers.containsKey(name.toString());
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		List<? extends Header> allHeaders = this.headers.get(name.toString());
		if(allHeaders != null) {
			for(Header h : allHeaders) {
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
		return Optional.ofNullable(this.headers.get(name.toString())).map(allHeaders -> {
			if(!allHeaders.isEmpty()) {
				return allHeaders.getFirst().getHeaderValue();
			}
			return null;
		});
	}

	@Override
	public List<String> getAll(CharSequence name) {
		List<? extends Header> allHeaders = this.headers.get(name.toString());
		if(allHeaders != null) {
			return allHeaders.stream().map(Header::getHeaderValue).collect(Collectors.toList());
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
		return Optional.ofNullable(this.headers.get(name.toString())).map(allHeaders -> {
			if(!allHeaders.isEmpty()) {
				return (T)allHeaders.getFirst();
			}
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		List<? extends Header> allHeaders = this.headers.get(name.toString());
		if(allHeaders != null) {
			return allHeaders.stream().map(header -> (T)header).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public List<Header> getAllHeader() {
		return this.headers.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
	}

	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return Optional.ofNullable(this.headers.get(name.toString())).map(allHeaders -> {
			if(!allHeaders.isEmpty()) {
				return new GenericParameter(allHeaders.getFirst().getHeaderName(), allHeaders.getFirst().getHeaderValue(), this.parameterConverter);
			}
			return null;
		});
	}

	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		List<? extends Header> allHeaders = this.headers.get(name.toString());
		if(allHeaders != null) {
			return allHeaders.stream().map(h -> new GenericParameter(h.getHeaderName(), h.getHeaderValue(), this.parameterConverter)).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public List<Parameter> getAllParameter() {
		return this.headers.values().stream().flatMap(l -> l.stream().map(h -> new GenericParameter(h.getHeaderName(), h.getHeaderValue(), this.parameterConverter))).collect(Collectors.toList());
	}
}
