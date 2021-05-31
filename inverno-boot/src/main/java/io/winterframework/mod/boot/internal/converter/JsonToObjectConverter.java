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
package io.inverno.mod.boot.internal.converter;

import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.inverno.mod.base.converter.Converter;
import io.inverno.mod.base.converter.ConverterException;

/**
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class JsonToObjectConverter implements Converter<String, Object> {

	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public <T> T decode(String value, Class<T> type) throws ConverterException {
		try {
			return this.mapper.readValue(value, type);
		} 
		catch (JsonProcessingException e) {
			throw new ConverterException(e);
		}
	}

	@Override
	public <T> T decode(String value, Type type) throws ConverterException {
		return null;
	}

	@Override
	public <T> String encode(T value) throws ConverterException {
		try {
			return this.mapper.writeValueAsString(value);
		} 
		catch (JsonProcessingException e) {
			throw new ConverterException(e);
		}
	}

	@Override
	public <T> String encode(T value, Class<T> type) throws ConverterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> String encode(T value, Type type) throws ConverterException {
		// TODO Auto-generated method stub
		return null;
	}

}
