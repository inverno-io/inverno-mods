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
package io.inverno.mod.boot.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.inverno.mod.base.Settable;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class InvernoBaseModuleTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder()
		.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
		.addModules(new InvernoBaseModule())
		.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
		.build();

	@Test
	public void testSettableSet() throws JsonProcessingException {
		TestDoc testSet = new TestDoc();
		testSet.setValue("value");

		String testSetJson = MAPPER.writeValueAsString(testSet);
		Assertions.assertEquals("{\"value\":\"value\"}", testSetJson);

		TestDoc testSetRead = MAPPER.readValue(testSetJson, TestDoc.class);

		Assertions.assertEquals(testSet, testSetRead);
	}

	@Test
	public void testSettableSetNull() throws JsonProcessingException {
		TestDoc testSet = new TestDoc();
		testSet.setValue(null);

		String testSetJson = MAPPER.writeValueAsString(testSet);
		Assertions.assertEquals("{\"value\":null}", testSetJson);

		TestDoc testSetRead = MAPPER.readValue(testSetJson, TestDoc.class);

		Assertions.assertTrue(testSetRead.getValue().isSet());
		Assertions.assertEquals(testSet, testSetRead);
	}

	@Test
	public void testSettableUnset() throws JsonProcessingException {
		TestDoc testUnset = new TestDoc();

		String testSetJson = MAPPER.writeValueAsString(testUnset);
		Assertions.assertEquals("{\"value\":null}", testSetJson);

		TestDoc testUnsetRead = MAPPER.readValue(testSetJson, TestDoc.class);

		Assertions.assertTrue(testUnsetRead.getValue().isSet());
		Assertions.assertNotEquals(testUnset, testUnsetRead); // value is set because we included empty settable values during serialization
	}

	@Test
	public void testSettableUnsetNonEmpty() throws JsonProcessingException {
		TestDocNonEmpty testUnset = new TestDocNonEmpty();

		String testSetJson = MAPPER.writeValueAsString(testUnset);
		Assertions.assertEquals("{}", testSetJson);

		TestDocNonEmpty testUnsetRead = MAPPER.readValue(testSetJson, TestDocNonEmpty.class);

		Assertions.assertFalse(testUnsetRead.getValue().isSet());
		Assertions.assertEquals(testUnset, testUnsetRead);
	}

	public static class TestDoc {

		private Settable<String> value = Settable.undefined();

		public Settable<String> getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = Settable.of(value);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestDoc testDoc = (TestDoc) o;
			return Objects.equals(value, testDoc.value);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public static class TestDocNonEmpty {

		private Settable<String> value = Settable.undefined();

		public Settable<String> getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = Settable.of(value);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestDocNonEmpty that = (TestDocNonEmpty) o;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value);
		}
	}
}
