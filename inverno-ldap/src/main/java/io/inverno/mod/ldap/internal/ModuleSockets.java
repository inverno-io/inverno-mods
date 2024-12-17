package io.inverno.mod.ldap.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
	 * Worker pool used to execute Ldap blocking operations.
	 * </p>
	 *
	 * <p>
	 * This Ldap client implementation relies on {@code java.naming} module which exposes blocking operations that must be executed in dedicated threads to protect I/O event loop.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	@Wrapper
	@Overridable
	@Bean( visibility = Bean.Visibility.PRIVATE )
	public static class WorkerPool implements Supplier<ExecutorService> {

		@Override
		public ExecutorService get() {
			return Executors.newCachedThreadPool();
		}
	}
}
