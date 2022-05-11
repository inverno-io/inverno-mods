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
package io.inverno.mod.security.jose.internal.jwe;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.converter.GenericDataConversionService;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKKeyResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKService;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKURLResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericX509JWKCertPathValidator;
import io.inverno.mod.security.jose.internal.jwk.JWKPKIXParameters;
import io.inverno.mod.security.jose.internal.jwk.NoOpJWKStore;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWK;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWK;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericEdECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWK;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWK;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWKFactory;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWK;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWKFactory;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEHeader;
import io.inverno.mod.security.jose.jwe.JsonJWE;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericJWEServiceTest {
	
	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final ExecutorService WORKER_POOL = Executors.newCachedThreadPool();
	
	@Test
	public void testRFC7516_A1() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		GenericRSAJWKBuilder rsaJWKBuilder = rsaJWKBuilder(mapper());
		
		String payload = "The true sign of intelligence is not knowledge but imagination.";
		String compact = "eyJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkEyNTZHQ00ifQ.OKOawDo13gRp2ojaHV7LFpZcgV7T6DVZKTyKOMTYUmKoTCVJRgckCL9kiMT03JGeipsEdY3mx_etLbbWSrFr05kLzcSr4qKAq7YN7e9jwQRb23nfa6c9d-StnImGyFDbSv04uVuxIp5Zms1gNxKKK2Da14B8S4rzVRltdYwam_lDp5XnZAYpQdb76FdIKLaVmqgfwX7XWRxv2322i-vDxRfqNzo_tETKzpVLzfiwQyeyPGLBIO56YJ7eObdv0je81860ppamavo35UgoRdbYaBcoh9QcfylQr66oc6vFWXRcZ_ZT2LawVCWTIy3brGPi6UklfCpIMfIjf7iGdXKHzg.48V1_ALb6US04U3b.5eym8TW_c8SuK0ltJ3rpYIzOeDQz7TALvtu6UG9oMo4vpzs9tX_EFShS8iB7j6jiSdiwkIr3ajwQzaBtQD_A.XFBoMYUZodetZdvTiFvSkQ";
		String[] splitCompact = compact.split("\\.");
		
		// cek = 32 bytes
		byte[] cek = new byte[]{ (byte)177, (byte)161, (byte)244, (byte)128, (byte)84, (byte)143, (byte)225, (byte)115, (byte)63, (byte)180, (byte)3, (byte)255, (byte)107, (byte)154, (byte)212, (byte)246, (byte)138, (byte)7, (byte)110, (byte)91, (byte)112, (byte)46, (byte)34, (byte)105, (byte)47, (byte)130, (byte)203, (byte)46, (byte)122, (byte)234, (byte)64, (byte)252 };
		// iv = 12 bytes
		byte[] iv = new byte[]{ (byte)227, (byte)197, (byte)117, (byte)252, (byte)2, (byte)219, (byte)233, (byte)68, (byte)180, (byte)225, (byte)77, (byte)219 };
		
		
		
		//{
		//	"kty": "RSA",
		//	"n": "oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw",
		//	"e": "AQAB",
		//	"d": "kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ",
		//	"p": "1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0",
		//	"q": "wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc",
		//	"dp": "ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE",
		//	"dq": "Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis",
		//	"qi": "VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY"
		//}
		Mono<GenericRSAJWK> key = rsaJWKBuilder
			.modulus("oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw")
			.publicExponent("AQAB")
			.privateExponent("kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ")
			.firstPrimeFactor("1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0")
			.secondPrimeFactor("wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc")
			.firstFactorExponent("ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE")
			.secondFactorExponent("Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis")
			.firstCoefficient("VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY")
			.build();
		
		SecureRandom secureRandom = new SecureRandom() {
			@Override
			public void nextBytes(byte[] bytes) {
				if(bytes.length == 32) {
					System.arraycopy(cek, 0, bytes, 0, cek.length);
					return;
				}
				else if(bytes.length == 12) {
					System.arraycopy(iv, 0, bytes, 0, iv.length);
					return;
				}
				super.nextBytes(bytes);
			}
		};
		
		// {"alg":"RSA-OAEP","enc":"A256GCM"}
		JWE<String> jwe = jweService.builder(String.class, key)
			.secureRandom(secureRandom)
			.header(header -> header
				.algorithm("RSA-OAEP")
				.encryptionAlgorithm("A256GCM")
				.contentType("text/plain")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// RSA-OAEP uses random value therefore result is non deterministic, so we can't check equality for eveything
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("RSA-OAEP", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("text/plain", jwe.getHeader().getContentType());
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("A256GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		Assertions.assertNotNull(jwe.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jweSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwe.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertEquals(splitCompact[3], jweSplitCompact[3]);
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		Assertions.assertArrayEquals(payload.getBytes(), jweService.reader(String.class, key).read(jwe.toCompact(), "text/plain").block().getPayload().getBytes());
		
		// Read representation from RFC (https://datatracker.ietf.org/doc/html/rfc7516#appendix-A.2.7)
		jwe = jweService.reader(String.class, key).read(compact, "text/plain").block();
		jweSplitCompact = jwe.toCompact().split("\\.");
		
		// We must have the same representation
		Assertions.assertEquals(compact, jwe.toCompact());
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("RSA-OAEP", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getContentType());
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("A256GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		Assertions.assertNotNull(jwe.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jweSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwe.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertEquals(splitCompact[3], jweSplitCompact[3]);
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	public void testRFC7516_A2() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		GenericRSAJWKBuilder rsaJWKBuilder = rsaJWKBuilder(mapper());
		
		String payload = "Live long and prosper.";
		String compact = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.UGhIOguC7IuEvf_NPVaXsGMoLOmwvc1GyqlIKOK1nN94nHPoltGRhWhw7Zx0-kFm1NJn8LE9XShH59_i8J0PH5ZZyNfGy2xGdULU7sHNF6Gp2vPLgNZ__deLKxGHZ7PcHALUzoOegEI-8E66jX2E4zyJKx-YxzZIItRzC5hlRirb6Y5Cl_p-ko3YvkkysZIFNPccxRU7qve1WYPxqbb2Yw8kZqa2rMWI5ng8OtvzlV7elprCbuPhcCdZ6XDP0_F8rkXds2vE4X-ncOIM8hAYHHi29NX0mcKiRaD0-D-ljQTP-cFPgwCp6X-nZZd9OHBv-B3oWh2TbqmScqXMR4gp_A.AxY8DCtDaGlsbGljb3RoZQ.KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY.9hH0vgRfYgPnAHOd8stkvw";
		String[] splitCompact = compact.split("\\.");
		
		// cek = 32 bytes
		byte[] cek = new byte[]{ (byte)4, (byte)211, (byte)31, (byte)197, (byte)84, (byte)157, (byte)252, (byte)254, (byte)11, (byte)100, (byte)157, (byte)250, (byte)63, (byte)170, (byte)106, (byte)206, (byte)107, (byte)124, (byte)212, (byte)45, (byte)111, (byte)107, (byte)9, (byte)219, (byte)200, (byte)177, (byte)0, (byte)240, (byte)143, (byte)156, (byte)44, (byte)207 };
		// iv = 16 bytes
		byte[] iv = new byte[]{ (byte)3, (byte)22, (byte)60, (byte)12, (byte)43, (byte)67, (byte)104, (byte)105, (byte)108, (byte)108, (byte)105, (byte)99, (byte)111, (byte)116, (byte)104, (byte)101 };
		
		//{
		//	"kty": "RSA",
		//	"n": "sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw",
		//	"e": "AQAB",
		//	"d": "VFCWOqXr8nvZNyaaJLXdnNPXZKRaWCjkU5Q2egQQpTBMwhprMzWzpR8Sxq1OPThh_J6MUD8Z35wky9b8eEO0pwNS8xlh1lOFRRBoNqDIKVOku0aZb-rynq8cxjDTLZQ6Fz7jSjR1Klop-YKaUHc9GsEofQqYruPhzSA-QgajZGPbE_0ZaVDJHfyd7UUBUKunFMScbflYAAOYJqVIVwaYR5zWEEceUjNnTNo_CVSj-VvXLO5VZfCUAVLgW4dpf1SrtZjSt34YLsRarSb127reG_DUwg9Ch-KyvjT1SkHgUWRVGcyly7uvVGRSDwsXypdrNinPA4jlhoNdizK2zF2CWQ",
		//	"p": "9gY2w6I6S6L0juEKsbeDAwpd9WMfgqFoeA9vEyEUuk4kLwBKcoe1x4HG68ik918hdDSE9vDQSccA3xXHOAFOPJ8R9EeIAbTi1VwBYnbTp87X-xcPWlEPkrdoUKW60tgs1aNd_Nnc9LEVVPMS390zbFxt8TN_biaBgelNgbC95sM",
		//	"q": "uKlCKvKv_ZJMVcdIs5vVSU_6cPtYI1ljWytExV_skstvRSNi9r66jdd9-yBhVfuG4shsp2j7rGnIio901RBeHo6TPKWVVykPu1iYhQXw1jIABfw-MVsN-3bQ76WLdt2SDxsHs7q7zPyUyHXmps7ycZ5c72wGkUwNOjYelmkiNS0",
		//	"dp": "w0kZbV63cVRvVX6yk3C8cMxo2qCM4Y8nsq1lmMSYhG4EcL6FWbX5h9yuvngs4iLEFk6eALoUS4vIWEwcL4txw9LsWH_zKI-hwoReoP77cOdSL4AVcraHawlkpyd2TWjE5evgbhWtOxnZee3cXJBkAi64Ik6jZxbvk-RR3pEhnCs",
		//	"dq": "o_8V14SezckO6CNLKs_btPdFiO9_kC1DsuUTd2LAfIIVeMZ7jn1Gus_Ff7B7IVx3p5KuBGOVF8L-qifLb6nQnLysgHDh132NDioZkhH7mI7hPG-PYE_odApKdnqECHWw0J-F0JWnUd6D2B_1TvF9mXA2Qx-iGYn8OVV1Bsmp6qU",
		//	"qi": "eNho5yRBEBxhGBtQRww9QirZsB66TrfFReG_CcteI1aCneT0ELGhYlRlCtUkTRclIfuEPmNsNDPbLoLqqCVznFbvdB7x-Tl-m0l_eFTj2KiqwGqE9PZB9nNTwMVvH3VRRSLWACvPnSiwP8N5Usy-WRXS-V7TbpxIhvepTfE0NNo"
		//}
		Mono<GenericRSAJWK> key = rsaJWKBuilder
			.modulus("sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw")
			.publicExponent("AQAB")
			.privateExponent("VFCWOqXr8nvZNyaaJLXdnNPXZKRaWCjkU5Q2egQQpTBMwhprMzWzpR8Sxq1OPThh_J6MUD8Z35wky9b8eEO0pwNS8xlh1lOFRRBoNqDIKVOku0aZb-rynq8cxjDTLZQ6Fz7jSjR1Klop-YKaUHc9GsEofQqYruPhzSA-QgajZGPbE_0ZaVDJHfyd7UUBUKunFMScbflYAAOYJqVIVwaYR5zWEEceUjNnTNo_CVSj-VvXLO5VZfCUAVLgW4dpf1SrtZjSt34YLsRarSb127reG_DUwg9Ch-KyvjT1SkHgUWRVGcyly7uvVGRSDwsXypdrNinPA4jlhoNdizK2zF2CWQ")
			.firstPrimeFactor("9gY2w6I6S6L0juEKsbeDAwpd9WMfgqFoeA9vEyEUuk4kLwBKcoe1x4HG68ik918hdDSE9vDQSccA3xXHOAFOPJ8R9EeIAbTi1VwBYnbTp87X-xcPWlEPkrdoUKW60tgs1aNd_Nnc9LEVVPMS390zbFxt8TN_biaBgelNgbC95sM")
			.secondPrimeFactor("uKlCKvKv_ZJMVcdIs5vVSU_6cPtYI1ljWytExV_skstvRSNi9r66jdd9-yBhVfuG4shsp2j7rGnIio901RBeHo6TPKWVVykPu1iYhQXw1jIABfw-MVsN-3bQ76WLdt2SDxsHs7q7zPyUyHXmps7ycZ5c72wGkUwNOjYelmkiNS0")
			.firstFactorExponent("w0kZbV63cVRvVX6yk3C8cMxo2qCM4Y8nsq1lmMSYhG4EcL6FWbX5h9yuvngs4iLEFk6eALoUS4vIWEwcL4txw9LsWH_zKI-hwoReoP77cOdSL4AVcraHawlkpyd2TWjE5evgbhWtOxnZee3cXJBkAi64Ik6jZxbvk-RR3pEhnCs")
			.secondFactorExponent("o_8V14SezckO6CNLKs_btPdFiO9_kC1DsuUTd2LAfIIVeMZ7jn1Gus_Ff7B7IVx3p5KuBGOVF8L-qifLb6nQnLysgHDh132NDioZkhH7mI7hPG-PYE_odApKdnqECHWw0J-F0JWnUd6D2B_1TvF9mXA2Qx-iGYn8OVV1Bsmp6qU")
			.firstCoefficient("eNho5yRBEBxhGBtQRww9QirZsB66TrfFReG_CcteI1aCneT0ELGhYlRlCtUkTRclIfuEPmNsNDPbLoLqqCVznFbvdB7x-Tl-m0l_eFTj2KiqwGqE9PZB9nNTwMVvH3VRRSLWACvPnSiwP8N5Usy-WRXS-V7TbpxIhvepTfE0NNo")
			.build();
		
		SecureRandom secureRandom = new SecureRandom() {
			@Override
			public void nextBytes(byte[] bytes) {
				if(bytes.length == 32) {
					System.arraycopy(cek, 0, bytes, 0, cek.length);
					return;
				}
				else if(bytes.length == 16) {
					System.arraycopy(iv, 0, bytes, 0, iv.length);
					return;
				}
				super.nextBytes(bytes);
			}
		};
		
		// {"alg":"RSA1_5","enc":"A128CBC-HS256"}
		JWE<String> jwe = jweService.builder(String.class, key)
			.secureRandom(secureRandom)
			.header(header -> header
				.algorithm("RSA1_5")
				.encryptionAlgorithm("A128CBC-HS256")
				.contentType("text/plain")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// RSA1_5 uses random value therefore result is non deterministic, so we can't check equality for eveything
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("RSA1_5", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("text/plain", jwe.getHeader().getContentType());
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		Assertions.assertNotNull(jwe.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jweSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwe.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertEquals(splitCompact[3], jweSplitCompact[3]);
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		Assertions.assertArrayEquals(payload.getBytes(), jweService.reader(String.class, key).read(jwe.toCompact(), "text/plain").block().getPayload().getBytes());
		
		// Read representation from RFC (https://datatracker.ietf.org/doc/html/rfc7516#appendix-A.2.7)
		jwe = jweService.reader(String.class, key).read(compact, "text/plain").block();
		jweSplitCompact = jwe.toCompact().split("\\.");
		
		// We must have the same representation
		Assertions.assertEquals(compact, jwe.toCompact());
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("RSA1_5", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getContentType());
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		Assertions.assertNotNull(jwe.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jweSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwe.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertEquals(splitCompact[3], jweSplitCompact[3]);
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	public void testRFC7516_A3() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		GenericOCTJWKBuilder octJWKBuilder = octJWKBuilder(mapper());
		
		String payload = "Live long and prosper.";
		String compact = "eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ.AxY8DCtDaGlsbGljb3RoZQ.KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY.U0m_YmjN04DJvceFICbCVQ";
		String[] splitCompact = compact.split("\\.");
		
		// cek = 32 bytes
		byte[] cek = new byte[]{ (byte)4, (byte)211, (byte)31, (byte)197, (byte)84, (byte)157, (byte)252, (byte)254, (byte)11, (byte)100, (byte)157, (byte)250, (byte)63, (byte)170, (byte)106, (byte)206, (byte)107, (byte)124, (byte)212, (byte)45, (byte)111, (byte)107, (byte)9, (byte)219, (byte)200, (byte)177, (byte)0, (byte)240, (byte)143, (byte)156, (byte)44, (byte)207 };
		// iv = 16 bytes
		byte[] iv = new byte[]{ (byte)3, (byte)22, (byte)60, (byte)12, (byte)43, (byte)67, (byte)104, (byte)105, (byte)108, (byte)108, (byte)105, (byte)99, (byte)111, (byte)116, (byte)104, (byte)101 };
		
		// {
		// 	"kty":"oct",
		// 	"k":"GawgguFyGrWKav7AX4VKUg"
		// }
		Mono<GenericOCTJWK> key = octJWKBuilder
			.keyValue("GawgguFyGrWKav7AX4VKUg")
			.build();
		
		SecureRandom secureRandom = new SecureRandom() {
			@Override
			public void nextBytes(byte[] bytes) {
				if(bytes.length == 32) {
					System.arraycopy(cek, 0, bytes, 0, cek.length);
					return;
				}
				else if(bytes.length == 16) {
					System.arraycopy(iv, 0, bytes, 0, iv.length);
					return;
				}
				super.nextBytes(bytes);
			}
		};
		
		// {"alg":"A128KW","enc":"A128CBC-HS256"}
		JWE<String> jwe = jweService.builder(String.class, key)
			.secureRandom(secureRandom)
			.header(header -> header
				.algorithm("A128KW")
				.encryptionAlgorithm("A128CBC-HS256")
				.contentType("text/plain")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// We must have the same representation since algorithms are deterministic
		Assertions.assertEquals(compact, jwe.toCompact());
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("text/plain", jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		Assertions.assertNotNull(jwe.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jweSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwe.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		jwe = jweService.reader(String.class, key).read(jwe.toCompact(), "text/plain").block();
		
		Assertions.assertArrayEquals(payload.getBytes(), jwe.getPayload().getBytes());
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		Assertions.assertNotNull(jwe.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jweSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwe.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	public void testRFC7516_A4() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		ObjectMapper mapper = mapper();
		GenericRSAJWKBuilder rsaJWKBuilder = rsaJWKBuilder(mapper);
		GenericOCTJWKBuilder octJWKBuilder = octJWKBuilder(mapper);
		
		//{
		//	"kty": "RSA",
		//	"n": "sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw",
		//	"e": "AQAB",
		//	"d": "VFCWOqXr8nvZNyaaJLXdnNPXZKRaWCjkU5Q2egQQpTBMwhprMzWzpR8Sxq1OPThh_J6MUD8Z35wky9b8eEO0pwNS8xlh1lOFRRBoNqDIKVOku0aZb-rynq8cxjDTLZQ6Fz7jSjR1Klop-YKaUHc9GsEofQqYruPhzSA-QgajZGPbE_0ZaVDJHfyd7UUBUKunFMScbflYAAOYJqVIVwaYR5zWEEceUjNnTNo_CVSj-VvXLO5VZfCUAVLgW4dpf1SrtZjSt34YLsRarSb127reG_DUwg9Ch-KyvjT1SkHgUWRVGcyly7uvVGRSDwsXypdrNinPA4jlhoNdizK2zF2CWQ",
		//	"p": "9gY2w6I6S6L0juEKsbeDAwpd9WMfgqFoeA9vEyEUuk4kLwBKcoe1x4HG68ik918hdDSE9vDQSccA3xXHOAFOPJ8R9EeIAbTi1VwBYnbTp87X-xcPWlEPkrdoUKW60tgs1aNd_Nnc9LEVVPMS390zbFxt8TN_biaBgelNgbC95sM",
		//	"q": "uKlCKvKv_ZJMVcdIs5vVSU_6cPtYI1ljWytExV_skstvRSNi9r66jdd9-yBhVfuG4shsp2j7rGnIio901RBeHo6TPKWVVykPu1iYhQXw1jIABfw-MVsN-3bQ76WLdt2SDxsHs7q7zPyUyHXmps7ycZ5c72wGkUwNOjYelmkiNS0",
		//	"dp": "w0kZbV63cVRvVX6yk3C8cMxo2qCM4Y8nsq1lmMSYhG4EcL6FWbX5h9yuvngs4iLEFk6eALoUS4vIWEwcL4txw9LsWH_zKI-hwoReoP77cOdSL4AVcraHawlkpyd2TWjE5evgbhWtOxnZee3cXJBkAi64Ik6jZxbvk-RR3pEhnCs",
		//	"dq": "o_8V14SezckO6CNLKs_btPdFiO9_kC1DsuUTd2LAfIIVeMZ7jn1Gus_Ff7B7IVx3p5KuBGOVF8L-qifLb6nQnLysgHDh132NDioZkhH7mI7hPG-PYE_odApKdnqECHWw0J-F0JWnUd6D2B_1TvF9mXA2Qx-iGYn8OVV1Bsmp6qU",
		//	"qi": "eNho5yRBEBxhGBtQRww9QirZsB66TrfFReG_CcteI1aCneT0ELGhYlRlCtUkTRclIfuEPmNsNDPbLoLqqCVznFbvdB7x-Tl-m0l_eFTj2KiqwGqE9PZB9nNTwMVvH3VRRSLWACvPnSiwP8N5Usy-WRXS-V7TbpxIhvepTfE0NNo"
		//}
		Mono<GenericRSAJWK> key1 = rsaJWKBuilder
			.modulus("sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw")
			.publicExponent("AQAB")
			.privateExponent("VFCWOqXr8nvZNyaaJLXdnNPXZKRaWCjkU5Q2egQQpTBMwhprMzWzpR8Sxq1OPThh_J6MUD8Z35wky9b8eEO0pwNS8xlh1lOFRRBoNqDIKVOku0aZb-rynq8cxjDTLZQ6Fz7jSjR1Klop-YKaUHc9GsEofQqYruPhzSA-QgajZGPbE_0ZaVDJHfyd7UUBUKunFMScbflYAAOYJqVIVwaYR5zWEEceUjNnTNo_CVSj-VvXLO5VZfCUAVLgW4dpf1SrtZjSt34YLsRarSb127reG_DUwg9Ch-KyvjT1SkHgUWRVGcyly7uvVGRSDwsXypdrNinPA4jlhoNdizK2zF2CWQ")
			.firstPrimeFactor("9gY2w6I6S6L0juEKsbeDAwpd9WMfgqFoeA9vEyEUuk4kLwBKcoe1x4HG68ik918hdDSE9vDQSccA3xXHOAFOPJ8R9EeIAbTi1VwBYnbTp87X-xcPWlEPkrdoUKW60tgs1aNd_Nnc9LEVVPMS390zbFxt8TN_biaBgelNgbC95sM")
			.secondPrimeFactor("uKlCKvKv_ZJMVcdIs5vVSU_6cPtYI1ljWytExV_skstvRSNi9r66jdd9-yBhVfuG4shsp2j7rGnIio901RBeHo6TPKWVVykPu1iYhQXw1jIABfw-MVsN-3bQ76WLdt2SDxsHs7q7zPyUyHXmps7ycZ5c72wGkUwNOjYelmkiNS0")
			.firstFactorExponent("w0kZbV63cVRvVX6yk3C8cMxo2qCM4Y8nsq1lmMSYhG4EcL6FWbX5h9yuvngs4iLEFk6eALoUS4vIWEwcL4txw9LsWH_zKI-hwoReoP77cOdSL4AVcraHawlkpyd2TWjE5evgbhWtOxnZee3cXJBkAi64Ik6jZxbvk-RR3pEhnCs")
			.secondFactorExponent("o_8V14SezckO6CNLKs_btPdFiO9_kC1DsuUTd2LAfIIVeMZ7jn1Gus_Ff7B7IVx3p5KuBGOVF8L-qifLb6nQnLysgHDh132NDioZkhH7mI7hPG-PYE_odApKdnqECHWw0J-F0JWnUd6D2B_1TvF9mXA2Qx-iGYn8OVV1Bsmp6qU")
			.firstCoefficient("eNho5yRBEBxhGBtQRww9QirZsB66TrfFReG_CcteI1aCneT0ELGhYlRlCtUkTRclIfuEPmNsNDPbLoLqqCVznFbvdB7x-Tl-m0l_eFTj2KiqwGqE9PZB9nNTwMVvH3VRRSLWACvPnSiwP8N5Usy-WRXS-V7TbpxIhvepTfE0NNo")
			.build();
		
		// {
		//	"kty":"oct",
		//	"k":"GawgguFyGrWKav7AX4VKUg"
		// }
		Mono<GenericOCTJWK> key2 = octJWKBuilder
			.keyValue("GawgguFyGrWKav7AX4VKUg")
			.build()
			.cache();
		
		String payload = "Live long and prosper.";
		
		JsonJWE<String, JsonJWE.BuiltRecipient<String>> builtJsonJWE = jweService.jsonBuilder(String.class)
			.headers(
					protectedHeader -> protectedHeader.encryptionAlgorithm("A128CBC-HS256"),
					unprotectedHeader -> unprotectedHeader.jwkSetURL(URI.create("https://server.example.com/keys.jwks"))
			)
			.payload(payload)
			.recipient(
				header -> header
					.algorithm("RSA1_5")
					.keyId("2011-04-29"),
				key1
			)
			.recipient(
				header -> header
					.algorithm("A128KW")
					.keyId("7"),
				key2
			)
			.build(MediaTypes.TEXT_PLAIN)
			.block();
		
		Assertions.assertNull(builtJsonJWE.getAdditionalAuthenticationData());
		Assertions.assertNotNull(builtJsonJWE.getAuthenticationTag());
		Assertions.assertNotNull(builtJsonJWE.getCipherText());
		Assertions.assertNotNull(builtJsonJWE.getInitializationVector());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getAlgorithm());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getContentType());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getCritical());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", builtJsonJWE.getProtectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getJWK());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getJWKSetURL());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getKey());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getKeyId());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getType());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getX509CertificateChain());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getX509CertificateURL());
		
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getAlgorithm());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getContentType());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getCritical());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getCustomParameters());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), builtJsonJWE.getUnprotectedHeader().getJWKSetURL());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getKey());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getKeyId());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getType());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getX509CertificateChain());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getX509CertificateURL());
		
		Assertions.assertEquals(2, builtJsonJWE.getRecipients().size());
		
		JsonJWE.BuiltRecipient<String> builtRecipient1 = builtJsonJWE.getRecipients().get(0);
		
		Assertions.assertNotNull(builtRecipient1.getEncryptedKey());
		Assertions.assertEquals("RSA1_5",builtRecipient1.getHeader().getAlgorithm());
		Assertions.assertNull(builtRecipient1.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtRecipient1.getHeader().getContentType());
		Assertions.assertNull(builtRecipient1.getHeader().getCritical());
		Assertions.assertNull(builtRecipient1.getHeader().getCustomParameters());
		Assertions.assertNull(builtRecipient1.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtRecipient1.getHeader().getJWK());
		Assertions.assertNull(builtRecipient1.getHeader().getJWKSetURL());
		Assertions.assertNull(builtRecipient1.getHeader().getKey());
		Assertions.assertEquals("2011-04-29", builtRecipient1.getHeader().getKeyId());
		Assertions.assertNull(builtRecipient1.getHeader().getType());
		Assertions.assertNull(builtRecipient1.getHeader().getX509CertificateChain());
		Assertions.assertNull(builtRecipient1.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtRecipient1.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtRecipient1.getHeader().getX509CertificateURL());
		
		JWE<String> builtJWE1 = builtRecipient1.getJWE();
		
		Assertions.assertEquals(builtJsonJWE.getCipherText(), builtJWE1.getCipherText());
		Assertions.assertEquals(builtRecipient1.getEncryptedKey(), builtJWE1.getEncryptedKey());
		Assertions.assertEquals(builtJsonJWE.getInitializationVector(), builtJWE1.getInitializationVector());
		Assertions.assertEquals(payload, builtJWE1.getPayload());
		Assertions.assertEquals("RSA1_5",builtJWE1.getHeader().getAlgorithm());
		Assertions.assertNull(builtJWE1.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtJWE1.getHeader().getContentType());
		Assertions.assertNull(builtJWE1.getHeader().getCritical());
		Assertions.assertNull(builtJWE1.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", builtJWE1.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtJWE1.getHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), builtJWE1.getHeader().getJWKSetURL());
		Assertions.assertNull(builtJWE1.getHeader().getKey());
		Assertions.assertEquals("2011-04-29", builtJWE1.getHeader().getKeyId());
		Assertions.assertNull(builtJWE1.getHeader().getType());
		Assertions.assertNull(builtJWE1.getHeader().getX509CertificateChain());
		Assertions.assertNull(builtJWE1.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtJWE1.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtJWE1.getHeader().getX509CertificateURL());
		
		
		JsonJWE.BuiltRecipient<String> builtRecipient2 = builtJsonJWE.getRecipients().get(1);
		
		Assertions.assertNotNull(builtRecipient2.getEncryptedKey());
		Assertions.assertEquals("A128KW",builtRecipient2.getHeader().getAlgorithm());
		Assertions.assertNull(builtRecipient2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtRecipient2.getHeader().getContentType());
		Assertions.assertNull(builtRecipient2.getHeader().getCritical());
		Assertions.assertNull(builtRecipient2.getHeader().getCustomParameters());
		Assertions.assertNull(builtRecipient2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtRecipient2.getHeader().getJWK());
		Assertions.assertNull(builtRecipient2.getHeader().getJWKSetURL());
		Assertions.assertNull(builtRecipient2.getHeader().getKey());
		Assertions.assertEquals("7", builtRecipient2.getHeader().getKeyId());
		Assertions.assertNull(builtRecipient2.getHeader().getType());
		Assertions.assertNull(builtRecipient2.getHeader().getX509CertificateChain());
		Assertions.assertNull(builtRecipient2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtRecipient2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtRecipient2.getHeader().getX509CertificateURL());
		
		JWE<String> builtJWE2 = builtRecipient2.getJWE();
		
		Assertions.assertEquals(builtJsonJWE.getCipherText(), builtJWE2.getCipherText());
		Assertions.assertEquals(builtRecipient2.getEncryptedKey(), builtJWE2.getEncryptedKey());
		Assertions.assertEquals(builtJsonJWE.getInitializationVector(), builtJWE2.getInitializationVector());
		Assertions.assertEquals(payload, builtJWE2.getPayload());
		Assertions.assertEquals("A128KW",builtJWE2.getHeader().getAlgorithm());
		Assertions.assertNull(builtJWE2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtJWE2.getHeader().getContentType());
		Assertions.assertNull(builtJWE2.getHeader().getCritical());
		Assertions.assertNull(builtJWE2.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", builtJWE2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtJWE2.getHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), builtJWE2.getHeader().getJWKSetURL());
		Assertions.assertNull(builtJWE2.getHeader().getKey());
		Assertions.assertEquals("7", builtJWE2.getHeader().getKeyId());
		Assertions.assertNull(builtJWE2.getHeader().getType());
		Assertions.assertNull(builtJWE2.getHeader().getX509CertificateChain());
		Assertions.assertNull(builtJWE2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtJWE2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtJWE2.getHeader().getX509CertificateURL());
		
		JsonJWE<String, JsonJWE.ReadRecipient<String>> readJsonJWE = jweService.jsonReader(String.class)
			.read(builtJsonJWE.toJson(), MediaTypes.TEXT_PLAIN)
			.block();
		
		Assertions.assertEquals(builtJsonJWE.getAdditionalAuthenticationData(), readJsonJWE.getAdditionalAuthenticationData());
		Assertions.assertEquals(builtJsonJWE.getAuthenticationTag(), readJsonJWE.getAuthenticationTag());
		Assertions.assertEquals(builtJsonJWE.getCipherText(), readJsonJWE.getCipherText());
		Assertions.assertEquals(builtJsonJWE.getInitializationVector(), readJsonJWE.getInitializationVector());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getContentType());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCritical());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJsonJWE.getProtectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getJWK());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getJWKSetURL());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getKey());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getKeyId());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getType());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateChain());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateURL());
		
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getContentType());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCritical());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCustomParameters());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJsonJWE.getUnprotectedHeader().getJWKSetURL());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getKey());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getKeyId());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getType());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateChain());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateURL());
		
		Assertions.assertEquals(2, readJsonJWE.getRecipients().size());
		
		JsonJWE.ReadRecipient<String> readRecipient1 = readJsonJWE.getRecipients().get(0);
		
		Assertions.assertNotNull(readRecipient1.getEncryptedKey());
		Assertions.assertEquals("RSA1_5",readRecipient1.getHeader().getAlgorithm());
		Assertions.assertNull(readRecipient1.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readRecipient1.getHeader().getContentType());
		Assertions.assertNull(readRecipient1.getHeader().getCritical());
		Assertions.assertNull(readRecipient1.getHeader().getCustomParameters());
		Assertions.assertNull(readRecipient1.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readRecipient1.getHeader().getJWK());
		Assertions.assertNull(readRecipient1.getHeader().getJWKSetURL());
		Assertions.assertNull(readRecipient1.getHeader().getKey());
		Assertions.assertEquals("2011-04-29", readRecipient1.getHeader().getKeyId());
		Assertions.assertNull(readRecipient1.getHeader().getType());
		Assertions.assertNull(readRecipient1.getHeader().getX509CertificateChain());
		Assertions.assertNull(readRecipient1.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readRecipient1.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readRecipient1.getHeader().getX509CertificateURL());
		
		JWE<String> readJWE1 = readRecipient1.readJWE(key1).block();
		
		Assertions.assertEquals(readJsonJWE.getCipherText(), readJWE1.getCipherText());
		Assertions.assertEquals(readRecipient1.getEncryptedKey(), readJWE1.getEncryptedKey());
		Assertions.assertEquals(readJsonJWE.getInitializationVector(), readJWE1.getInitializationVector());
		Assertions.assertEquals(payload, readJWE1.getPayload());
		Assertions.assertEquals("RSA1_5",readJWE1.getHeader().getAlgorithm());
		Assertions.assertNull(readJWE1.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJWE1.getHeader().getContentType());
		Assertions.assertNull(readJWE1.getHeader().getCritical());
		Assertions.assertNull(readJWE1.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJWE1.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJWE1.getHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJWE1.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWE1.getHeader().getKey());
		Assertions.assertEquals("2011-04-29", readJWE1.getHeader().getKeyId());
		Assertions.assertNull(readJWE1.getHeader().getType());
		Assertions.assertNull(readJWE1.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWE1.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWE1.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWE1.getHeader().getX509CertificateURL());
		
		JsonJWE.ReadRecipient<String> readRecipient2 = readJsonJWE.getRecipients().get(1);
		
		Assertions.assertNotNull(readRecipient2.getEncryptedKey());
		Assertions.assertEquals("A128KW",readRecipient2.getHeader().getAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getContentType());
		Assertions.assertNull(readRecipient2.getHeader().getCritical());
		Assertions.assertNull(readRecipient2.getHeader().getCustomParameters());
		Assertions.assertNull(readRecipient2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getJWK());
		Assertions.assertNull(readRecipient2.getHeader().getJWKSetURL());
		Assertions.assertNull(readRecipient2.getHeader().getKey());
		Assertions.assertEquals("7", readRecipient2.getHeader().getKeyId());
		Assertions.assertNull(readRecipient2.getHeader().getType());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateChain());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateURL());
		
		JWE<String> readJWE2 = readRecipient2.readJWE(key2).block();
		
		Assertions.assertEquals(readJsonJWE.getCipherText(), readJWE2.getCipherText());
		Assertions.assertEquals(readRecipient2.getEncryptedKey(), readJWE2.getEncryptedKey());
		Assertions.assertEquals(readJsonJWE.getInitializationVector(), readJWE2.getInitializationVector());
		Assertions.assertEquals(payload, readJWE2.getPayload());
		Assertions.assertEquals("A128KW",readJWE2.getHeader().getAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getContentType());
		Assertions.assertNull(readJWE2.getHeader().getCritical());
		Assertions.assertNull(readJWE2.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJWE2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJWE2.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWE2.getHeader().getKey());
		Assertions.assertEquals("7", readJWE2.getHeader().getKeyId());
		Assertions.assertNull(readJWE2.getHeader().getType());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateURL());
		
		// RFC7516 A.4
		String completeJson = "{\"protected\":\"eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0\",\"unprotected\":{\"jku\":\"https://server.example.com/keys.jwks\"},\"recipients\":[{\"header\":{\"alg\":\"RSA1_5\",\"kid\":\"2011-04-29\"},\"encrypted_key\":\"UGhIOguC7IuEvf_NPVaXsGMoLOmwvc1GyqlIKOK1nN94nHPoltGRhWhw7Zx0-kFm1NJn8LE9XShH59_i8J0PH5ZZyNfGy2xGdULU7sHNF6Gp2vPLgNZ__deLKxGHZ7PcHALUzoOegEI-8E66jX2E4zyJKx-YxzZIItRzC5hlRirb6Y5Cl_p-ko3YvkkysZIFNPccxRU7qve1WYPxqbb2Yw8kZqa2rMWI5ng8OtvzlV7elprCbuPhcCdZ6XDP0_F8rkXds2vE4X-ncOIM8hAYHHi29NX0mcKiRaD0-D-ljQTP-cFPgwCp6X-nZZd9OHBv-B3oWh2TbqmScqXMR4gp_A\"},{\"header\":{\"alg\":\"A128KW\",\"kid\":\"7\"},\"encrypted_key\":\"6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ\"}],\"iv\":\"AxY8DCtDaGlsbGljb3RoZQ\",\"ciphertext\":\"KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY\",\"tag\":\"Mz-VPPyU4RlcuYv1IwIvzw\"}";
	
		readJsonJWE = jweService.jsonReader(String.class)
			.read(completeJson, MediaTypes.TEXT_PLAIN)
			.block();
		
		Assertions.assertNull(readJsonJWE.getAdditionalAuthenticationData());
		Assertions.assertEquals("Mz-VPPyU4RlcuYv1IwIvzw", readJsonJWE.getAuthenticationTag());
		Assertions.assertEquals("KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY", readJsonJWE.getCipherText());
		Assertions.assertEquals("AxY8DCtDaGlsbGljb3RoZQ", readJsonJWE.getInitializationVector());
		Assertions.assertEquals("eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0", readJsonJWE.getProtectedHeader().getEncoded());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getContentType());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCritical());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJsonJWE.getProtectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getJWK());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getJWKSetURL());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getKey());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getKeyId());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getType());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateChain());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateURL());
		
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getContentType());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCritical());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCustomParameters());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJsonJWE.getUnprotectedHeader().getJWKSetURL());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getKey());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getKeyId());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getType());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateChain());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateURL());
		
		Assertions.assertEquals(2, readJsonJWE.getRecipients().size());
		
		readRecipient1 = readJsonJWE.getRecipients().get(0);
		
		Assertions.assertNotNull(readRecipient1.getEncryptedKey());
		Assertions.assertEquals("RSA1_5",readRecipient1.getHeader().getAlgorithm());
		Assertions.assertNull(readRecipient1.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readRecipient1.getHeader().getContentType());
		Assertions.assertNull(readRecipient1.getHeader().getCritical());
		Assertions.assertNull(readRecipient1.getHeader().getCustomParameters());
		Assertions.assertNull(readRecipient1.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readRecipient1.getHeader().getJWK());
		Assertions.assertNull(readRecipient1.getHeader().getJWKSetURL());
		Assertions.assertNull(readRecipient1.getHeader().getKey());
		Assertions.assertEquals("2011-04-29", readRecipient1.getHeader().getKeyId());
		Assertions.assertNull(readRecipient1.getHeader().getType());
		Assertions.assertNull(readRecipient1.getHeader().getX509CertificateChain());
		Assertions.assertNull(readRecipient1.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readRecipient1.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readRecipient1.getHeader().getX509CertificateURL());
		
		readJWE1 = readRecipient1.readJWE(key1).block();
		
		Assertions.assertEquals(readJsonJWE.getCipherText(), readJWE1.getCipherText());
		Assertions.assertEquals(readRecipient1.getEncryptedKey(), readJWE1.getEncryptedKey());
		Assertions.assertEquals(readJsonJWE.getInitializationVector(), readJWE1.getInitializationVector());
		Assertions.assertEquals(payload, readJWE1.getPayload());
		Assertions.assertEquals("RSA1_5",readJWE1.getHeader().getAlgorithm());
		Assertions.assertNull(readJWE1.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJWE1.getHeader().getContentType());
		Assertions.assertNull(readJWE1.getHeader().getCritical());
		Assertions.assertNull(readJWE1.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJWE1.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJWE1.getHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJWE1.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWE1.getHeader().getKey());
		Assertions.assertEquals("2011-04-29", readJWE1.getHeader().getKeyId());
		Assertions.assertNull(readJWE1.getHeader().getType());
		Assertions.assertNull(readJWE1.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWE1.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWE1.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWE1.getHeader().getX509CertificateURL());
		
		readRecipient2 = readJsonJWE.getRecipients().get(1);
		
		Assertions.assertNotNull(readRecipient2.getEncryptedKey());
		Assertions.assertEquals("A128KW",readRecipient2.getHeader().getAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getContentType());
		Assertions.assertNull(readRecipient2.getHeader().getCritical());
		Assertions.assertNull(readRecipient2.getHeader().getCustomParameters());
		Assertions.assertNull(readRecipient2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getJWK());
		Assertions.assertNull(readRecipient2.getHeader().getJWKSetURL());
		Assertions.assertNull(readRecipient2.getHeader().getKey());
		Assertions.assertEquals("7", readRecipient2.getHeader().getKeyId());
		Assertions.assertNull(readRecipient2.getHeader().getType());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateChain());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateURL());
		
		readJWE2 = readRecipient2.readJWE(key2).block();
		
		Assertions.assertEquals(readJsonJWE.getCipherText(), readJWE2.getCipherText());
		Assertions.assertEquals(readRecipient2.getEncryptedKey(), readJWE2.getEncryptedKey());
		Assertions.assertEquals(readJsonJWE.getInitializationVector(), readJWE2.getInitializationVector());
		Assertions.assertEquals(payload, readJWE2.getPayload());
		Assertions.assertEquals("A128KW",readJWE2.getHeader().getAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getContentType());
		Assertions.assertNull(readJWE2.getHeader().getCritical());
		Assertions.assertNull(readJWE2.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJWE2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJWE2.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWE2.getHeader().getKey());
		Assertions.assertEquals("7", readJWE2.getHeader().getKeyId());
		Assertions.assertNull(readJWE2.getHeader().getType());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateURL());
	}
	
	@Test
	public void testRFC7516_A5() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		ObjectMapper mapper = mapper();
		GenericOCTJWKBuilder octJWKBuilder = octJWKBuilder(mapper);
		
		// {
		//	"kty":"oct",
		//	"k":"GawgguFyGrWKav7AX4VKUg"
		// }
		Mono<GenericOCTJWK> key2 = octJWKBuilder
			.keyValue("GawgguFyGrWKav7AX4VKUg")
			.build()
			.cache();
		
		String payload = "Live long and prosper.";
		
		JsonJWE<String, JsonJWE.BuiltRecipient<String>> builtJsonJWE = jweService.jsonBuilder(String.class)
			.headers(
					protectedHeader -> protectedHeader.encryptionAlgorithm("A128CBC-HS256"),
					unprotectedHeader -> unprotectedHeader.jwkSetURL(URI.create("https://server.example.com/keys.jwks"))
			)
			.payload(payload)
			.recipient(
				header -> header
					.algorithm("A128KW")
					.keyId("7"),
				key2
			)
			.build(MediaTypes.TEXT_PLAIN)
			.block();
		
		Assertions.assertNull(builtJsonJWE.getAdditionalAuthenticationData());
		Assertions.assertNotNull(builtJsonJWE.getAuthenticationTag());
		Assertions.assertNotNull(builtJsonJWE.getCipherText());
		Assertions.assertNotNull(builtJsonJWE.getInitializationVector());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getAlgorithm());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getContentType());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getCritical());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", builtJsonJWE.getProtectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getJWK());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getJWKSetURL());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getKey());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getKeyId());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getType());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getX509CertificateChain());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtJsonJWE.getProtectedHeader().getX509CertificateURL());
		
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getAlgorithm());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getContentType());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getCritical());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getCustomParameters());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), builtJsonJWE.getUnprotectedHeader().getJWKSetURL());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getKey());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getKeyId());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getType());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getX509CertificateChain());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtJsonJWE.getUnprotectedHeader().getX509CertificateURL());
		
		Assertions.assertEquals(1, builtJsonJWE.getRecipients().size());
		
		JsonJWE.BuiltRecipient<String> builtRecipient2 = builtJsonJWE.getRecipients().get(0);
		
		Assertions.assertNotNull(builtRecipient2.getEncryptedKey());
		Assertions.assertEquals("A128KW",builtRecipient2.getHeader().getAlgorithm());
		Assertions.assertNull(builtRecipient2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtRecipient2.getHeader().getContentType());
		Assertions.assertNull(builtRecipient2.getHeader().getCritical());
		Assertions.assertNull(builtRecipient2.getHeader().getCustomParameters());
		Assertions.assertNull(builtRecipient2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtRecipient2.getHeader().getJWK());
		Assertions.assertNull(builtRecipient2.getHeader().getJWKSetURL());
		Assertions.assertNull(builtRecipient2.getHeader().getKey());
		Assertions.assertEquals("7", builtRecipient2.getHeader().getKeyId());
		Assertions.assertNull(builtRecipient2.getHeader().getType());
		Assertions.assertNull(builtRecipient2.getHeader().getX509CertificateChain());
		Assertions.assertNull(builtRecipient2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtRecipient2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtRecipient2.getHeader().getX509CertificateURL());
		
		JWE<String> builtJWE2 = builtRecipient2.getJWE();
		
		Assertions.assertEquals(builtJsonJWE.getCipherText(), builtJWE2.getCipherText());
		Assertions.assertEquals(builtRecipient2.getEncryptedKey(), builtJWE2.getEncryptedKey());
		Assertions.assertEquals(builtJsonJWE.getInitializationVector(), builtJWE2.getInitializationVector());
		Assertions.assertEquals(payload, builtJWE2.getPayload());
		Assertions.assertEquals("A128KW",builtJWE2.getHeader().getAlgorithm());
		Assertions.assertNull(builtJWE2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(builtJWE2.getHeader().getContentType());
		Assertions.assertNull(builtJWE2.getHeader().getCritical());
		Assertions.assertNull(builtJWE2.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", builtJWE2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(builtJWE2.getHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), builtJWE2.getHeader().getJWKSetURL());
		Assertions.assertNull(builtJWE2.getHeader().getKey());
		Assertions.assertEquals("7", builtJWE2.getHeader().getKeyId());
		Assertions.assertNull(builtJWE2.getHeader().getType());
		Assertions.assertNull(builtJWE2.getHeader().getX509CertificateChain());
		Assertions.assertNull(builtJWE2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(builtJWE2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(builtJWE2.getHeader().getX509CertificateURL());
		
		JsonJWE<String, JsonJWE.ReadRecipient<String>> readJsonJWE = jweService.jsonReader(String.class)
			.read(builtJsonJWE.toJson(), MediaTypes.TEXT_PLAIN)
			.block();
		
		Assertions.assertEquals(builtJsonJWE.getAdditionalAuthenticationData(), readJsonJWE.getAdditionalAuthenticationData());
		Assertions.assertEquals(builtJsonJWE.getAuthenticationTag(), readJsonJWE.getAuthenticationTag());
		Assertions.assertEquals(builtJsonJWE.getCipherText(), readJsonJWE.getCipherText());
		Assertions.assertEquals(builtJsonJWE.getInitializationVector(), readJsonJWE.getInitializationVector());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getContentType());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCritical());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJsonJWE.getProtectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getJWK());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getJWKSetURL());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getKey());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getKeyId());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getType());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateChain());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateURL());
		
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getContentType());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCritical());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCustomParameters());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJsonJWE.getUnprotectedHeader().getJWKSetURL());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getKey());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getKeyId());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getType());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateChain());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateURL());
		
		Assertions.assertEquals(1, readJsonJWE.getRecipients().size());
		
		JsonJWE.ReadRecipient<String> readRecipient2 = readJsonJWE.getRecipients().get(0);
		
		Assertions.assertNotNull(readRecipient2.getEncryptedKey());
		Assertions.assertEquals("A128KW",readRecipient2.getHeader().getAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getContentType());
		Assertions.assertNull(readRecipient2.getHeader().getCritical());
		Assertions.assertNull(readRecipient2.getHeader().getCustomParameters());
		Assertions.assertNull(readRecipient2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getJWK());
		Assertions.assertNull(readRecipient2.getHeader().getJWKSetURL());
		Assertions.assertNull(readRecipient2.getHeader().getKey());
		Assertions.assertEquals("7", readRecipient2.getHeader().getKeyId());
		Assertions.assertNull(readRecipient2.getHeader().getType());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateChain());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateURL());
		
		JWE<String> readJWE2 = readRecipient2.readJWE(key2).block();
		
		Assertions.assertEquals(readJsonJWE.getCipherText(), readJWE2.getCipherText());
		Assertions.assertEquals(readRecipient2.getEncryptedKey(), readJWE2.getEncryptedKey());
		Assertions.assertEquals(readJsonJWE.getInitializationVector(), readJWE2.getInitializationVector());
		Assertions.assertEquals(payload, readJWE2.getPayload());
		Assertions.assertEquals("A128KW",readJWE2.getHeader().getAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getContentType());
		Assertions.assertNull(readJWE2.getHeader().getCritical());
		Assertions.assertNull(readJWE2.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJWE2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJWE2.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWE2.getHeader().getKey());
		Assertions.assertEquals("7", readJWE2.getHeader().getKeyId());
		Assertions.assertNull(readJWE2.getHeader().getType());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateURL());
		
		// RFC7516 A.4
		String completeJson = "{\"protected\":\"eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0\",\"unprotected\":{\"jku\":\"https://server.example.com/keys.jwks\"},\"header\":{\"alg\":\"A128KW\",\"kid\":\"7\"},\"encrypted_key\":\"6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ\",\"iv\":\"AxY8DCtDaGlsbGljb3RoZQ\",\"ciphertext\":\"KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY\",\"tag\":\"Mz-VPPyU4RlcuYv1IwIvzw\"}";
	
		readJsonJWE = jweService.jsonReader(String.class)
			.read(completeJson, MediaTypes.TEXT_PLAIN)
			.block();
		
		Assertions.assertNull(readJsonJWE.getAdditionalAuthenticationData());
		Assertions.assertEquals("Mz-VPPyU4RlcuYv1IwIvzw", readJsonJWE.getAuthenticationTag());
		Assertions.assertEquals("KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY", readJsonJWE.getCipherText());
		Assertions.assertEquals("AxY8DCtDaGlsbGljb3RoZQ", readJsonJWE.getInitializationVector());
		Assertions.assertEquals("eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0", readJsonJWE.getProtectedHeader().getEncoded());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getContentType());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCritical());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJsonJWE.getProtectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getJWK());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getJWKSetURL());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getKey());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getKeyId());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getType());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateChain());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJsonJWE.getProtectedHeader().getX509CertificateURL());
		
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getContentType());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCritical());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getCustomParameters());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJsonJWE.getUnprotectedHeader().getJWKSetURL());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getKey());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getKeyId());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getType());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateChain());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJsonJWE.getUnprotectedHeader().getX509CertificateURL());
		
		Assertions.assertEquals(1, readJsonJWE.getRecipients().size());
		
		readRecipient2 = readJsonJWE.getRecipients().get(0);
		
		Assertions.assertNotNull(readRecipient2.getEncryptedKey());
		Assertions.assertEquals("A128KW",readRecipient2.getHeader().getAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getContentType());
		Assertions.assertNull(readRecipient2.getHeader().getCritical());
		Assertions.assertNull(readRecipient2.getHeader().getCustomParameters());
		Assertions.assertNull(readRecipient2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readRecipient2.getHeader().getJWK());
		Assertions.assertNull(readRecipient2.getHeader().getJWKSetURL());
		Assertions.assertNull(readRecipient2.getHeader().getKey());
		Assertions.assertEquals("7", readRecipient2.getHeader().getKeyId());
		Assertions.assertNull(readRecipient2.getHeader().getType());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateChain());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readRecipient2.getHeader().getX509CertificateURL());
		
		readJWE2 = readRecipient2.readJWE(key2).block();
		
		Assertions.assertEquals(readJsonJWE.getCipherText(), readJWE2.getCipherText());
		Assertions.assertEquals(readRecipient2.getEncryptedKey(), readJWE2.getEncryptedKey());
		Assertions.assertEquals(readJsonJWE.getInitializationVector(), readJWE2.getInitializationVector());
		Assertions.assertEquals(payload, readJWE2.getPayload());
		Assertions.assertEquals("A128KW",readJWE2.getHeader().getAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getContentType());
		Assertions.assertNull(readJWE2.getHeader().getCritical());
		Assertions.assertNull(readJWE2.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", readJWE2.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(readJWE2.getHeader().getJWK());
		Assertions.assertEquals(URI.create("https://server.example.com/keys.jwks"), readJWE2.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWE2.getHeader().getKey());
		Assertions.assertEquals("7", readJWE2.getHeader().getKeyId());
		Assertions.assertNull(readJWE2.getHeader().getType());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWE2.getHeader().getX509CertificateURL());
	}
	
	@Test
	public void testRFC7517_C() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		ObjectMapper mapper = mapper();
		GenericRSAJWKBuilder rsaJWKBuilder = rsaJWKBuilder(mapper);
		GenericPBES2JWKBuilder pbesJWKBuilder = pbes2JWKBuilder(mapper);
		
		//{
		//	"kty": "RSA",
		//	"kid": "juliet@capulet.lit",
		//	"use": "enc",
		//	"n": "t6Q8PWSi1dkJj9hTP8hNYFlvadM7DflW9mWepOJhJ66w7nyoK1gPNqFMSQRyO125Gp-TEkodhWr0iujjHVx7BcV0llS4w5ACGgPrcAd6ZcSR0-Iqom-QFcNP8Sjg086MwoqQU_LYywlAGZ21WSdS_PERyGFiNnj3QQlO8Yns5jCtLCRwLHL0Pb1fEv45AuRIuUfVcPySBWYnDyGxvjYGDSM-AqWS9zIQ2ZilgT-GqUmipg0XOC0Cc20rgLe2ymLHjpHciCKVAbY5-L32-lSeZO-Os6U15_aXrk9Gw8cPUaX1_I8sLGuSiVdt3C_Fn2PZ3Z8i744FPFGGcG1qs2Wz-Q",
		//	"e": "AQAB",
		//	"d": "GRtbIQmhOZtyszfgKdg4u_N-R_mZGU_9k7JQ_jn1DnfTuMdSNprTeaSTyWfSNkuaAwnOEbIQVy1IQbWVV25NY3ybc_IhUJtfri7bAXYEReWaCl3hdlPKXy9UvqPYGR0kIXTQRqns-dVJ7jahlI7LyckrpTmrM8dWBo4_PMaenNnPiQgO0xnuToxutRZJfJvG4Ox4ka3GORQd9CsCZ2vsUDmsXOfUENOyMqADC6p1M3h33tsurY15k9qMSpG9OX_IJAXmxzAh_tWiZOwk2K4yxH9tS3Lq1yX8C1EWmeRDkK2ahecG85-oLKQt5VEpWHKmjOi_gJSdSgqcN96X52esAQ",
		//	"p": "2rnSOV4hKSN8sS4CgcQHFbs08XboFDqKum3sc4h3GRxrTmQdl1ZK9uw-PIHfQP0FkxXVrx-WE-ZEbrqivH_2iCLUS7wAl6XvARt1KkIaUxPPSYB9yk31s0Q8UK96E3_OrADAYtAJs-M3JxCLfNgqh56HDnETTQhH3rCT5T3yJws",
		//	"q": "1u_RiFDP7LBYh3N4GXLT9OpSKYP0uQZyiaZwBtOCBNJgQxaj10RWjsZu0c6Iedis4S7B_coSKB0Kj9PaPaBzg-IySRvvcQuPamQu66riMhjVtG6TlV8CLCYKrYl52ziqK0E_ym2QnkwsUX7eYTB7LbAHRK9GqocDE5B0f808I4s",
		//	"dp": "KkMTWqBUefVwZ2_Dbj1pPQqyHSHjj90L5x_MOzqYAJMcLMZtbUtwKqvVDq3tbEo3ZIcohbDtt6SbfmWzggabpQxNxuBpoOOf_a_HgMXK_lhqigI4y_kqS1wY52IwjUn5rgRrJ-yYo1h41KR-vz2pYhEAeYrhttWtxVqLCRViD6c",
		//	"dq": "AvfS0-gRxvn0bwJoMSnFxYcK1WnuEjQFluMGfwGitQBWtfZ1Er7t1xDkbN9GQTB9yqpDoYaN06H7CFtrkxhJIBQaj6nkF5KKS3TQtQ5qCzkOkmxIe3KRbBymXxkb5qwUpX5ELD5xFc6FeiafWYY63TmmEAu_lRFCOJ3xDea-ots",
		//	"qi": "lSQi-w9CpyUReMErP1RsBLk7wNtOvs5EQpPqmuMvqW57NBUczScEoPwmUqqabu9V0-Py4dQ57_bapoKRu1R90bvuFnU63SHWEFglZQvJDMeAvmj4sm-Fp0oYu_neotgQ0hzbI5gry7ajdYy9-2lNx_76aBZoOUu9HCJ-UsfSOI8"
		//}
		GenericRSAJWK payload = rsaJWKBuilder
			.keyId("juliet@capulet.lit")
			.publicKeyUse("enc")
			.modulus("t6Q8PWSi1dkJj9hTP8hNYFlvadM7DflW9mWepOJhJ66w7nyoK1gPNqFMSQRyO125Gp-TEkodhWr0iujjHVx7BcV0llS4w5ACGgPrcAd6ZcSR0-Iqom-QFcNP8Sjg086MwoqQU_LYywlAGZ21WSdS_PERyGFiNnj3QQlO8Yns5jCtLCRwLHL0Pb1fEv45AuRIuUfVcPySBWYnDyGxvjYGDSM-AqWS9zIQ2ZilgT-GqUmipg0XOC0Cc20rgLe2ymLHjpHciCKVAbY5-L32-lSeZO-Os6U15_aXrk9Gw8cPUaX1_I8sLGuSiVdt3C_Fn2PZ3Z8i744FPFGGcG1qs2Wz-Q")
			.publicExponent("AQAB")
			.privateExponent("GRtbIQmhOZtyszfgKdg4u_N-R_mZGU_9k7JQ_jn1DnfTuMdSNprTeaSTyWfSNkuaAwnOEbIQVy1IQbWVV25NY3ybc_IhUJtfri7bAXYEReWaCl3hdlPKXy9UvqPYGR0kIXTQRqns-dVJ7jahlI7LyckrpTmrM8dWBo4_PMaenNnPiQgO0xnuToxutRZJfJvG4Ox4ka3GORQd9CsCZ2vsUDmsXOfUENOyMqADC6p1M3h33tsurY15k9qMSpG9OX_IJAXmxzAh_tWiZOwk2K4yxH9tS3Lq1yX8C1EWmeRDkK2ahecG85-oLKQt5VEpWHKmjOi_gJSdSgqcN96X52esAQ")
			.firstPrimeFactor("2rnSOV4hKSN8sS4CgcQHFbs08XboFDqKum3sc4h3GRxrTmQdl1ZK9uw-PIHfQP0FkxXVrx-WE-ZEbrqivH_2iCLUS7wAl6XvARt1KkIaUxPPSYB9yk31s0Q8UK96E3_OrADAYtAJs-M3JxCLfNgqh56HDnETTQhH3rCT5T3yJws")
			.secondPrimeFactor("1u_RiFDP7LBYh3N4GXLT9OpSKYP0uQZyiaZwBtOCBNJgQxaj10RWjsZu0c6Iedis4S7B_coSKB0Kj9PaPaBzg-IySRvvcQuPamQu66riMhjVtG6TlV8CLCYKrYl52ziqK0E_ym2QnkwsUX7eYTB7LbAHRK9GqocDE5B0f808I4s")
			.firstFactorExponent("KkMTWqBUefVwZ2_Dbj1pPQqyHSHjj90L5x_MOzqYAJMcLMZtbUtwKqvVDq3tbEo3ZIcohbDtt6SbfmWzggabpQxNxuBpoOOf_a_HgMXK_lhqigI4y_kqS1wY52IwjUn5rgRrJ-yYo1h41KR-vz2pYhEAeYrhttWtxVqLCRViD6c")
			.secondFactorExponent("AvfS0-gRxvn0bwJoMSnFxYcK1WnuEjQFluMGfwGitQBWtfZ1Er7t1xDkbN9GQTB9yqpDoYaN06H7CFtrkxhJIBQaj6nkF5KKS3TQtQ5qCzkOkmxIe3KRbBymXxkb5qwUpX5ELD5xFc6FeiafWYY63TmmEAu_lRFCOJ3xDea-ots")
			.firstCoefficient("lSQi-w9CpyUReMErP1RsBLk7wNtOvs5EQpPqmuMvqW57NBUczScEoPwmUqqabu9V0-Py4dQ57_bapoKRu1R90bvuFnU63SHWEFglZQvJDMeAvmj4sm-Fp0oYu_neotgQ0hzbI5gry7ajdYy9-2lNx_76aBZoOUu9HCJ-UsfSOI8")
			.build()
			.block();
		
//		String payload = "{\"kty\":\"RSA\",\"kid\":\"juliet@capulet.lit\",\"use\":\"enc\",\"n\":\"t6Q8PWSi1dkJj9hTP8hNYFlvadM7DflW9mWepOJhJ66w7nyoK1gPNqFMSQRyO125Gp-TEkodhWr0iujjHVx7BcV0llS4w5ACGgPrcAd6ZcSR0-Iqom-QFcNP8Sjg086MwoqQU_LYywlAGZ21WSdS_PERyGFiNnj3QQlO8Yns5jCtLCRwLHL0Pb1fEv45AuRIuUfVcPySBWYnDyGxvjYGDSM-AqWS9zIQ2ZilgT-GqUmipg0XOC0Cc20rgLe2ymLHjpHciCKVAbY5-L32-lSeZO-Os6U15_aXrk9Gw8cPUaX1_I8sLGuSiVdt3C_Fn2PZ3Z8i744FPFGGcG1qs2Wz-Q\",\"e\":\"AQAB\",\"d\":\"GRtbIQmhOZtyszfgKdg4u_N-R_mZGU_9k7JQ_jn1DnfTuMdSNprTeaSTyWfSNkuaAwnOEbIQVy1IQbWVV25NY3ybc_IhUJtfri7bAXYEReWaCl3hdlPKXy9UvqPYGR0kIXTQRqns-dVJ7jahlI7LyckrpTmrM8dWBo4_PMaenNnPiQgO0xnuToxutRZJfJvG4Ox4ka3GORQd9CsCZ2vsUDmsXOfUENOyMqADC6p1M3h33tsurY15k9qMSpG9OX_IJAXmxzAh_tWiZOwk2K4yxH9tS3Lq1yX8C1EWmeRDkK2ahecG85-oLKQt5VEpWHKmjOi_gJSdSgqcN96X52esAQ\",\"p\":\"2rnSOV4hKSN8sS4CgcQHFbs08XboFDqKum3sc4h3GRxrTmQdl1ZK9uw-PIHfQP0FkxXVrx-WE-ZEbrqivH_2iCLUS7wAl6XvARt1KkIaUxPPSYB9yk31s0Q8UK96E3_OrADAYtAJs-M3JxCLfNgqh56HDnETTQhH3rCT5T3yJws\",\"q\":\"1u_RiFDP7LBYh3N4GXLT9OpSKYP0uQZyiaZwBtOCBNJgQxaj10RWjsZu0c6Iedis4S7B_coSKB0Kj9PaPaBzg-IySRvvcQuPamQu66riMhjVtG6TlV8CLCYKrYl52ziqK0E_ym2QnkwsUX7eYTB7LbAHRK9GqocDE5B0f808I4s\",\"dp\":\"KkMTWqBUefVwZ2_Dbj1pPQqyHSHjj90L5x_MOzqYAJMcLMZtbUtwKqvVDq3tbEo3ZIcohbDtt6SbfmWzggabpQxNxuBpoOOf_a_HgMXK_lhqigI4y_kqS1wY52IwjUn5rgRrJ-yYo1h41KR-vz2pYhEAeYrhttWtxVqLCRViD6c\",\"dq\":\"AvfS0-gRxvn0bwJoMSnFxYcK1WnuEjQFluMGfwGitQBWtfZ1Er7t1xDkbN9GQTB9yqpDoYaN06H7CFtrkxhJIBQaj6nkF5KKS3TQtQ5qCzkOkmxIe3KRbBymXxkb5qwUpX5ELD5xFc6FeiafWYY63TmmEAu_lRFCOJ3xDea-ots\",\"qi\":\"lSQi-w9CpyUReMErP1RsBLk7wNtOvs5EQpPqmuMvqW57NBUczScEoPwmUqqabu9V0-Py4dQ57_bapoKRu1R90bvuFnU63SHWEFglZQvJDMeAvmj4sm-Fp0oYu_neotgQ0hzbI5gry7ajdYy9-2lNx_76aBZoOUu9HCJ-UsfSOI8\"}";
		String compact = "eyJhbGciOiJQQkVTMi1IUzI1NitBMTI4S1ciLCJwMnMiOiIyV0NUY0paMVJ2ZF9DSnVKcmlwUTF3IiwicDJjIjo0MDk2LCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiY3R5IjoiandrK2pzb24ifQ.TrqXOwuNUfDV9VPTNbyGvEJ9JMjefAVn-TR1uIxR9p6hsRQh9Tk7BA.Ye9j1qs22DmRSAddIh-VnA.AwhB8lxrlKjFn02LGWEqg27H4Tg9fyZAbFv3p5ZicHpj64QyHC44qqlZ3JEmnZTgQowIqZJ13jbyHB8LgePiqUJ1hf6M2HPLgzw8L-mEeQ0jvDUTrE07NtOerBk8bwBQyZ6g0kQ3DEOIglfYxV8-FJvNBYwbqN1Bck6d_i7OtjSHV-8DIrp-3JcRIe05YKy3Oi34Z_GOiAc1EK21B11c_AE11PII_wvvtRiUiG8YofQXakWd1_O98Kap-UgmyWPfreUJ3lJPnbD4Ve95owEfMGLOPflo2MnjaTDCwQokoJ_xplQ2vNPz8iguLcHBoKllyQFJL2mOWBwqhBo9Oj-O800as5mmLsvQMTflIrIEbbTMzHMBZ8EFW9fWwwFu0DWQJGkMNhmBZQ-3lvqTc-M6-gWA6D8PDhONfP2Oib2HGizwG1iEaX8GRyUpfLuljCLIe1DkGOewhKuKkZh04DKNM5Nbugf2atmU9OP0Ldx5peCUtRG1gMVl7Qup5ZXHTjgPDr5b2N731UooCGAUqHdgGhg0JVJ_ObCTdjsH4CF1SJsdUhrXvYx3HJh2Xd7CwJRzU_3Y1GxYU6-s3GFPbirfqqEipJDBTHpcoCmyrwYjYHFgnlqBZRotRrS95g8F95bRXqsaDY7UgQGwBQBwy665d0zpvTasvfXf_c0MWAl-neFaKOW_Px6g4EUDjG1GWSXV9cLStLw_0ovdApDIFLHYHePyagyHjouQUuGiq7BsYwYrwaF06tgB8hV8omLNfMEmDPJaZUzMuHw6tBDwGkzD-tS_ub9hxrpJ4UsOWnt5rGUyoN2N_c1-TQlXxm5oto14MxnoAyBQBpwIEgSH3Y4ZhwKBhHPjSo0cdwuNdYbGPpb-YUvF-2NZzODiQ1OvWQBRHSbPWYz_xbGkgD504LRtqRwCO7CC_CyyURi1sEssPVsMJRX_U4LFEOc82TiDdqjKOjRUfKK5rqLi8nBE9soQ0DSaOoFQZiGrBrqxDsNYiAYAmxxkos-i3nX4qtByVx85sCE5U_0MqG7COxZWMOPEFrDaepUV-cOyrvoUIng8i8ljKBKxETY2BgPegKBYCxsAUcAkKamSCC9AiBxA0UOHyhTqtlvMksO7AEhNC2-YzPyx1FkhMoS4LLe6E_pFsMlmjA6P1NSge9C5G5tETYXGAn6b1xZbHtmwrPScro9LWhVmAaA7_bxYObnFUxgWtK4vzzQBjZJ36UTk4OTB-JvKWgfVWCFsaw5WCHj6Oo4jpO7d2yN7WMfAj2hTEabz9wumQ0TMhBduZ-QON3pYObSy7TSC1vVme0NJrwF_cJRehKTFmdlXGVldPxZCplr7ZQqRQhF8JP-l4mEQVnCaWGn9ONHlemczGOS-A-wwtnmwjIB1V_vgJRf4FdpV-4hUk4-QLpu3-1lWFxrtZKcggq3tWTduRo5_QebQbUUT_VSCgsFcOmyWKoj56lbxthN19hq1XGWbLGfrrR6MWh23vk01zn8FVwi7uFwEnRYSafsnWLa1Z5TpBj9GvAdl2H9NHwzpB5NqHpZNkQ3NMDj13Fn8fzO0JB83Etbm_tnFQfcb13X3bJ15Cz-Ww1MGhvIpGGnMBT_ADp9xSIyAM9dQ1yeVXk-AIgWBUlN5uyWSGyCxp0cJwx7HxM38z0UIeBu-MytL-eqndM7LxytsVzCbjOTSVRmhYEMIzUAnS1gs7uMQAGRdgRIElTJESGMjb_4bZq9s6Ve1LKkSi0_QDsrABaLe55UY0zF4ZSfOV5PMyPtocwV_dcNPlxLgNAD1BFX_Z9kAdMZQW6fAmsfFle0zAoMe4l9pMESH0JB4sJGdCKtQXj1cXNydDYozF7l8H00BV_Er7zd6VtIw0MxwkFCTatsv_R-GsBCH218RgVPsfYhwVuT8R4HarpzsDBufC4r8_c8fc9Z278sQ081jFjOja6L2x0N_ImzFNXU6xwO-Ska-QeuvYZ3X_L31ZOX4Llp-7QSfgDoHnOxFv1Xws-D5mDHD3zxOup2b2TppdKTZb9eW2vxUVviM8OI9atBfPKMGAOv9omA-6vv5IxUH0-lWMiHLQ_g8vnswp-Jav0c4t6URVUzujNOoNd_CBGGVnHiJTCHl88LQxsqLHHIu4Fz-U2SGnlxGTj0-ihit2ELGRv4vO8E1BosTmf0cx3qgG0Pq0eOLBDIHsrdZ_CCAiTc0HVkMbyq1M6qEhM-q5P6y1QCIrwg.0HFmhOzsQ98nNWJjIHkR7A";
		String[] splitCompact = compact.split("\\.");
		
		// 32 bytes
		byte[] cek = new byte[] { (byte)111, (byte)27, (byte)25, (byte)52, (byte)66, (byte)29, (byte)20, (byte)78, (byte)92, (byte)176, (byte)56, (byte)240, (byte)65, (byte)208, (byte)82, (byte)112, (byte)161, (byte)131, (byte)36, (byte)55, (byte)202, (byte)236, (byte)185, (byte)172, (byte)129, (byte)23, (byte)153, (byte)194, (byte)195, (byte)48, (byte)253, (byte)182 };
		// 16 bytes
		byte[] p2s = new byte[] { (byte)217, (byte)96, (byte)147, (byte)112, (byte)150, (byte)117, (byte)70, (byte)247, (byte)127, (byte)8, (byte)155, (byte)137, (byte)174, (byte)42, (byte)80, (byte)215 };
		// 16 bytes
		byte[] iv = new byte[] { (byte)97, (byte)239, (byte)99, (byte)214, (byte)171, (byte)54, (byte)216, (byte)57, (byte)145, (byte)72, (byte)7, (byte)93, (byte)34, (byte)31, (byte)149, (byte)156 };
		String password = "Thus from my lips, by yours, my sin is purged.";
		
		// {
		// 	"kty":"oct",
		// 	"p":"VGh1cyBmcm9tIG15IGxpcHMsIGJ5IHlvdXJzLCBteSBzaW4gaXMgcHVyZ2VkLg"
		// }
		Mono<GenericPBES2JWK> key = pbesJWKBuilder
			.password(Base64.getUrlEncoder().withoutPadding().encodeToString(password.getBytes()))
			.build();
		
		SecureRandom secureRandom = new SecureRandom() {
			@Override
			public void nextBytes(byte[] bytes) {
				if(bytes.length == 32) {
					System.arraycopy(cek, 0, bytes, 0, cek.length);
					return;
				}
				else if(bytes.length == 16) {
					System.arraycopy(iv, 0, bytes, 0, iv.length);
					return;
				}
				super.nextBytes(bytes);
			}
		};
		
		// {"alg":"PBES2-HS256+A128KW","p2s":"2WCTcJZ1Rvd_CJuJripQ1w","p2c":4096,"enc":"A128CBC-HS256","cty":"jwk+json"}
		JWE<JWK> jwe = jweService.<JWK>builder(key)
			.secureRandom(secureRandom)
			.header(header -> header
				.algorithm("PBES2-HS256+A128KW")
				.encryptionAlgorithm("A128CBC-HS256")
				.contentType("jwk+json")
				.addCustomParameter("p2c", 4096)
				.addCustomParameter("p2s", "2WCTcJZ1Rvd_CJuJripQ1w")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// We must have the same representation since algorithms are deterministic
		Assertions.assertEquals(compact, jwe.toCompact());
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("PBES2-HS256+A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("jwk+json", jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals(Map.<String, Object>of("p2c", 4096, "p2s", "2WCTcJZ1Rvd_CJuJripQ1w"), jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		Assertions.assertNotNull(jwe.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jweSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwe.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		
		Assertions.assertInstanceOf(RSAJWK.class, jwe.getPayload());
		RSAJWK jwePayload = (RSAJWK)jwe.getPayload();
		Assertions.assertEquals(payload.getKeyType(), jwePayload.getKeyType());
		Assertions.assertEquals(payload.getKeyId(), jwePayload.getKeyId());
		Assertions.assertEquals(payload.getPublicKeyUse(), jwePayload.getPublicKeyUse());
		Assertions.assertEquals(payload.getModulus(), jwePayload.getModulus());
		Assertions.assertEquals(payload.getPublicExponent(), jwePayload.getPublicExponent());
		Assertions.assertEquals(payload.getPrivateExponent(), jwePayload.getPrivateExponent());
		Assertions.assertEquals(payload.getFirstPrimeFactor(), jwePayload.getFirstPrimeFactor());
		Assertions.assertEquals(payload.getSecondPrimeFactor(), jwePayload.getSecondPrimeFactor());
		Assertions.assertEquals(payload.getFirstFactorExponent(), jwePayload.getFirstFactorExponent());
		Assertions.assertEquals(payload.getSecondFactorExponent(), jwePayload.getSecondFactorExponent());
		Assertions.assertEquals(payload.getFirstCoefficient(), jwePayload.getFirstCoefficient());
		Assertions.assertNull(jwePayload.getOtherPrimesInfo());
		Assertions.assertNull(jwePayload.getAlgorithm());
		Assertions.assertNull(jwePayload.getKeyOperations());
		Assertions.assertTrue(jwePayload.getX509Certificate().isEmpty());
		Assertions.assertNull(jwePayload.getX509CertificateChain());
		Assertions.assertNull(jwePayload.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwePayload.getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwePayload.getX509CertificateURL());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		jwe = jweService.reader(JWK.class, key).read(jwe.toCompact(), "jwk+json").block();
		jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("PBES2-HS256+A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("jwk+json", jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals(Map.<String, Object>of("p2c", 4096, "p2s", "2WCTcJZ1Rvd_CJuJripQ1w"), jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		Assertions.assertNotNull(jwe.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jweSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwe.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		
		Assertions.assertInstanceOf(RSAJWK.class, jwe.getPayload());
		jwePayload = (RSAJWK)jwe.getPayload();
		Assertions.assertEquals(payload.getKeyType(), jwePayload.getKeyType());
		Assertions.assertEquals(payload.getKeyId(), jwePayload.getKeyId());
		Assertions.assertEquals(payload.getPublicKeyUse(), jwePayload.getPublicKeyUse());
		Assertions.assertEquals(payload.getModulus(), jwePayload.getModulus());
		Assertions.assertEquals(payload.getPublicExponent(), jwePayload.getPublicExponent());
		Assertions.assertEquals(payload.getPrivateExponent(), jwePayload.getPrivateExponent());
		Assertions.assertEquals(payload.getFirstPrimeFactor(), jwePayload.getFirstPrimeFactor());
		Assertions.assertEquals(payload.getSecondPrimeFactor(), jwePayload.getSecondPrimeFactor());
		Assertions.assertEquals(payload.getFirstFactorExponent(), jwePayload.getFirstFactorExponent());
		Assertions.assertEquals(payload.getSecondFactorExponent(), jwePayload.getSecondFactorExponent());
		Assertions.assertEquals(payload.getFirstCoefficient(), jwePayload.getFirstCoefficient());
		Assertions.assertNull(jwePayload.getOtherPrimesInfo());
		Assertions.assertNull(jwePayload.getAlgorithm());
		Assertions.assertNull(jwePayload.getKeyOperations());
		Assertions.assertTrue(jwePayload.getX509Certificate().isEmpty());
		Assertions.assertNull(jwePayload.getX509CertificateChain());
		Assertions.assertNull(jwePayload.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwePayload.getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwePayload.getX509CertificateURL());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC7518_C() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		ObjectMapper mapper = mapper();
		GenericECJWKBuilder ecJWKBuilder = ecJWKBuilder(mapper);
		
		String payload = "Live long and prosper.";
		
		// Bob
		// {
		//   "kty":"EC",
        //   "crv":"P-256",
        //   "x":"weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ",
        //   "y":"e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck",
		//   "d":"VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw"
        // }
		Mono<GenericECJWK> bobPublicKey = ecJWKBuilder
			.curve("P-256")
			.xCoordinate("weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ")
			.yCoordinate("e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck")
			.build();
		
		Mono<GenericECJWK> bobPrivateKey = ecJWKBuilder
			.curve("P-256")
			.xCoordinate("weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ")
			.yCoordinate("e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck")
			.eccPrivateKey("VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw")
			.build();
		
		
		// {
		//   "alg":"ECDH-ES",
		//   "enc":"A128GCM",
		//   "apu":"QWxpY2U",
		//   "apv":"Qm9i",
		//   "epk": {
		//     "kty":"EC",
		//     "crv":"P-256",
		//     "x":"gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0",
		//     "y":"SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps"
		//   }
		// }
		JWE<String> jwe = jweService.builder(String.class, bobPublicKey)
			.header(header -> header
				.algorithm("ECDH-ES")
				.encryptionAlgorithm("A128GCM")
				.contentType("text/plain")
				.addCustomParameter("apu", "QWxpY2U")
				.addCustomParameter("apv", "Qm9i")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("text/plain", jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		ECJWK epk = (ECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("P-256", epk.getCurve());
		Assertions.assertNull(epk.getEccPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("EC", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getXCoordinate());
		Assertions.assertNotNull(epk.getYCoordinate());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must be empty
		Assertions.assertTrue(StringUtils.isBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		jwe = jweService.reader(String.class, bobPrivateKey).read(jwe.toCompact(), "text/plain").block();
		
		jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		epk = (ECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("P-256", epk.getCurve());
		Assertions.assertNull(epk.getEccPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("EC", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getXCoordinate());
		Assertions.assertNotNull(epk.getYCoordinate());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must be empty
		Assertions.assertTrue(StringUtils.isBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC7518_C_A128KW() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		ObjectMapper mapper = mapper();
		GenericECJWKBuilder ecJWKBuilder = ecJWKBuilder(mapper);
		
		String payload = "Live long and prosper.";
		
		// Bob
		// {
		//   "kty":"EC",
        //   "crv":"P-256",
        //   "x":"weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ",
        //   "y":"e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck",
		//   "d":"VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw"
        // }
		Mono<GenericECJWK> bobPublicKey = ecJWKBuilder
			.curve("P-256")
			.xCoordinate("weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ")
			.yCoordinate("e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck")
			.build();
		
		Mono<GenericECJWK> bobPrivateKey = ecJWKBuilder
			.curve("P-256")
			.xCoordinate("weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ")
			.yCoordinate("e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck")
			.eccPrivateKey("VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw")
			.build();
		
		
		// {
		//   "alg":"ECDH-ES",
		//   "enc":"A128GCM",
		//   "apu":"QWxpY2U",
		//   "apv":"Qm9i",
		//   "epk": {
		//     "kty":"EC",
		//     "crv":"P-256",
		//     "x":"gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0",
		//     "y":"SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps"
		//   }
		// }
		JWE<String> jwe = jweService.builder(String.class, bobPublicKey)
			.header(header -> header
				.algorithm("ECDH-ES+A128KW")
				.encryptionAlgorithm("A128GCM")
				.contentType("text/plain")
				.addCustomParameter("apu", "QWxpY2U")
				.addCustomParameter("apv", "Qm9i")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES+A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("text/plain", jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		ECJWK epk = (ECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("P-256", epk.getCurve());
		Assertions.assertNull(epk.getEccPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("EC", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getXCoordinate());
		Assertions.assertNotNull(epk.getYCoordinate());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must not be empty
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		jwe = jweService.reader(String.class, bobPrivateKey).read(jwe.toCompact(), "text/plain").block();
		
		jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES+A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		epk = (ECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("P-256", epk.getCurve());
		Assertions.assertNull(epk.getEccPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("EC", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getXCoordinate());
		Assertions.assertNotNull(epk.getYCoordinate());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must not be empty
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC8037_A6() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		ObjectMapper mapper = mapper();
		GenericXECJWKBuilder xecJWKBuilder = xecJWKBuilder(mapper);
		
		String payload = "Live long and prosper.";
		
		// Bob
		// {
		//   "kty":"OKP",
        //   "crv":"X25519",
        //   "x":"3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08",
        //   "d":"XasIfmJKikt54X-Lg4AO5m87sSkmGLb9HC-LJ_-I4Os",
        // }
		Mono<GenericXECJWK> bobPublicKey = xecJWKBuilder
			.curve("X25519")
			.publicKey("3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08")
			.build();
		
		Mono<GenericXECJWK> bobPrivateKey = xecJWKBuilder
			.curve("X25519")
			.publicKey("3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08")
			.privateKey("XasIfmJKikt54X-Lg4AO5m87sSkmGLb9HC-LJ_-I4Os")
			.build();
		
		// {
		//   "alg":"ECDH-ES",
		//   "enc":"A128GCM",
		//   "apu":"QWxpY2U",
		//   "apv":"Qm9i",
		//   "epk": {
		//     "kty":"EC",
		//     "crv":"P-256",
		//     "x":"gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0",
		//     "y":"SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps"
		//   }
		// }
		JWE<String> jwe = jweService.builder(String.class, bobPublicKey)
			.header(header -> header
					.algorithm("ECDH-ES")
					.encryptionAlgorithm("A128GCM")
					.contentType("text/plain")
					.addCustomParameter("apu", "QWxpY2U")
					.addCustomParameter("apv", "Qm9i")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("text/plain", jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		XECJWK epk = (XECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("X25519", epk.getCurve());
		Assertions.assertNull(epk.getPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("OKP", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getPublicKey());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must be empty
		Assertions.assertTrue(StringUtils.isBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		jwe = jweService.reader(String.class, bobPrivateKey).read(jwe.toCompact(), "text/plain").block();
		
		jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		epk = (XECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("X25519", epk.getCurve());
		Assertions.assertNull(epk.getPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("OKP", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getPublicKey());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must be empty
		Assertions.assertTrue(StringUtils.isBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC8037_A6_A128KW() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		ObjectMapper mapper = mapper();
		GenericXECJWKBuilder xecJWKBuilder = xecJWKBuilder(mapper);
		
		String payload = "Live long and prosper.";
		
		// Bob
		// {
		//   "kty":"OKP",
        //   "crv":"X25519",
        //   "x":"3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08",
        //   "d":"XasIfmJKikt54X-Lg4AO5m87sSkmGLb9HC-LJ_-I4Os",
        // }
		Mono<GenericXECJWK> bobPublicKey = xecJWKBuilder
			.curve("X25519")
			.publicKey("3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08")
			.build();
		
		Mono<GenericXECJWK> bobPrivateKey = xecJWKBuilder
			.curve("X25519")
			.publicKey("3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08")
			.privateKey("XasIfmJKikt54X-Lg4AO5m87sSkmGLb9HC-LJ_-I4Os")
			.build();
		
		// {
		//   "alg":"ECDH-ES",
		//   "enc":"A128GCM",
		//   "apu":"QWxpY2U",
		//   "apv":"Qm9i",
		//   "epk": {
		//     "kty":"EC",
		//     "crv":"P-256",
		//     "x":"gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0",
		//     "y":"SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps"
		//   }
		// }
		JWE<String> jwe = jweService.builder(String.class, bobPublicKey)
			.header(header -> header
					.algorithm("ECDH-ES+A128KW")
					.encryptionAlgorithm("A128GCM")
					.contentType("text/plain")
					.addCustomParameter("apu", "QWxpY2U")
					.addCustomParameter("apv", "Qm9i")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES+A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("text/plain", jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		XECJWK epk = (XECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("X25519", epk.getCurve());
		Assertions.assertNull(epk.getPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("OKP", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getPublicKey());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must not be empty
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		jwe = jweService.reader(String.class, bobPrivateKey).read(jwe.toCompact(), "text/plain").block();
		
		jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES+A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		epk = (XECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("X25519", epk.getCurve());
		Assertions.assertNull(epk.getPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("OKP", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getPublicKey());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must not be empty
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC8037_A7() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		ObjectMapper mapper = mapper();
		GenericXECJWKBuilder xecJWKBuilder = xecJWKBuilder(mapper);
		
		String payload = "Live long and prosper.";
		
		// Bob
		// {
		//   "kty":"OKP",
        //   "crv":"X25519",
        //   "x":"3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08",
        //   "d":"XasIfmJKikt54X-Lg4AO5m87sSkmGLb9HC-LJ_-I4Os",
        // }
		Mono<GenericXECJWK> bobPublicKey = xecJWKBuilder
			.curve("X448")
			.publicKey("PreoKbDNIPW8_AtZm2_sz22kYnEHvbDU80W0MCfYuXL8PjT7QjKhPKcG3LV67D2uB73BxnvzNgk")
			.build();
		
		Mono<GenericXECJWK> bobPrivateKey = xecJWKBuilder
			.curve("X448")
			.publicKey("PreoKbDNIPW8_AtZm2_sz22kYnEHvbDU80W0MCfYuXL8PjT7QjKhPKcG3LV67D2uB73BxnvzNgk")
			.privateKey("HDBqesKg4uCZCylEcMujOeZFN3KwdYEdj60NHWknwSC7XuiXKw0-ITdMnJIbCdGwNm8QtlFzmS0")
			.build();
		
		// {
		//   "alg":"ECDH-ES",
		//   "enc":"A128GCM",
		//   "apu":"QWxpY2U",
		//   "apv":"Qm9i",
		//   "epk": {
		//     "kty":"EC",
		//     "crv":"P-256",
		//     "x":"gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0",
		//     "y":"SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps"
		//   }
		// }
		JWE<String> jwe = jweService.builder(String.class, bobPublicKey)
			.header(header -> header
					.algorithm("ECDH-ES")
					.encryptionAlgorithm("A128GCM")
					.contentType("text/plain")
					.addCustomParameter("apu", "QWxpY2U")
					.addCustomParameter("apv", "Qm9i")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("text/plain", jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		XECJWK epk = (XECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("X448", epk.getCurve());
		Assertions.assertNull(epk.getPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("OKP", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getPublicKey());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must be empty
		Assertions.assertTrue(StringUtils.isBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		jwe = jweService.reader(String.class, bobPrivateKey).read(jwe.toCompact(), "text/plain").block();
		
		jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		epk = (XECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("X448", epk.getCurve());
		Assertions.assertNull(epk.getPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("OKP", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getPublicKey());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must be empty
		Assertions.assertTrue(StringUtils.isBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC8037_A7_A128KW() {
		GenericJWEService jweService = jweService(mapperRFC7516());
		ObjectMapper mapper = mapper();
		GenericXECJWKBuilder xecJWKBuilder = xecJWKBuilder(mapper);
		
		String payload = "Live long and prosper.";
		
		// Bob
		// {
		//   "kty":"OKP",
        //   "crv":"X25519",
        //   "x":"3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08",
        //   "d":"XasIfmJKikt54X-Lg4AO5m87sSkmGLb9HC-LJ_-I4Os",
        // }
		Mono<GenericXECJWK> bobPublicKey = xecJWKBuilder
			.curve("X448")
			.publicKey("PreoKbDNIPW8_AtZm2_sz22kYnEHvbDU80W0MCfYuXL8PjT7QjKhPKcG3LV67D2uB73BxnvzNgk")
			.build();
		
		Mono<GenericXECJWK> bobPrivateKey = xecJWKBuilder
			.curve("X448")
			.publicKey("PreoKbDNIPW8_AtZm2_sz22kYnEHvbDU80W0MCfYuXL8PjT7QjKhPKcG3LV67D2uB73BxnvzNgk")
			.privateKey("HDBqesKg4uCZCylEcMujOeZFN3KwdYEdj60NHWknwSC7XuiXKw0-ITdMnJIbCdGwNm8QtlFzmS0")
			.build();
		
		// {
		//   "alg":"ECDH-ES+A128KW",
		//   "enc":"A128GCM",
		//   "apu":"QWxpY2U",
		//   "apv":"Qm9i",
		//   "epk": {
		//     "kty":"EC",
		//     "crv":"P-256",
		//     "x":"gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0",
		//     "y":"SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps"
		//   }
		// }
		JWE<String> jwe = jweService.builder(String.class, bobPublicKey)
			.header(header -> header
					.algorithm("ECDH-ES+A128KW")
					.encryptionAlgorithm("A128GCM")
					.contentType("text/plain")
					.addCustomParameter("apu", "QWxpY2U")
					.addCustomParameter("apv", "Qm9i")
			)
			.payload(payload)
			.build()
			.block();
		
		String[] jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES+A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("text/plain", jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		XECJWK epk = (XECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("X448", epk.getCurve());
		Assertions.assertNull(epk.getPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("OKP", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getPublicKey());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must not be empty
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
		
		// Check that we can read what we generate
		jwe = jweService.reader(String.class, bobPrivateKey).read(jwe.toCompact(), "text/plain").block();
		
		jweSplitCompact = jwe.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[0]));
		Assertions.assertEquals("ECDH-ES+A128KW", jwe.getHeader().getAlgorithm());
		Assertions.assertNull(jwe.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getContentType()); // This is not present in the encoded header
		Assertions.assertNull(jwe.getHeader().getCritical());
		Assertions.assertNotNull(jwe.getHeader().getCustomParameters());
		Assertions.assertEquals("QWxpY2U", jwe.getHeader().getCustomParameters().get("apu"));
		Assertions.assertEquals("Qm9i", jwe.getHeader().getCustomParameters().get("apv"));
		epk = (XECJWK)jwe.getHeader().getCustomParameters().get("epk");
		Assertions.assertNotNull(epk);
		Assertions.assertNull(epk.getAlgorithm());
		Assertions.assertEquals("X448", epk.getCurve());
		Assertions.assertNull(epk.getPrivateKey());
		Assertions.assertNull(epk.getKeyId());
		Assertions.assertNull(epk.getKeyOperations());
		Assertions.assertEquals("OKP", epk.getKeyType());
		Assertions.assertNull(epk.getPublicKeyUse());
		Assertions.assertTrue(epk.getX509Certificate().isEmpty());
		Assertions.assertNull(epk.getX509CertificateChain());
		Assertions.assertNull(epk.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(epk.getX509CertificateSHA256Thumbprint());
		Assertions.assertNotNull(epk.getPublicKey());
		Assertions.assertEquals("A128GCM", jwe.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwe.getHeader().getJWK());
		Assertions.assertNull(jwe.getHeader().getJWKSetURL());
		Assertions.assertNull(jwe.getHeader().getKey());
		Assertions.assertNull(jwe.getHeader().getKeyId());
		Assertions.assertNull(jwe.getHeader().getType());
		Assertions.assertNull(jwe.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwe.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key must be empty
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[1]));
		
		// 2. JWE Initialization vector
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[2]));
		
		// 3. JWE cipher text
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[3]));
		Assertions.assertEquals(payload, jwe.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jweSplitCompact[4]));
		Assertions.assertNotNull(jwe.getAuthenticationTag());
	}
	
	@Test
	public void testJoseJson() {
		GenericJWKService jwkService = jwkService();
		GenericJWEService jweService = jweService(mapper());
		
		String payload = "Do or do not, there's no try.";
		
		Mono<? extends OCTJWK> key1 = jwkService.oct().generator().algorithm(OCTAlgorithm.A128KW.getAlgorithm()).keyId("key1").generate().cache();
		
		JsonJWE<String, JsonJWE.BuiltRecipient<String>> jsonJWEPayload = jweService.jsonBuilder(String.class)
			.headers(pheader -> pheader.encryptionAlgorithm("A128CBC-HS256"), uheader -> uheader.contentType(MediaTypes.TEXT_PLAIN))
			.recipient(
				header -> header
					.algorithm(OCTAlgorithm.A128KW.getAlgorithm())
					.keyId("key1"), 
				key1
			)
			.payload(payload)
			.build()
			.block();
		
		Mono<? extends OCTJWK> key2 = jwkService.oct().generator().algorithm(OCTAlgorithm.A128KW.getAlgorithm()).keyId("key2").generate().cache();
		
		JWE<JsonJWE<String, JsonJWE.BuiltRecipient<String>>> jwe = jweService.<JsonJWE<String, JsonJWE.BuiltRecipient<String>>>builder(JsonJWE.class, key2)
			.header(header -> header
				.encryptionAlgorithm("A128CBC-HS256")
				.algorithm(OCTAlgorithm.A128KW.toString())
				.keyId("key2")
				.contentType(MediaTypes.APPLICATION_JOSE_JSON)
			)
			.payload(jsonJWEPayload)
			.build()
			.block();
		
		Assertions.assertEquals(jsonJWEPayload, jwe.getPayload());
		
		JWE<JsonJWE<String, JsonJWE.ReadRecipient<String>>> readJWE = jweService.<JsonJWE<String, JsonJWE.ReadRecipient<String>>>reader(JsonJWE.class, key2)
			.read(jwe.toCompact())
			.block();
		
		Assertions.assertEquals(jsonJWEPayload.toJson(), readJWE.getPayload().toJson());
		
		Assertions.assertEquals(1, readJWE.getPayload().getRecipients().size());
		
		// Check that signature is valid
		readJWE.getPayload().getRecipients().get(0).readJWE(key1).block();
	}
	
	private static ObjectMapper mapper() {
		return new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
	}
	
	private static ObjectMapper mapperRFC7516() {
		SimpleModule module = new SimpleModule();
		module.addSerializer(GenericJWEHeader.class, new GenericJWEServiceTest.RFC7516JWEHeaderSerializer());
		module.addSerializer(RSAJWK.class, new GenericJWEServiceTest.RFC7516RSAJWKSerializer());
		
		return new ObjectMapper()
			.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
			.registerModule(module);
	}
	
	private static GenericECJWKBuilder ecJWKBuilder(ObjectMapper mapper) {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, mapper);
		urlResolver.setResourceService(resourceService);
		
		return new GenericECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private static GenericXECJWKBuilder xecJWKBuilder(ObjectMapper mapper) {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, mapper);
		urlResolver.setResourceService(resourceService);
		
		return new GenericXECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private GenericOCTJWKBuilder octJWKBuilder(ObjectMapper mapper) {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);

		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, mapper);
		urlResolver.setResourceService(resourceService);
		
		return new GenericOCTJWKBuilder(configuration, jwkStore, keyResolver);
	}
	
	private GenericPBES2JWKBuilder pbes2JWKBuilder(ObjectMapper mapper) {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);

		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, mapper);
		urlResolver.setResourceService(resourceService);
		
		return new GenericPBES2JWKBuilder(configuration, jwkStore, keyResolver);
	}
	
	private static GenericRSAJWKBuilder rsaJWKBuilder(ObjectMapper mapper) {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);

		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, mapper);
		urlResolver.setResourceService(resourceService);
		
		return new GenericRSAJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	@SuppressWarnings("unchecked")
	private static GenericDataConversionService dataConversionService(JWKService jwkService, ObjectMapper mapper) {
		MediaTypeConverter<String> textStringConverter = Mockito.mock(MediaTypeConverter.class);
		Mockito.when(textStringConverter.canConvert("text/plain")).thenReturn(true);

		Mockito.when(textStringConverter.encode(Mockito.anyString(), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, String.class));		
		Mockito.when(textStringConverter.encodeOne(Mockito.isA(Mono.class), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class));
		Mockito.when(textStringConverter.decode(Mockito.anyString(), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, String.class));
		Mockito.when(textStringConverter.decodeOne(Mockito.isA(Publisher.class), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class));
		
		MediaTypeConverter<String> jsonStringConverter = Mockito.mock(MediaTypeConverter.class);
		Mockito.when(jsonStringConverter.canConvert("application/json")).thenReturn(true);
		
		Mockito.when(jsonStringConverter.encode(Mockito.any(), Mockito.isA(Type.class))).thenAnswer(ans -> mapper.writeValueAsString(ans.getArgument(0)));
		Mockito.when(jsonStringConverter.encodeOne(Mockito.isA(Mono.class), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class).map(obj -> {
			try {
				return mapper.writeValueAsString(obj);
			} 
			catch(JsonProcessingException ex) {
				throw Exceptions.propagate(ex);
			}
		}));
		Mockito.when(jsonStringConverter.decode(Mockito.anyString(), Mockito.isA(Type.class))).thenAnswer(ans -> mapper.readValue((String)ans.getArgument(0), mapper.constructType((Type)ans.getArgument(1))));
		Mockito.when(jsonStringConverter.decodeOne(Mockito.isA(Publisher.class), Mockito.isA(Type.class))).thenAnswer(
			ans -> Flux.from(ans.getArgument(0, Publisher.class))
				.cast(String.class)
				.reduceWith(() -> new StringBuilder(), (acc, v) -> ((StringBuilder)acc).append(v))
				.map(Object::toString)
				.map(obj -> {
					try {
						return mapper.readValue((String)obj, mapper.constructType((Type)ans.getArgument(1)));
					} 
					catch(JsonProcessingException ex) {
						throw Exceptions.propagate(ex);
					}
				})
		);
		
		return new GenericDataConversionService(List.of(textStringConverter, jsonStringConverter), jwkService, mapper);
	}
	
	private static GenericJWKService jwkService() {
		return jwkService(null);
	}
	
	private static GenericJWKService jwkService(PKIXParameters pkixParameters) {
		ObjectMapper mapper = new ObjectMapper();
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		JWKStore jwkStore = new NoOpJWKStore();
		if(pkixParameters == null) {
			pkixParameters = new JWKPKIXParameters().get();
		}
		GenericX509JWKCertPathValidator certPathValidator = new GenericX509JWKCertPathValidator(configuration, pkixParameters, WORKER_POOL);
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, certPathValidator, mapper);
		
		GenericECJWKFactory ecJWKFactory = new GenericECJWKFactory(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
		GenericRSAJWKFactory rsaJWKFactory = new GenericRSAJWKFactory(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
		GenericOCTJWKFactory symetricJWKFactory = new GenericOCTJWKFactory(configuration, jwkStore, keyResolver, mapper);
		GenericEdECJWKFactory edecJWKFactory = new GenericEdECJWKFactory(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
		GenericXECJWKFactory xecJWKFactory = new GenericXECJWKFactory(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
		GenericPBES2JWKFactory pbes2JWKFactory = new GenericPBES2JWKFactory(configuration, jwkStore, keyResolver, mapper);
		
		return new GenericJWKService(ecJWKFactory , rsaJWKFactory, symetricJWKFactory, edecJWKFactory, xecJWKFactory, pbes2JWKFactory, jwkStore, urlResolver, mapper);
	}
	
	private static PKIXParameters pkixParameters(String cert, Date date) throws CertificateException, InvalidAlgorithmParameterException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		
		X509Certificate certificate = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(cert)));
		PKIXParameters parameters = new PKIXParameters(Set.of(new TrustAnchor(certificate, null)));
		parameters.setRevocationEnabled(false);
		parameters.setDate(date);
			
		return parameters;
	}
	
	private static GenericJWEService jweService(ObjectMapper mapper) {
		GenericJWKService jwkService = jwkService();
		GenericDataConversionService dataConversionService = dataConversionService(jwkService, mapper);
		GenericJWEService jweService = new GenericJWEService(mapper, dataConversionService, jwkService);
		jweService.init();
		
		return jweService;
	}
	
	private static class RFC7516JWEHeaderSerializer extends StdSerializer<JWEHeader> {

		private static final long serialVersionUID = 1L;

		public RFC7516JWEHeaderSerializer() {
			this(null);
		}

		public RFC7516JWEHeaderSerializer(Class<JWEHeader> t) {
			super(t);
		}

		@Override
		public void serialize(JWEHeader jweHeader, JsonGenerator jg, SerializerProvider sp) throws IOException {
			jg.writeStartObject();
			if(jweHeader.getType() != null) {
				jg.writeStringField("typ", jweHeader.getType());
			}
			if(jweHeader.getAlgorithm() != null) {
				jg.writeStringField("alg", jweHeader.getAlgorithm());
			}
			if(jweHeader.getJWKSetURL()!= null) {
				jg.writeStringField("jku", jweHeader.getJWKSetURL().toString());
			}
			if(jweHeader.getKeyId()!= null) {
				jg.writeStringField("kid", jweHeader.getKeyId());
			}
			Map<String, Object> customParameters = jweHeader.getCustomParameters();
			if(customParameters != null) {
				if(customParameters.containsKey("p2s")) {
					jg.writeStringField("p2s", (String)customParameters.get("p2s"));
				}
				if(customParameters.containsKey("p2c")) {
					jg.writeNumberField("p2c", (Integer)customParameters.get("p2c"));
				}
				if(customParameters.containsKey("apu")) {
					jg.writeStringField("apu", (String)customParameters.get("apu"));
				}
				if(customParameters.containsKey("apv")) {
					jg.writeStringField("apv", (String)customParameters.get("apv"));
				}
				if(customParameters.containsKey("epk")) {
					jg.writeObjectField("epk", customParameters.get("epk"));
				}
			}
			if(jweHeader.getEncryptionAlgorithm() != null) {
				jg.writeStringField("enc", jweHeader.getEncryptionAlgorithm());
			}
			if(jweHeader.getContentType() != null && jweHeader.getContentType().equals("jwk+json")) {
				jg.writeStringField("cty", jweHeader.getContentType());
			}
			jg.writeEndObject();
		}
	}
	
	private static class RFC7516RSAJWKSerializer extends StdSerializer<RSAJWK> {

		private static final long serialVersionUID = 1L;

		public RFC7516RSAJWKSerializer() {
			this(null);
		}

		public RFC7516RSAJWKSerializer(Class<RSAJWK> t) {
			super(t);
		}

		@Override
		public void serialize(RSAJWK rsaJWK, JsonGenerator jg, SerializerProvider sp) throws IOException {
			
			jg.writeStartObject();
			jg.writeStringField("kty", rsaJWK.getKeyType());
			if(rsaJWK.getKeyId() != null) {
				jg.writeStringField("kid", rsaJWK.getKeyId());
			}
			if(rsaJWK.getPublicKeyUse()!= null) {
				jg.writeStringField("use", rsaJWK.getPublicKeyUse());
			}
			jg.writeStringField("n", rsaJWK.getModulus());
			jg.writeStringField("e", rsaJWK.getPublicExponent());
			if(rsaJWK.getPrivateExponent() != null) {
				jg.writeStringField("d", rsaJWK.getPrivateExponent());
			}
			if(rsaJWK.getFirstPrimeFactor()!= null) {
				jg.writeStringField("p", rsaJWK.getFirstPrimeFactor());
			}
			if(rsaJWK.getSecondPrimeFactor()!= null) {
				jg.writeStringField("q", rsaJWK.getSecondPrimeFactor());
			}
			if(rsaJWK.getFirstFactorExponent() != null) {
				jg.writeStringField("dp", rsaJWK.getFirstFactorExponent());
			}
			if(rsaJWK.getSecondFactorExponent()!= null) {
				jg.writeStringField("dq", rsaJWK.getSecondFactorExponent());
			}
			if(rsaJWK.getFirstCoefficient()!= null) {
				jg.writeStringField("qi", rsaJWK.getFirstCoefficient());
			}
			jg.writeEndObject();
		}
	}
}
