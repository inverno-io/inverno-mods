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
package io.inverno.mod.boot.converter;

import java.util.List;
import java.util.function.Supplier;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.CompositeConverter;
import io.inverno.mod.base.converter.CompoundDecoder;
import io.inverno.mod.base.converter.CompoundEncoder;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.converter.StringCompositeConverter;

/**
 * <p>
 * String to object converter used basically to convert string parameter values into primitive and common types.
 * </p>
 *
 * <p>
 * This converter implements the {@link CompositeConverter} interface and as such it is possible to extend its capabilities by injecting specific {@link CompoundDecoder} and {@link CompoundEncoder}
 * instances.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ObjectConverter
 * @see CompositeConverter
 */
@Bean( name = "parameterConverter" )
public class ParameterConverter extends StringCompositeConverter implements @Provide ObjectConverter<String>  {
	
	@Override
	public void setDecoders(List<CompoundDecoder<String, ?>> decoders) {
		super.setDecoders(decoders);
	}
	
	@Override
	public void setEncoders(List<CompoundEncoder<?, String>> encoders) {
		super.setEncoders(encoders);
	}
	
	/**
	 * <p>
	 * The compound decoders socket.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ParameterConverter
	 */
	@Bean( name = "compoundDecoders" )
	public static interface CompoundDecodersSocket extends Supplier<List<CompoundDecoder<String, ?>>> {}
	
	/**
	 * <p>
	 * The compound encoders socket.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ParameterConverter
	 */
	@Bean( name = "compoundEncoders" )
	public static interface CompoundEncodersSocket extends Supplier<List<CompoundEncoder<?, String>>> {}
}

