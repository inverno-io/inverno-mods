package io.inverno.mod.redis.lettuce.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.concurrent.Reactor;
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
	 * The reactor is providing event loop group to the Lettuce client.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	@Bean( name = "reactor")
	public interface ReactorSocket extends Supplier<Reactor> {}
}
