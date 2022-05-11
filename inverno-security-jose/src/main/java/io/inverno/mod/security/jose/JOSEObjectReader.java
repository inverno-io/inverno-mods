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

import io.inverno.mod.security.jose.jwe.JWEReader;
import io.inverno.mod.security.jose.jws.JWSReader;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JOSE object reader is used to read single JOSE objects serialized in the compact representation.
 * </p>
 * 
 * <p>
 * JOSE objects can be serialized to compact representations as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7515#section-7.1">RFC7515 Section 7.1</a> or
 * <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-7.1">RFC7516 Section 7.1</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see JWSReader
 * @see JWEReader
 * 
 * @param <A> the payload type
 * @param <B> the JOSE header type
 * @param <C> the JOSE object type
 * @param <D> the JOSE object reader type
 */
public interface JOSEObjectReader<A, B extends JOSEHeader, C extends JOSEObject<A, B>, D extends JOSEObjectReader<A, B, C, D>> {
	
	/**
	 * <p>
	 * Specifies the JOSE header custom parameters processed by the application.
	 * </p>
	 * 
	 * <p>
	 * These parameters are expected to be present in the JOSE header, they are not processed by the reader but by the application reading the JOSE object. This enables the reader to check that the
	 * critical parameters set defined in the JOSE header actually contains parameters that are understood and processed by either the reader or the application.
	 * </p>
	 * 
	 * @param parameters a list of parameters
	 * 
	 * @return this reader
	 */
	D processedParameters(String... parameters);
	
	/**
	 * <p>
	 * Reads the specified compact representation and returns the corresponding JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The reader will rely on the payload media type specified in the JOSE header to determine how to deserialize the payload. The operation will fail if it is not able to determine the media type
	 * converter to use. Please consider other read methods {@link #read(java.lang.String, java.lang.String)} and {@link #read(java.lang.String, java.util.function.Function) } to explicitly specifies
	 * how the payload should be deserialized.
	 * </p>
	 *
	 * @param compact a JOSE object compact representation
	 *
	 * @return a single JOSE object publisher
	 *
	 * @throws JOSEObjectReadException  if there was an error reading the JOSE object
	 * @throws JOSEObjectBuildException if there was an error building the JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	default Mono<C> read(String compact) throws JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(compact, (String) null);
	}

	/**
	 * <p>
	 * Reads the specified compact representation using the specified payload media type and returns the corresponding JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The reader will use the specified media type to determine the converter to use to deserialize the payload and ignore the content type specified in the JOSE header. The operation will fail if
	 * there is no media type converters defined for that particular media type. Media types converters are provided when building the JOSE module. Please consider read method
	 * {@link #read(java.lang.String, java.util.function.Function)} to provide a custom payload decoder.
	 * </p>
	 * 
	 * @param compact     a JOSE object compact representation
	 * @param contentType the expected payload media type
	 * 
	 * @return a single JOSE object publisher
	 * 
	 * @throws JOSEObjectReadException  if there was an error reading the JOSE object
	 * @throws JOSEObjectBuildException if there was an error building the JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	Mono<C> read(String compact, String contentType) throws JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException;

	/**
	 * <p>
	 * Reads the specified compact representation using the specified payload decoder and returns the corresponding JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The reader will use the specified payload decoder to deserialize the payload and ignore the content type specified in the JOSE header.
	 * </p>
	 * 
	 * @param compact        a JOSE object compact representation
	 * @param payloadDecoder a payload decoder
	 * 
	 * @return a single JOSE object publisher
	 * 
	 * @throws JOSEObjectReadException  if there was an error reading the JOSE object
	 * @throws JOSEObjectBuildException if there was an error building the JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	Mono<C> read(String compact, Function<String, Mono<A>> payloadDecoder) throws JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException;
}
