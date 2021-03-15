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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

import io.winterframework.mod.http.base.internal.header.MalformedHeaderException;
import io.winterframework.mod.http.base.internal.header.MultiParameterizedHeader;
import io.winterframework.mod.http.base.internal.header.ParameterizedHeader;
import io.winterframework.mod.http.base.internal.header.ParameterizedHeaderCodec;

/**
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class ParameterizedHeaderCodecTest {

	@Test
	public void testAllowEmptyValue() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', true, false, false, false, false, false);
		
		ParameterizedHeader header = codec.decode("toto", "value;tata=132");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("tata","132"), header.getParameters());
		
		header = codec.decode("toto", ";tata=132");

		Assertions.assertEquals("", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("tata","132"), header.getParameters());
		
		header = codec.decode("toto", "tata=132");
		
		Assertions.assertNull(header.getParameterizedValue());
		Assertions.assertEquals(Map.of("tata","132"), header.getParameters());
	}
	
	@Test
	public void testNotAllowEmptyValue() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, false, false, false, false);
		
		ParameterizedHeader header = codec.decode("toto", "value;tata=132");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("tata","132"), header.getParameters());
		
		try {
			codec.decode("toto", ";tata=132");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		}
		catch(MalformedHeaderException e) {
			Assertions.assertEquals("toto: empty value not allowed", e.getMessage());
		}
		
		try {
			codec.decode("toto", "tata=132");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		}
		catch(MalformedHeaderException e) {
			Assertions.assertEquals("toto: empty value not allowed", e.getMessage());
		}
	}
	
	@Test
	public void testExpectNoValue() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', true, true, false, false, false, false);
		
		ParameterizedHeader header = codec.decode("toto", "tata=132");
		
		Assertions.assertNull(header.getParameterizedValue());
		Assertions.assertEquals(Map.of("tata","132"), header.getParameters());
		
		try {
			codec.decode("toto", "value;tata=132");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		}
		catch(MalformedHeaderException e) {
			Assertions.assertEquals("toto: expect no value", e.getMessage());
		}
		
		try {
			codec.decode("toto", ";tata=132");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		}
		catch(MalformedHeaderException e) {
			Assertions.assertEquals("toto: expect no value", e.getMessage());
		}
		
		try {
			codec.decode("toto", "value");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		}
		catch(MalformedHeaderException e) {
			Assertions.assertEquals("toto: expect no value", e.getMessage());
		}
		
		try {
			new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, true, false, false, false, false);
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		}
		catch(IllegalArgumentException e) {
			Assertions.assertEquals("Can't expect no value and not allow empty value", e.getMessage());
		}
	}
	
	@Test
	public void testAllowFlagParameter() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, true, false, false, false);
		
		ParameterizedHeader header = codec.decode("toto", "value;flag;tata=132;");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(2, header.getParameters().size());
		Assertions.assertEquals("132", header.getParameters().get("tata"));
		Assertions.assertNull(header.getParameters().get("flag"));
		
		header = codec.decode("toto", "value;flag;tata=132");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(2, header.getParameters().size());
		Assertions.assertEquals("132", header.getParameters().get("tata"));
		Assertions.assertNull(header.getParameters().get("flag"));
		
		header = codec.decode("toto", "value;tata=132;flag;");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(2, header.getParameters().size());
		Assertions.assertEquals("132", header.getParameters().get("tata"));
		Assertions.assertNull(header.getParameters().get("flag"));
		
		header = codec.decode("toto", "value;tata=132;flag");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(2, header.getParameters().size());
		Assertions.assertEquals("132", header.getParameters().get("tata"));
		Assertions.assertNull(header.getParameters().get("flag"));
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, false, false, false, false);
		
		try {
			codec.decode("toto", "value;flag;tata=132;");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		} 
		catch (MalformedHeaderException e) {
			Assertions.assertEquals("toto: flag parameters not allowed", e.getMessage());
		}
		
		try {
			codec.decode("toto", "value;flag;tata=132");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		} 
		catch (MalformedHeaderException e) {
			Assertions.assertEquals("toto: flag parameters not allowed", e.getMessage());
		}
		
		try {
			codec.decode("toto", "value;tata=132;flag;");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		} 
		catch (MalformedHeaderException e) {
			Assertions.assertEquals("toto: flag parameters not allowed", e.getMessage());
		}
		
		try {
			codec.decode("toto", "value;tata=132;flag");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		} 
		catch (MalformedHeaderException e) {
			Assertions.assertEquals("toto: flag parameters not allowed", e.getMessage());
		}
	}

	@Test
	public void testAllowSpaceInValue() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, false, true, false, false);
		
		ParameterizedHeader header = codec.decode("toto", "  value   ;titi=abc;tata=  132  ;toto=1 3 6;tutu= 4 6 9  ");
		
		Assertions.assertEquals("  value   ", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","  132  ","toto", "1 3 6", "tutu", " 4 6 9  "), header.getParameters());
		
		header = codec.decode("toto", "  value   ;titi=abc;tata=  132  ;toto=1 3 6;tutu= 4 6 9  ;    ");
		
		Assertions.assertEquals("  value   ", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","  132  ","toto", "1 3 6", "tutu", " 4 6 9  "), header.getParameters());
		
		header = codec.decode("toto", "  value   ;titi=abc;tata=  132  ;toto=1 3 6;tutu= 4 6 9  ");
		
		Assertions.assertEquals("  value   ", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","  132  ","toto", "1 3 6", "tutu", " 4 6 9  "), header.getParameters());
		
		header = codec.decode("toto", "  value  d ");
		
		Assertions.assertEquals("  value  d ", header.getParameterizedValue());
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, false, false, false, false);
		
		try {
			codec.decode("toto", "  value   ;titi=abc;tata=  132  ;toto=1 3 6;tutu= 4 6 9  ;   ");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		} 
		catch (MalformedHeaderException e) {
			Assertions.assertEquals("toto: space not allowed in value", e.getMessage());
		}
		
		header = codec.decode("toto", "  value   ;titi=abc;tata=  132  ;toto=136;tutu= 469  ");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","132","toto", "136", "tutu", "469"), header.getParameters());
		
		try {
			codec.decode("toto", "  value d  ;titi=abc;tata=  132  ;toto=136;tutu= 469  ");
			Assertions.fail("Expect " + MalformedHeaderException.class.getName());
		} 
		catch (MalformedHeaderException e) {
			Assertions.assertEquals("toto: space not allowed in value", e.getMessage());
		}
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, false, false, true, false);
		
		header = codec.decode("toto", "  value   ;titi=\"abc\";tata=\"  132  \";toto=\"1 3 6\";tutu=\" 4 6 9  \";   ");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","  132  ","toto", "1 3 6", "tutu", " 4 6 9  "), header.getParameters());
	}
	
	@Test
	public void testAllowQuotedValue() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, false, true, true, false);
		
		ParameterizedHeader header = codec.decode("toto", "  value   ;titi=abc;tata=  \"132\"  ;toto=1 3 6;tutu= \"4 6 9  \"  ");
		
		Assertions.assertEquals("  value   ", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","132","toto", "1 3 6", "tutu", "4 6 9  "), header.getParameters());
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, false, false, true, false);
		
		try {
			codec.decode("toto", "  value   ;titi=abc;tata=  \"132\"  ;toto=1 3 6;tutu= \"4 6 9  \"  ");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		} 
		catch (Exception e) {
			Assertions.assertEquals("toto: space not allowed in value", e.getMessage());
		}
		
		header = codec.decode("toto", "  value   ;titi=abc;tata=  \"132\"  ;toto=136;tutu= \"4 6 9  \"  ");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","132","toto", "136", "tutu", "4 6 9  "), header.getParameters());
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, false, false, false, false);
		
		header = codec.decode("toto", "  value   ;titi=abc;tata=  \"132\"  ;toto=136;tutu= \"469\"  ");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","\"132\"","toto", "136", "tutu", "\"469\""), header.getParameters());
	}
	
	@Test
	public void testAllowMultiple() {
		ParameterizedHeaderCodec<MultiParameterizedHeader, MultiParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(MultiParameterizedHeader.Builder::new, Set.of("*"), ';', ',', false, false, false, false, false, true);
		
		// TODO deal with empty parameter ;param=;
		
		MultiParameterizedHeader header = codec.decode("Accept", "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
		
		Assertions.assertEquals(4, header.getHeaders().size());
		
		Iterator<ParameterizedHeader> headersIterator = header.getHeaders().iterator();
		
		ParameterizedHeader singleHeader = headersIterator.next();
		Assertions.assertEquals("text/plain", singleHeader.getParameterizedValue());
		Assertions.assertEquals(Map.of("q","0.5"), singleHeader.getParameters());
		
		singleHeader = headersIterator.next();
		Assertions.assertEquals("text/html", singleHeader.getParameterizedValue());
		Assertions.assertEquals(Map.of(), singleHeader.getParameters());
		
		singleHeader = headersIterator.next();
		Assertions.assertEquals("text/x-dvi", singleHeader.getParameterizedValue());
		Assertions.assertEquals(Map.of("q", "0.8"), singleHeader.getParameters());
		
		singleHeader = headersIterator.next();
		Assertions.assertEquals("text/x-c", singleHeader.getParameterizedValue());
		Assertions.assertEquals(Map.of(), singleHeader.getParameters());
	}
}
