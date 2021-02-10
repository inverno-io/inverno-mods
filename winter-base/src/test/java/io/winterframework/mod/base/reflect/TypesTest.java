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
package io.winterframework.mod.base.reflect;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

/**
 * @author jkuhn
 *
 */
public class TypesTest {

	@Test
	public void testType() {
		Type type = Types.type(List.class)
			.wildcardType()
				.upperBoundType(Map.class)
					.type(String.class).and().type(int.class).and()
				.and()
			.and()
		.build();
		
		Assertions.assertEquals("java.util.List<? extends java.util.Map<java.lang.String, java.lang.Integer>>", type.toString());
		
		try {
			type = Types.type(List.class)
				.type(String.class).and()
				.type(String.class).and()
			.build();
			Assertions.fail("Should throw a " + MalformedParameterizedTypeException.class);
		} 
		catch (MalformedParameterizedTypeException e) {}
		
		type = Types.type(List.class)
			.wildcardType()
				.upperBoundType(String.class).and()
				.upperBoundType(Runnable.class).and()
			.and()
		.build();
		
		Assertions.assertEquals("java.util.List<? extends java.lang.String & java.lang.Runnable>", type.toString());
	}

	@Test
	public void testArray() {
		Type type = Types.arrayType()
			.componentType(int.class)
			.and()
		.build();
		
		Assertions.assertEquals(int[].class, type);
		
		type = Types.arrayType()
			.componentType(List.class)
				.type(String.class).and()
			.and()
		.build();
		
		Assertions.assertEquals("java.util.List<java.lang.String>[]", type.toString());
	}

}
