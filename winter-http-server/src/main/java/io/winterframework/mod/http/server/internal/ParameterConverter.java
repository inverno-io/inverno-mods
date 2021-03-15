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
package io.winterframework.mod.http.server.internal;

import java.util.function.Supplier;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Overridable;
import io.winterframework.core.annotation.Wrapper;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.base.converter.StringConverter;

/**
 * <p>
 * The parameter value converter using {@link StringConverter} by default.
 * </p>
 * 
 * <p>
 * The parameter value converter is used everywhere there's a need to convert
 * parameters (ie. query parameters, cookies, headers...).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean( name = "parameterConverter", visibility = Visibility.PRIVATE )
@Wrapper
@Overridable
public class ParameterConverter implements Supplier<ObjectConverter<String>> {

	@Override
	public ObjectConverter<String> get() {
		return new StringConverter();
	}
}
