/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.configuration;

import io.winterframework.mod.base.converter.JoinablePrimitiveEncoder;
import io.winterframework.mod.base.converter.SplittablePrimitiveDecoder;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractConfigurableConfigurationSource<A extends ConfigurationQuery<A, B, C>, B extends ExecutableConfigurationQuery<A, B, C>, C extends ConfigurationQueryResult<?,?>, D extends ConfigurationUpdate<D, E, F>, E extends ExecutableConfigurationUpdate<D, E, F>, F extends ConfigurationUpdateResult<?>, G>
		extends AbstractConfigurationSource<A, B, C, G> implements ConfigurableConfigurationSource<A, B, C, D, E, F> {

	protected JoinablePrimitiveEncoder<G> encoder;
	
	public AbstractConfigurableConfigurationSource(JoinablePrimitiveEncoder<G> encoder, SplittablePrimitiveDecoder<G> decoder) {
		super(decoder);
		if(encoder == null) {
			throw new NullPointerException("Value encoder can't be null");
		}
		this.encoder = encoder;
	}
	
	public JoinablePrimitiveEncoder<G> getEncoder() {
		return encoder;
	}

	public void setEncoder(JoinablePrimitiveEncoder<G> encoder) {
		this.encoder = encoder;
	}
}
