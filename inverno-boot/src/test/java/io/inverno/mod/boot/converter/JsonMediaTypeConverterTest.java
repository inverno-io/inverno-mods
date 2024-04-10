/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.boot.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.boot.Person;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class JsonMediaTypeConverterTest {

	private static final JsonByteBufMediaTypeConverter CONVERTER = new JsonByteBufMediaTypeConverter(new JacksonByteBufConverter(new ObjectMapper()));
	
	@Test
	public void testEncodeMany() throws IOException {
		Flux<Person> in = Flux.empty();
		Publisher<ByteBuf> out = CONVERTER.encodeMany(in);
		List<ByteBuf> outList = Flux.from(out).collectList().block();
		
		Assertions.assertEquals(2, outList.size());
		
		Assertions.assertEquals("[", outList.get(0).toString(Charset.defaultCharset()));
		Assertions.assertEquals("]", outList.get(1).toString(Charset.defaultCharset()));
		
		
		in = Flux.just(new Person("John", "Smith", 42));
		out = CONVERTER.encodeMany(in);
		outList = Flux.from(out).collectList().block();
		
		Assertions.assertEquals(3, outList.size());
		
		Assertions.assertEquals("[", outList.get(0).toString(Charset.defaultCharset()));
		Assertions.assertEquals("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}", outList.get(1).toString(Charset.defaultCharset()));
		Assertions.assertEquals("]", outList.get(2).toString(Charset.defaultCharset()));
		
		in = Flux.just(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40));
		out = CONVERTER.encodeMany(in);
		outList = Flux.from(out).collectList().block();
		
		Assertions.assertEquals(4, outList.size());
		
		Assertions.assertEquals("[", outList.get(0).toString(Charset.defaultCharset()));
		Assertions.assertEquals("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}", outList.get(1).toString(Charset.defaultCharset()));
		Assertions.assertEquals(",{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40}", outList.get(2).toString(Charset.defaultCharset()));
		Assertions.assertEquals("]", outList.get(3).toString(Charset.defaultCharset()));
		
		in = Flux.just(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10));
		out = CONVERTER.encodeMany(in);
		outList = Flux.from(out).collectList().block();
		
		Assertions.assertEquals(5, outList.size());
		
		Assertions.assertEquals("[", outList.get(0).toString(Charset.defaultCharset()));
		Assertions.assertEquals("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}", outList.get(1).toString(Charset.defaultCharset()));
		Assertions.assertEquals(",{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40}", outList.get(2).toString(Charset.defaultCharset()));
		Assertions.assertEquals(",{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}", outList.get(3).toString(Charset.defaultCharset()));
		Assertions.assertEquals("]", outList.get(4).toString(Charset.defaultCharset()));
	}
	
	@Test
	public void testGetSupportedMediaTypes() {
		Assertions.assertTrue(CONVERTER.canConvert(MediaTypes.APPLICATION_JSON));
	}
}
