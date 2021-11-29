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
package io.inverno.mod.http.base.header;

import io.inverno.mod.base.Charsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.inverno.mod.http.base.header.Headers.AcceptLanguage;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class AcceptCodecTest {
	
	@Test
	public void test() {
		ByteBuf buffer = Unpooled.wrappedBuffer("*".getBytes(Charsets.UTF_8));
		System.out.println(buffer.maxCapacity());
//		buffer.writeByte(HeaderCodec.LF);
//		System.out.println(buffer.readableBytes());
	}

//	@Test
	public void testAcceptCodec() {
		// application/*;q=1, text/*;q=0.8, */*;q=0.2, audio/basic, audio/*;q=0.6, application/json, text/html
		List<AcceptCodec.Accept.MediaRange> ranges = new ArrayList<>();
		
		ranges.add(new AcceptCodec.Accept.MediaRange("application/*", 1.0f, Map.of()));
		ranges.add(new AcceptCodec.Accept.MediaRange("text/*", 0.8f, Map.of()));
		ranges.add(new AcceptCodec.Accept.MediaRange("*/*", 0.2f, Map.of()));
		ranges.add(new AcceptCodec.Accept.MediaRange("audio/basic", 1.0f, Map.of()));
		ranges.add(new AcceptCodec.Accept.MediaRange("audio/basic", 1.0f, Map.of("toto", "tata")));
		ranges.add(new AcceptCodec.Accept.MediaRange("audio/*", 0.6f, Map.of()));
		ranges.add(new AcceptCodec.Accept.MediaRange("application/json", 1.0f, Map.of()));
		ranges.add(new AcceptCodec.Accept.MediaRange("text/html", 1.0f, Map.of()));
		
		Collections.sort(ranges, AcceptCodec.Accept.MediaRange.COMPARATOR);
		
		/*ranges.forEach(range -> {
			System.out.println(range.getType() + "/" + range.getSubType() + " = " + range.getWeight() + " - " + range.getParameters());
		});*/
		
		Map<AcceptCodec.Accept.MediaRange, String> rangeMap = new LinkedHashMap<>();
		
		rangeMap.put(new AcceptCodec.Accept.MediaRange("text/html", 1.0f, Map.of()), "text/html");
		rangeMap.put(new AcceptCodec.Accept.MediaRange("application/*", 1.0f, Map.of()), "application/*");
		rangeMap.put(new AcceptCodec.Accept.MediaRange("*/json", 1.0f, Map.of()), "*/json");
		rangeMap.put(new AcceptCodec.Accept.MediaRange("text/plain", 1.0f, Map.of()), "text/plain");
		rangeMap.put(new AcceptCodec.Accept.MediaRange("*/*", 1.0f, Map.of()), "*/*");
		rangeMap.put(new AcceptCodec.Accept.MediaRange("application/json", 1.0f, Map.of()), "application/json");
		rangeMap.put(new AcceptCodec.Accept.MediaRange("text/*", 1.0f, Map.of()), "text/*");

		rangeMap = rangeMap.entrySet().stream().sorted(Comparator.comparing(Entry::getKey, AcceptCodec.Accept.MediaRange.COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
		
		AcceptLanguageCodec acceptLanguageCodec = new AcceptLanguageCodec(false);
		
		AcceptLanguage allLanguage = acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, "*");
		AcceptLanguage fr_language = acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, "fr");

		AcceptLanguage fr_FR_language = acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, "fr-FR");
		AcceptLanguage fr_CA_language = acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, "fr-CA");
		
		Assertions.assertTrue(allLanguage.getLanguageRanges().get(0).matches(fr_language.getLanguageRanges().get(0)));
		Assertions.assertTrue(allLanguage.getLanguageRanges().get(0).matches(fr_FR_language.getLanguageRanges().get(0)));
		Assertions.assertTrue(allLanguage.getLanguageRanges().get(0).matches(fr_CA_language.getLanguageRanges().get(0)));
		
		Assertions.assertTrue(fr_language.getLanguageRanges().get(0).matches(fr_FR_language.getLanguageRanges().get(0)));
		Assertions.assertTrue(fr_language.getLanguageRanges().get(0).matches(fr_CA_language.getLanguageRanges().get(0)));
		
		
		AcceptCodec.Accept.MediaRange range_a_b = new AcceptCodec.Accept.MediaRange("a/b", 1.0f, Map.of());
		AcceptCodec.Accept.MediaRange range_a_all = new AcceptCodec.Accept.MediaRange("a/*", 1.0f, Map.of());
		AcceptCodec.Accept.MediaRange range_all_b = new AcceptCodec.Accept.MediaRange("*/b", 1.0f, Map.of());
		AcceptCodec.Accept.MediaRange range_all_all = new AcceptCodec.Accept.MediaRange("*/*", 1.0f, Map.of());
		
		Headers.ContentType content_a_b = new ContentTypeCodec.ContentType("a/b", null, null, Map.of());
		Headers.ContentType content_a_all = new ContentTypeCodec.ContentType("a/*", null, null, Map.of());
		Headers.ContentType content_all_b = new ContentTypeCodec.ContentType("*/b", null, null, Map.of());
		Headers.ContentType content_all_all = new ContentTypeCodec.ContentType("*/*", null, null, Map.of());
		
		Assertions.assertTrue(range_a_b.matches(content_a_b));
		Assertions.assertTrue(range_a_b.matches(content_a_all));
		Assertions.assertTrue(range_a_b.matches(content_all_b));
		Assertions.assertTrue(range_a_b.matches(content_all_all));
		
		Assertions.assertTrue(range_a_all.matches(content_a_b));
		Assertions.assertTrue(range_a_all.matches(content_a_all));
		Assertions.assertTrue(range_a_all.matches(content_all_b));
		Assertions.assertTrue(range_a_all.matches(content_all_all));
		
		Assertions.assertTrue(range_all_b.matches(content_a_b));
		Assertions.assertTrue(range_all_b.matches(content_a_all));
		Assertions.assertTrue(range_all_b.matches(content_all_b));
		Assertions.assertTrue(range_all_b.matches(content_all_all));
		
		Assertions.assertTrue(range_all_all.matches(content_a_b));
		Assertions.assertTrue(range_all_all.matches(content_a_all));
		Assertions.assertTrue(range_all_all.matches(content_all_b));
		Assertions.assertTrue(range_all_all.matches(content_all_all));
		
		
		// TODO
		
		
	}

}
