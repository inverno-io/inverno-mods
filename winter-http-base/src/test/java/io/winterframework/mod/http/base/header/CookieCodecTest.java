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
package io.winterframework.mod.http.base.header;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.base.converter.StringConverter;
import io.winterframework.mod.http.base.internal.header.CookieCodec;
import io.winterframework.mod.http.base.internal.header.GenericCookieParameter;

/**
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class CookieCodecTest {
	
	@Test
	public void testDecode() {
		String headerValue = "1P_JAR=2020-10-13-12; "
			+ "NID=204=PLZEn90MwzM-lfB8aE_-Zcp6bBvw196G_2lLPt4L_L2Y2YsGNpsOx628wCt4iKr8mep92WQKAZcKkt2yjdpZL4YMYlxo0xRBKvKaBNON6jYfHbM7KdW4bGWAObih3zPDY_Zgl9iStP3SFyHBxS6oKppbOHniTIJ46iv-KCrjhmBkBuoMcC72EI_L4bYI491ebSu04lb3WzNTpMh_WN9fJxFaJdgXNQA8I8iY8SlMzn3ettLHG13Co8ZUit0EXuppdYMEVF72dX8RL-KnWNYXc5lsES4tUtjrtiRo5ugWW9Dq-iCx-_69o8mPnxBMiFvDSSLcXv; "
			+ "CONSENT=YES+FR.fr+210-02-0; "
			+ "ANID=AHWqTUlv9Nri9v0HOUQmy1cr2wsOYCi-AhUTgJtHtqQ7lDKDDh; "
			+ "SID=1wfys4gsGRgjP-z8Imj6224tOBW0hk0JdK4oDEc8_mQ_ZMB930CA.; "
			+ "HSID=ALLza9Syaf_; "
			+ "SSID=AT_wMTZQecv; "
			+ "APISID=Bp3ZNWav5t/AuMXkQoUqDvmoNTjO; "
			+ "SAPISID=yfu9B/AfTiOfU7EADey05; "
			+ "SIDCC=AJi4QfGeY7eKj4Ak_JeXy-0ipxdSiWEMkSngLO1LTkC_lUd8FstjY6YAy1DYRyVY; "
			+ "SEARCH_SAMESITE=CIwpAB; "
			+ "__Secure-3PSID=1wfys4gsGRgjP-z8Imj6224tOBW0hks6d_Q0ckTs0pTWe2WFzRhA.; "
			+ "__Secure-3PAPISID=yfu9B/AfTiOfU7EADey05; "
			+ "__Secure-3PSIDCC=AJi4QfE4uNCt-r5rWZq8Dq_xogH0Uq8hpCWcoVzaSF8RXFkD92FYMmrY8A1Csu1Lxh9R; "
			+ "OTZ=5645221_48_52_123900_48_436380";
		
		ObjectConverter<String> parameterConverter = new StringConverter();
		CookieCodec codec = new CookieCodec(parameterConverter);
		
		Headers.Cookie cookie = codec.decode(Headers.NAME_COOKIE, headerValue);
		
		Map<String, List<GenericCookieParameter>> expected = Map.ofEntries(
			Map.entry("1P_JAR", List.of(new GenericCookieParameter(parameterConverter, "1P_JAR", "2020-10-13-12"))),
			Map.entry("NID", List.of(new GenericCookieParameter(parameterConverter, "NID", "204=PLZEn90MwzM-lfB8aE_-Zcp6bBvw196G_2lLPt4L_L2Y2YsGNpsOx628wCt4iKr8mep92WQKAZcKkt2yjdpZL4YMYlxo0xRBKvKaBNON6jYfHbM7KdW4bGWAObih3zPDY_Zgl9iStP3SFyHBxS6oKppbOHniTIJ46iv-KCrjhmBkBuoMcC72EI_L4bYI491ebSu04lb3WzNTpMh_WN9fJxFaJdgXNQA8I8iY8SlMzn3ettLHG13Co8ZUit0EXuppdYMEVF72dX8RL-KnWNYXc5lsES4tUtjrtiRo5ugWW9Dq-iCx-_69o8mPnxBMiFvDSSLcXv"))),
			Map.entry("CONSENT", List.of(new GenericCookieParameter(parameterConverter, "CONSENT", "YES+FR.fr+210-02-0"))),
			Map.entry("ANID", List.of(new GenericCookieParameter(parameterConverter, "ANID", "AHWqTUlv9Nri9v0HOUQmy1cr2wsOYCi-AhUTgJtHtqQ7lDKDDh"))),
			Map.entry("SID", List.of(new GenericCookieParameter(parameterConverter, "SID", "1wfys4gsGRgjP-z8Imj6224tOBW0hk0JdK4oDEc8_mQ_ZMB930CA."))),
			Map.entry("HSID", List.of(new GenericCookieParameter(parameterConverter, "HSID", "ALLza9Syaf_"))),
			Map.entry("SSID", List.of(new GenericCookieParameter(parameterConverter, "SSID", "AT_wMTZQecv"))),
			Map.entry("APISID", List.of(new GenericCookieParameter(parameterConverter, "APISID", "Bp3ZNWav5t/AuMXkQoUqDvmoNTjO"))),
			Map.entry("SAPISID", List.of(new GenericCookieParameter(parameterConverter, "SAPISID", "yfu9B/AfTiOfU7EADey05"))),
			Map.entry("SIDCC", List.of(new GenericCookieParameter(parameterConverter, "SIDCC", "AJi4QfGeY7eKj4Ak_JeXy-0ipxdSiWEMkSngLO1LTkC_lUd8FstjY6YAy1DYRyVY"))),
			Map.entry("SEARCH_SAMESITE", List.of(new GenericCookieParameter(parameterConverter, "SEARCH_SAMESITE", "CIwpAB"))),
			Map.entry("__Secure-3PSID", List.of(new GenericCookieParameter(parameterConverter, "__Secure-3PSID", "1wfys4gsGRgjP-z8Imj6224tOBW0hks6d_Q0ckTs0pTWe2WFzRhA."))),
			Map.entry("__Secure-3PAPISID", List.of(new GenericCookieParameter(parameterConverter, "__Secure-3PAPISID", "yfu9B/AfTiOfU7EADey05"))),
			Map.entry("__Secure-3PSIDCC", List.of(new GenericCookieParameter(parameterConverter, "__Secure-3PSIDCC", "AJi4QfE4uNCt-r5rWZq8Dq_xogH0Uq8hpCWcoVzaSF8RXFkD92FYMmrY8A1Csu1Lxh9R"))),
			Map.entry("OTZ", List.of(new GenericCookieParameter(parameterConverter, "OTZ", "5645221_48_52_123900_48_436380")))
		);
		
		Assertions.assertEquals(expected, cookie.getPairs());
	}

	@Test
	public void testEncode() {
		String cookieValue = " toto=tata; titi=tutu; abc=def; toto=tutu";
		
		ObjectConverter<String> parameterConverter = new StringConverter();
		CookieCodec codec = new CookieCodec(parameterConverter);
		
		CookieCodec.Cookie cookie = codec.decode(Headers.NAME_COOKIE, cookieValue);
		
		Assertions.assertEquals("cookie: toto=tata; toto=tutu; abc=def; titi=tutu", codec.encode(cookie));
	}
}
