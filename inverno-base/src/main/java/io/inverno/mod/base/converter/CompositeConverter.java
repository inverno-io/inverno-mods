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

/**
 * <p>
 * A composite converter relies on multiple {@link CompoundEncoder} and {@link CompoundDecoder} to convert objects.
 * </p>
 * 
 * <p>
 * Such implementation makes it possible to create extensible converter able to convert various type of objects by composition of many specific compound encoders and decoders.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Converter
 * 
 * @param <A> the converted type
 */
public class CompositeConverter<A> implements Converter<A, Object> {

	private final CompositeDecoder<A> decoder;
	
	private final CompositeEncoder<A> encoder;
	
	/**
	 * <p>
	 * Creates a composite converter.
	 * </p>
	 */
	public CompositeConverter() {
		this.decoder = new CompositeDecoder<>();
		this.encoder = new CompositeEncoder<>();
	}

	/**
	 * <p>
	 * Sets the compound decoders used to decode objects.
	 * </p>
	 * 
	 * @param decoders a list of compound decoders
	 */
	public void setDecoders(List<CompoundDecoder<A, ?>> decoders) {
		this.decoder.setDecoders(decoders);
	}

	/**
	 * <p>
	 * Sets the compound encoders used to encode objects.
	 * </p>
	 * 
	 * @param encoders a list of compound encoders
	 */
	public void setEncoders(List<CompoundEncoder<?, A>> encoders) {
		this.encoder.setEncoders(encoders);
	}
	
	/**
	 * <p>
	 * Sets a default decoder to use when no compound decoder is able to decode an object.
	 * </p>
	 * 
	 * @param defaultDecoder the default decoder
	 */
	public void setDefaultDecoder(Decoder<A, Object> defaultDecoder) {
		this.decoder.setDefaultDecoder(defaultDecoder);
	}

	/**
	 * <p>
	 * Sets a default encoder to use when no compound encoder is able to encode an object.
	 * </p>
	 * 
	 * @param defaultEncoder the default decoder
	 */
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
