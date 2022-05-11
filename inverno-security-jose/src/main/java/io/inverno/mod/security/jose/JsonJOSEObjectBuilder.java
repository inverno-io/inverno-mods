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

import io.inverno.mod.security.jose.jwe.JsonJWEBuilder;
import io.inverno.mod.security.jose.jws.JsonJWSBuilder;
import java.util.function.Consumer;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JSON JOSE object builder is used to build JSON JOSE objects that can be serialized to the JSON representation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see JsonJWSBuilder
 * @see JsonJWEBuilder
 * 
 * @param <A> the payload type
 * @param <B> the JSON JOSE object type
 * @param <C> the JOSE header configurator type
 * @param <D> the JSON JOSE object builder type
 */
public interface JsonJOSEObjectBuilder<A, B extends JsonJOSEObject<A>, C extends JOSEHeaderConfigurator<C>, D extends JsonJOSEObjectBuilder<A, B, C, D>> {
	
	/**
	 * <p>
	 * Specifies the common protected and unprotected headers.
	 * </p>
	 * 
	 * <p>
	 * The payload is common and as a result it is recommended to specify the payload content type in the common headers.
	 * </p>
	 * 
	 * @param protectedHeaderConfigurer   the protected JOSE header configurer
	 * @param unprotectedHeaderConfigurer the unprotected JOSE header configurer
	 * 
	 * @return this builder
	 */
	D headers(Consumer<C> protectedHeaderConfigurer, Consumer<C> unprotectedHeaderConfigurer);
	
	/**
	 * <p>
	 * Specifies the payload.
	 * </p>
	 * 
	 * @param payload the payload
	 * 
	 * @return this builder
	 */
	D payload(A payload);
	
	/**
	 * <p>
	 * Builds the JSON JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The builder will rely on the payload media type specified in the JOSE headers to determine how to serialize the payload. The operation will fail if it is not able to determine the media type
	 * converter to use. Please consider other build methods {@link #build(java.lang.String)} and {@link #build(java.util.function.Function)} to explicitly specifies how the payload should be
	 * serialized.
	 * </p>
	 * 
	 * @return a single JSON JOSE object publisher 
	 * 
	 * @throws JOSEObjectBuildException if there was an error building the JSON JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	default Mono<B> build() throws JOSEObjectBuildException, JOSEProcessingException {
		return this.build((String)null);
	}
	
	/**
	 * <p>
	 * Builds the JSON JOSE object using the specified payload media type.
	 * </p>
	 * 
	 * <p>
	 * The builder will use the specified media type to determine the converter to use to serialize the payload and ignore the content type specified in the JOSE headers. The operation will fail if
	 * there is no media type converters defined for that particular media type. Media types converters are provided when building the JOSE module. Please consider build method {@link #build(java.util.function.Function)
	 * } to provide a custom payload encoder.
	 * </p>
	 * 
	 * @param contentType the payload media type
	 * 
	 * @return a single JSON JOSE object publisher 
	 * 
	 * @throws JOSEObjectBuildException if there was an error building the JSON JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	Mono<B> build(String contentType) throws JOSEObjectBuildException, JOSEProcessingException; 
	
	/**
	 * <p>
	 * Builds the JSON JOSE object using the specified payload encoder.
	 * </p>
	 * 
	 * <p>
	 * The builder will use the specified payload encoder to serialize the payload and ignore the content type specified in the JOSE headers.
	 * </p>
	 * 
	 * @param payloadEncoder a payload encoder
	 * 
	 * @return a single JSON JOSE object publisher 
	 * 
	 * @throws JOSEObjectBuildException if there was an error building the JSON JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	Mono<B> build(Function<A, Mono<String>> payloadEncoder) throws JOSEObjectBuildException, JOSEProcessingException;
}
