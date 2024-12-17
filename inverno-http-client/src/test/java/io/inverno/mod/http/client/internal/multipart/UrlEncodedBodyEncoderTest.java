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

package io.inverno.mod.http.client.internal.multipart;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.base.internal.header.GenericHeaderService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class UrlEncodedBodyEncoderTest {

	@Test
	public void test() {
		GenericHeaderService headerService = new GenericHeaderService(List.of(new ContentTypeCodec()));
		
		Headers.ContentType contentType = headerService.<Headers.ContentType>decode("content-type: application/x-www-form-urlencoded");
		
		StringConverter converter = new StringConverter();
		
		UrlEncodedBodyEncoder encoder = new UrlEncodedBodyEncoder();
		
		Parameter p1 = new GenericParameter("k1", converter.encodeList(List.of("a", "b", "c")), converter);
		
		String result = encoder.encode(Flux.just(p1), contentType)
			.map(buf -> buf.toString(Charsets.DEFAULT))
			.reduceWith(() -> new StringBuilder(), (acc, chunk) -> acc.append(chunk))
			.map(StringBuilder::toString)
			.block();
		
		Assertions.assertEquals("k1=a,b,c", result);
		
		Parameter p2 = new GenericParameter("k2", "v2", converter);
		Parameter p3 = new GenericParameter("k3", "v3", converter);
		
		result = encoder.encode(Flux.just(p1, p2, p3), contentType)
			.map(buf -> buf.toString(Charsets.DEFAULT))
			.reduceWith(() -> new StringBuilder(), (acc, chunk) -> acc.append(chunk))
			.map(StringBuilder::toString)
			.block();
		
		Assertions.assertEquals("k1=a,b,c&k2=v2&k3=v3", result);
		
		result = encoder.encode(Flux.just(new GenericParameter("escaped", "v2/test_#_/_?_:_[_]", converter)), contentType)
			.map(buf -> buf.toString(Charsets.DEFAULT))
			.reduceWith(() -> new StringBuilder(), (acc, chunk) -> acc.append(chunk))
			.map(StringBuilder::toString)
			.block();
		
		Assertions.assertEquals("escaped=v2/test_%23_/_?_:_%5B_%5D", result);
	}
}
