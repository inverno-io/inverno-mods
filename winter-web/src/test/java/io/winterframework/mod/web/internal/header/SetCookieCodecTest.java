package io.winterframework.mod.web.internal.header;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

import io.winterframework.mod.web.Headers;

public class SetCookieCodecTest {

	@Test
	public void testDecode() {
		String setCookieValue = " toto=tata; Path=/; Domain=localhost; Expires=Thu, 05-Nov-2020 13:00:04 GMT; Max-Age=123; Secure; HttpOnly";
		
		SetCookieCodec codec = new SetCookieCodec();
		
		Headers.SetCookie cookie = codec.decode(Headers.SET_COOKIE, setCookieValue);
		
		Assertions.assertEquals("toto", cookie.getName());
		Assertions.assertEquals("tata", cookie.getValue());
		Assertions.assertEquals("/", cookie.getPath());
		Assertions.assertEquals("localhost", cookie.getDomain());
		Assertions.assertEquals("Thu, 05-Nov-2020 13:00:04 GMT", cookie.getExpires());
		Assertions.assertEquals(123, cookie.getMaxAge());
		Assertions.assertTrue(cookie.isSecure());
		Assertions.assertTrue(cookie.isHttpOnly());
	}
	
	@Test
	public void testEncode() {
		String setCookieValue = " toto=tata; Path=/; Domain=localhost; Expires=Thu, 05-Nov-2020 13:00:04 GMT; Max-Age=123; Secure; HttpOnly";
		
		SetCookieCodec codec = new SetCookieCodec();
		
		SetCookieCodec.SetCookie cookie = codec.decode(Headers.SET_COOKIE, setCookieValue);
		
		Assertions.assertEquals("set-cookie: toto=tata; Expires=Thu, 05-Nov-2020 13:00:04 GMT; Max-Age=123; Domain=localhost; Path=/; Secure; HttpOnly", codec.encode(cookie));
	}

}
