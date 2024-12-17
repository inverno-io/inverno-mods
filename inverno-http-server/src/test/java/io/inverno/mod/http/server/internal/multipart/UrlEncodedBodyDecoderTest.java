/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.server.internal.multipart;

import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.ContentDispositionCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.base.internal.header.GenericHeaderService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class UrlEncodedBodyDecoderTest {
	
	private static final HeaderService HEADER_SERVICE = new GenericHeaderService(List.of(new ContentTypeCodec(), new ContentDispositionCodec()));
	
	private static final UrlEncodedBodyDecoder DECODER = new UrlEncodedBodyDecoder(new StringConverter());

	@Test
	public void test() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: application/x-www-form-urlencoded");
		
		ByteBuf buf = Unpooled.copiedBuffer("formParam=a,b,c&formParam=d,e,f", StandardCharsets.UTF_8);
		
		List<Parameter> parameters = DECODER.decode(Flux.just(buf), contentType).collectList().block();
		
		Assertions.assertEquals(2, parameters.size());
		
		Assertions.assertEquals("formParam", parameters.get(0).getName());
		Assertions.assertEquals("a,b,c", parameters.get(0).getValue());
		Assertions.assertEquals("formParam", parameters.get(1).getName());
		Assertions.assertEquals("d,e,f", parameters.get(1).getValue());
	}
	
	@Test
	public void test_multi() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: application/x-www-form-urlencoded");
		
		ByteBuf buf1 = Unpooled.copiedBuffer("a=1", StandardCharsets.UTF_8);
		ByteBuf buf2 = Unpooled.copiedBuffer("&b=2", StandardCharsets.UTF_8);
		ByteBuf buf3 = Unpooled.copiedBuffer("&c=3", StandardCharsets.UTF_8);
		ByteBuf buf4 = Unpooled.copiedBuffer("&c=4", StandardCharsets.UTF_8);
		
		List<Parameter> parameters = DECODER.decode(Flux.just(buf1, buf2, buf3, buf4), contentType).collectList().block();
		
		Assertions.assertEquals(4, parameters.size());
		
		Assertions.assertEquals("a", parameters.get(0).getName());
		Assertions.assertEquals("1", parameters.get(0).getValue());
		Assertions.assertEquals("b", parameters.get(1).getName());
		Assertions.assertEquals("2", parameters.get(1).getValue());
		Assertions.assertEquals("c", parameters.get(2).getName());
		Assertions.assertEquals("3", parameters.get(2).getValue());
		Assertions.assertEquals("c", parameters.get(3).getName());
		Assertions.assertEquals("4", parameters.get(3).getValue());
		
	}
	
	@Test
	public void test_randomSplit() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: application/x-www-form-urlencoded");
		
		ByteBuf buf1 = Unpooled.copiedBuffer("a=1&", StandardCharsets.UTF_8);
		ByteBuf buf2 = Unpooled.copiedBuffer("b=2&c", StandardCharsets.UTF_8);
		ByteBuf buf3 = Unpooled.copiedBuffer("=3", StandardCharsets.UTF_8);
		ByteBuf buf4 = Unpooled.copiedBuffer("&c", StandardCharsets.UTF_8);
		ByteBuf buf5 = Unpooled.copiedBuffer("=4", StandardCharsets.UTF_8);
		
		List<Parameter> parameters = DECODER.decode(Flux.just(buf1, buf2, buf3, buf4, buf5), contentType).collectList().block();
		
		Assertions.assertEquals(4, parameters.size());
		
		Assertions.assertEquals("a", parameters.get(0).getName());
		Assertions.assertEquals("1", parameters.get(0).getValue());
		Assertions.assertEquals("b", parameters.get(1).getName());
		Assertions.assertEquals("2", parameters.get(1).getValue());
		Assertions.assertEquals("c", parameters.get(2).getName());
		Assertions.assertEquals("3", parameters.get(2).getValue());
		Assertions.assertEquals("c", parameters.get(3).getName());
		Assertions.assertEquals("4", parameters.get(3).getValue());
		
	}
}
