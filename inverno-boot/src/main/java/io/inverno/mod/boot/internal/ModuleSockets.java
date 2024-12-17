package io.inverno.mod.boot.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.converter.CompoundDecoder;
import io.inverno.mod.base.converter.CompoundEncoder;
import io.inverno.mod.base.resource.ResourceProvider;
import java.util.List;
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
	 * The compound decoders socket.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @see io.inverno.mod.boot.converter.ParameterConverter ParameterConverter
	 */
	@Bean( name = "compoundDecoders" )
	public interface CompoundDecodersSocket extends Supplier<List<CompoundDecoder<String, ?>>> {}

	/**
	 * <p>
	 * The compound encoders socket.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @see io.inverno.mod.boot.converter.ParameterConverter ParameterConverter
	 */
	@Bean( name = "compoundEncoders" )
	public interface CompoundEncodersSocket extends Supplier<List<CompoundEncoder<?, String>>> {}

	/**
	 * <p>
	 * Resource providers socket.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @see io.inverno.mod.boot.internal.resource.GenericResourceService GenericResourceService
	 */
	@Bean( name = "resourceProviders" )
	public interface ResourceProvidersSocket extends Supplier<ResourceProvider<?>> {

	}

	/**
	 * <p>
	 * General worker pool used whenever there's a need for a {@link ExecutorService}.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	@Bean
	@Wrapper
	@Overridable
	public static class WorkerPool implements Supplier<ExecutorService> {

		@Override
		public ExecutorService get() {
			return Executors.newCachedThreadPool();
		}
	}
}
