package io.inverno.mod.http.server.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.internal.header.GenericHeaderService;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ServerController;
import io.netty.buffer.Unpooled;
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
	 * @since 1.0
	 *
	 * @see GenericHeaderService
	 */
	@Bean( name = "headerCodecs" )
	public interface HeaderCodecsSocket extends Supplier<List<HeaderCodec<?>>> {}

	/**
	 * <p>
	 * The controller used by the HTTP server to handle exchanges, error exchanges and create exchange contexts.
	 * </p>
	 *
	 * <p>
	 * By default this returns {@code Hello} when a request is made to the root path {@code /}.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	@Overridable @Wrapper @Bean( name = "controller" )
	public static class HttpServerController implements Supplier<ServerController<? extends ExchangeContext, ? extends Exchange<? extends ExchangeContext>, ? extends ErrorExchange<? extends ExchangeContext>>> {

		@Override
		public ServerController<? extends ExchangeContext, ? extends Exchange<? extends ExchangeContext>, ? extends ErrorExchange<? extends ExchangeContext>> get() {
			return (ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>>)exchange -> {
				if(exchange.request().getPathAbsolute().equalsIgnoreCase("/")) {
					exchange.response().body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello", Charsets.DEFAULT)));
				}
				else {
					throw new NotFoundException();
				}
			};
		}
	}

	/**
	 * <p>
	 * The {@link NetService} socket.
	 * </p>
	 *
	 * <p>
	 * The net service is used when creating the HTTP server.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @see HttpServer
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
	 * @since 1.0
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
	 * The {@link Reactor} socket.
	 * </p>
	 *
	 * <p>
	 * The reactor is used for tracking HTTP connections.
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
	 * @since 1.0
	 */
	@Bean(name = "resourceService")
	public interface ResourceServiceSocket extends Supplier<ResourceService> {}
}
