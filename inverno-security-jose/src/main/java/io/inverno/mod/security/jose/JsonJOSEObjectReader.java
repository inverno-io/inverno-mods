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

import io.inverno.mod.security.jose.jwe.JsonJWEReader;
import io.inverno.mod.security.jose.jws.JsonJWSReader;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JSON JWE reader is used to read JSON JOSE objects serialized using the JSON representation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see JsonJWSReader
 * @see JsonJWEReader
 * 
 * @param <A> the payload type
 * @param <B> the JSON JOSE object type
 * @param <C> the JSON JOSE object reader type
 */
public interface JsonJOSEObjectReader<A, B extends JsonJOSEObject<A>, C extends JsonJOSEObjectReader<A, B, C>> {
	
	/**
	 * <p>
	 * Specifies the JOSE header custom parameters processed by the application.
	 * </p>
	 * 
	 * <p>
	 * These parameters are expected to be present in JOSE headers, they are not processed by the reader but by the application reading the JSON JOSE object. This enables the reader to check that the
	 * critical parameters set defined in the JOSE headers actually contains parameters that are understood and processed by either the reader or the application.
	 * </p>
	 * 
	 * @param parameters a list of parameters
	 * 
	 * @return this reader
	 */
	C processedParameters(String... parameters);
	
	/**
	 * <p>
	 * Reads the specified JSON representation and returns the corresponding JSON JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The reader will rely on the payload media type specified in the JOSE headers to determine how to deserialize the payload. The operation will fail if it is not able to determine the media type
	 * converter to use. Please consider other read methods {@link #read(java.lang.String, java.lang.String)} and {@link #read(java.lang.String, java.util.function.Function) } to explicitly specifies
	 * how the payload should be deserialized.
	 * </p>
	 *
	 * @param json a JSON JOSE object JSON representation
	 *
	 * @return a single JSON JOSE object publisher
	 *
	 * @throws JOSEObjectReadException  if there was an error reading the JSON JOSE object
	 * @throws JOSEObjectBuildException if there was an error building the JSON JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	default Mono<B> read(String json) throws JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(json, (String) null);
	}

	/**
	 * <p>
	 * Reads the specified JSON representation using the specified payload media type and returns the corresponding JSON JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The reader will use the specified media type to determine the converter to use to deserialize the payload and ignore the content type specified in the JOSE headers. The operation will fail if
	 * there is no media type converters defined for that particular media type. Media types converters are provided when building the JOSE module. Please consider read method
	 * {@link #read(java.lang.String, java.util.function.Function)} to provide a custom payload decoder.
	 * </p>
	 * 
	 * @param json        a JSON JOSE object JSON representation
	 * @param contentType the expected payload media type
	 * 
	 * @return a single JSON JOSE object publisher
	 * 
	 * @throws JOSEObjectReadException  if there was an error reading the JSON JOSE object
	 * @throws JOSEObjectBuildException if there was an error building the JSON JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	Mono<B> read(String json, String contentType) throws JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException;

	/**
	 * <p>
	 * Reads the specified JSON representation using the specified payload decoder and returns the corresponding JSON JOSE object.
	 * </p>
	 * 
	 * <p>
	 * The reader will use the specified payload decoder to deserialize the payload and ignore the content type specified in the JOSE headers.
	 * </p>
	 * 
	 * @param json           a JSON JOSE object JSON representation
	 * @param payloadDecoder a payload decoder
	 * 
	 * @return a single JSON JOSE object publisher
	 * 
	 * @throws JOSEObjectReadException  if there was an error reading the JSON JOSE object
	 * @throws JOSEObjectBuildException if there was an error building the JSON JOSE object
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	Mono<B> read(String json, Function<String, Mono<A>> payloadDecoder) throws JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException;
}
