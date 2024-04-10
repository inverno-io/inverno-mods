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
import io.netty.buffer.Unpooled;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class NdJsonMediaTypeConverterTest {

	private static final NdJsonByteBufMediaTypeConverter CONVERTER = new NdJsonByteBufMediaTypeConverter(new JacksonByteBufConverter(new ObjectMapper()));
	
	@Test
	public void testEncodeOne() {
		Mono<Person> in = Mono.just(new Person("John", "Smith", 42));
		
		Publisher<ByteBuf> out = CONVERTER.encodeOne(in);
		
		Assertions.assertEquals("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}\n", Flux.from(out).single().block().toString(Charset.defaultCharset()));
	}

	@Test
	public void testEncodeMany() {
		Flux<Person> in = Flux.just(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10));
		
		Publisher<ByteBuf> out = CONVERTER.encodeMany(in);
		
		List<ByteBuf> outList = Flux.from(out).collectList().block();
		
		Assertions.assertEquals(3, outList.size());
		
		Assertions.assertEquals("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}\n", outList.get(0).toString(Charset.defaultCharset()));
		Assertions.assertEquals("{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40}\n", outList.get(1).toString(Charset.defaultCharset()));
		Assertions.assertEquals("{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}\n", outList.get(2).toString(Charset.defaultCharset()));
	}

	@Test
	public void testEncode() {
		Person in = new Person("John", "Smith", 42);
		
		ByteBuf out = CONVERTER.encode(in);
		
		Assertions.assertEquals("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}\n", out.toString(Charset.defaultCharset()));
	}
	
	@Test
	public void testDecode() {
		Publisher<ByteBuf> in = Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}\n" + "{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40}\n" + "{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}\n", Charset.defaultCharset())));
		
		Publisher<Person> out = CONVERTER.decodeMany(in, Person.class);
		
		List<Person> outList = Flux.from(out).collectList().block();
		
		Assertions.assertEquals(3, outList.size());
		Assertions.assertEquals(new Person("John", "Smith", 42), outList.get(0));
		Assertions.assertEquals(new Person("Jane", "Smith", 40), outList.get(1));
		Assertions.assertEquals(new Person("Junior", "Smith", 10), outList.get(2));
	}

	/*@Test
	public void testEncodeList() {
		List<Person> in = List.of(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10));
		
		ByteBuf out = CONVERTER.encodeList(in);
		
		Assertions.assertEquals("[{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40},{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}]\n", out.toString(Charset.defaultCharset()));
	}

	@Test
	public void testEncodeSet() {
		Set<Person> in = new LinkedHashSet<>(List.of(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10)));
		
		ByteBuf out = CONVERTER.encodeSet(in);
		
		Assertions.assertEquals("[{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40},{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}]\n", out.toString(Charset.defaultCharset()));
	}

	@Test
	public void testEncodeArray() {
		Person[] in = new Person[] {new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10)};
		
		ByteBuf out = CONVERTER.encodeArray(in);
		
		Assertions.assertEquals("[{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40},{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}]\n", out.toString(Charset.defaultCharset()));
	}*/

	@Test
	public void testGetSupportedMediaTypes() {
		Assertions.assertTrue(CONVERTER.canConvert(MediaTypes.APPLICATION_X_NDJSON));
	}

}
