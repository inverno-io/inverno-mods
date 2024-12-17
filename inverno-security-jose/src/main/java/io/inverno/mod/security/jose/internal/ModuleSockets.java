package io.inverno.mod.security.jose.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.internal.converter.GenericDataConversionService;
import io.inverno.mod.security.jose.internal.jwe.GenericJWEService;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwk.JWKFactory;
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
	 * {@link com.fasterxml.jackson.databind.ObjectMapper} used to serialize/deserialize JSON JOSE objects and JSON Web Keys.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	@Overridable
	@Wrapper
	@Bean( name = "objectMapper", visibility = Bean.Visibility.PRIVATE )
	public static class JOSEObjectMapper implements Supplier<ObjectMapper> {

		private com.fasterxml.jackson.databind.ObjectMapper instance;

		@Override
		public ObjectMapper get() {
			if(this.instance == null) {
				this.instance = new ObjectMapper();
				this.instance.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
			}
			return this.instance;
		}
	}

	/**
	 * <p>
	 * JWE compression algorithms socket used to inject JWE compression algorithms when building the JOSE module.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 *
	 * @see GenericJWEService
	 */
	@Bean(name = "jweZips")
	public interface JWEZipsSocket extends Supplier<List<JWEZip>> {}

	/**
	 * <p>
	 * JWK Service extra JWK factories.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	@Bean( name = "jwkFactories")
	public interface JWKFactoriesSocket extends Supplier<List<JWKFactory<?, ?, ?>>> {}

	/**
	 * <p>
	 * Media type converters socket used to inject media type converters when building the JOSE module.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 *
	 * @see GenericDataConversionService
	 */
	@Bean(name = "mediaTypeConverters")
	public interface MediaTypeConvertersSocket extends Supplier<List<MediaTypeConverter<String>>> {}


	/**
	 * <p>
	 * The {@link ResourceService} socket.
	 * </p>
	 *
	 * <p>
	 * A resource service is required to resolve X.509 certificates or JWK Set URL when building or reading JSON Web Keys or JOSE objects.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	@Bean( name = "resourceService" )
	public interface ResourceServiceSocket extends Supplier<ResourceService> {}

	/**
	 * <p>
	 * Worker pool used to execute blocking operations such as certificate verification.
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
