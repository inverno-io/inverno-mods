/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.ResponseCookies;
import io.winterframework.mod.web.SetCookie.Configurator;
import io.winterframework.mod.web.internal.header.SetCookieCodec;

/**
 * @author jkuhn
 *
 */
public class GenericResponseCookies implements ResponseCookies {

	private HeaderService headerService;
	
	private List<SetCookieCodec.SetCookie> cookies;
	
	public GenericResponseCookies(HeaderService headerService) {
		this.headerService = headerService;
		this.cookies = new LinkedList<>();
	}
	
	public List<Headers.SetCookie> getAll() {
		return Collections.unmodifiableList(this.cookies);
	}
	
	@Override
	public ResponseCookies addCookie(Consumer<Configurator> configurer) {
		SetCookieCodec.SetCookie setCookie = new SetCookieCodec.SetCookie();
		configurer.accept(setCookie);
		setCookie.setHeaderValue(this.headerService.encodeValue(setCookie));
		this.cookies.add(setCookie);
		return this;
	}
}
