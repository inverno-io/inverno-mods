/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.security.jose.internal.jwk;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.security.jose.jwk.JWKStore;
import java.util.function.Supplier;

/**
 * <p>
 * JSON Web Key store used to store and load JWK.
 * </p>
 * 
 * <p>
 * This is an overridable wrapper bean which provides a {@link NoOpJWKStore}. It can be overriden by injecting a custom {@link JWKStore} instance when building the JOSE module.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Wrapper
@Overridable
@Bean( name = "jwkStore", visibility = Visibility.PRIVATE )
public class GenericJWKStore implements Supplier<JWKStore> {

	private JWKStore instance;
	
	@Override
	public JWKStore get() {
		if(this.instance == null) {
			this.instance = new NoOpJWKStore();
		}
		return this.instance;
	}
}
