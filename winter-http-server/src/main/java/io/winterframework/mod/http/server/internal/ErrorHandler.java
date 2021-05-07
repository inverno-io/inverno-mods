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
import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.http.server.ExchangeHandler;

/**
 * <p>
 * The Server error exchange handler using {@link GenericErrorHandler} by default.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see GenericErrorHandler
 */
@Bean
@Wrapper
@Overridable 
public class ErrorHandler implements Supplier<ExchangeHandler<ErrorExchange<Throwable>>> {

	@Override
	public ExchangeHandler<ErrorExchange<Throwable>> get() {
		return new GenericErrorHandler();
	}
}