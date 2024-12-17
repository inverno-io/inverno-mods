package io.inverno.mod.http.client.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.internal.header.GenericHeaderService;
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
	 * Header codecs socket.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 *
	 * @see GenericHeaderService
	 */
	@Bean( name = "headerCodecs" )
	public interface HeaderCodecsSocket extends Supplier<List<HeaderCodec<?>>> {}

	/**
	 * <p>
	 * The {@link NetService} socket.
	 * </p>
	 *
	 * <p>
	 * The net service is used when establishing connection to the endpoint.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	@Bean(name = "netService")
	public interface NetServiceSocket extends Supplier<NetService> {}

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
	 * @since 1.6
	 */
	@Overridable @Wrapper @Bean( name = "parameterConverter", visibility = Bean.Visibility.PRIVATE )
	public static class ParameterConverterSocket implements Supplier<ObjectConverter<String>> {

		@Override
		public ObjectConverter<String> get() {
			return new StringConverter();
		}
	}

	/**
	 * <p>
	 * The {@link Reactor} socket.
	 * </p>
	 *
	 * <p>
	 * The reactor is used for connection pooling.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	@Bean(name = "reactor")
	public interface ReactorSocket extends Supplier<Reactor> {}

	/**
	 * <p>
	 * The {@link ResourceService} socket.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.7
	 */
	@Bean(name = "resourceService")
	public interface ResourceServiceSocket extends Supplier<ResourceService> {}
}
