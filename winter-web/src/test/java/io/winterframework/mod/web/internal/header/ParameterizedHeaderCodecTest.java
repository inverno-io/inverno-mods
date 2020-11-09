package io.winterframework.mod.web.internal.header;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

public class ParameterizedHeaderCodecTest {

	@Test
	public void testAllowEmptyValue() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', true, false, false, false, false);
		
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
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, false, false, false, false);
		
		ParameterizedHeader header = codec.decode("toto", "value;tata=132");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("tata","132"), header.getParameters());
		
		try {
			codec.decode("toto", ";tata=132");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		}
		catch(IllegalArgumentException e) {
			Assertions.assertEquals("Malformed Header: empty value not allowed", e.getMessage());
		}
		
		try {
			codec.decode("toto", "tata=132");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		}
		catch(IllegalArgumentException e) {
			Assertions.assertEquals("Malformed Header: empty value not allowed", e.getMessage());
		}
	}
	
	@Test
	public void testExpectNoValue() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', true, true, false, false, false);
		
		ParameterizedHeader header = codec.decode("toto", "tata=132");
		
		Assertions.assertNull(header.getParameterizedValue());
		Assertions.assertEquals(Map.of("tata","132"), header.getParameters());
		
		try {
			codec.decode("toto", "value;tata=132");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		}
		catch(IllegalArgumentException e) {
			Assertions.assertEquals("Malformed Header: expect no value", e.getMessage());
		}
		
		try {
			codec.decode("toto", ";tata=132");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		}
		catch(IllegalArgumentException e) {
			Assertions.assertEquals("Malformed Header: expect no value", e.getMessage());
		}
		
		try {
			codec.decode("toto", "value");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		}
		catch(IllegalArgumentException e) {
			Assertions.assertEquals("Malformed Header: expect no value", e.getMessage());
		}
		
		
		try {
			new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, true, false, false, false);
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		}
		catch(IllegalArgumentException e) {
			Assertions.assertEquals("Can't expect no value and not allow empty value", e.getMessage());
		}
	}
	
	@Test
	public void testAllowFlagParameter() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, false, true, false, false);
		
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
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, false, false, false, false);
		
		try {
			codec.decode("toto", "value;flag;tata=132;");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		} 
		catch (IllegalArgumentException e) {
			Assertions.assertEquals("Malformed Header: flag parameter not allowed", e.getMessage());
		}
		
		try {
			codec.decode("toto", "value;flag;tata=132");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		} 
		catch (IllegalArgumentException e) {
			Assertions.assertEquals("Malformed Header: flag parameter not allowed", e.getMessage());
		}
		
		try {
			codec.decode("toto", "value;tata=132;flag;");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		} 
		catch (IllegalArgumentException e) {
			Assertions.assertEquals("Malformed Header: flag parameter not allowed", e.getMessage());
		}
		
		try {
			codec.decode("toto", "value;tata=132;flag");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		} 
		catch (IllegalArgumentException e) {
			Assertions.assertEquals("Malformed Header: flag parameter not allowed", e.getMessage());
		}
	}

	@Test
	public void testAllowSpaceInValue() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, false, false, true, false);
		
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
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, false, false, false, false);
		
		try {
			codec.decode("toto", "  value   ;titi=abc;tata=  132  ;toto=1 3 6;tutu= 4 6 9  ;   ");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		} 
		catch (Exception e) {
			Assertions.assertEquals("Malformed Header: space not allowed in value", e.getMessage());
		}
		
		header = codec.decode("toto", "  value   ;titi=abc;tata=  132  ;toto=136;tutu= 469  ");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","132","toto", "136", "tutu", "469"), header.getParameters());
		
		try {
			codec.decode("toto", "  value d  ;titi=abc;tata=  132  ;toto=136;tutu= 469  ");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		} 
		catch (Exception e) {
			Assertions.assertEquals("Malformed Header: space not allowed in value", e.getMessage());
		}
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, false, false, false, true);
		
		header = codec.decode("toto", "  value   ;titi=\"abc\";tata=\"  132  \";toto=\"1 3 6\";tutu=\" 4 6 9  \";   ");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","  132  ","toto", "1 3 6", "tutu", " 4 6 9  "), header.getParameters());
	}
	
	@Test
	public void testAllowQuotedValue() {
		ParameterizedHeaderCodec<ParameterizedHeader, ParameterizedHeader.Builder> codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, false, false, true, true);
		
		ParameterizedHeader header = codec.decode("toto", "  value   ;titi=abc;tata=  \"132\"  ;toto=1 3 6;tutu= \"4 6 9  \"  ");
		
		Assertions.assertEquals("  value   ", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","132","toto", "1 3 6", "tutu", "4 6 9  "), header.getParameters());
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, false, false, false, true);
		
		try {
			codec.decode("toto", "  value   ;titi=abc;tata=  \"132\"  ;toto=1 3 6;tutu= \"4 6 9  \"  ");
			Assertions.fail("Expect " + IllegalArgumentException.class.getName());
		} 
		catch (Exception e) {
			Assertions.assertEquals("Malformed Header: space not allowed in value", e.getMessage());
		}
		
		
		header = codec.decode("toto", "  value   ;titi=abc;tata=  \"132\"  ;toto=136;tutu= \"4 6 9  \"  ");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","132","toto", "136", "tutu", "4 6 9  "), header.getParameters());
		
		codec = new ParameterizedHeaderCodec<>(ParameterizedHeader.Builder::new, Set.of("*"), ';', false, false, false, false, false);
		
		header = codec.decode("toto", "  value   ;titi=abc;tata=  \"132\"  ;toto=136;tutu= \"469\"  ");
		
		Assertions.assertEquals("value", header.getParameterizedValue());
		Assertions.assertEquals(Map.of("titi","abc", "tata","\"132\"","toto", "136", "tutu", "\"469\""), header.getParameters());
	}
}
