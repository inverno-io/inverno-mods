package io.inverno.mod.grpc.server.internal;

import com.google.protobuf.ExtensionRegistry;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.grpc.base.GrpcMessageCompressor;
import java.util.List;
import java.util.function.Supplier;

/**
 * <p>
 *
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public final class ModuleSockets {

	private ModuleSockets() {}

	/**
	 * <p>
	 * The Protocol buffer {@link ExtensionRegistry} wrapper bean.
	 * </p>
	 *
	 * <p>
	 * An empty registry is provided by default.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 */
	@Bean( name = "extensionRegistry", visibility = Bean.Visibility.PRIVATE )
	@Wrapper
	@Overridable
	public static class ExtensionRegistryWrapper implements Supplier<ExtensionRegistry> {

		@Override
		public ExtensionRegistry get() {
			return ExtensionRegistry.getEmptyRegistry();
		}
	}

	/**
	 * <p>
	 * The gRPC message compressors socket.
	 * </p>
	 *
	 * <p>
	 * This is used to inject custom {@link GrpcMessageCompressor} implementations.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 */
	@Bean( name = "messageCompressors" )
	public interface GrpcMessageCompressorsSocket extends Supplier<List<GrpcMessageCompressor>> {}

	/**
	 * <p>
	 * The {@link NetService} socket.
	 * </p>
	 *
	 * <p>
	 * The net service is used when creating the http server.
	 * </p>
	 *
	 * @author <a href="mailto:mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 */
	@Bean(name = "netService")
	public interface NetServiceSocket extends Supplier<NetService> {}
}
