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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A composite encoder relies on multiple {@link CompoundEncoder} to encode objects.
 * </p>
 * 
 * <p>
 * Such implementation makes it possible to create extensible encoder able to encode various type of objects by composition of many specific compound encoders.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Encoder
 * 
 * @param <To> the encoded type
 */
public class CompositeEncoder<To> implements Encoder<Object, To> {

	private Map<Type, CompoundEncoder<?, To>> encodersCache;
	
	private List<CompoundEncoder<?, To>> encoders;
	
	private Encoder<Object, To> defaultEncoder;
	
	/**
	 * <p>
	 * Creates a composite encoder.
	 * </p>
	 */
	public CompositeEncoder() {
		this.setEncoders(List.of());
	}
	
	/**
	 * <p>
	 * Sets the compound encoders used to encode objects.
	 * </p>
	 * 
	 * @param encoders a list of compound encoders
	 */
	public void setEncoders(List<CompoundEncoder<?, To>> encoders) {
		this.encoders = encoders;
		this.encodersCache = new HashMap<>();
	}
	
	/**
	 * <p>
	 * Sets a default encoder to use when no compound encoder is able to encode an object.
	 * </p>
	 * 
	 * @param defaultEncoder the default decoder
	 */
	public void setDefaultEncoder(Encoder<Object, To> defaultEncoder) {
		this.defaultEncoder = defaultEncoder;
	}

	/**
	 * <p>
	 * Returns the first compound encoder that can encode the specified type.
	 * </p>
	 * 
	 * @param <T>  the type of object encoded by the returned encoder
	 * @param type the type to encode
	 * 
	 * @return a compound encoder
	 *
	 * @throws EncoderNotFoundException if no encoder can encode the specified type
	 */
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
	
	@Override
	public <T> To encode(T value, Type type) throws ConverterException {
		if(value == null) {
			return null;
		}
		try {
			return this.getEncoder(type).encode(value);
		}
		catch(EncoderNotFoundException e) {
			if(this.defaultEncoder != null) {
				return this.defaultEncoder.encode(value);
			}
			throw e;
		}
	}
}
