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
package io.winterframework.mod.web.internal.header;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

import io.winterframework.mod.web.Headers;

/**
 * 
 * @author jkuhn
 *
 */
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
