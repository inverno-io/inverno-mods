/*
 * Copyright 2022 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.security.jose;

import io.inverno.mod.security.jose.jwe.JWEBuilder;
import io.inverno.mod.security.jose.jws.JWSBuilder;
import java.util.function.Consumer;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JOSE object builder is used to build single JOSE objects.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see JWSBuilder
 * @see JWEBuilder
 * 
 * @param <A> the payload type
 * @param <B> the JOSE header type
 * @param <C> the JOSE object type
 * @param <D> the JOSE header configurator type
 * @param <E> the JOSE object builder type
 */
public interface JOSEObjectBuilder<A, B extends JOSEHeader, C extends JOSEObject<A, B>, D extends JOSEHeaderConfigurator<D>, E extends JOSEObjectBuilder<A, B, C, D, E>> {

	/**
	 * <p>
	 * Specifies the JOSE header.
	 * </p>
	 * 
	 * @param configurer a JOSE header configurer
	 * 
	 * @return this builder
	 */
	E header(Consumer<D> configurer);
	
	/**
	 * <p>
	 * Specifies the JOSE object payload.
	 * </p>
	 * 
	 * @param payload a payload
	 * 
	 * @return this builder
	 */
	E payload(A payload);
	
	/**
	 * <p>
	 * Builds the JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The builder will rely on the payload media type specified in the JOSE header to determine how to serialize the payload. The operation will fail if it is not able to determine the media type
	 * converter to use. Please consider other build methods {@link #build(java.lang.String)} and {@link #build(java.util.function.Function)} to explicitly specifies how the payload should be
	 * serialized.
	 * </p>
	 * 
	 * @return a single JOSE object publisher 
	 * 
	 * @throws JOSEObjectBuildException if there was an error building the JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	default Mono<C> build() throws JOSEObjectBuildException, JOSEProcessingException {
		return this.build((String)null);
	}
	
	/**
	 * <p>
	 * Builds the JOSE object using the specified payload media type.
	 * </p>
	 * 
	 * <p>
	 * The builder will use the specified media type to determine the converter to use to serialize the payload and ignore the content type specified in the JOSE header. The operation will fail if
	 * there is no media type converters defined for that particular media type. Media types converters are provided when building the JOSE module. Please consider build method {@link #build(java.util.function.Function)
	 * } to provide a custom payload encoder.
	 * </p>
	 * 
	 * @param contentType the payload media type
	 * 
	 * @return a single JOSE object publisher 
	 * 
	 * @throws JOSEObjectBuildException if there was an error building the JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	Mono<C> build(String contentType) throws JOSEObjectBuildException, JOSEProcessingException; 
	
	/**
	 * <p>
	 * Builds the JOSE object using the specified payload encoder.
	 * </p>
	 * 
	 * <p>
	 * The builder will use the specified payload encoder to serialize the payload and ignore the content type specified in the JOSE header.
	 * </p>
	 * 
	 * @param payloadEncoder a payload encoder
	 * 
	 * @return a single JOSE object publisher 
	 * 
	 * @throws JOSEObjectBuildException if there was an error building the JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	Mono<C> build(Function<A, Mono<String>> payloadEncoder) throws JOSEObjectBuildException, JOSEProcessingException;
}
