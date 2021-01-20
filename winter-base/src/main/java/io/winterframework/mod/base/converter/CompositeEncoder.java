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
public class CompositeEncoder<To> implements Encoder<Object, To> {

	private Map<Class<?>, CompoundEncoder<?, To>> encoders;
	
	private Encoder<Object, To> defaultEncoder;
	
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
	
	public CompositeEncoder() {
		this.encoders = new HashMap<>();
	}
	
	public void setEncoders(List<CompoundEncoder<?, To>> encoders) {
		this.encoders = new HashMap<>();
		if(encoders != null) {
			for(CompoundEncoder<?, To> encoder : encoders) {
				// TODO at some point this is an issue in Spring as well, we should fix this in winter
				// provide annotation for sorting at compile time and be able to inject maps as well 
				// - annotations defined on the beans with some meta data
				// - annotations defined on multiple bean socket to specify sorting for list, array or sets
				// - we can also group by key to inject a map => new multi socket type
				// - this is a bit tricky as for selector when it comes to the injection of list along with single values 
				CompoundEncoder<?, To> previousEncoder = this.encoders.put(encoder.getEncodedType(), encoder); 
				if(previousEncoder != null) {
					throw new IllegalStateException("Multiple decoders found for type " + encoder.getEncodedType() + ": " + previousEncoder.toString() + ", " + encoder.toString());
				}
			}
		}
	}
	
	public void setDefaultEncoder(Encoder<Object, To> defaultEncoder) {
		this.defaultEncoder = defaultEncoder;
	}

	@SuppressWarnings("unchecked")
	protected <T> CompoundEncoder<T, To> getEncoder(Class<T> type) throws EncoderNotFoundException {
		CompoundEncoder<T, To> result = (CompoundEncoder<T, To>) this.encoders.get(type);
		if(result == null && !this.encoders.containsKey(type)) {
			CompoundEncoder<?, To> encoder = this.encoders.values().stream()
				.filter(e -> e != null && e.getEncodedType().isAssignableFrom(type))
				.sorted(Comparator.comparing(e -> e.getEncodedType(), CLASS_COMPARATOR))
				.findFirst().orElse(null);
			
			this.encoders.put(type, encoder);
			result = (CompoundEncoder<T, To>) encoder;
		}
		if(result == null) {
			throw new EncoderNotFoundException("No encoder found for type: " + type.getCanonicalName());
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> To encode(T data) throws EncoderNotFoundException {
		if(data == null) {
			return null;
		}
		try {
			return ((CompoundEncoder<T, To>)this.getEncoder(data.getClass())).encode(data);
		}
		catch(EncoderNotFoundException e) {
			if(this.defaultEncoder != null) {
				return this.defaultEncoder.encode(data);
			}
			throw e;
		}
	}
}
