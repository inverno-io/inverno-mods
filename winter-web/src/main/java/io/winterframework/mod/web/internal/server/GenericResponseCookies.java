/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import java.util.List;
import java.util.function.Consumer;

import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.ResponseCookies;
import io.winterframework.mod.web.SetCookie;
import io.winterframework.mod.web.internal.header.SetCookieCodec;

/**
 * @author jkuhn
 *
 */
public class GenericResponseCookies implements ResponseCookies {

	private final HeaderService headerService;
	
	private final AbstractResponseHeaders responseHeaders;
	
	public GenericResponseCookies(HeaderService headerService, AbstractResponseHeaders responseHeaders) {
		this.headerService = headerService;
		this.responseHeaders = responseHeaders;
	}
	
	public List<Headers.SetCookie> getAll() {
		return this.responseHeaders.getAll(Headers.NAME_SET_COOKIE);
	}
	
	@Override
	public ResponseCookies addCookie(Consumer<SetCookie.Configurator> configurer) {
		SetCookieCodec.SetCookie setCookie = new SetCookieCodec.SetCookie();
		configurer.accept(setCookie);
		setCookie.setHeaderValue(this.headerService.encodeValue(setCookie));
		this.responseHeaders.add(setCookie);
		return this;
	}
}
