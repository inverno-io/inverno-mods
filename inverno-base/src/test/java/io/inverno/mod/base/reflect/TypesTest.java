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
package io.inverno.mod.base.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class TypesTest {

	@SuppressWarnings("unused")
	private static class TestType<T extends List<String>> {
		
		T f1;
	}
	
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
				.upperBoundType(Comparable.class)
					.type(String.class).and()
				.and()
				.upperBoundType(Runnable.class).and()
			.and()
		.build();
		
		Assertions.assertEquals("java.util.List<? extends java.lang.Comparable<java.lang.String> & java.lang.Runnable>", type.toString());
		
		Assertions.assertEquals("io.inverno.mod.base.reflect.TypesTest$TestType<java.util.ArrayList<java.lang.String>>", Types.type(TestType.class).type(ArrayList.class).type(String.class).and().and().build().toString());

		try {
			Types.type(TestType.class).type(String.class).and().build();
			Assertions.fail("Should throw a " + MalformedParameterizedTypeException.class);
		} 
		catch (MalformedParameterizedTypeException e) {}
		
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
		
		type = Types.arrayType()
			.componentArrayType()
				.componentType(int.class)
				.and()
			.and()
		.build();
		
		Assertions.assertEquals("class [[I", type.toString());
		
		Type stringType = Types.type(String.class).build();
		
		int depth = 4;
		
		ArrayTypeBuilder arrayTypeBuilder = Types.arrayType();
		if(depth == 1) {
			arrayTypeBuilder.componentType(stringType);
		}
		else {
			ArrayTypeArgumentBuilder<?> currentBuilder = arrayTypeBuilder.componentArrayType();
			for(int i=2;i<depth;i++) {
				currentBuilder = currentBuilder.componentArrayType();
			}
			currentBuilder.componentType(stringType);
			for(int i=2;i<depth;i++) {
				currentBuilder = (ArrayTypeArgumentBuilder<?>)currentBuilder.and();
			}
			currentBuilder.and();
		}
		
		Assertions.assertEquals(String[][][][].class, arrayTypeBuilder.build());
	}
	
	
	@Test
	public void testMalformedParameterizedType() {
		try {
			Types.type(Map.class).type(String.class).and().build();
			Assertions.fail("Should throw a MalformedParameterizedTypeException");
		} 
		catch (MalformedParameterizedTypeException e) {
		}
		
		try {
			Types.type(Map.class).type(String.class).and().type(String.class).and().type(String.class).and().build();
			Assertions.fail("Should throw a MalformedParameterizedTypeException");
		} 
		catch (MalformedParameterizedTypeException e) {
		}
	}

	@SuppressWarnings("unused")
	private static class TestMemberOf<A, B> {
		
		String f1;
		A f2;
		B f3;
		List<A> f4;
		List<B> f5;
		List<? extends A> f6;
		List<? extends B> f7;
		List<? super A> f8;
		List<? super B> f9;
		List<? extends Callable<A>> f10;
		List<? extends Callable<B>> f11;
		List<? extends Callable<? super A>> f12;
		List<? extends Callable<? extends B>> f13;
		List<? extends Callable<List<A>>> f14;
		List<? extends Callable<List<B>>> f15;
		List<? extends Callable<List<? super A>>> f16;
		List<? extends Callable<List<? extends B>>> f17;
		List<? extends Callable<List<?>>> f18;
		List<Callable<A>> f19;
		List<Callable<B>> f20;
		A[] f21;
		B[] f22;
		A[][] f23;
		B[][] f24;
		List<A>[] f25;
		List<? extends B>[] f26;
		List<? super A[]>[] f27;
		
		List<? extends Callable<List<? extends B>>> m1() {return null;}
	}
	
	@Test
	public void testAsMemberOf() throws NoSuchMethodException, SecurityException, NoSuchFieldException {
		Class<?> testMemberOfClass = TestMemberOf.class;
		
		Type stringInteger = Types.type(testMemberOfClass).type(String.class).and().type(Integer.class).and().build();
		
		Field f1 = testMemberOfClass.getDeclaredField("f1");
		Assertions.assertEquals(String.class, Types.typeAsMemberOf(stringInteger, f1));
		
		Field f2 = testMemberOfClass.getDeclaredField("f2");
		Assertions.assertEquals(String.class, Types.typeAsMemberOf(stringInteger, f2));
		
		Field f3 = testMemberOfClass.getDeclaredField("f3");
		Assertions.assertEquals(Integer.class, Types.typeAsMemberOf(stringInteger, f3));
		
		Field f4 = testMemberOfClass.getDeclaredField("f4");
		Assertions.assertEquals(Types.type(List.class).type(String.class).and().build(), Types.typeAsMemberOf(stringInteger, f4));
		
		Field f5 = testMemberOfClass.getDeclaredField("f5");
		Assertions.assertEquals(Types.type(List.class).type(Integer.class).and().build(), Types.typeAsMemberOf(stringInteger, f5));
		
		Field f6 = testMemberOfClass.getDeclaredField("f6");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(String.class).and().and().build(), Types.typeAsMemberOf(stringInteger, f6));
		
		Field f7 = testMemberOfClass.getDeclaredField("f7");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Integer.class).and().and().build(), Types.typeAsMemberOf(stringInteger, f7));
		
		Field f8 = testMemberOfClass.getDeclaredField("f8");
		Assertions.assertEquals(Types.type(List.class).wildcardType().lowerBoundType(String.class).and().and().build(), Types.typeAsMemberOf(stringInteger, f8));
		
		Field f9 = testMemberOfClass.getDeclaredField("f9");
		Assertions.assertEquals(Types.type(List.class).wildcardType().lowerBoundType(Integer.class).and().and().build(), Types.typeAsMemberOf(stringInteger, f9));
		
		Field f10 = testMemberOfClass.getDeclaredField("f10");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).type(String.class).and().and().and().build(), Types.typeAsMemberOf(stringInteger, f10));
		
		Field f11 = testMemberOfClass.getDeclaredField("f11");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).type(Integer.class).and().and().and().build(), Types.typeAsMemberOf(stringInteger, f11));
		
		Field f12 = testMemberOfClass.getDeclaredField("f12");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).wildcardType().lowerBoundType(String.class).and().and().and().and().build(), Types.typeAsMemberOf(stringInteger, f12));
		
		Field f13 = testMemberOfClass.getDeclaredField("f13");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).wildcardType().upperBoundType(Integer.class).and().and().and().and().build(), Types.typeAsMemberOf(stringInteger, f13));
		
		Field f14 = testMemberOfClass.getDeclaredField("f14");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).type(List.class).type(String.class).and().and().and().and().build(), Types.typeAsMemberOf(stringInteger, f14));
		
		Field f15 = testMemberOfClass.getDeclaredField("f15");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).type(List.class).type(Integer.class).and().and().and().and().build(), Types.typeAsMemberOf(stringInteger, f15));
		
		Field f16 = testMemberOfClass.getDeclaredField("f16");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).type(List.class).wildcardType().lowerBoundType(String.class).and().and().and().and().and().build(), Types.typeAsMemberOf(stringInteger, f16));
		
		Field f17 = testMemberOfClass.getDeclaredField("f17");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).type(List.class).wildcardType().upperBoundType(Integer.class).and().and().and().and().and().build(), Types.typeAsMemberOf(stringInteger, f17));
		
		Field f18 = testMemberOfClass.getDeclaredField("f18");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).type(List.class).wildcardType().and().and().and().and().build(), Types.typeAsMemberOf(stringInteger, f18));
		
		Field f19 = testMemberOfClass.getDeclaredField("f19");
		Assertions.assertEquals(Types.type(List.class).type(Callable.class).type(String.class).and().and().build(), Types.typeAsMemberOf(stringInteger, f19));
				
		Field f20 = testMemberOfClass.getDeclaredField("f20");
		Assertions.assertEquals(Types.type(List.class).type(Callable.class).type(Integer.class).and().and().build(), Types.typeAsMemberOf(stringInteger, f20));
		
		Field f21 = testMemberOfClass.getDeclaredField("f21");
		Assertions.assertEquals(Types.arrayType().componentType(String.class).and().build(), Types.typeAsMemberOf(stringInteger, f21));
		
		Field f22 = testMemberOfClass.getDeclaredField("f22");
		Assertions.assertEquals(Types.arrayType().componentType(Integer.class).and().build(), Types.typeAsMemberOf(stringInteger, f22));
		
		Field f23 = testMemberOfClass.getDeclaredField("f23");
		Assertions.assertEquals(Types.arrayType().componentArrayType().componentType(String.class).and().and().build(), Types.typeAsMemberOf(stringInteger, f23));
		
		Field f24 = testMemberOfClass.getDeclaredField("f24");
		Assertions.assertEquals(Types.arrayType().componentArrayType().componentType(Integer.class).and().and().build(), Types.typeAsMemberOf(stringInteger, f24));
		
		Field f25 = testMemberOfClass.getDeclaredField("f25");
		Assertions.assertEquals(Types.arrayType().componentType(List.class).type(String.class).and().and().build(), Types.typeAsMemberOf(stringInteger, f25));
		
		Field f26 = testMemberOfClass.getDeclaredField("f26");
		Assertions.assertEquals(Types.arrayType().componentType(List.class).wildcardType().upperBoundType(Integer.class).and().and().and().build(), Types.typeAsMemberOf(stringInteger, f26));

		Field f27 = testMemberOfClass.getDeclaredField("f27");
		Assertions.assertEquals(Types.arrayType().componentType(List.class).wildcardType().lowerBoundArrayType().componentType(String.class).and().and().and().and().build(), Types.typeAsMemberOf(stringInteger, f27));
		
		Method m1 = testMemberOfClass.getDeclaredMethod("m1");
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).type(List.class).wildcardType().upperBoundType(Integer.class).and().and().and().and().and().build(), Types.typeAsMemberOf(stringInteger, m1));
		
		Type wildcardExtendsRunnable = Types.type(testMemberOfClass).wildcardType().and().wildcardType().upperBoundType(Runnable.class).and().and().build();
		
		Assertions.assertEquals("?", Types.typeAsMemberOf(wildcardExtendsRunnable, f2).toString());
		Assertions.assertEquals("? extends java.lang.Runnable", Types.typeAsMemberOf(wildcardExtendsRunnable, f3).toString());
		Assertions.assertEquals(Types.type(List.class).wildcardType().and().build(), Types.typeAsMemberOf(wildcardExtendsRunnable, f8));
		Assertions.assertEquals(Types.type(List.class).wildcardType().and().build(), Types.typeAsMemberOf(wildcardExtendsRunnable, f9));
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).wildcardType().and().and().and().build(), Types.typeAsMemberOf(wildcardExtendsRunnable, f10));
		Assertions.assertEquals(Types.type(List.class).wildcardType().upperBoundType(Callable.class).wildcardType().upperBoundType(Runnable.class).and().and().and().and().build(), Types.typeAsMemberOf(wildcardExtendsRunnable, f11));
		
		// List test
		Method list_get = List.class.getMethod("get", int.class);
		Method list_subList = List.class.getMethod("subList", int.class, int.class);
		
		Type listOfString = Types.type(List.class).type(String.class).and().build();
		Assertions.assertEquals(String.class, Types.typeAsMemberOf(listOfString, list_get));
		Assertions.assertEquals(listOfString, Types.typeAsMemberOf(listOfString, list_subList));
		
		Type listOfInteger = Types.type(List.class).type(Integer.class).and().build();
		Assertions.assertEquals(Integer.class, Types.typeAsMemberOf(listOfInteger, list_get));
		Assertions.assertEquals(listOfInteger, Types.typeAsMemberOf(listOfInteger, list_subList));
		
		Type listOfExtendsInteger = Types.type(List.class).wildcardType().upperBoundType(Integer.class).and().and().build();
		Assertions.assertEquals("? extends java.lang.Integer", Types.typeAsMemberOf(listOfExtendsInteger, list_get).toString());
		Assertions.assertEquals(listOfExtendsInteger, Types.typeAsMemberOf(listOfExtendsInteger, list_subList));
		
		Type listOfSuperInteger = Types.type(List.class).wildcardType().lowerBoundType(Integer.class).and().and().build();
		Assertions.assertEquals("? super java.lang.Integer", Types.typeAsMemberOf(listOfSuperInteger, list_get).toString());
		Assertions.assertEquals(listOfSuperInteger, Types.typeAsMemberOf(listOfSuperInteger, list_subList));
	}
	
	@Test
	public void testToClass() throws NoSuchFieldException, SecurityException {
		Class<?> testMemberOfClass = TestMemberOf.class;
		
		Field f2 = testMemberOfClass.getDeclaredField("f2");
		Field f5 = testMemberOfClass.getDeclaredField("f5");
		Field f6 = testMemberOfClass.getDeclaredField("f6");
		Field f9 = testMemberOfClass.getDeclaredField("f9");
		Field f21 = testMemberOfClass.getDeclaredField("f21");
		Field f24 = testMemberOfClass.getDeclaredField("f24");
		Field f27 = testMemberOfClass.getDeclaredField("f27");
		
		Type StringExtendsRunnable = Types.type(testMemberOfClass).type(String.class).and().wildcardType().upperBoundType(Runnable.class).and().and().build();
		
		Assertions.assertEquals(String.class, Types.toClass(Types.typeAsMemberOf(StringExtendsRunnable, f2)));
		Assertions.assertEquals(List.class, Types.toClass(Types.typeAsMemberOf(StringExtendsRunnable, f5)));
		Assertions.assertEquals(List.class, Types.toClass(Types.typeAsMemberOf(StringExtendsRunnable, f6)));
		Assertions.assertEquals(List.class, Types.toClass(Types.typeAsMemberOf(StringExtendsRunnable, f9)));
		Assertions.assertEquals(String[].class, Types.toClass(Types.typeAsMemberOf(StringExtendsRunnable, f21)));
		Assertions.assertEquals(Runnable[][].class, Types.toClass(Types.typeAsMemberOf(StringExtendsRunnable, f24)));
		Assertions.assertEquals(List[].class, Types.toClass(Types.typeAsMemberOf(StringExtendsRunnable, f27)));
		
		Type wildcardSuperRunnable = Types.type(testMemberOfClass).wildcardType().and().wildcardType().lowerBoundType(Runnable.class).and().and().build();
		
		Assertions.assertEquals(Object.class, Types.toClass(Types.typeAsMemberOf(wildcardSuperRunnable, f2)));
		Assertions.assertEquals(List.class, Types.toClass(Types.typeAsMemberOf(wildcardSuperRunnable, f5)));
		Assertions.assertEquals(List.class, Types.toClass(Types.typeAsMemberOf(wildcardSuperRunnable, f6)));
		Assertions.assertEquals(List.class, Types.toClass(Types.typeAsMemberOf(wildcardSuperRunnable, f9)));
		Assertions.assertEquals(Object[].class, Types.toClass(Types.typeAsMemberOf(wildcardSuperRunnable, f21)));
		Assertions.assertEquals(Object[][].class, Types.toClass(Types.typeAsMemberOf(wildcardSuperRunnable, f24)));
		Assertions.assertEquals(List[].class, Types.toClass(Types.typeAsMemberOf(wildcardSuperRunnable, f27)));
	}
	
	@SuppressWarnings("unused")
	private static class TestOwner<T extends Number> {
		
		public static class Nested1<A> {
			
			A f1;
		}
		
		public class Nested2<A, B extends T> {
			A f1;
			B f2;
			T f3;
			
			<U extends B> U m1() {
				return null;
			}
			
			<U extends T> U m2() {
				return null;
			}
			
			A m3() {
				return null;
			}
			
			B m4() {
				return null;
			}
		}
	}
	
	@Test
	public void testOwner() throws NoSuchFieldException, SecurityException, NoSuchMethodException {
		Type testOwnerNumberType = Types.type(TestOwner.class).type(Number.class).and().build();
		
		try {
			Types.type(TestOwner.Nested1.class).type(String.class).and().ownerType(List.class).and().build();
			Assertions.fail("Should throw a " + MalformedParameterizedTypeException.class);
		} 
		catch (MalformedParameterizedTypeException e) {}
		
		Field f1 = TestOwner.Nested2.class.getDeclaredField("f1");
		Field f2 = TestOwner.Nested2.class.getDeclaredField("f2");
		Field f3 = TestOwner.Nested2.class.getDeclaredField("f3");
		
		Method m1 = TestOwner.Nested2.class.getDeclaredMethod("m1");
		Method m2 = TestOwner.Nested2.class.getDeclaredMethod("m2");
		Method m3 = TestOwner.Nested2.class.getDeclaredMethod("m3");
		Method m4 = TestOwner.Nested2.class.getDeclaredMethod("m4");
		
		Type nested2StringInteger_Number = Types.type(TestOwner.Nested2.class).type(String.class).and().type(Integer.class).and().ownerType(testOwnerNumberType).build();
		
		Assertions.assertEquals(String.class, Types.typeAsMemberOf(nested2StringInteger_Number, f1));
		Assertions.assertEquals(Integer.class, Types.typeAsMemberOf(nested2StringInteger_Number, f2));
		Assertions.assertEquals(Number.class, Types.typeAsMemberOf(nested2StringInteger_Number, f3));
		
		Assertions.assertEquals(Integer.class, Types.typeAsMemberOf(nested2StringInteger_Number, m1));
		Assertions.assertEquals(Number.class, Types.typeAsMemberOf(nested2StringInteger_Number, m2));
		Assertions.assertEquals(String.class, Types.typeAsMemberOf(nested2StringInteger_Number, m3));
		Assertions.assertEquals(Integer.class, Types.typeAsMemberOf(nested2StringInteger_Number, m4));
		
		Type nested2StringInteger_Wild = Types.type(TestOwner.Nested2.class).type(String.class).and().type(Integer.class).and().ownerType(TestOwner.class).wildcardType().and().and().build();
		
		Assertions.assertEquals(String.class, Types.typeAsMemberOf(nested2StringInteger_Wild, f1));
		Assertions.assertEquals(Integer.class, Types.typeAsMemberOf(nested2StringInteger_Wild, f2));
		Assertions.assertEquals(Object.class, Types.toClass(Types.typeAsMemberOf(nested2StringInteger_Wild, f3)));
		
		Assertions.assertEquals(Integer.class, Types.typeAsMemberOf(nested2StringInteger_Wild, m1));
		Assertions.assertEquals(Object.class, Types.toClass(Types.typeAsMemberOf(nested2StringInteger_Wild, m2)));
		Assertions.assertEquals(String.class, Types.typeAsMemberOf(nested2StringInteger_Wild, m3));
		Assertions.assertEquals(Integer.class, Types.typeAsMemberOf(nested2StringInteger_Wild, m4));
	}
	
	@Test
	public void testTypeVariable() {
		Assertions.assertEquals("java.util.List<T>", Types.type(List.class).variableType("T").and().build().toString());
		Assertions.assertEquals("java.util.List<T extends java.lang.Runnable>", Types.type(List.class).variableType("T").boundType(Runnable.class).and().and().build().toString());
		Assertions.assertEquals("java.util.List<T extends java.lang.Runnable & java.lang.Comparable<java.lang.Runnable>>", Types.type(List.class).variableType("T").boundType(Runnable.class).and().boundType(Comparable.class).type(Runnable.class).and().and().and().build().toString());
		
		Assertions.assertEquals("java.util.Map<T, java.lang.String>", Types.type(Map.class).variableType("T").and().type(String.class).and().build().toString());
		
		try {
			Types.type(Map.class).variableType("T").and().variableType("T").and().build();
			Assertions.fail("Should throw a " + MalformedParameterizedTypeException.class);
		} 
		catch (MalformedParameterizedTypeException e) {}
		
		try {
			Types.type(TestType.class).variableType("T").boundType(Runnable.class).and().and().build();
		} 
		catch (MalformedParameterizedTypeException e) {}
	}
}
