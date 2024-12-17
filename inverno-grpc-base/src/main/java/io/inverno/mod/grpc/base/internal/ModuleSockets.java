package io.inverno.mod.grpc.base.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.net.NetService;
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
	 * The {@link NetService} socket.
	 * </p>
	 *
	 * <p>
	 * The net service is used to provide the ByteBuf allocator used for reading and writing gRPC messages.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 */
	@Bean(name = "netService")
	public interface NetServiceSocket extends Supplier<NetService> {}
}
