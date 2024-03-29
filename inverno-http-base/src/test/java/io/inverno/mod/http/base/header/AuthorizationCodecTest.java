/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.http.base.header;

import io.inverno.mod.http.base.internal.header.AuthorizationCodec;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class AuthorizationCodecTest {
	
	@Test
	public void testAuthorization() {
		AuthorizationCodec codec = new AuthorizationCodec();

		AuthorizationCodec.Authorization header = codec.decode(Headers.NAME_AUTHORIZATION, "digest username=\"Mufasa\",realm=\"http-auth@example.org\",uri=\"/dir/index.html\",algorithm=SHA-256,nonce=\"7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v\",nc=00000001,cnonce=\"f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ\",qop=auth,response=\"753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1\",opaque=\"FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS\"");

		Assertions.assertEquals("digest", header.getAuthScheme());
		Assertions.assertNull(header.getToken());
		Assertions.assertEquals(10, header.getParameters().size());
		
		Assertions.assertEquals("Mufasa", header.getParameters().get("username"));
		Assertions.assertEquals("http-auth@example.org", header.getParameters().get("realm"));
		Assertions.assertEquals("/dir/index.html", header.getParameters().get("uri"));
		Assertions.assertEquals("SHA-256", header.getParameters().get("algorithm"));
		Assertions.assertEquals("7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v", header.getParameters().get("nonce"));
		Assertions.assertEquals("00000001", header.getParameters().get("nc"));
		Assertions.assertEquals("f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ", header.getParameters().get("cnonce"));
		Assertions.assertEquals("auth", header.getParameters().get("qop"));
		Assertions.assertEquals("753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1", header.getParameters().get("response"));
		Assertions.assertEquals("FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS", header.getParameters().get("opaque"));

		Assertions.assertEquals("authorization: digest qop=auth,opaque=FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS,nc=00000001,response=753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1,realm=http-auth@example.org,uri=/dir/index.html,nonce=7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v,cnonce=f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ,username=Mufasa,algorithm=SHA-256", codec.encode(header));
		
		header = codec.decode("Authorization", "bearer 7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v=");
		
		Assertions.assertEquals("bearer", header.getAuthScheme());
		Assertions.assertEquals("7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v=", header.getToken());
		Assertions.assertEquals(0, header.getParameters().size());
		
		Assertions.assertEquals("authorization: bearer 7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v=", codec.encode(header));
	}
}
