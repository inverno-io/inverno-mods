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

/**
 * <p>
 * The Inverno framework JOSE security module provides support for JSON Object Signing and Encryption.
 * </p>
 * 
 * <p>It currently implements the following RFCs:</p>
 * <ul>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc7515">RFC7515</a> JSON Web Signature (JWS)</li>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc7516">RFC7516</a> JSON Web Encryption (JWE)</li>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc7517">RFC7517</a> JSON Web Key (JWK)</li>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc7518">RFC7518</a> JSON Web Algorithms (JWA)</li>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC7519</a> JSON Web Token (JWT)</li>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc7638">RFC7638</a> JSON Web Key (JWK) Thumbprint</li>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc7797">RFC7797</a> JSON Web Signature (JWS) Unencoded Payload Option</li>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc8037">RFC8037</a> CFRG Elliptic Curve Diffie-Hellman (ECDH) and Signatures in JSON Object Signing and Encryption (JOSE)</li>
 * <li><a href="https://datatracker.ietf.org/doc/html/rfc8812">RFC8812</a> CBOR Object Signing and Encryption (COSE) and JSON Object Signing and Encryption (JOSE) Registrations for Web Authentication (WebAuthn) Algorithms</li>
 * </ul>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt>configuration</dt>
 * <dd>the JOSE module configuration</dd>
 * <dt>jwkKeyResolver</dt>
 * <dd>A {@code JWKKeyResolver} used to resolve private and public (X.509 certificate) keys from a key store based on Key ids or X.509 thumbprints.</dd>
 * <dt>jwkURLResolver</dt>
 * <dd>A {@code JWKURLResolver} used to resolve JWK Set URLs.</dd>
 * <dt>jwkStore</dt>
 * <dd>A {@code JWKStore} used to store and cache JWKs.</dd>
 * <dt>jwkPKIXParameters</dt>
 * <dd>{@code PKIXParameters} providing the parameters used to validate X.509 certificate paths.</dd>
 * <dt>jwkX509CertPathValidator</dt>
 * <dd>An {@code X509JWKCertPathValidator} used to validate X.509 certificate paths.</dd>
 * <dt>jweZips</dt>
 * <dd>A set of {@code JWEZip} used to compress/decompress JWE payloads.</dd>
 * <dt>mediaTypeConverters (required)</dt>
 * <dd>A list of {@code MediaTypeConverter} used to encode/decode JOSE objects payloads.</dd>
 * <dt>resourceService</dt>
 * <dd>The {@code ResourceService} used to resolve external resources such as key store, JWK Set URL, X.509 URL...</dd>
 * <dt>objectMapper</dt>
 * <dd>The {@code ObjectMapper} used to serialize/deserialize JSON.</dd>
 * <dt>workerPool</dt>
 * <dd>The {@code ExecutorService} used to execute blocking operations.</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt>jwkService</dt>
 * <dd>A {@code JWKService} used to build, resolve, generate, store or load JSON Web Keys.</dd>
 * <dt>jwsService</dt>
 * <dd>A {@code JWSService} used to build and read JSON Web Signatures.</dd>
 * <dt>jweService</dt>
 * <dd>A {@code JWSService} used to build and read JSON Web Encryptions.</dd>
 * <dt>jwtService</dt>
 * <dd>A {@code JWSService} used to build and read JSON Web Tokens.</dd>
 * </dl>
 * 
 * <p>The JOSE module can be started as follows:</p>
 * 
 * <pre>{@code
 * List<MediaTypeConverter> mediaTypeConverters = ... // provided by the Boot module 
 * 
 * Jose jose = new Jose.Builder(MEDIA_TYPE_CONVERTERS).build();
 * jose.start();
 * try {
 *     ...
 * }
 * finally {
 *     jose.stop();
 * }
 * }</pre>
 * 
 * <p>A symmetric key can be generated as follows:</p>
 * 
 * <pre>{@code
 * // Generate a symmetric key
 * Mono<? extends OCTJWK> gen_jwk = jose.jwkService().oct().generator()
 *     .keyId("gen_jwk")
 *     .algorithm(OCTAlgorithm.HS256.getAlgorithm())
 *     .generate()
 *     .cache();
 * }</pre>
 * 
 * <p>A JWS can be created using previously created key as follows:</p>
 * 
 * <pre>{@code
 * // Build a JSON Web Signature using the previous key and containing a String payload
 * JWS<String> builtJWS = jose.jwsService().builder(String.class, gen_jwk)
 *     .header(header -> header
 *         .algorithm(OCTAlgorithm.HS256.getAlgorithm())
 *         .keyId("gen_jwk")
 *         .contentType(MediaTypes.TEXT_PLAIN)
 *     )
 *     .payload("This is the way.")
 *     .build()
 *     .block();
 * }</pre>
 * 
 * <p>A symmetric key can be built from a key value as follows:</p>
 * 
 * <pre>{@code
 * // Build a symmetric key from a key value
 * Mono<? extends OCTJWK> jwk = jose.jwkService().oct().builder()
 *     .keyId("jwk")
 *     .algorithm(OCTAlgorithm.HS256.getAlgorithm())
 *     .keyValue("fQ8SyzEg5_gXIGujsdsI5PJKu39MOwiZHGJ6iNtBjXs")
 *     .build()
 *     .cache();
 * }</pre>
 * 
 * <p>A compact JWS can be read and validated against previous key as follows:</p>
 * 
 * <pre>{@code
 * String compactJWS = "eyJhbGciOiJIUzI1NiIsImN0eSI6InRleHQvcGxhaW4iLCJraWQiOiJnZW5fandrIn0.VGhpcyBpcyB0aGUgd2F5Lg.faPIo0ZkqJyJh1BUsNw5NsXZ18L0z4eq-xEba5k6Os4";
 * 
 * // Read and check a compact JWS signature against the previous key
 * JWS<String> readJWS = jose.jwsService().reader(String.class, jwk)
 *     .read(compactJWS)
 *     .block();
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@io.inverno.core.annotation.Module
module io.inverno.mod.security.jose {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;

	requires transitive com.fasterxml.jackson.annotation;
	requires transitive com.fasterxml.jackson.core; // for javadoc...
	requires transitive com.fasterxml.jackson.databind;
	requires org.apache.commons.lang3;
	requires transitive org.apache.commons.text;
	requires org.apache.logging.log4j;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	
	exports io.inverno.mod.security.jose.internal to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.jose.internal.jwe to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.jose.internal.jwk to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.jose.internal.jwk.ec to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.jose.internal.jwk.rsa to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.jose.internal.jwk.oct to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.jose.internal.jwk.pbes2 to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.jose.internal.jwk.okp to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.jose.internal.jws to com.fasterxml.jackson.databind;
	
	/*
	 * Unfortunately We can't use @JsonAnySetter on a constructor parameter
	 * https://github.com/FasterXML/jackson-databind/issues/562
	 *
	 * This has to go when/if this is supported
	 */
	opens io.inverno.mod.security.jose.jwt to com.fasterxml.jackson.databind;
	
	exports io.inverno.mod.security.jose;
	exports io.inverno.mod.security.jose.jwa;
	exports io.inverno.mod.security.jose.jwe;
	exports io.inverno.mod.security.jose.jwk;
	exports io.inverno.mod.security.jose.jwk.ec;
	exports io.inverno.mod.security.jose.jwk.oct;
	exports io.inverno.mod.security.jose.jwk.okp;
	exports io.inverno.mod.security.jose.jwk.pbes2;
	exports io.inverno.mod.security.jose.jwk.rsa;
	exports io.inverno.mod.security.jose.jws;
	exports io.inverno.mod.security.jose.jwt;
}
