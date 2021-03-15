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
package io.winterframework.mod.boot.internal.converter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.winterframework.mod.base.converter.ConverterException;
import io.winterframework.mod.boot.Person;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class JacksonByteBufConverterTest {

	private static final JacksonByteBufConverter CONVERTER = new JacksonByteBufConverter(new ObjectMapper());
	
	@Test
	public void testEncodeOne() {
		Mono<Person> in = Mono.just(new Person("John", "Smith", 42));
		
		Publisher<ByteBuf> out = CONVERTER.encodeOne(in);
		
		Assertions.assertEquals("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}", Flux.from(out).single().block().toString(Charset.defaultCharset()));
	}

	@Test
	public void testEncodeMany() {
		Flux<Person> in = Flux.just(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10));
		
		Publisher<ByteBuf> out = CONVERTER.encodeMany(in);
		
		List<ByteBuf> outList = Flux.from(out).collectList().block();
		
		Assertions.assertEquals(3, outList.size());
		
		Assertions.assertEquals("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}", outList.get(0).toString(Charset.defaultCharset()));
		Assertions.assertEquals("{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40}", outList.get(1).toString(Charset.defaultCharset()));
		Assertions.assertEquals("{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}", outList.get(2).toString(Charset.defaultCharset()));
	}
	
	private Map<String, Person> mapStringPerson;
	
	@Test
	public void testEncodeManyType() throws IOException, NoSuchFieldException, SecurityException {
		
		LinkedHashMap<String, Person> smithMap = new LinkedHashMap<>();
		smithMap.put("John Smith", new Person("John", "Smith", 42));
		smithMap.put("Jane Smith", new Person("Jane", "Smith", 40));
		
		LinkedHashMap<String, Person> cooperMap = new LinkedHashMap<>();
		cooperMap.put("Gary Cooper", new Person("Gary", "Cooper", 60));
		
		Flux<Map<String, Person>> in = Flux.just(smithMap, cooperMap);
		
		Type mapStringPersonType = JacksonByteBufConverterTest.class.getDeclaredField("mapStringPerson").getGenericType();
		
		Publisher<ByteBuf> out = CONVERTER.encodeMany(in, mapStringPersonType);
		
		List<ByteBuf> outList = Flux.from(out).collectList().block();
		
		Assertions.assertEquals(2, outList.size());
		
		Assertions.assertEquals("{\"John Smith\":{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},\"Jane Smith\":{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40}}", outList.get(0).toString(Charset.defaultCharset()));
		Assertions.assertEquals("{\"Gary Cooper\":{\"firstname\":\"Gary\",\"name\":\"Cooper\",\"age\":60}}", outList.get(1).toString(Charset.defaultCharset()));
	}

	@Test
	public void testEncode() {
		Person in = new Person("John", "Smith", 42);
		
		ByteBuf out = CONVERTER.encode(in);
		
		Assertions.assertEquals("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}", out.toString(Charset.defaultCharset()));
	}

	@Test
	public void testEncodeList() {
		List<Person> in = List.of(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10));
		
		ByteBuf out = CONVERTER.encodeList(in);
		
		Assertions.assertEquals("[{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40},{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}]", out.toString(Charset.defaultCharset()));
	}

	@Test
	public void testEncodeSet() {
		Set<Person> in = new LinkedHashSet<>(List.of(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10)));
		
		ByteBuf out = CONVERTER.encodeSet(in);
		
		Assertions.assertEquals("[{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40},{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}]", out.toString(Charset.defaultCharset()));
	}

	@Test
	public void testEncodeArray() {
		Person[] in = new Person[] {new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10)};
		
		ByteBuf out = CONVERTER.encodeArray(in);
		
		Assertions.assertEquals("[{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40},{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}]", out.toString(Charset.defaultCharset()));
	}

	@Test
	public void testDecodeOne() {
		Publisher<ByteBuf> in = Flux.just(
			Unpooled.copiedBuffer("{\"firstname\":\"John\",\"na", Charset.defaultCharset()),
			Unpooled.copiedBuffer("me\":\"Smith\",\"age\":42}", Charset.defaultCharset())
		);
		
		Mono<Person> out = CONVERTER.decodeOne(in, Person.class);
		
		Assertions.assertEquals(new Person("John", "Smith", 42), out.block());
	}

	@Test
	public void testDecodeMany() {
		Publisher<ByteBuf> in = Flux.just(
			Unpooled.copiedBuffer("{\"firstname\":\"John\",\"na", Charset.defaultCharset()),
			Unpooled.copiedBuffer("me\":\"Smith\",\"age\":42}", Charset.defaultCharset()),
			Unpooled.copiedBuffer("{\"firstname\":\"Jane\",", Charset.defaultCharset()),
			Unpooled.copiedBuffer("\"name\":\"Smith\",\"age\":40}{\"fi", Charset.defaultCharset()),
			Unpooled.copiedBuffer("rstname\":\"Junior\",\"name\":", Charset.defaultCharset()),
			Unpooled.copiedBuffer("\"Smith\",\"age\":10}", Charset.defaultCharset())
		);
			
		Flux<Person> out = CONVERTER.decodeMany(in, Person.class);
		
		List<Person> outList = out.collectList().block();
		
		Assertions.assertEquals(3, outList.size());
		
		Assertions.assertEquals(new Person("John", "Smith", 42), outList.get(0));
		Assertions.assertEquals(new Person("Jane", "Smith", 40), outList.get(1));
		Assertions.assertEquals(new Person("Junior", "Smith", 10), outList.get(2));
		
		in = Flux.just(
			Unpooled.copiedBuffer("[{\"firstname\":\"John\",\"na", Charset.defaultCharset()),
			Unpooled.copiedBuffer("me\":\"Smith\",\"age\":42},", Charset.defaultCharset()),
			Unpooled.copiedBuffer("{\"firstname\":\"Jane\",", Charset.defaultCharset()),
			Unpooled.copiedBuffer("\"name\":\"Smith\",\"age\":40},{\"fi", Charset.defaultCharset()),
			Unpooled.copiedBuffer("rstname\":\"Junior\",\"name\":", Charset.defaultCharset()),
			Unpooled.copiedBuffer("\"Smith\",\"age\":10}]", Charset.defaultCharset())
		);
			
		out = CONVERTER.decodeMany(in, Person.class);
		
		outList = out.collectList().block();
		
		Assertions.assertEquals(3, outList.size());
		
		Assertions.assertEquals(new Person("John", "Smith", 42), outList.get(0));
		Assertions.assertEquals(new Person("Jane", "Smith", 40), outList.get(1));
		Assertions.assertEquals(new Person("Junior", "Smith", 10), outList.get(2));
	}

	@Test
	public void testDecode() {
		ByteBuf in = Unpooled.copiedBuffer("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}", Charset.defaultCharset());
		
		Person out = CONVERTER.decode(in, Person.class);
		
		Assertions.assertEquals(new Person("John", "Smith", 42), out);
		
		in = Unpooled.copiedBuffer("{\"firstname\":\"John\"\"name\":\"Smith\",\"age\":42}", Charset.defaultCharset());
		
		try {
			CONVERTER.decode(in, Person.class);
			Assertions.fail("Should throw " + ConverterException.class);
		} 
		catch (ConverterException e) {
		
		}
	}

	@Test
	public void testDecodeToList() {
		ByteBuf in = Unpooled.copiedBuffer("[{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40},{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}]", Charset.defaultCharset());
		
		List<Person> out = CONVERTER.decodeToList(in, Person.class);
		
		Assertions.assertEquals(3, out.size());
		
		Assertions.assertEquals(new Person("John", "Smith", 42), out.get(0));
		Assertions.assertEquals(new Person("Jane", "Smith", 40), out.get(1));
		Assertions.assertEquals(new Person("Junior", "Smith", 10), out.get(2));
		
		in = Unpooled.copiedBuffer("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40}{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}", Charset.defaultCharset());
		
		out = CONVERTER.decodeToList(in, Person.class);
		
		Assertions.assertEquals(3, out.size());
		
		Assertions.assertEquals(new Person("John", "Smith", 42), out.get(0));
		Assertions.assertEquals(new Person("Jane", "Smith", 40), out.get(1));
		Assertions.assertEquals(new Person("Junior", "Smith", 10), out.get(2));
	}

	@Test
	public void testDecodeToSet() {
		ByteBuf in = Unpooled.copiedBuffer("[{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40},{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}]", Charset.defaultCharset());
		
		Set<Person> out = CONVERTER.decodeToSet(in, Person.class);
		
		Assertions.assertEquals(3, out.size());
		Assertions.assertTrue(out.containsAll(List.of(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10))));
		
		in = Unpooled.copiedBuffer("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40}{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}", Charset.defaultCharset());
		
		out = CONVERTER.decodeToSet(in, Person.class);
		
		Assertions.assertEquals(3, out.size());
		Assertions.assertTrue(out.containsAll(List.of(new Person("John", "Smith", 42), new Person("Jane", "Smith", 40), new Person("Junior", "Smith", 10))));
	}

	@Test
	public void testDecodeToArray() {
		ByteBuf in = Unpooled.copiedBuffer("[{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42},{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40},{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}]", Charset.defaultCharset());
		
		Person[] out = CONVERTER.decodeToArray(in, Person.class);
		
		Assertions.assertEquals(3, out.length);
		
		Assertions.assertEquals(new Person("John", "Smith", 42), out[0]);
		Assertions.assertEquals(new Person("Jane", "Smith", 40), out[1]);
		Assertions.assertEquals(new Person("Junior", "Smith", 10), out[2]);
		
		in = Unpooled.copiedBuffer("{\"firstname\":\"John\",\"name\":\"Smith\",\"age\":42}{\"firstname\":\"Jane\",\"name\":\"Smith\",\"age\":40}{\"firstname\":\"Junior\",\"name\":\"Smith\",\"age\":10}", Charset.defaultCharset());
		
		out = CONVERTER.decodeToArray(in, Person.class);
		
		Assertions.assertEquals(3, out.length);
		
		Assertions.assertEquals(new Person("John", "Smith", 42), out[0]);
		Assertions.assertEquals(new Person("Jane", "Smith", 40), out[1]);
		Assertions.assertEquals(new Person("Junior", "Smith", 10), out[2]);
	}
}
