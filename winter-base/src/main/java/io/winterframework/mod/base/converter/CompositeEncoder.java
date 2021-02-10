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
package io.winterframework.mod.base.converter;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jkuhn
 *
 */
public class CompositeEncoder<To> implements Encoder<Object, To> {

	private Map<Type, CompoundEncoder<?, To>> encodersCache;
	
	private List<CompoundEncoder<?, To>> encoders;
	
	private Encoder<Object, To> defaultEncoder;
	
	public CompositeEncoder() {
		this.setEncoders(List.of());
	}
	
	public void setEncoders(List<CompoundEncoder<?, To>> encoders) {
		this.encoders = encoders;
		this.encodersCache = new HashMap<>();
	}
	
	public void setDefaultEncoder(Encoder<Object, To> defaultEncoder) {
		this.defaultEncoder = defaultEncoder;
	}

	@SuppressWarnings("unchecked")
	protected <T> CompoundEncoder<T, To> getEncoder(Type type) throws EncoderNotFoundException {
		CompoundEncoder<T, To> result = (CompoundEncoder<T, To>) this.encodersCache.get(type);
		if(result == null && !this.encodersCache.containsKey(type)) {
			for(CompoundEncoder<?, To> encoder : this.encoders) {
				if(encoder.canEncode(type)) {
					this.encodersCache.put(type, encoder);
					result = (CompoundEncoder<T, To>) encoder;
					break;
				}
			}
			if(result == null) {
				this.encodersCache.put(type, null);
			}
		}
		if(result == null) {
			throw new EncoderNotFoundException("No encoder found for type: " + type.getTypeName());
		}
		return result;
	}

	@Override
	public <T> To encode(T value) throws EncoderNotFoundException {
		return this.encode(value, (Type)value.getClass());
	}
	
	@Override
	public <T> To encode(T value, Class<T> type) throws ConverterException {
		return this.encode(value, (Type)type);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> To encode(T value, Type type) throws ConverterException {
		if(value == null) {
			return null;
		}
		try {
			return ((CompoundEncoder<T, To>)this.getEncoder(type)).encode(value);
		}
		catch(EncoderNotFoundException e) {
			if(this.defaultEncoder != null) {
				return this.defaultEncoder.encode(value);
			}
			throw e;
		}
	}
}
