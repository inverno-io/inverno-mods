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
package io.inverno.mod.security.jose.internal.jwk.oct;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKKeyResolver;
import io.inverno.mod.security.jose.internal.jwk.NoOpJWKStore;
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * <p>
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class OCTJWKTest {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7515#appendix-A.1">RFC7515 Appendix A.1</a> Example JWS Using HMAC SHA-256
	 * 
	 * {
	 *     "kty":"oct",
     *     "k":"AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
     * }
	 */
	@Test
	public void testRFC7515_A1() {
		OCTJWK jwk = octJWKBuilder()
			.keyValue("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
			.build()
			.block();
		
		String jws = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
		String[] jws_parts = jws.split("\\.");
		
		Assertions.assertTrue(jwk.signer("HS256").verify((jws_parts[0]+"."+jws_parts[1]).getBytes(), Base64.getUrlDecoder().decode(jws_parts[2])));
	}
	
	@Test
	public void testSymmetricGenerator() throws IOException {
		OCTJWK jwk = new GenericOCTJWKGenerator().keyId("myNewKey").generate().block();
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		String payload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
		String payload64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
		
		// HS256
		String jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"HS256\"}";
		String jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		String m = jose64 + "." + payload64;
		
		byte[] signature = jwk.signer("HS256").sign(m.getBytes());
		Assertions.assertEquals(32, signature.length);
		Assertions.assertTrue(jwk.signer("HS256").verify(m.getBytes(), signature));
		
		// HS384
		jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"HS384\"}";
		jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		m = jose64 + "." + payload64;
		
		signature = jwk.signer("HS384").sign(m.getBytes());
		Assertions.assertEquals(48, signature.length);
		Assertions.assertTrue(jwk.signer("HS384").verify(m.getBytes(), signature));
		
		// HS512
		jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"HS512\"}";
		jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		m = jose64 + "." + payload64;
		
		signature = jwk.signer("HS512").sign(m.getBytes());
		Assertions.assertEquals(64, signature.length);
		Assertions.assertTrue(jwk.signer("HS512").verify(m.getBytes(), signature));
	}
	
	@Test
	public void testRFC7516_A1() {
		byte[] data = new byte[] { (byte)84, (byte)104, (byte)101, (byte)32, (byte)116, (byte)114, (byte)117, (byte)101, (byte)32, (byte)115, (byte)105, (byte)103, (byte)110, (byte)32, (byte)111, (byte)102, (byte)32, (byte)105, (byte)110, (byte)116, (byte)101, (byte)108, (byte)108, (byte)105, (byte)103, (byte)101, (byte)110, (byte)99, (byte)101, (byte)32, (byte)105, (byte)115, (byte)32, (byte)110, (byte)111, (byte)116, (byte)32, (byte)107, (byte)110, (byte)111, (byte)119, (byte)108, (byte)101, (byte)100, (byte)103, (byte)101, (byte)32, (byte)98, (byte)117, (byte)116, (byte)32, (byte)105, (byte)109, (byte)97, (byte)103, (byte)105, (byte)110, (byte)97, (byte)116, (byte)105, (byte)111, (byte)110, (byte)46 };
		byte[] cek = new byte[] { (byte)177, (byte)161, (byte)244, (byte)128, (byte)84, (byte)143, (byte)225, (byte)115, (byte)63, (byte)180, (byte)3, (byte)255, (byte)107, (byte)154, (byte)212, (byte)246, (byte)138, (byte)7, (byte)110, (byte)91, (byte)112, (byte)46, (byte)34, (byte)105, (byte)47, (byte)130, (byte)203, (byte)46, (byte)122, (byte)234, (byte)64, (byte)252 };
		byte[] iv = new byte[] { (byte)227, (byte)197, (byte)117, (byte)252, (byte)2, (byte)219, (byte)233, (byte)68, (byte)180, (byte)225, (byte)77, (byte)219 };
		byte[] aad = new byte[] { (byte)101, (byte)121, (byte)74, (byte)104, (byte)98, (byte)71, (byte)99, (byte)105, (byte)79, (byte)105, (byte)74, (byte)83, (byte)85, (byte)48, (byte)69, (byte)116, (byte)84, (byte)48, (byte)70, (byte)70, (byte)85, (byte)67, (byte)73, (byte)115, (byte)73, (byte)109, (byte)86, (byte)117, (byte)89, (byte)121, (byte)73, (byte)54, (byte)73, (byte)107, (byte)69, (byte)121, (byte)78, (byte)84, (byte)90, (byte)72, (byte)81, (byte)48, (byte)48, (byte)105, (byte)102, (byte)81 };

		String enc = "A256GCM";
		String k = Base64.getUrlEncoder().withoutPadding().encodeToString(cek);
		
		OCTJWK jwk = octJWKBuilder()
			.algorithm(OCTAlgorithm.A256GCM.getAlgorithm())
			.keyValue(k)
			.build()
			.block();
		
		SecureRandom secureRandom = new SecureRandom() {
			@Override
			public void nextBytes(byte[] bytes) {
				System.arraycopy(iv, 0, bytes, 0, iv.length);
			}
		};
		
		JWACipher.EncryptedData encrypt = jwk.cipher(enc).encrypt(data, aad, secureRandom);
		
		byte[] cipherText = new byte[] { (byte)229, (byte)236, (byte)166, (byte)241, (byte)53, (byte)191, (byte)115, (byte)196, (byte)174, (byte)43, (byte)73, (byte)109, (byte)39, (byte)122, (byte)233, (byte)96, (byte)140, (byte)206, (byte)120, (byte)52, (byte)51, (byte)237, (byte)48, (byte)11, (byte)190, (byte)219, (byte)186, (byte)80, (byte)111, (byte)104, (byte)50, (byte)142, (byte)47, (byte)167, (byte)59, (byte)61, (byte)181, (byte)127, (byte)196, (byte)21, (byte)40, (byte)82, (byte)242, (byte)32, (byte)123, (byte)143, (byte)168, (byte)226, (byte)73, (byte)216, (byte)176, (byte)144, (byte)138, (byte)247, (byte)106, (byte)60, (byte)16, (byte)205, (byte)160, (byte)109, (byte)64, (byte)63, (byte)192 };
		byte[] authenticationTag = new byte[] { (byte)92, (byte)80, (byte)104, (byte)49, (byte)133, (byte)25, (byte)161, (byte)215, (byte)173, (byte)101, (byte)219, (byte)211, (byte)136, (byte)91, (byte)210, (byte)145 };
		
		Assertions.assertArrayEquals(cipherText, encrypt.getCipherText());
		Assertions.assertArrayEquals(authenticationTag, encrypt.getAuthenticationTag());
		
		Assertions.assertArrayEquals(data, jwk.cipher(enc).decrypt(cipherText, aad, iv, authenticationTag));
	}
	
	@Test
	public void testRFC7516_A2() {
		byte[] data = new byte[] { (byte)76, (byte)105, (byte)118, (byte)101, (byte)32, (byte)108, (byte)111, (byte)110, (byte)103, (byte)32, (byte)97, (byte)110, (byte)100, (byte)32, (byte)112, (byte)114, (byte)111, (byte)115, (byte)112, (byte)101, (byte)114, (byte)46 };
		byte[] cek = new byte[] { (byte)4, (byte)211, (byte)31, (byte)197, (byte)84, (byte)157, (byte)252, (byte)254, (byte)11, (byte)100, (byte)157, (byte)250, (byte)63, (byte)170, (byte)106, (byte)206, (byte)107, (byte)124, (byte)212, (byte)45, (byte)111, (byte)107, (byte)9, (byte)219, (byte)200, (byte)177, (byte)0, (byte)240, (byte)143, (byte)156, (byte)44, (byte)207 };
		byte[] iv = new byte[] { (byte)3, (byte)22, (byte)60, (byte)12, (byte)43, (byte)67, (byte)104, (byte)105, (byte)108, (byte)108, (byte)105, (byte)99, (byte)111, (byte)116, (byte)104, (byte)101 };
		byte[] aad = new byte[] { (byte)101, (byte)121, (byte)74, (byte)104, (byte)98, (byte)71, (byte)99, (byte)105, (byte)79, (byte)105, (byte)74, (byte)83, (byte)85, (byte)48, (byte)69, (byte)120, (byte)88, (byte)122, (byte)85, (byte)105, (byte)76, (byte)67, (byte)74, (byte)108, (byte)98, (byte)109, (byte)77, (byte)105, (byte)79, (byte)105, (byte)74, (byte)66, (byte)77, (byte)84, (byte)73, (byte)52, (byte)81, (byte)48, (byte)74, (byte)68, (byte)76, (byte)85, (byte)104, (byte)84, (byte)77, (byte)106, (byte)85, (byte)50, (byte)73, (byte)110, (byte)48 };

		String enc = "A128CBC-HS256";
		String k = Base64.getUrlEncoder().withoutPadding().encodeToString(cek);
		
		OCTJWK jwk = octJWKBuilder()
			.algorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
			.keyValue(k)
			.build()
			.block();
		
		SecureRandom secureRandom = new SecureRandom() {
			@Override
			public void nextBytes(byte[] bytes) {
				System.arraycopy(iv, 0, bytes, 0, iv.length);
			}
		};
		
		JWACipher.EncryptedData encrypt = jwk.cipher(enc).encrypt(data, aad, secureRandom);

		byte[] cipherText = new byte[] { (byte)40, (byte)57, (byte)83, (byte)181, (byte)119, (byte)33, (byte)133, (byte)148, (byte)198, (byte)185, (byte)243, (byte)24, (byte)152, (byte)230, (byte)6, (byte)75, (byte)129, (byte)223, (byte)127, (byte)19, (byte)210, (byte)82, (byte)183, (byte)230, (byte)168, (byte)33, (byte)215, (byte)104, (byte)143, (byte)112, (byte)56, (byte)102 };
		byte[] authenticationTag = new byte[] { (byte)246, (byte)17, (byte)244, (byte)190, (byte)4, (byte)95, (byte)98, (byte)3, (byte)231, (byte)0, (byte)115, (byte)157, (byte)242, (byte)203, (byte)100, (byte)191 };
		
		Assertions.assertArrayEquals(cipherText, encrypt.getCipherText());
		Assertions.assertArrayEquals(authenticationTag, encrypt.getAuthenticationTag());
		
		Assertions.assertArrayEquals(data, jwk.cipher(enc).decrypt(cipherText, aad, iv, authenticationTag));
	}
	
	private GenericOCTJWKBuilder octJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
	
		return new GenericOCTJWKBuilder(configuration, jwkStore, keyResolver);
	}
}
