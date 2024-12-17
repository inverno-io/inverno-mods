package io.inverno.mod.http.base.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.http.base.header.HeaderCodec;
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
	 * @see io.inverno.mod.http.base.internal.header.GenericHeaderService GenericHeaderService
	 */
	@Bean( name = "headerCodecs" )
	public interface HeaderCodecsSocket extends Supplier<List<HeaderCodec<?>>> {}

	/**
	 * <p>
	 * Module's parameter value converter socket.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	@Bean( name = "parameterConverter", visibility = Bean.Visibility.PRIVATE )
	@Wrapper
	@Overridable
	public static class ParameterConverter implements Supplier<ObjectConverter<String>> {

		@Override
		public ObjectConverter<String> get() {
			return new StringConverter();
		}
	}
}
