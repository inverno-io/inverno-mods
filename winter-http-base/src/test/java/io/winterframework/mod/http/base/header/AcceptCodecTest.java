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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.winterframework.mod.http.base.header.Headers.AcceptLanguage;
import io.winterframework.mod.http.base.internal.header.AcceptCodec;
import io.winterframework.mod.http.base.internal.header.AcceptLanguageCodec;

/**
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class AcceptCodecTest {

	@Test
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
		AcceptLanguage frLanguage = acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, "fr");
		
		// TODO
		
	}

}
