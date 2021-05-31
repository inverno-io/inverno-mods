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

import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class StringCompositeConverterTest {

	public static class Message {
		
		private String message;

		public Message(String message) {
			this.message = message;
		}
		
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
	
	public static class MessageDecoder implements CompoundDecoder<String, Message> {

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Message> T decode(String value, Class<T> type) throws ConverterException {
			return (T) new Message(value);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Message> T decode(String value, Type type) throws ConverterException {
			return (T) new Message(value);
		}

		@Override
		public <T extends Message> boolean canDecode(Class<T> type) {
			return Message.class.equals(type);
		}

		@Override
		public boolean canDecode(Type type) {
			return Message.class.equals(type);
		}
	}
	
	public static class MessageEncoder implements CompoundEncoder<Message, String> {

		@Override
		public <T extends Message> String encode(T value) throws ConverterException {
			return value.getMessage();
		}

		@Override
		public <T extends Message> String encode(T value, Class<T> type) throws ConverterException {
			return value.getMessage();
		}

		@Override
		public <T extends Message> String encode(T value, Type type) throws ConverterException {
			return value.getMessage();
		}

		@Override
		public <T extends Message> boolean canEncode(Class<T> type) {
			return Message.class.equals(type);
		}

		@Override
		public boolean canEncode(Type type) {
			return Message.class.equals(type);
		}
	}
	
	@Test
	public void testStringCompositeConverter() {
		StringCompositeConverter converter = new StringCompositeConverter();
		converter.setDecoders(List.of(new MessageDecoder()));
		converter.setEncoders(List.of(new MessageEncoder()));
		
		Message decodedMessage = converter.decode("this is an encoded message", Message.class);
		Assertions.assertEquals("this is an encoded message", decodedMessage.getMessage());
		
		String encodedMessage = converter.encode(new Message("this is a decoded message"));
		Assertions.assertEquals("this is a decoded message", encodedMessage);
	}

}
