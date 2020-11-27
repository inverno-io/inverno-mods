/**
 * 
 */
package io.winterframework.mod.web.internal.server.http1x;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.HttpRequest;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestHeaders;
import io.winterframework.mod.web.internal.Charsets;

/**
 * @author jkuhn
 *
 */
public class Http1xRequestHeaders  implements RequestHeaders {

	private HeaderService HeaderService;
	
	private HttpRequest request;
	
	private boolean isSsl;
	
	public Http1xRequestHeaders(HeaderService HeaderService, HttpRequest request, boolean isSsl) {
		this.HeaderService = HeaderService;
		this.request = request;
		this.isSsl = isSsl;
	}

	private String getHeader(String name) {
		CharSequence header = this.request.headers().get(name);
		return header != null ? header.toString() : null;
	}
	
	@Override
	public String getAuthority() {
		return this.getHeader(Headers.HOST);
	}

	@Override
	public String getPath() {
		return this.request.uri();
	}

	@Override
	public Method getMethod() {
		return Method.valueOf(this.request.method().name());
	}

	@Override
	public String getScheme() {
		return this.isSsl ? "https" : "http";
	}

	@Override
	public String getContentType() {
		return this.getHeader(Headers.CONTENT_TYPE);
	}

	@Override
	public Charset getCharset() {
		return this.<Headers.ContentType>get(Headers.CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
	}

	@Override
	public Long getSize() {
		return this.request.headers().contains(Headers.CONTENT_LENGTH) ? Long.parseLong(this.request.headers().get(Headers.CONTENT_LENGTH)) : null;
	}

	@Override
	public Set<String> getNames() {
		return this.request.headers().names().stream().map(CharSequence::toString).collect(Collectors.toSet());
	}

	@Override
	public <T extends Header> Optional<T> get(String name) {
		return this.<T>getAll(name).stream().findFirst();
	}
	
	@Override
	public <T extends Header> List<T> getAll(String name) {
		return this.request.headers().getAll(name).stream().map(value -> this.HeaderService.<T>decode(name, value.toString())).collect(Collectors.toList());
	}

	@Override
	public Map<String, List<? extends Header>> getAll() {
		return this.request.headers().names().stream().collect(Collectors.toMap(Function.identity(), this::<Header>getAll));
	}
}
