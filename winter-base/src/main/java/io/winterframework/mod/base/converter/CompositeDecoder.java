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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jkuhn
 *
 */
public class CompositeDecoder<From> implements Decoder<From, Object> {

	private Map<Class<?>, CompoundDecoder<From, ?>> decoders;
	
	private Decoder<From, Object> defaultDecoder;
	
	private static final Comparator<Class<?>> CLASS_COMPARATOR = (t1, t2) -> {
		if(t1.isAssignableFrom(t2)) {
			return -1;
		}
		else if(t2.isAssignableFrom(t1)) {
			return 1;
		}
		else {
			return 0;
		}
	};
	
	public CompositeDecoder() {
		this.decoders = new HashMap<>();
	}
	
	public void setDecoders(List<CompoundDecoder<From, ?>> decoders) {
		this.decoders = new HashMap<>();
		if(decoders != null) {
			for(CompoundDecoder<From, ?> decoder : decoders) {
				// TODO at some point this is an issue in Spring as well, we should fix this in winter
				// provide annotation for sorting at compile time and be able to inject maps as well 
				// - annotations defined on the beans with some meta data
				// - annotations defined on multiple bean socket to specify sorting for list, array or sets
				// - we can also group by key to inject a map => new multi socket type
				// - this is a bit tricky as for selector when it comes to the injection of list along with single values 
				CompoundDecoder<From, ?> previousDecoder = this.decoders.put(decoder.getDecodedType(), decoder); 
				if(previousDecoder != null) {
					throw new IllegalStateException("Multiple decoders found for type " + decoder.getDecodedType() + ": " + previousDecoder.toString() + ", " + decoder.toString());
				}
			}
		}
	}
	
	public void setDefaultDecoder(Decoder<From, Object> defaultDecoder) {
		this.defaultDecoder = defaultDecoder;
	}

	@SuppressWarnings("unchecked")
	protected <T> CompoundDecoder<From, T> getDecoder(Class<T> type) {
		CompoundDecoder<From, T> result = (CompoundDecoder<From, T>) this.decoders.get(type);
		if(result == null && !this.decoders.containsKey(type)) {
			CompoundDecoder<From, ?> decoder = this.decoders.values().stream()
				.filter(d -> d != null && d.getDecodedType().isAssignableFrom(type))
				.sorted(Comparator.comparing(d -> d.getDecodedType(), CLASS_COMPARATOR))
				.findFirst().orElse(null);
			
			this.decoders.put(type, decoder);
			result = (CompoundDecoder<From, T>) decoder;
		}
		if(result == null) {
			throw new DecoderNotFoundException("No decoder found for type: " + type.getCanonicalName());
		}
		return result;
	}
	
	@Override
	public <T> T decode(From data, Class<T> type) throws DecoderNotFoundException {
		if(data == null) {
			return null;
		}
		try {
			return this.getDecoder(type).decode(data, type);
		}
		catch(DecoderNotFoundException e) {
			if(this.defaultDecoder != null) {
				return this.defaultDecoder.decode(data, type);
			}
			throw e;
		}
	}
}
