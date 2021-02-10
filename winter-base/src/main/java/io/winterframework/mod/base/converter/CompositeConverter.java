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
import java.util.List;

/**
 * @author jkuhn
 *
 */
public class CompositeConverter<A> implements Converter<A, Object> {

	private CompositeDecoder<A> decoder;
	
	private CompositeEncoder<A> encoder;
	
	public CompositeConverter() {
		this.decoder = new CompositeDecoder<>();
		this.encoder = new CompositeEncoder<>();
	}

	public void setDecoders(List<CompoundDecoder<A, ?>> decoders) {
		this.decoder.setDecoders(decoders);
	}
	
	public void setEncoders(List<CompoundEncoder<?, A>> encoders) {
		this.encoder.setEncoders(encoders);
	}
	
	public void setDefaultDecoder(Decoder<A, Object> defaultDecoder) {
		this.decoder.setDefaultDecoder(defaultDecoder);
	}

	public void setDefaultEncoder(Encoder<Object, A> defaultEncoder) {
		this.encoder.setDefaultEncoder(defaultEncoder);
	}
	
	@Override
	public <T> T decode(A value, Class<T> type) {
		return this.decoder.decode(value, type);
	}
	
	@Override
	public <T> T decode(A value, Type type) throws ConverterException {
		return this.decoder.decode(value, type);
	}

	@Override
	public <T> A encode(T value) {
		return this.encoder.encode(value);
	}
	
	@Override
	public <T> A encode(T value, Class<T> type) throws ConverterException {
		return this.encoder.encode(value, type);
	}
	
	@Override
	public <T> A encode(T value, Type type) throws ConverterException {
		return this.encoder.encode(value, type);
	}
}
