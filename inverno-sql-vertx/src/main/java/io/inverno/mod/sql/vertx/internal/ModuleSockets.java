package io.inverno.mod.sql.vertx.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.concurrent.VertxReactor;
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
	 * The {@link Reactor} socket
	 * </p>
	 *
	 * <p>
	 * The reactor is required to access the underlying Vert.x instance when the reactor is a {@link VertxReactor}. This instance is then used to create Vert.x SQL clients and pools.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	@Bean( name = "reactor")
	public interface ReactorSocket extends Supplier<Reactor> {}
}
