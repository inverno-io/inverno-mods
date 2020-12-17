/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Cookie;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.RequestCookies;
import io.winterframework.mod.web.RequestHeaders;

/**
 * @author jkuhn
 *
 */
public class GenericRequestCookies implements RequestCookies {

	private Map<String, List<Cookie>> pairs; 
	
	public GenericRequestCookies(RequestHeaders requestHeaders) {
		this.pairs = requestHeaders.<Headers.Cookie>getAllHeader(Headers.NAME_COOKIE)
			.stream()
			.flatMap(cookieHeader -> cookieHeader.getPairs().values().stream().flatMap(List::stream))
			.collect(Collectors.groupingBy(Cookie::getName));
	}

	@Override
	public Set<String> getNames() {
		return this.pairs.keySet();
	}
	
	@Override
	public Optional<Cookie> get(String name) {
		return Optional.ofNullable(this.getAll(name)).map(cookies ->  {
			if(!cookies.isEmpty()) {
				return cookies.get(0);
			}
			return null;
		});
	}
	
	@Override
	public List<Cookie> getAll(String name) {
		return this.pairs.get(name);
	}

	@Override
	public Map<String, List<Cookie>> getAll() {
		return this.pairs;
	}
}
