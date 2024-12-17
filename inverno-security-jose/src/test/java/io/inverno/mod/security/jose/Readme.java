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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.reflect.Types;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.security.jose.jwa.DirectJWAKeyManager;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwa.EdECAlgorithm;
import io.inverno.mod.security.jose.jwa.EncryptingJWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWASigner;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwa.PBES2Algorithm;
import io.inverno.mod.security.jose.jwa.RSAAlgorithm;
import io.inverno.mod.security.jose.jwa.WrappingJWAKeyManager;
import io.inverno.mod.security.jose.jwa.XECAlgorithm;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEService;
import io.inverno.mod.security.jose.jwe.JsonJWE;
import io.inverno.mod.security.jose.jwe.JsonJWE.BuiltRecipient;
import io.inverno.mod.security.jose.jwe.JsonJWE.ReadRecipient;
import io.inverno.mod.security.jose.jwk.InMemoryJWKStore;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.JWKSet;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.jose.jws.JsonJWS;
import io.inverno.mod.security.jose.jws.JsonJWS.BuiltSignature;
import io.inverno.mod.security.jose.jws.JsonJWS.ReadSignature;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.security.jose.jwt.JWTService;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {

	public static void main(String[] args) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		/*JsonStringMediaTypeConverter jsonConverter = new JsonStringMediaTypeConverter(new JacksonStringConverter(mapper));
		TextStringMediaTypeConverter textConverter = new TextStringMediaTypeConverter(new StringConverter());*/
		
		InMemoryJWKStore inMemoryJWKStore = new InMemoryJWKStore();
		
		Jose jose = new Jose.Builder(List.of(/*jsonConverter, textConverter*/)).setJwkStore(inMemoryJWKStore)/*.setJwkKeyResolver(new CustomJWKKeyResolver())*/.build();
		try {
			// Initialize Jose module
			jose.start();
			
			// Create, load or store JSON Web keys
			JWKService jwkService = jose.jwkService();
			
			// Create, sign and verify JSON Web Signature tokens
			JWSService jwsService = jose.jwsService();
			
			// Create, encrypt and decrypt JSON Web encryption tokens
			JWEService jweService = jose.jweService();
			
			// Create JSON Web Token as JWS or JWE
			JWTService jwtService = jose.jwtService();
			
			Mono<? extends OCTJWK> key = jwkService.oct().builder()
			    .keyId("keyId")
			    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
			    .keyValue("GkilETj3L4jpinuRiaNq6zd7-_1JPbfU9DY3xHl9HEE")
			    .build()
			    .cache();
			
			/*JWE<String> jwe = jweService.builder(String.class, key)
				.header(header -> header
					.keyId("keyId")
					.algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
					.critical("http://example.com/application_parameter")
					.addCustomParameter("http://example.com/application_parameter", true)
				)
				.payload("Lorem ipsum")
				.build(MediaTypes.TEXT_PLAIN)
				.block();
			
			System.out.println(jwe.toCompact());
			System.out.println(mapper.writeValueAsString(jwe));*/
			
			/* 
			 * {
			 *   "header": {
			 *     "enc": "A128CBC-HS256",
			 *     "alg": "A256GCMKW",
			 *     "kid": "keyId",
			 *     "crit": [
			 *       "http://example.com/application_parameter"
			 *     ],
			 *     "http://example.com/application_parameter": true,
			 *     "tag": "pq1OChvU6GZcMDLZqTEo0Q",
			 *     "iv": "VcuwU871tvGMGOHB"
			 *   },
			 *   "payload": "Lorem ipsum",
			 *   "initializationVector": "i1GTQ9xyOL89vza7hNCiAQ",
			 *   "authenticationTag": "5EiKTUS272wTHd978QOuHQ",
			 *   "encryptedKey": "Aq7NWm_h4LmGjJynbUYOO7O9juKlUMFWXS_HMpAAR1g",
			 *   "cipherText": "mDeuwt3QO199_h6diPwu_w"
			 * }
			 */
			String jweCompact = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoia2V5SWQiLCJjcml0IjpbImh0dHA6Ly9leGFtcGxlLmNvbS9hcHBsaWNhdGlvbl9wYXJhbWV0ZXIiXSwiaHR0cDovL2V4YW1wbGUuY29tL2FwcGxpY2F0aW9uX3BhcmFtZXRlciI6dHJ1ZSwidGFnIjoicHExT0NodlU2R1pjTURMWnFURW8wUSIsIml2IjoiVmN1d1U4NzF0dkdNR09IQiJ9."
				+ "Aq7NWm_h4LmGjJynbUYOO7O9juKlUMFWXS_HMpAAR1g."
				+ "i1GTQ9xyOL89vza7hNCiAQ."
				+ "mDeuwt3QO199_h6diPwu_w."
				+ "5EiKTUS272wTHd978QOuHQ";
			
			JWE<String> jwe = jweService.reader(String.class, key)
				.processedParameters("http://example.com/application_parameter")
				.read(jweCompact, MediaTypes.TEXT_PLAIN)
				.block();
			
		}
		finally {
			jose.stop();
		}
	}
	
	public void jose() {
		ObjectMapper mapper = new ObjectMapper();
		/*JsonStringMediaTypeConverter jsonConverter = new JsonStringMediaTypeConverter(new JacksonStringConverter(mapper));
		TextStringMediaTypeConverter textConverter = new TextStringMediaTypeConverter(new StringConverter());*/
		
		Jose jose = new Jose.Builder(List.of(/*jsonConverter, textConverter*/)).setJwkStore(new InMemoryJWKStore())/*.setJwkKeyResolver(new CustomJWKKeyResolver())*/.build();
		try {
			// Initialize Jose module
			jose.start();
			
			// Create, load or store JSON Web keys
			JWKService jwkService = jose.jwkService();
			
			// Create, sign and verify JSON Web Signature tokens
			JWSService jwsService = jose.jwsService();
			
			// Create, encrypt and decrypt JSON Web encryption tokens
			JWEService jweService = jose.jweService();
			
			// Create JSON Web Token as JWS or JWE
			JWTService jwtService = jose.jwtService();
			
			/*
			 * {
			 *   "alg":"HS512",
			 *   "k":"h92UNTmd5NpTl5UUalbp03z4AygiLZrDYHOaSjwjYQ_fma8_aO6A8MwOUjJGJyFEGPLJ46ujcTLlKoO_AjK3UQ",
			 *   "kty":"oct",
			 *   "kid":"octKey"
			 * }
			 */ 
			Mono<? extends OCTJWK> octKey = jwkService.oct().generator()
			    .keyId("octKey")
			    .algorithm(OCTAlgorithm.HS512.getAlgorithm())
			    .generate()
			    .cache();
			
			/* 
			 * {
			 *   "header":{
			 *     "alg":"HS512",
			 *     "cty":"text/plain",
			 *     "kid":"octKey"
			 *   },
			 *   "payload":"This is a simple payload",
			 *   "signature":"mwq--Ke2Om3zA2y1F9cQlw5SyFPzhkvwoRaaezbzqifL5joJWuJEddPbtFDKLaBUD9Ufwi6R6IFbbOe-nxkr4w"
			 * }
			 */
			Mono<JWS<String>> jws = jwsService.builder(String.class, octKey)
			    .header(header -> header
			        .keyId("octKey")
			        .algorithm(OCTAlgorithm.HS512.getAlgorithm())
			        .contentType(MediaTypes.TEXT_PLAIN)
			    )
			    .payload("This is a simple payload")
			    .build();

			// eyJjdHkiOiJ0ZXh0L3BsYWluIiwia2lkIjoib2N0S2V5IiwiYWxnIjoiSFM1MTIifQ.VGhpcyBpcyBhIHNpbXBsZSBwYXlsb2Fk.mwq--Ke2Om3zA2y1F9cQlw5SyFPzhkvwoRaaezbzqifL5joJWuJEddPbtFDKLaBUD9Ufwi6R6IFbbOe-nxkr4w
			String jwsCompact = jws.block().toCompact();
			
			
			jws = jwsService.reader(String.class, octKey)
			    .read(jwsCompact);

			// Returns "This is a simple payload" or throw a JWSReadException if the token is invalid
			jws.block().getPayload();
			
			
			/*
			 * From RFC7516 Section A.1:
			 * {
			 *   "n":"oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw",
			 *   "e":"AQAB","d":"kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ",
			 *   "p":"1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0",
			 *   "q":"wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc",
			 *   "dp":"ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE",
			 *   "dq":"Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis",
			 *   "qi":"VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY",
			 *   "kty":"RSA",
			 *   "kid":"rsaKey"
			 * }
			 */
			Mono<? extends RSAJWK> rsaKey = jwkService.rsa().builder()
			    .keyId("rsaKey")
			    .modulus("oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw")
			    .publicExponent("AQAB")
			    .privateExponent("kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ")
			    .firstPrimeFactor("1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0")
			    .secondPrimeFactor("wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc")
			    .firstFactorExponent("ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE")
			    .secondFactorExponent("Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis")
			    .firstCoefficient("VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY")
			    .build()
			    .cache();

			/*
			 * {
			 *   "header":{
			 *     "enc":"A256GCM",
			 *     "alg":"RSA-OAEP",
			 *     "cty":"text/plain",
			 *     "kid":"rsaKey"
			 *   },
			 *   "payload":"This is a simple payload",
			 *   "initializationVector":"97ZuhWEQOygN7T3g",
			 *   "authenticationTag":"_e-vSUwj5LawcnXROqKvmQ",
			 *   "encryptedKey":"VOk1HQDwucfkljliz8RzxvuKXX_B6sTMwZbwKJztZjL0Ga8i3yrRl_4jumBTKBIyWMDdZYxcbHtkzZQhQDFJVpvNcf1QxEryhe3OnFOEF2BGJDPwSYc-AVmAq01gHrUaTF02xvWntfvzu3ePq5vVHl4eiL72POVdoN9w8ck4HaOjeoooYcrkaV8l15cYurXsJ8oo_KQ40SBmKnK99CRrqR1QggPscTpE1QeVj2Z9tw5A3rqYGbCX2d2QwP-zc7w5o1bsuB5qE99i0iAKtMwEdaz6iC97nDry8Vo2uSPf3YviwpzmLbbwJlb_bHhl1aeTZaNQl9JLvxvqCDQehdAx7g",
			 *   "processedParameters":null,"cipherText":"YFPMGQXbmI5ZWZXkpH04vEWsBLCmBJ4G"
			  * }
			 */
			Mono<JWE<String>> jwe = jweService.builder(String.class, rsaKey)
			    .header(header -> header
			        .keyId("rsaKey")
			        .algorithm("RSA-OAEP")
			        .encryptionAlgorithm("A256GCM")
			    )
			    .payload("This is a simple payload")
			    .build(MediaTypes.TEXT_PLAIN);

			// eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAiLCJjdHkiOiJ0ZXh0L3BsYWluIiwia2lkIjoicnNhS2V5In0.EG3dFsn0MAxWRadls1UHpmfNFspczXldNTwr9LfO8BZXsliEJJ8J9-Z25oFnpaI7q3lXazNg06C9upJW2ZiDg2hmmqoCzYD7xdFEz_YkgO7_92tPxCm0XSGZJUtX1d8gpJBoIQWmPCmO6vVveoCds-kmtTQEigokSewKmkIQyOQcyAhLT5y_gkL0JrKLTPjTKGept7dl9uTzuZenWi-5apdVynDhOkraOkCSu8ahVPPPSf5s9aHUS8th-pjWAtS7OFwMOrjLzYXmcqdNPAYM0Pcg88Fw_uI8J7I6tzDInV31rVZ9pDlVarmVSYhS9Rfa91gZaba-onCiFURceUaeOg.im9v2BnFnFp_uGtX.VItnFUA2xtrgrO-Fs-LukV0RZbRUrNkv.eFgbb8i1olfIkSHFM8IkXA
			String jweCompact = jwe.block().toCompact();

			jwe = jweService.reader(String.class, rsaKey)
			    .read(jweCompact);

			// Returns "This is a simple payload" or throw a JWEReadException if the token is invalid
			jwe.block().getPayload();
			
			/*
			 * {
			 *   "header":{
			 *     "typ":"JWT",
			 *     "kid":"octKey",
			 *     "alg":"HS512"
			 *   },
			 *   "payload":{
			 *     "iss":"john",
			 *     "exp":1659346862,
			 *     "http://example.com/is_root":true
			 *   },
			 *   "signature":"hX_m668usLB1DHGW4cD2NJ1UzCs3T6sGCa0ctvGTkresiZ87iIeKnY0-EoIvWmDy3SY69rGLMsbsEjsru1QdZw"
			 * }
			 */
			Mono<JWS<JWTClaimsSet>> jwt = jwtService.jwsBuilder(octKey)
			    .header(header -> header
			        .keyId("octKey")
			        .algorithm("HS512")
			        .type("JWT")
			    )
			    .payload(JWTClaimsSet.of("john", ZonedDateTime.now().plusMinutes(10).toEpochSecond())
			        .addCustomClaim("http://example.com/is_root", true)
			        .build()
			    )
			    .build();

			// eyJ0eXAiOiJKV1QiLCJraWQiOiJvY3RLZXkiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJqb2huIiwiZXhwIjoxNjU5MzQ2ODYyLCJodHRwOi8vZXhhbXBsZS5jb20vaXNfcm9vdCI6dHJ1ZX0.hX_m668usLB1DHGW4cD2NJ1UzCs3T6sGCa0ctvGTkresiZ87iIeKnY0-EoIvWmDy3SY69rGLMsbsEjsru1QdZw
			String jwtCompact = jwt.block().toCompact();

			jwt = jwtService.jwsReader(octKey)
			    .read(jwtCompact);

			// Throw a JWSReadException if the signature is invalid or an InvalidJWTException if the JWT Claims set is invalid (e.g. expired, inactive...) 
			jwt.block().getPayload().ifInvalidThrow();
		}
		finally {
			jose.stop();
		}
	}
	
	public void jwk() {
		JWKService jwkService = null;
		
		ECJWK ecJWK = jwkService.ec().generator()
		    .curve(ECCurve.P_256.getCurve())
		    .generate()
		    .block();

		// Throw a JWKProcessingException since Elliptic-curve algorithms cannot be used to encrypt data
		ecJWK.cipher();

		// Throw a JWKProcessingException since no algorithm was specified in the JWK
		ecJWK.signer();

		// Throw a JWKProcessingException since ES512 algorithm is not a key management algorithm
		ecJWK.keyManager(ECAlgorithm.ES512.getAlgorithm());

		// Throw a JWKProcessingException since ES512 algorithm is not consistent with curve P_256 (P_512 is expected)
		ecJWK.signer(ECAlgorithm.ES512.getAlgorithm());

		// Return a key manager using ECDH ES algorithm on curve P_256
		ecJWK.keyManager(ECAlgorithm.ECDH_ES.getAlgorithm());

		OCTJWK octJWK = jwkService.oct().generator()
		    .algorithm(OCTAlgorithm.HS512.getAlgorithm())
		    .generate()
		    .block();

		// Throw a JWKProcessingException since HS256 algorithm is requested which is not consistent with HS512 algorithm specified in the JWK
		octJWK.signer(OCTAlgorithm.HS256.getAlgorithm());

		// Return a signer using HS512 algorithm 
		octJWK.signer();
		
		// Return the ECJWKFactory
		jwkService.ec();
				
		// Return the RSAJWKFactory
		jwkService.rsa();
				
		// Return the OCTJWKFactory
		jwkService.oct();

		// Return the EdECJWKFactory
		jwkService.edec();

		// Return the XECJWKFactory
		jwkService.xec();

		// Return the PBES2JWKFactory
		jwkService.pbes2();
		
		// Return one or more JWKs
		Publisher<? extends JWK> read = jwkService.read(URI.create("https://host/jwks.json"));
		
		OCTJWK mySymmetricKey = jwkService.oct().generator()
			.keyId("mySymmetricKey")
			.algorithm(OCTAlgorithm.HS512.getAlgorithm())
			.keySize(24)
			.generate()
			.block();
		
		Mono<? extends RSAJWK> myAsymmetricKey = jwkService.rsa().generator()
			.keyId("myAsymmetricKey")
			.algorithm(RSAAlgorithm.PS256.getAlgorithm())
			.generate()
			.cache();
		
		RSAJWK rsaKey = jwkService.rsa().builder()
		    .keyId("rsaKey")
		    .modulus("oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw")
		    .publicExponent("AQAB")
		    .privateExponent("kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ")
		    .firstPrimeFactor("1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0")
		    .secondPrimeFactor("wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc")
		    .firstFactorExponent("ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE")
		    .secondFactorExponent("Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis")
		    .firstCoefficient("VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY")
		    .build()
		    .block();
		
		rsaKey.trust();
		
		rsaKey = jwkService.rsa().builder()
		    .keyId("rsaKey")
		    .build()
		    .block();
		
		rsaKey = jwkService.rsa().builder()
		    .keyId("rsaKey")
		    .build()
		    .block();
		
		String rsaJwkJSON = "{\n"
		    + "   \"n\":\"oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw\",\n"
		    + "   \"e\":\"AQAB\",\"d\":\"kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ\",\n"
		    + "   \"p\":\"1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0\",\n"
		    + "   \"q\":\"wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc\",\n"
		    + "   \"dp\":\"ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE\",\n"
		    + "   \"dq\":\"Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis\",\n"
		    + "   \"qi\":\"VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY\",\n"
		    + "   \"kty\":\"RSA\",\n"
		    + "   \"kid\":\"rsaKey\"\n"
		    + "}";

		rsaKey = jwkService.rsa().read(rsaJwkJSON).block();
		
		jwkService.oct().generator()
		    .keyId("octKey")
		    .algorithm(OCTAlgorithm.HS512.getAlgorithm())
		    .generate()
		    .map(JWK::trust)
		    .flatMap(jwkService.store()::set)
		    .block();
	}
	
	public void jwa() {
		JWKService jwkService = null;
		
		byte[] payload = "This is a payload".getBytes();

		JWASigner signer = null;

		byte[] signature = signer.sign(payload);

		if(signer.verify(payload, signature)) {
		}
		
		// Specified in RFC 7516
		byte[] aad = null;

		JWACipher cipher = null;

		JWACipher.EncryptedData encryptedData = cipher.encrypt(payload, aad);

		byte[] decryptedPayload = cipher.decrypt(encryptedData.getCipherText(), aad, encryptedData.getInitializationVector(), encryptedData.getAuthenticationTag());
		
		// e.g. Ephemeral public key (epk), Agreement PartyUInfo (apu), Agreement PartyVInfo (apv) when using ECDH-ES algorithm
		Map<String, Object> parameters = null;

		DirectJWAKeyManager directKeyManager = null;

		DirectJWAKeyManager.DirectCEK directCEK = directKeyManager.deriveCEK("ECDH-ES", parameters);
		OCTJWK cek = directCEK.getEncryptionKey();
		
		// e.g. PBES2 Salt Input (p2s), PBES2 Count (p2c) when using PBES2-HS256+A128KW algorithm
//		Map<String, Object> parameters = null;
		// Generated when building a JWE
//		JWK cek = null;

		EncryptingJWAKeyManager encryptingKeyManager = null;

		EncryptingJWAKeyManager.EncryptedCEK encryptedCEK = encryptingKeyManager.encryptCEK(cek, parameters);
		byte[] encryptedKey = encryptedCEK.getEncryptedKey();

		JWK decryptedCEK = encryptingKeyManager.decryptCEK(encryptedKey, "PBES2-HS256+A128KW", parameters);
		
//		Map<String, Object> parameters = null;
		// Generated when building a JWE
//		JWK cek = null;

		WrappingJWAKeyManager wrappingKeyManager = null;

		WrappingJWAKeyManager.WrappedCEK wrappedCEK = wrappingKeyManager.wrapCEK(cek, parameters);
		byte[] wrappedKey = wrappedCEK.getWrappedKey();

		JWK unwrappedCEK = wrappingKeyManager.unwrapCEK(wrappedKey, "A192KW", parameters);
		
		cipher = jwkService.oct().generator()
		    .algorithm(OCTAlgorithm.A128GCM.getAlgorithm())
		    .generate()
		    .block()
		    .cipher();
		
		signer = jwkService.ec().generator()
		    .algorithm(ECAlgorithm.ES384.getAlgorithm())
		    .generate()
		    .block()
		    .signer();
		
		JWAKeyManager keyManager = jwkService.rsa().generator()
		    .algorithm(RSAAlgorithm.RSA_OAEP.getAlgorithm())
		    .generate()
		    .block()
		    .keyManager();
		
		keyManager = jwkService.pbes2().generator()
		    .algorithm(PBES2Algorithm.PBES2_HS256_A128KW.getAlgorithm())
		    .length(32) // generate a 32 characters long password
		    .generate()
		    .block()
		    .keyManager();
		
		signer = jwkService.edec().generator()
		    .algorithm(EdECAlgorithm.EDDSA_ED25519.getAlgorithm())
		    .generate()
		    .block()
		    .signer();
		
		jwkService.xec().generator()
		    .algorithm(XECAlgorithm.ECDH_ES_A128KW.getAlgorithm())
		    .curve(OKPCurve.X25519.getCurve())
		    .generate()
		    .block()
		    .keyManager();
	}
	
	public void jws() {
		JWS<?> jws = null;

		// <header>.<payload>.<signature> 
		// e.g. eyJhbGciOiJFUzUxMiJ9.UGF5bG9hZA.AdwMgeerwtHoh-l192l60hp9wAHZFVJbLfD_UxMi70cwnZOYaRI1bKPWROc-mZZqwqT2SI-KGDKB34XO0aw_7XdtAG8GaSwFKdCAPZgoXD2YBJZCPEX3xKpRwcdOO8KpEHwJjyqOgzDO7iKvU8vcnwNrmxYbSW9ERBXukOXolLzeO_Jn
		String jwsCompact = jws.toCompact();
		
		JsonJWS<?, ?> jsonJWS = null;
		
		/* {
		 *   "payload": "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ",
		 *   "signatures": [
		 *     {
		 *       "protected":"eyJhbGciOiJSUzI1NiJ9",
		 *       "header": {
		 *         "kid":"2010-12-29"
		 *       },
		 *       "signature": "cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZ
		 *                     mh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjb
		 *                     KBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHl
		 *                     b1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZES
		 *                     c6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AX
		 *                     LIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw"
		 *     },
		 *     {
		 *       "protected":"eyJhbGciOiJFUzI1NiJ9",
		 *       "header": {
		 *         "kid":"e9bc097a-ce51-4036-9562-d2ade882db0d"
		 *       },
		 *       "signature": "DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8IS
		 *                     lSApmWQxfKTUJqPP3-Kg6NU1Q"
		 *     }
		 *   ]
		 * }
		 */
		String jwsJson = jsonJWS.toJson();
		
//		JWS jws = null;

		// <header>..<signature> 
		// e.g. eyJhbGciOiJFUzUxMiJ9..AdwMgeerwtHoh-l192l60hp9wAHZFVJbLfD_UxMi70cwnZOYaRI1bKPWROc-mZZqwqT2SI-KGDKB34XO0aw_7XdtAG8GaSwFKdCAPZgoXD2YBJZCPEX3xKpRwcdOO8KpEHwJjyqOgzDO7iKvU8vcnwNrmxYbSW9ERBXukOXolLzeO_Jn
		String jwsDetachedCompact = jws.toDetachedCompact();
	}
	
	public void jwsBuild() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWSService jwsService = null;

		Mono<? extends OCTJWK> key = jwkService.oct().generator()
		    .keyId("keyId")
		    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
		    .generate()
		    .cache();

		JWS<Message> jws = jwsService.builder(Message.class, key)
		    .header(header -> header
		        .keyId("keyId")
		        .algorithm(OCTAlgorithm.HS256.getAlgorithm())
		    )
		    .payload(new Message("John", "Hello world!"))
		    .build(MediaTypes.APPLICATION_JSON)
		    .block();

		// eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIn0.eyJhdXRob3IiOiJKb2huIiwibWVzc2FnZSI6IkhlbGxvIHdvcmxkISJ9.aSRmKH3ZiTGm2MrKBLqBJH-d-rBEt5bWPY6TEC15B7s
		String jwsCompact = jws.toCompact();
		
		// Using an 'InMemoryJWKStore', we can store the key so it can be resolved by key id by the 'JWKService'
		key.map(JWK::trust).map(jwkService.store()::set).block();

		// Key 'keyId' is then automatically resolved
		jws = jwsService.builder(Message.class)
			.header(header -> header
		        .keyId("keyId")
		        .algorithm(OCTAlgorithm.HS256.getAlgorithm())
		    )
		    .payload(new Message("John", "Hello world!"))
		    .build(MediaTypes.APPLICATION_JSON)
		    .block();
		
		Mono<? extends OCTJWK> key1 = jwkService.oct().generator()
		    .keyId("key1")
		    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
		    .generate()
		    .cache();

		Mono<? extends RSAJWK> key2 = jwkService.rsa().generator()
		    .keyId("key2")
		    .algorithm(RSAAlgorithm.RS256.getAlgorithm())
		    .generate()
		    .cache();

		JsonJWS<Message, BuiltSignature<Message>> jsonJWS = jwsService.jsonBuilder(Message.class)
		    .signature(
		        protectedHeader -> protectedHeader
		            .keyId("key1")
		            .algorithm(OCTAlgorithm.HS256.getAlgorithm()),
		        unprotectedHeader -> {},
		        key1
		    )
		    .signature(
		        protectedHeader -> protectedHeader
		            .keyId("key2")
		            .algorithm(RSAAlgorithm.RS256.getAlgorithm()),
		        unprotectedHeader -> {},
		        key2
		    )
		    .payload(new Message("Alice", "Hi John!"))
		    .build(message -> Mono.just(message.getAuthor() + " > " + message.getMessage()))
		    .block();

		/*
		 * {
		 *   "signatures": [
		 *     {
		 *       "header": {
		 *         "kid": "key1",
		 *         "cty": "text/plain"
		 *       },
		 *       "signature": "u38wYs0v1M-zgw0lr2Gw3PKRALPxWH6I4wfpLFF_E3I",
		 *       "protected": "eyJhbGciOiJIUzI1NiJ9"
		 *     },
		 *     {
		 *       "header": {
		 *         "kid": "key2",
		 *         "cty": "text/plain"
		 *       },
		 *       "signature": "X6J77kf7sXW_7j7tLvgwJR2hy2kvDjuEGdT-1WU_Po2Z0sMPvHJd9LRdgYWUCn10V6
		 *                     xgNatDQuwEnegOrIOVTI2yN6_T74rQY1-VWO8kESg_MyGRoieC3s6beQAt0JdWKgSs
		 *                     xNZjCbRLTu_bxTpIl90j2MgPNHiL8ox2uDwA3pg-6cgEzswMQx6x_KQ-e3VPuqdiSd
		 *                     6PNeFNiYN-s9xBTlN_m-0k8MDHSzQ612Ms3Q1ox2gONdpVG3wcoIPX63zaRmt-a3r6
		 *                     KReL9bPBs1hCRHxp6ermxwJRf0yjKfo2KH2fWV_wMiPsCdbJSlIL3MPreR0yi5iVDu
		 *                     iXK-yWoJ2XOg",
		 *       "protected": "eyJhbGciOiJSUzI1NiJ9"
		 *     }
		 *   ],
		 *   "payload": "QWxpY2UgPiBIaSBKb2huIQ"
		 * }
		 */
		String jwsJson = jsonJWS.toJson();
		
		Message message = jsonJWS.getPayload();

		List<JWS> jwsSignatures = jsonJWS.getSignatures().stream()
		    .map(signature -> signature.getJWS())
		    .collect(Collectors.toList());
	}
	
	public void jwsRead() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWSService jwsService = null;

		Mono<? extends OCTJWK> key = jwkService.oct().builder()
		    .keyId("keyId")
		    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
		    .keyValue("xqf1haCsSJGuueZivcq4YafdWw6n5CH2BTT6vDwUSaM")
		    .build()
		    .cache();

		String jwsCompact = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIiwiY3R5IjoiYXBwbGljYXRpb24vanNvbiJ9."
		    + "eyJhdXRob3IiOiJCaWxsIiwibWVzc2FnZSI6IkhleSEifQ."
		    + "pNS2tZmB20ezMA-twecOhobDk3H5AgWyh-m5eV5xE14";

		JWS<Message> jws = jwsService.reader(Message.class, key)
		    .read(jwsCompact)
		    .block();

		// Bill says Hey!
		Message message = jws.getPayload();
		
		// Using an 'InMemoryJWKStore', we can store the key so it can be resolved by key id by the 'JWKService'
		key.map(JWK::trust).map(jwkService.store()::set).block();

		// Key 'keyId' is then automatically resolved
		jws = jwsService.reader(Message.class)
			.read(jwsCompact)
		    .block();
		
		Mono<? extends ECJWK> key2 = jwkService.ec().builder()
		    .keyId("key2")
		    .algorithm(ECAlgorithm.ES256.getAlgorithm())
		    .curve(ECCurve.P_256.getCurve())
		    .xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
		    .yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
		    .eccPrivateKey("jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI")
		    .build()
		    .cache();

		String jwsJson = "{"
		    + "  \"signatures\": ["
		    + "    {"
		    + "      \"header\": {"
		    + "        \"cty\": \"text/plain\","
		    + "        \"kid\": \"key1\""
		    + "      },"
		    + "      \"signature\": \"PxhpMkmTep5obGFZv50OsRGA-e7-fxhUmWdUyLC74ms\","
		    + "      \"protected\": \"eyJhbGciOiJIUzI1NiJ9\""
		    + "    },"
		    + "    {"
		    + "      \"header\": {"
		    + "        \"cty\": \"text/plain\","
		    + "        \"kid\": \"key2\""
		    + "      },"
		    + "      \"signature\": \"KqjGSxiBD5GhwFhLs8H_RBg8nXsKtp4nj5PsdxCzd0ZqMed874ZAxTgnyd0KmQEZwmYvvgM-o8NC9VdIWalMvw\","
		    + "      \"protected\": \"eyJhbGciOiJFUzI1NiJ9\""
		    + "    }"
		    + "  ],"
		    + "  \"payload\": \"TGluZGEgPiBTaGFsbCB3ZSBiZWdpbj8\""
		    + "}";   

		JsonJWS<Message, ReadSignature<Message>> jsonJWS = jwsService.jsonReader(Message.class)
		    .read(jwsJson, p -> 
		        Mono.fromSupplier(() -> {
		            int separatorIndex = p.indexOf(">");
		            return new Message(p.substring(0, separatorIndex - 1), p.substring(separatorIndex + 2));
		        })
		    )
		    .block();

		// Return as soon as one of the signatures could have been verified with key2
		JWS<Message> verifiedJWS = Flux.fromIterable(jsonJWS.getSignatures())
		    .flatMap(signature -> signature.readJWS(key2).onErrorResume(e -> Mono.empty()))
		    .blockFirst();

		if(verifiedJWS != null) {
		    // Linda says Shall we begin?
		    message = verifiedJWS.getPayload();
		}
		
		/*
		 * {
		 *   "header": {
		 *     "alg": "HS256",
		 *     "kid": "keyId",
		 *     "crit": [
		 *       "http://example.com/application_parameter"
		 *     ],
		 *     "http://example.com/application_parameter": true
		 *   },
		 *   "payload": "Lorem ipsum",
		 *   "signature": "aQMWohoxZWOcpYVm04FBJwGc7fBO4xzUKVJz9qfjpxc"
		 * }
		 * 
		 */
		jwsCompact = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIiwiY3JpdCI6WyJodHRwOi8vZXhhbXBsZS5jb20vYXBwbGljYXRpb25fcGFyYW1ldGVyIl0sImh0dHA6Ly9leGFtcGxlLmNvbS9hcHBsaWNhdGlvbl9wYXJhbWV0ZXIiOnRydWV9."
			+ "TG9yZW0gaXBzdW0."
			+ "aQMWohoxZWOcpYVm04FBJwGc7fBO4xzUKVJz9qfjpxc";

		JWS<String> jws2 = jwsService.reader(String.class, key)
			.processedParameters("http://example.com/application_parameter")
		    .read(jwsCompact, MediaTypes.TEXT_PLAIN)
		    .block();
	}
	
	public void jwe() {
		JWE<?> jwe = null;

		// <header>.<encrypted_key>.<initialization_vector>.<cipher_text>.<authentication_tag>
		// e.g. eyJhbGciOiJFUzUxMiJ9.UGF5bG9hZA.AdwMgeerwtHoh-l192l60hp9wAHZFVJbLfD_UxMi70cwnZOYaRI1bKPWROc-mZZqwqT2SI-KGDKB34XO0aw_7XdtAG8GaSwFKdCAPZgoXD2YBJZCPEX3xKpRwcdOO8KpEHwJjyqOgzDO7iKvU8vcnwNrmxYbSW9ERBXukOXolLzeO_Jn
		String jweCompact = jwe.toCompact();
		
		JsonJWE<?, ?> jsonJWE = null;

		/* {
		 *   "payload": "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ",
		 *   "signatures": [
		 *     {
		 *       "protected":"eyJhbGciOiJSUzI1NiJ9",
		 *       "header": {
		 *         "kid":"2010-12-29"
		 *       },
		 *       "signature": "cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZ
		 *                     mh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjb
		 *                     KBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHl
		 *                     b1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZES
		 *                     c6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AX
		 *                     LIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw"
		 *     },
		 *     {
		 *       "protected":"eyJhbGciOiJFUzI1NiJ9",
		 *       "header": {
		 *         "kid":"e9bc097a-ce51-4036-9562-d2ade882db0d"
		 *       },
		 *       "signature": "DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8IS
		 *                     lSApmWQxfKTUJqPP3-Kg6NU1Q"
		 *     }
		 *   ]
		 * }
		 */
		String jweJson = jsonJWE.toJson();
	}
	
	public void jweBuild() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWEService jweService = null;
				
		Mono<? extends OCTJWK> key = jwkService.oct().generator()
		    .keyId("keyId")
		    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
		    .generate()
		    .cache();

		JWE<Message> jwe = jweService.builder(Message.class, key)
			.header(header -> header
				.keyId("keyId")
				.algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
				.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
			)
			.payload(new Message("John", "Hello world!"))
			.build(MediaTypes.APPLICATION_JSON)
		    .block();

		// eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoia2V5SWQiLCJ0YWciOiJVVjlFY1V5RTQ3SmdDa3A4dVFnSjJ3IiwiaXYiOiJwWHRhUUxXNC1HZngxR2xNIn0.Ygo62KaP8hNmU55nuitIKHWJDRexgps6mLuduoCRLjE.MtCDVA1Mxy4hh0g2yuswZg.uug4kTMkgxzBpwL1B3NyxPIWxCuZVv1FrzDvK2ebYZDsqvzSRZMnx3sr47lnLsLS.Bqu8YnTUbsE_BaOfNo9McQ
		String jweCompact = jwe.toCompact();
		
		// Using an 'InMemoryJWKStore', we can store the key so it can be resolved by key id by the 'JWKService'
		key.map(JWK::trust).map(jwkService.store()::set).block();

		// Key 'keyId' is then automatically resolved
		jwe = jweService.builder(Message.class)
			.header(header -> header
				.keyId("keyId")
				.algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
				.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
			)
			.payload(new Message("John", "Hello world!"))
			.build(MediaTypes.APPLICATION_JSON)
		    .block();
		
		Mono<? extends RSAJWK> key1 = jwkService.rsa().generator()
			.keyId("key1")
			.algorithm(RSAAlgorithm.RSA1_5.getAlgorithm())
			.generate()
			.cache();
		
		Mono<? extends OCTJWK> key2 = jwkService.oct().generator()
			.keyId("key2")
			.algorithm(OCTAlgorithm.A128KW.getAlgorithm())
			.generate()
			.cache();
		
		JsonJWE<Message, BuiltRecipient<Message>> jsonJWE = jweService.jsonBuilder(Message.class)
			.headers(
				protectedHeader -> protectedHeader
					.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm()),
				unprotectedHeader -> {}
			)
			.payload(new Message("Alice", "Hi John!"))
			.recipient(
				header -> header
					.keyId("key1")
					.algorithm(RSAAlgorithm.RSA1_5.getAlgorithm()),
				key1
			)
			.recipient(
				header -> header
					.keyId("key2")
					.algorithm(OCTAlgorithm.A128KW.getAlgorithm()),
				key2
			)
			.build(message -> Mono.just(message.getAuthor() + " > " + message.getMessage()))
			.block();
		
		String jweJson = jsonJWE.toJson();
		
		List<JWE<Message>> jweRecipients = jsonJWE.getRecipients().stream()
			.map(recipient -> recipient.getJWE())
			.collect(Collectors.toList());
	}
	
	public void jweRead() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWEService jweService = null;

		Mono<? extends OCTJWK> key = jwkService.oct().builder()
		    .keyId("keyId")
		    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
		    .keyValue("GkilETj3L4jpinuRiaNq6zd7-_1JPbfU9DY3xHl9HEE")
		    .build()
		    .cache();
		
		JWE<Message> jwe = jweService.builder(Message.class, key)
			.header(header -> header
				.keyId("keyId")
				.algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
				.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
				.contentType(MediaTypes.APPLICATION_JSON)
			)
			.payload(new Message("Bill", "Hey!"))
			.build()
		    .block();

		// eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoia2V5SWQiLCJjdHkiOiJhcHBsaWNhdGlvbi9qc29uIiwidGFnIjoiYUtlc2VBelZoenh5Vk9pRVNvVEdoQSIsIml2IjoieEJFSTlYeHBDVTZwcVVSaCJ9.MNYqpQCQPrUSZTwP-C7kUCGOFqFGGciUU2qW54jc3NM._nfKSroUwjqzdJcPETt-ow.1dL8rLmhKF7hqVNzQf5oWPOSZN7Z_V46w0UvIBDuFjH5pqvhbs4ltrTsk6E_NF-y.RJ8QOGLuT2fz5VrzG1EHbg
		String jweCompact = jwe.toCompact();
		
		jweCompact = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoia2V5SWQiLCJjdHkiOiJhcHBsaWNhdGlvbi9qc29uIiwidGFnIjoiYUtlc2VBelZoenh5Vk9pRVNvVEdoQSIsIml2IjoieEJFSTlYeHBDVTZwcVVSaCJ9."
			+ "MNYqpQCQPrUSZTwP-C7kUCGOFqFGGciUU2qW54jc3NM."
			+ "_nfKSroUwjqzdJcPETt-ow."
			+ "1dL8rLmhKF7hqVNzQf5oWPOSZN7Z_V46w0UvIBDuFjH5pqvhbs4ltrTsk6E_NF-y."
			+ "RJ8QOGLuT2fz5VrzG1EHbg";

		jwe = jweService.reader(Message.class, key)
		    .read(jweCompact)
		    .block();

		// Bill says Hey!
		Message message = jwe.getPayload();

		// Using an 'InMemoryJWKStore', we can store the key so it can be resolved by key id by the 'JWKService'
		key.map(JWK::trust).map(jwkService.store()::set).block();

		// Key 'keyId' is then automatically resolved
		jwe = jweService.reader(Message.class)
			.read(jweCompact)
		    .block();
		
		Mono<? extends OCTJWK> key2 = jwkService.oct().builder()
		    .keyValue("GawgguFyGrWKav7AX4VKUg")
		    .build()
		    .cache();

		String jweJson = "{"
		    + "  \"unprotected\": {"
		    + "  },"
		    + "  \"ciphertext\": \"2jtWSZdL-TJGyktUwldH4sphYuz2VbseUS9el_vh_tU\","
		    + "  \"recipients\": ["
		    + "    {"
		    + "      \"header\": {"
		    + "        \"alg\": \"RSA1_5\","
		    + "        \"kid\": \"key1\""
		    + "      },"
		    + "      \"encrypted_key\": \"kIHuM-OZU1wvmb6ocdDsn1ljF11kIbfvv9y7XpTPGfdYeaz2AhJvpHfPZ6LKk5-yDfHAVWTXz_RbgjPATURNKyu0hdogfWBWXEpQEk8WaBafI8kSk0GzhJrR2tcXhrxs0xWPMthjfZ38zNql1oZuL9pzUZ3PicNhcCXD2XN52kw7VGMvPus8r89orY4q2l_xA65wkxHtG3JDG9Je_CidYuX_PXHqMkrbszsUPbyCspPIRTP5yWMeFmMp8KiEnyGaQITt0vZuea4u3tWuhX0Wa2AN74qesuArMhx81NWxaMnuDNrF6eQFIQw4QJ41MqVchHRAoXYKQvB8DYce9fHhPQ\""
		    + "    },"
		    + "    {"
		    + "      \"header\": {"
		    + "        \"alg\": \"A128KW\","
		    + "        \"kid\": \"key2\""
		    + "      },"
		    + "      \"encrypted_key\": \"OSMIf3Elx-NmfzP1Y_aZbae6k6yU2rl7o2uHd7v3lHgS4UjJURVYTQ\""
		    + "    }"
		    + "  ],"
		    + "  \"iv\": \"vrCX8Yr9oOs--KiBtkQ6kw\","
		    + "  \"tag\": \"gHpLPXRRDjUNJ1HDivaSTg\","
		    + "  \"protected\": \"eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0\""
		    + "}";

		JsonJWE<Message, ReadRecipient<Message>> readJsonJWE = jweService.jsonReader(Message.class)
		    .read(jweJson, payload -> 
		        Mono.fromSupplier(() -> {
		            int separatorIndex = payload.indexOf(">");
		            return new Message(payload.substring(0, separatorIndex - 1), payload.substring(separatorIndex + 2));
		        })
		    )
		    .block();

		// Return as soon as one of the recipients could have been verified and decrypted with key2
		JWE<Message> decryptedJWE = Flux.fromIterable(readJsonJWE.getRecipients())
		    .flatMap(recipient -> recipient.readJWE(key2).onErrorResume(e -> Mono.empty()))
		    .blockFirst();

		if(decryptedJWE != null) {
		    // Linda says Shall we begin?
		    message = decryptedJWE.getPayload();
		}
		
		/*JWE<String> jwe = jweService.builder(String.class, key)
			.header(header -> header
				.keyId("keyId")
				.algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
				.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
				.critical("http://example.com/application_parameter")
				.addCustomParameter("http://example.com/application_parameter", true)
			)
			.payload("Lorem ipsum")
			.build(MediaTypes.TEXT_PLAIN)
			.block();
		
		System.out.println(jwe.toCompact());
		System.out.println(mapper.writeValueAsString(jwe));*/
		
		/* 
		 * {
		 *   "header": {
		 *     "enc": "A128CBC-HS256",
		 *     "alg": "A256GCMKW",
		 *     "kid": "keyId",
		 *     "crit": [
		 *       "http://example.com/application_parameter"
		 *     ],
		 *     "http://example.com/application_parameter": true,
		 *     "tag": "pq1OChvU6GZcMDLZqTEo0Q",
		 *     "iv": "VcuwU871tvGMGOHB"
		 *   },
		 *   "payload": "Lorem ipsum",
		 *   "initializationVector": "i1GTQ9xyOL89vza7hNCiAQ",
		 *   "authenticationTag": "5EiKTUS272wTHd978QOuHQ",
		 *   "encryptedKey": "Aq7NWm_h4LmGjJynbUYOO7O9juKlUMFWXS_HMpAAR1g",
		 *   "cipherText": "mDeuwt3QO199_h6diPwu_w"
		 * }
		 */
		jweCompact = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoia2V5SWQiLCJjcml0IjpbImh0dHA6Ly9leGFtcGxlLmNvbS9hcHBsaWNhdGlvbl9wYXJhbWV0ZXIiXSwiaHR0cDovL2V4YW1wbGUuY29tL2FwcGxpY2F0aW9uX3BhcmFtZXRlciI6dHJ1ZSwidGFnIjoicHExT0NodlU2R1pjTURMWnFURW8wUSIsIml2IjoiVmN1d1U4NzF0dkdNR09IQiJ9."
			+ "Aq7NWm_h4LmGjJynbUYOO7O9juKlUMFWXS_HMpAAR1g."
			+ "i1GTQ9xyOL89vza7hNCiAQ."
			+ "mDeuwt3QO199_h6diPwu_w."
			+ "5EiKTUS272wTHd978QOuHQ";
		
		JWE<String> jwe2 = jweService.reader(String.class, key)
			.processedParameters("http://example.com/application_parameter")
			.read(jweCompact, MediaTypes.TEXT_PLAIN)
			.block();
	}
	
	public void jwt() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWTService jwtService = null;
				
		JWTClaimsSet jwtClaimsSet = JWTClaimsSet.of("joe", ZonedDateTime.now().plusDays(1).toEpochSecond())
			.addCustomClaim("http://example.com/is_root", true)
			.build();
		
		if(jwtClaimsSet.isValid()) {
			
		}
		
		// Run an action only if the JWT claims set is valid
		jwtClaimsSet.ifValid(() -> {
			
		});
		
		// Run an action the JWT claims set is valid and another action if it is not
		jwtClaimsSet.ifValidOrElse(
			() -> {}, 
			() -> {}
		);
		
		// Throws an InvalidJWTException if the JWT claims set is invalid
		jwtClaimsSet.ifInvalidThrow();
		
		// Throws the provided exception if the JWT claims set is invalid
//		jwtClaimsSet.ifInvalidThrow(() -> new CustomException("Invalid credentials"));
		
		
	}
	
	public void jwtBuildJws() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWTService jwtService = null;
		
		Mono<? extends OCTJWK> key = jwkService.oct().generator()
			.generate()
			.cache();

		JWTClaimsSet claims = JWTClaimsSet.of("joe", ZonedDateTime.now().plusYears(1).toEpochSecond())
			.addCustomClaim("http://example.com/is_root", true)
			.build();
		
		JWS<JWTClaimsSet> jwts = jwtService.jwsBuilder(key)
			.header(header -> header
				.algorithm("HS256")
				.type("JWT")
			)
			.payload(claims)
			.build()
			.block();
		
		// eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLCJleHAiOjEzMDA4MTkzODAsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.hchSJqZw74lwai4QQ6fZ7oEmXgFSdH74ISux2ukgyrY
		String jwtsCompact = jwts.toCompact();
		
		System.out.println(jwtsCompact);
	}
	
	public void jwtBuildJwe() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWTService jwtService = null;
		
		Mono<? extends ECJWK> key = jwkService.ec().generator()
			.keyId("keyId")
			.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
			.curve(ECCurve.P_256.getCurve())
			.generate()
			.cache();
		
		JWTClaimsSet claims = JWTClaimsSet.of("joe", ZonedDateTime.now().plusYears(1).toEpochSecond())
			.addCustomClaim("http://example.com/is_root", true)
			.build();
		
		JWE<JWTClaimsSet> jwte = jwtService.jweBuilder(key)
			.header(header -> header
				.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
				.encryptionAlgorithm(OCTAlgorithm.A256GCM.getAlgorithm())
				.type("JWT")
			)
			.payload(claims)
			.build()
			.block();
		
		// eyJlbmMiOiJBMjU2R0NNIiwidHlwIjoiSldUIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJjcnYiOiJQLTI1NiIsIngiOiJvaGxQbXBPMEhxOW02SXFobVZLRDVleDhnMzd1NGtuWUJuUUNKUmFDc3RVIiwieSI6ImRSa25UeE5IYVZUclhfWFVxNHFadi1WcFY2cFI1RU9LaGtuakRYNUtvVGciLCJrdHkiOiJFQyJ9fQ..e0sdLDEvAr4AXoKK.VFxHmiWYA6G0s0eA0Ln7EZ5GlEHrT8_t9eGeTUHYckRo4H2NlQ58hoL_lgqBY1U0pBHJpVTxbDY1o58CObSsaQ.U06UpH2BtdMALdnOiKEs7A
		String jwteCompact = jwte.toCompact();
		
		System.out.println(jwteCompact);
	}
	
	
	public void jwtReadJws() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWTService jwtService = null;

		Mono<? extends OCTJWK> key = jwkService.oct().builder()
		    .keyId("keyId")
		    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
		    .keyValue("xqf1haCsSJGuueZivcq4YafdWw6n5CH2BTT6vDwUSaM")
		    .build()
		    .cache();
		
		JWTClaimsSet claims = JWTClaimsSet.of("joe", ZonedDateTime.now().plusYears(1).toEpochSecond())
			.addCustomClaim("http://example.com/is_root", true)
			.build();
		
		JWS<JWTClaimsSet> jwts = jwtService.jwsBuilder(key)
			.header(header -> header
				.algorithm("HS256")
				.type("JWT")
			)
			.payload(claims)
			.build()
			.block();
		
		// eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLCJleHAiOjEzMDA4MTkzODAsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.hchSJqZw74lwai4QQ6fZ7oEmXgFSdH74ISux2ukgyrY
		String jwtsCompact = jwts.toCompact();
		
		jwtsCompact = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."
			+ "eyJpc3MiOiJqb2UiLCJleHAiOjE2OTExMzMyMTMsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ."
			+ "4p0_3W8DBrjTpw2e2KI1__v-6QOT_5dWIMKbfsSvTo0";
		
		JWTClaimsSet validClaims = jwtService.jwsReader(key)
			.read(jwtsCompact)
			.map(JWS::getPayload)
			.filter(JWTClaimsSet::isValid)
			.block();
	}
	
	public void jwtReadJwe() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWTService jwtService = null;
		
		Mono<? extends ECJWK> key = jwkService.ec().builder()
			.keyId("keyId")
			.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate("a9HrKi7kwXR0EumziK_B5ZRlsk7QbXGPJfx_c3OGoZs")
			.yCoordinate("fixJ3kr2abu0huetFyhs0OMqd3_M6xMIKE8hr3FggOM")
			.eccPrivateKey("VCSeZseVoZ1E4TyWmRqD0nt5I_ipSbKfXcRHQSTPqUw")
			.build()
			.cache();
		
		JWTClaimsSet claims = JWTClaimsSet.of("joe", ZonedDateTime.now().plusYears(1).toEpochSecond())
			.addCustomClaim("http://example.com/is_root", true)
			.build();
		
		JWE<JWTClaimsSet> jwte = jwtService.jweBuilder(key)
			.header(header -> header
				.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
				.encryptionAlgorithm(OCTAlgorithm.A256GCM.getAlgorithm())
				.type("JWT")
			)
			.payload(claims)
			.build()
			.block();
		
		// eyJlbmMiOiJBMjU2R0NNIiwidHlwIjoiSldUIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJjcnYiOiJQLTI1NiIsIngiOiJ6bEczQzVwUEtZVG4yVHpiZlJZYm5KOTZTai0yNDJGeTlwVVRmUWN0MUlzIiwieSI6IkUyeE9hNnNlb0dJVHpkRHdxVjZlT2NIc2dzNmI2M082NlJVWXlsV2N6LTgiLCJrdHkiOiJFQyJ9fQ.._1eQRi8ukFZDwa27.WjPLHYGHu1zpg3QSbhB9ciraoRU7UXpeJJXz76UZAkwJ-rxEXwkimnflTnEymG_oK1i7hKwCANRhqWwr22GqNg.Zos43NFBxdh_brO1ae-7vA
		String jwteCompact = jwte.toCompact();

		System.out.println(jwteCompact);
		
		jwteCompact = "eyJlbmMiOiJBMjU2R0NNIiwidHlwIjoiSldUIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJjcnYiOiJQLTI1NiIsIngiOiJ6bEczQzVwUEtZVG4yVHpiZlJZYm5KOTZTai0yNDJGeTlwVVRmUWN0MUlzIiwieSI6IkUyeE9hNnNlb0dJVHpkRHdxVjZlT2NIc2dzNmI2M082NlJVWXlsV2N6LTgiLCJrdHkiOiJFQyJ9fQ."
				+ "."
				+ "_1eQRi8ukFZDwa27."
				+ "WjPLHYGHu1zpg3QSbhB9ciraoRU7UXpeJJXz76UZAkwJ-rxEXwkimnflTnEymG_oK1i7hKwCANRhqWwr22GqNg."
				+ "Zos43NFBxdh_brO1ae-7vA";
		
		JWTClaimsSet validClaims = jwtService.jweReader(key)
			.read(jwteCompact)
			.map(JWE::getPayload)
			.filter(JWTClaimsSet::isValid)
			.block();
		
		System.out.println(validClaims);
	}
	
	public void mediaTypeConverters_jwkSet() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWEService jweService = null;
		
		OCTJWK key1 = jwkService.oct().builder()
		    .keyId("key1")
		    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
		    .keyValue("GkilETj3L4jpinuRiaNq6zd7-_1JPbfU9DY3xHl9HEE")
		    .build()
		    .block();

		OCTJWK key2 = jwkService.oct().builder()
		    .keyId("key2")
		    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
		    .keyValue("xqf1haCsSJGuueZivcq4YafdWw6n5CH2BTT6vDwUSaM")
		    .build()
		    .block();

		JWKSet jwkSet = new JWKSet(key1, key2);

		Mono<? extends ECJWK> key = jwkService.ec().builder()
			.keyId("keyId")
			.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate("a9HrKi7kwXR0EumziK_B5ZRlsk7QbXGPJfx_c3OGoZs")
			.yCoordinate("fixJ3kr2abu0huetFyhs0OMqd3_M6xMIKE8hr3FggOM")
			.eccPrivateKey("VCSeZseVoZ1E4TyWmRqD0nt5I_ipSbKfXcRHQSTPqUw")
			.build()
			.cache();

		JWE<JWKSet> jwe = jweService.builder(JWKSet.class, key)
		    .header(header -> header
		        .algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
		        .encryptionAlgorithm(OCTAlgorithm.A128GCM.getAlgorithm())
		        .contentType(MediaTypes.APPLICATION_JWK_SET_JSON)
		    )
		    .payload(jwkSet)
		    .build()
		    .block();

		// eyJlbmMiOiJBMTI4R0NNIiwiY3R5IjoiYXBwbGljYXRpb24vandrLXNldCtqc29uIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJjcnYiOiJQLTI1NiIsIngiOiJPcW5NbjBKcDNQcGZ6VlFCQW1ZanU2MVEwWUNkUHJuMkI3eW5ZdlRLN3FJIiwieSI6ImhZXzI2am9tS1QzX2QzaGQ2VVNRSm1zSjV5blBtaDN5QmRkZVdHbEs5ZDgiLCJrdHkiOiJFQyJ9fQ..XvpO0GyH44d8GeWc.5aV-epA4DaoWAD84EyYqFnaFv2HtQJlNF33jwSIuxHaMG0nK1Cm6yKcdzzC4e1pG1FNY7wg9SI_JlkFDYqjp6EuMe64vFUOiPCj28QtPaafEx7jOt5nbGNRvzBZJdDWQbhlZomXL7cKzLjfYpv8Y4SWPzcua6FJMSH7DoZwUZfKZDzDk_-2fpXvE_LLw7rTbi8Vltm9AClzmy2QS1tu5R4hY5E9Ew5QIWC06IErtldHF_y_oZIy7iSxf55GjgBVs0roFkA.OujlTScT9qOM6wWsFJMUlA
		String jweCompact = jwe.toCompact();
		
		jweCompact = "eyJlbmMiOiJBMTI4R0NNIiwiY3R5IjoiYXBwbGljYXRpb24vandrLXNldCtqc29uIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJjcnYiOiJQLTI1NiIsIngiOiJPcW5NbjBKcDNQcGZ6VlFCQW1ZanU2MVEwWUNkUHJuMkI3eW5ZdlRLN3FJIiwieSI6ImhZXzI2am9tS1QzX2QzaGQ2VVNRSm1zSjV5blBtaDN5QmRkZVdHbEs5ZDgiLCJrdHkiOiJFQyJ9fQ."
			+ "."
			+ "XvpO0GyH44d8GeWc."
			+ "5aV-epA4DaoWAD84EyYqFnaFv2HtQJlNF33jwSIuxHaMG0nK1Cm6yKcdzzC4e1pG1FNY7wg9SI_JlkFDYqjp6EuMe64vFUOiPCj28QtPaafEx7jOt5nbGNRvzBZJdDWQbhlZomXL7cKzLjfYpv8Y4SWPzcua6FJMSH7DoZwUZfKZDzDk_-2fpXvE_LLw7rTbi8Vltm9AClzmy2QS1tu5R4hY5E9Ew5QIWC06IErtldHF_y_oZIy7iSxf55GjgBVs0roFkA."
			+ "OujlTScT9qOM6wWsFJMUlA";
	
		jwkSet = jweService.reader(JWKSet.class, key)
			.read(jweCompact)
			.map(JWE::getPayload)
			.block();
	}
	
	public void mediaTypeConverters_jwejws() {
		// Injected or obtained from a 'Jose' instance
		JWKService jwkService = null;
		JWSService jwsService = null;
		JWEService jweService = null;
		
		jwkService.oct().builder()
		    .keyId("jwsKey")
		    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
		    .keyValue("xqf1haCsSJGuueZivcq4YafdWw6n5CH2BTT6vDwUSaM")
		    .build()
		    .map(JWK::trust)
		    .flatMap(jwkService.store()::set)
		    .block();
	
		jwkService.oct().builder()
		    .keyId("jweKey")
		    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
		    .keyValue("GkilETj3L4jpinuRiaNq6zd7-_1JPbfU9DY3xHl9HEE")
		    .build()
		    .map(JWK::trust)
		    .flatMap(jwkService.store()::set)
		    .block();
		
		/*JWS<Message> jws = jwsService.builder(Message.class)
			.header(header -> header
				.keyId("jwsKey")
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				.contentType(MediaTypes.APPLICATION_JSON)
			)
			.payload(new Message("Marcel", "Finally!"))
			.build()
			.block();
		
		// eyJhbGciOiJIUzI1NiIsImtpZCI6Imp3c0tleSIsImN0eSI6ImFwcGxpY2F0aW9uL2pzb24ifQ.eyJhdXRob3IiOiJNYXJjZWwiLCJtZXNzYWdlIjoiRmluYWxseSEifQ.wjnBucCNvQXHtL8QBWuXbutRECtIhazISQhR0NfYOQs
		String jwsCompact = jws.toCompact();*/
		
		String jwsCompact = "eyJhbGciOiJIUzI1NiIsImtpZCI6Imp3c0tleSIsImN0eSI6ImFwcGxpY2F0aW9uL2pzb24ifQ."
			+ "eyJhdXRob3IiOiJNYXJjZWwiLCJtZXNzYWdlIjoiRmluYWxseSEifQ."
			+ "wjnBucCNvQXHtL8QBWuXbutRECtIhazISQhR0NfYOQs";
		
		JWS<Message> block = jwsService.reader(Message.class)
			.read(jwsCompact)
			.block();
		
		JWE<JWS<Message>> jwe = jwsService.reader(Message.class)
			.read(jwsCompact)
			.flatMap(jws -> jweService.<JWS<Message>>builder(Types.type(JWS.class).type(Message.class).and().build())
				.header(header -> header
					.keyId("jweKey")
					.algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
					.contentType(MediaTypes.APPLICATION_JOSE)
				)
				.payload(jws)
				.build()
			)
			.block();
		
		String jweCompact = jwe.toCompact();
		
		jweCompact = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoiandlS2V5IiwiY3R5IjoiYXBwbGljYXRpb24vam9zZSIsInRhZyI6IlBjT2tjZWNsNUswaW92a2hnMEhwUEEiLCJpdiI6InFNMUtfeHhIcmZocXFuRFMifQ."
			+ "LLn2scpDiAdRRSFIrvTXTXsVwQp9mSH4dPv1I-IruFM."
			+ "LfCNkDe5r3eE2Kjadmpkww."
			+ "5AjCbDExRhRsLy-iXX2RAavfXVWFEcinKcXu3t_BObnC4mzgxmaqvfwUC8QMu8KM8C3gjt36Qa89nqajVYmJwRrZ0ZMoH68JgXvp2npIEdJSruL3CqTHm3ObK5-7TbYLP1K3t9v995wOIAajUsXaHfpNODqAsFlc83A6wwxv37WVq4mWy-WZ7ZwIpwHY5semqMxv0FbpNMPtkLaG0JzqYLnzH7yaT2DSBQKIxlCZ0hc."
			+ "ZML3thQjah7dtXdv17LJXA";
		
		Message message = jweService.<JWS<Message>>reader(Types.type(JWS.class).type(Message.class).and().build())
			.read(jweCompact)
			.map(JWE::getPayload)
			.map(JWS::getPayload)
			.block();
		
	}
	
	@Wrapper
	@Bean
	public static class JWKStoreWrapper implements Supplier<JWKStore> {

		@Override
		public JWKStore get() {
			return new InMemoryJWKStore();
		}
	}
	
	public static class Message {

		private String author;

		private String message;

		public Message() {

		}

		public Message(String author, String message) {
			super();
			this.author = author;
			this.message = message;
		}

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public String toString() {
			return "Message [author=" + author + ", message=" + message + "]";
		}
	}
}
