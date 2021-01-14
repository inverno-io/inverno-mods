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

import io.winterframework.mod.base.converter.PrimitiveDecoder;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractConfigurationSource<A extends ConfigurationQuery<A, B, C>, B extends ExecutableConfigurationQuery<A, B, C>, C extends ConfigurationQueryResult<?,?>, D> implements ConfigurationSource<A, B, C> {

	protected PrimitiveDecoder<D> decoder;
	
	public AbstractConfigurationSource(PrimitiveDecoder<D> decoder) {
		if(decoder == null) {
			throw new NullPointerException("Value decoder can't be null");
		}
		this.decoder = decoder;
	}
	
	public PrimitiveDecoder<D> getDecoder() {
		return decoder;
	}

	public void setDecoder(PrimitiveDecoder<D> decoder) {
		this.decoder = decoder;
	}
}
