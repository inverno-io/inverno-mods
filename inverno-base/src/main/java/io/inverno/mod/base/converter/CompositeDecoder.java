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
 * A composite decoder relies on multiple {@link CompoundDecoder} to decode objects.
 * </p>
 * 
 * <p>
 * Such implementation makes it possible to create extensible decoder able to decode various type of objects by composition of many specific compound decoders.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <From> the decoded type
 */
public class CompositeDecoder<From> implements Decoder<From, Object> {

	private Map<Type, CompoundDecoder<From, ?>> decodersCache;
	
	private List<CompoundDecoder<From, ?>> decoders;
	
	private Decoder<From, Object> defaultDecoder;
	
	/**
	 * <p>
	 * Creates a composite decoder.
	 * </p>
	 */
	public CompositeDecoder() {
		this.setDecoders(List.of());
	}
	
	/**
	 * <p>
	 * Sets the compound decoders used to decode objects.
	 * </p>
	 * 
	 * @param decoders a list of compound decoders
	 */
	public void setDecoders(List<CompoundDecoder<From, ?>> decoders) {
		this.decoders = decoders;
		this.decodersCache = new HashMap<>();
	}
	
	/**
	 * <p>
	 * Sets a default decoder to use when no compound decoder is able to decode an object.
	 * </p>
	 * 
	 * @param defaultDecoder the default decoder
	 */
	public void setDefaultDecoder(Decoder<From, Object> defaultDecoder) {
		this.defaultDecoder = defaultDecoder;
	}

	/**
	 * <p>
	 * Returns the first compound decoder that can decode the specified type.
	 * </p>
	 * 
	 * @param <T>  the type of object decoded by the returned decoder
	 * @param type the type to decode
	 * 
	 * @return a compound decoder
	 *
	 * @throws DecoderNotFoundException if no decoder can decode the specified type
	 */
	@SuppressWarnings("unchecked")
	protected <T> CompoundDecoder<From, T> getDecoder(Type type) {
		CompoundDecoder<From, T> result = (CompoundDecoder<From, T>) this.decodersCache.get(type);
		if(result == null && !this.decodersCache.containsKey(type)) {
			for(CompoundDecoder<From, ?> decoder : this.decoders) {
				if(decoder.canDecode(type)) {
					this.decodersCache.put(type, decoder);
					result = (CompoundDecoder<From, T>) decoder;
					break;
				}
			}
			if(result == null) {
				this.decodersCache.put(type, null);
			}
		}
		if(result == null) {
			throw new DecoderNotFoundException("No decoder found for type: " + type.getTypeName());
		}
		return result;
	}
	
	@Override
	public <T> T decode(From value, Class<T> type) throws DecoderNotFoundException {
		return this.decode(value, (Type)type);
	}
	
	@Override
	public <T> T decode(From value, Type type) throws ConverterException {
		if(value == null) {
			return null;
		}
		try {
			return this.getDecoder(type).decode(value, type);
		}
		catch(DecoderNotFoundException e) {
			if(this.defaultDecoder != null) {
				return this.defaultDecoder.decode(value, type);
			}
			throw e;
		}
	}
}
