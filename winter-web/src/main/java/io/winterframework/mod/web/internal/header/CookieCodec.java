/**
 * 
 */
package io.winterframework.mod.web.internal.header;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.internal.server.GenericCookie;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class CookieCodec extends ParameterizedHeaderCodec<CookieCodec.Cookie, CookieCodec.Cookie.Builder> {

	public CookieCodec() {
		super(CookieCodec.Cookie.Builder::new, Set.of(Headers.COOKIE), DEFAULT_DELIMITER, true, true, false, false, false);
	}
	
	@Override
	public String encodeValue(Cookie headerField) {
		return headerField.getPairs().values().stream().flatMap(List::stream).map(cookie -> cookie.getName() + "=" + cookie.getValue()).collect(Collectors.joining("; "));
	}
	
	public static final class Cookie extends ParameterizedHeader implements Headers.Cookie {
		
		private Map<String, List<io.winterframework.mod.web.Cookie>> pairs;
		
		private Cookie(String headerName, String headerValue, Map<String, List<io.winterframework.mod.web.Cookie>> pairs, Map<String, String> parameters) {
			super(Headers.COOKIE, headerValue, null, parameters);
			
			this.pairs = pairs;
		}
		
		@Override
		public Map<String, List<io.winterframework.mod.web.Cookie>> getPairs() {
			return this.pairs;
		}

		public static final class Builder extends ParameterizedHeader.AbstractBuilder<Cookie, Builder> {

			private Map<String, List<io.winterframework.mod.web.Cookie>> pairs = new HashMap<>();

			@Override
			public Builder parameter(String name, String value) {
				if(!this.pairs.containsKey(name)) {
					this.pairs.put(name, new LinkedList<>());
				}
				this.pairs.get(name).add(new GenericCookie(name, value));
				return this;
			}
			
			@Override
			public Cookie build() {
				return new Cookie(this.headerName, this.headerValue, this.pairs, this.parameters);
			}
		}
	}
}
