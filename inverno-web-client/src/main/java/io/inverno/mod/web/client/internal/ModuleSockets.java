/*
 * Copyright 2024 Jeremy KUHN
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
package io.inverno.mod.web.client.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.http.client.HttpClient;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Supplier;

/**
 * <p>
 * Defines the sockets bean exposed by the module.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public final class ModuleSockets {

	private ModuleSockets() {}

	/**
	 * <p>
	 * The HTTP discovery services socket bean.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@Bean( name = "discoveryServices" )
	public interface DiscoveryServicesSocket extends Supplier<List<? extends HttpDiscoveryService>> {}

	/**
	 * <p>
	 * The {@link HttpClient} socket bean.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@Bean( name = "httpClient" )
	public interface HttpClientSocket extends Supplier<HttpClient> {}

	/**
	 * <p>
	 * Media type converters socket bean.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @see io.inverno.mod.web.base.DataConversionService
	 */
	@Bean( name = "mediaTypeConverters" )
	public interface MediaTypeConvertersSocket extends Supplier<List<MediaTypeConverter<ByteBuf>>> {}

	/**
	 * <p>
	 * The parameter value converter using {@link StringConverter} by default.
	 * </p>
	 *
	 * <p>
	 * The parameter value converter is used everywhere there's a need to convert parameters (ie. query parameters, cookies, headers...).
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@Overridable @Wrapper @Bean( name = "parameterConverter", visibility = Bean.Visibility.PRIVATE )
	public static class ParameterConverter implements Supplier<ObjectConverter<String>> {

		@Override
		public ObjectConverter<String> get() {
			return new StringConverter();
		}
	}

	/**
	 * <p>
	 * The {@link Reactor} socket bean.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@Bean( name = "reactor" )
	public interface ReactorSocket extends Supplier<Reactor> {}
}
