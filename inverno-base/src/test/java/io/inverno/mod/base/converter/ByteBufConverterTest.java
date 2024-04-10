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
package io.inverno.mod.base.converter;

import io.inverno.mod.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class ByteBufConverterTest {

	private static final ByteBufConverter CONVERTER = new ByteBufConverter(new StringConverter()); 
	
	@Test
	public void testEncode() throws ConverterException, URISyntaxException, MalformedURLException, UnknownHostException {
		ByteBuf buffer = CONVERTER.encode(List.of("a","b","c"));
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encodeList(List.of("a","b","c"));
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));

		buffer = CONVERTER.encode(new LinkedHashSet<>(List.of("a","b","c")));
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encodeSet(new LinkedHashSet<>(List.of("a","b","c")));
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(new String[]{"a","b","c"});
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encodeArray(new String[]{"a","b","c"});
		Assertions.assertEquals("a,b,c", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode((byte)64);
		Assertions.assertEquals("64", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode((short)256);
		Assertions.assertEquals("256", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode((int)1024);
		Assertions.assertEquals("1024", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode((long)2048);
		Assertions.assertEquals("2048", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode((float)12345.5);
		Assertions.assertEquals("12345.5", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode((double)123456789.54321);
		Assertions.assertEquals("1.2345678954321E8", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode('a');
		Assertions.assertEquals("a", buffer.toString(Charsets.DEFAULT));
	
		buffer = CONVERTER.encode(true);
		Assertions.assertEquals("true", buffer.toString(Charsets.DEFAULT));
	
		buffer = CONVERTER.encode("abcdef");
		Assertions.assertEquals("abcdef", buffer.toString(Charsets.DEFAULT));
	
		buffer = CONVERTER.encode(new BigInteger("123457891011121314151617181920212223242526"));
		Assertions.assertEquals("123457891011121314151617181920212223242526", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(new BigDecimal("123457891011121314151617181920212223242526.987654321"));
		Assertions.assertEquals("123457891011121314151617181920212223242526.987654321", buffer.toString(Charsets.DEFAULT));
	
		buffer = CONVERTER.encode(LocalDate.of(2021, 1, 15));
		Assertions.assertEquals("2021-01-15", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(LocalDateTime.of(2021, 1, 15, 18, 0, 52));
		Assertions.assertEquals("2021-01-15T18:00:52", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(ZonedDateTime.of(LocalDate.of(2021, 1, 15), LocalTime.of(18, 0, 52), ZoneId.of("UTC")));
		Assertions.assertEquals("2021-01-15T18:00:52Z[UTC]", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(Currency.getInstance("EUR"));
		Assertions.assertEquals("EUR", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(Locale.FRANCE);
		Assertions.assertEquals("fr_FR", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(new File("/abc.txt"));
		Assertions.assertEquals(File.separator + "abc.txt", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(Path.of("/abc.txt"));
		Assertions.assertEquals(File.separator + "abc.txt", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(new URI("http://127.0.0.1:8080/abc"));
		Assertions.assertEquals("http://127.0.0.1:8080/abc", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(new URI("http://127.0.0.1:8080/abc").toURL());
		Assertions.assertEquals("http://127.0.0.1:8080/abc", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(Pattern.compile("[a-b]+.*"));
		Assertions.assertEquals("[a-b]+.*", buffer.toString(Charsets.DEFAULT));
		
		buffer = CONVERTER.encode(InetAddress.getByName("localhost"));
		Assertions.assertTrue(buffer.toString(Charsets.DEFAULT).matches("localhost|127\\.0\\.0\\.1"));
		
		buffer = CONVERTER.encode(String.class);
		Assertions.assertEquals("java.lang.String", buffer.toString(Charsets.DEFAULT));
	}

	@Test
	public void testByteBufConverter() throws URISyntaxException, MalformedURLException, UnknownHostException {
		Byte byte_value = Byte.valueOf("5");
		Assertions.assertEquals(byte_value, CONVERTER.decode(CONVERTER.encode(byte_value), Byte.class));
		
		Short short_value = Short.valueOf("6");
		Assertions.assertEquals(short_value, CONVERTER.decode(CONVERTER.encode(short_value), Short.class));
		
		Integer integer_value = Integer.valueOf("55656");
		Assertions.assertEquals(integer_value, CONVERTER.decode(CONVERTER.encode(integer_value), Integer.class));
		
		Long long_value = Long.valueOf("456654894984");
		Assertions.assertEquals(long_value, CONVERTER.decode(CONVERTER.encode(long_value), Long.class));
		
		Float float_value = Float.valueOf("52.36");
		Assertions.assertEquals(float_value, CONVERTER.decode(CONVERTER.encode(float_value), Float.class));
		
		Double double_value = Double.valueOf("5654842.36");
		Assertions.assertEquals(double_value, CONVERTER.decode(CONVERTER.encode(double_value), Double.class));
		
		Character character_value = Character.valueOf('\n');
		Assertions.assertEquals(character_value, CONVERTER.decode(CONVERTER.encode(character_value), Character.class));
		
		Boolean boolean_value = Boolean.valueOf(true);
		Assertions.assertEquals(boolean_value, CONVERTER.decode(CONVERTER.encode(boolean_value), Boolean.class));
		
		String string_value = "dslkfdgf\nsfkjdfhd\t\"fdlgf";
		Assertions.assertEquals(string_value, CONVERTER.decode(CONVERTER.encode(string_value), String.class));
		
		BigInteger bigInteger_value = new BigInteger("464984894894894894894651321564794135168798745318964651564894651684841654894651468974651518946168916198764165479797");
		Assertions.assertEquals(bigInteger_value, CONVERTER.decode(CONVERTER.encode(bigInteger_value), BigInteger.class));
		
		BigDecimal bigDecimal_value = new BigDecimal("464984894894894894894651321564794135168798745318964651564894651684841654894651468974651518946168916198764165479797.26554946400000000");
		Assertions.assertEquals(bigDecimal_value, CONVERTER.decode(CONVERTER.encode(bigDecimal_value), BigDecimal.class));
		
		LocalDate localDate_value = LocalDate.now();
		Assertions.assertEquals(localDate_value, CONVERTER.decode(CONVERTER.encode(localDate_value), LocalDate.class));
		
		LocalDateTime localDateTime_value = LocalDateTime.now();
		Assertions.assertEquals(localDateTime_value, CONVERTER.decode(CONVERTER.encode(localDateTime_value), LocalDateTime.class));
		
		ZonedDateTime zonedDateTime_value = ZonedDateTime.now();
		Assertions.assertEquals(zonedDateTime_value, CONVERTER.decode(CONVERTER.encode(zonedDateTime_value), ZonedDateTime.class));
		
		Currency currency_value = Currency.getInstance("GBP");
		Assertions.assertEquals(currency_value, CONVERTER.decode(CONVERTER.encode(currency_value), Currency.class));
		
		Locale locale_value = Locale.getDefault();
		Assertions.assertEquals(locale_value, CONVERTER.decode(CONVERTER.encode(locale_value), Locale.class));
		
		File file_value = new File(".");
		Assertions.assertEquals(file_value, CONVERTER.decode(CONVERTER.encode(file_value), File.class));
		
		Path path_value = Path.of(".");
		Assertions.assertEquals(path_value, CONVERTER.decode(CONVERTER.encode(path_value), Path.class));
		
		URI uri_value = new URI("file:/tmp");
		Assertions.assertEquals(uri_value, CONVERTER.decode(CONVERTER.encode(uri_value), URI.class));
		
		URL url_value = new URL("file:/tmp");
		Assertions.assertEquals(url_value, CONVERTER.decode(CONVERTER.encode(url_value), URL.class));
		
		Pattern pattern_value = Pattern.compile("\\d+\\{.*\\}");
		Assertions.assertEquals(pattern_value.pattern(), CONVERTER.decode(CONVERTER.encode(pattern_value), Pattern.class).pattern());
		
		InetAddress inetAddress_value = InetAddress.getLocalHost();
		Assertions.assertEquals(inetAddress_value, CONVERTER.decode(CONVERTER.encode(inetAddress_value), InetAddress.class));
		
		InetSocketAddress inetSocketAddress_value = new InetSocketAddress("1.2.3.4", 1234);
		Assertions.assertEquals(inetSocketAddress_value, CONVERTER.decode(CONVERTER.encode(inetSocketAddress_value), InetSocketAddress.class));
		
		Class<?> class_value = List.class;
		Assertions.assertEquals(class_value, CONVERTER.decode(CONVERTER.encode(class_value), Class.class));
	}

	@Test
	public void testEncodeOne() {
		Mono<Integer> in = Mono.just(52);
		
		Publisher<ByteBuf> out = CONVERTER.encodeOne(in);
		
		Assertions.assertEquals("52", Mono.from(out).block().toString(Charsets.DEFAULT));
	}
	
	@Test
	public void testEncodeMany() {
		Flux<Integer> in = Flux.just(2,3,5,7,11,13,17);
		
		Publisher<ByteBuf> out = CONVERTER.encodeMany(in);
		
		List<ByteBuf> outList = Flux.from(out).collectList().block();
		
		Assertions.assertEquals(7, outList.size());
		Assertions.assertEquals("2", outList.get(0).toString(Charsets.DEFAULT));
		Assertions.assertEquals("3", outList.get(1).toString(Charsets.DEFAULT));
		Assertions.assertEquals("5", outList.get(2).toString(Charsets.DEFAULT));
		Assertions.assertEquals("7", outList.get(3).toString(Charsets.DEFAULT));
		Assertions.assertEquals("11", outList.get(4).toString(Charsets.DEFAULT));
		Assertions.assertEquals("13", outList.get(5).toString(Charsets.DEFAULT));
		Assertions.assertEquals("17", outList.get(6).toString(Charsets.DEFAULT));
	}
	
	@Test
	public void testDecodeOne() {
		Flux<ByteBuf> in = Flux.just(Unpooled.copiedBuffer("123", Charsets.DEFAULT), Unpooled.copiedBuffer("456", Charsets.DEFAULT), Unpooled.copiedBuffer("789", Charsets.DEFAULT));
		
		Publisher<Integer> out = CONVERTER.decodeOne(in, Integer.class);
		
		Assertions.assertEquals(Integer.valueOf(123456789), Mono.from(out).block());
	}
	
	@Test
	public void testDecodeMany() {
		Flux<ByteBuf> in1 = Flux.just("12,3","4,56,","78,9").map(s -> Unpooled.copiedBuffer(s, Charsets.DEFAULT));
		Publisher<Integer> out1 = CONVERTER.decodeMany(in1, Integer.class);
		Assertions.assertEquals(List.of(12, 34, 56,78, 9), Flux.from(out1).collectList().block());
				
		Flux<ByteBuf> in2 = Flux.just("a,b,","c,","d,").map(s -> Unpooled.copiedBuffer(s, Charsets.DEFAULT));
		Publisher<String> out2 = CONVERTER.decodeMany(in2, String.class);
		Assertions.assertEquals(List.of("a", "b", "c", "d"), Flux.from(out2).collectList().block());
		
		Flux<ByteBuf> in3 = Flux.just("ab","c","d").map(s -> Unpooled.copiedBuffer(s, Charsets.DEFAULT));
		Publisher<String> out3 = CONVERTER.decodeMany(in3, String.class);
		Assertions.assertEquals(List.of("abcd"), Flux.from(out3).collectList().block());
	}
}
