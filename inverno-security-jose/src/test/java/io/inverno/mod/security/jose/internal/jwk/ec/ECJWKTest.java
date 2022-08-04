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
package io.inverno.mod.security.jose.internal.jwk.ec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKKeyResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKURLResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericX509JWKCertPathValidator;
import io.inverno.mod.security.jose.internal.jwk.NoOpJWKStore;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class ECJWKTest {
	
	private static final ExecutorService WORKER_POOL = Executors.newCachedThreadPool();
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7515#appendix-A.3">RFC7515 Appendix A.3</a> Example JWS Using ECDSA P-256 SHA-256
	 * 
     * {
	 *     "kty":"EC",
     *     "crv":"P-256",
     *     "x":"f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
     *     "y":"x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0",
     *     "d":"jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI"
     * }
	 */
	@Test
	public void testRFC7515_A3() {
		ECJWK jwk = ecJWKBuilder()
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
			.yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
			.build()
			.block();
		
		String jws = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q";
		String[] jws_parts = jws.split("\\.");
		
		Assertions.assertTrue(jwk.signer("ES256").verify((jws_parts[0]+"."+jws_parts[1]).getBytes(), Base64.getUrlDecoder().decode(jws_parts[2])));
	}
	
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7515#appendix-A.4">RFC7515 Appendix A.4</a> Example JWS Using ECDSA P-521 SHA-512
	 * 
	 * {
	 *     "kty":"EC",
     *     "crv":"P-521",
     *     "x":"AekpBQ8ST8a8VcfVOTNl353vSrDCLLJXmPk06wTjxrrjcBpXp5EOnYG_NjFZ6OvLFV1jSfS9tsz4qUxcWceqwQGk",
     *     "y":"ADSmRA43Z1DSNx_RvcLI87cdL07l6jQyyBXMoxVg_l2Th-x3S1WDhjDly79ajL4Kkd0AZMaZmh9ubmf63e3kyMj2",
     *     "d":"AY5pb7A0UFiB3RELSD64fTLOSV_jazdF7fLYyuTw8lOfRhWg6Y6rUrPAxerEzgdRhajnu0ferB0d53vM9mE15j2C"
     * }
	 */
	@Test
	public void testRFC7515_A4() {
		ECJWK jwk = ecJWKBuilder()
			.curve(ECCurve.P_521.getCurve())
			.xCoordinate("AekpBQ8ST8a8VcfVOTNl353vSrDCLLJXmPk06wTjxrrjcBpXp5EOnYG_NjFZ6OvLFV1jSfS9tsz4qUxcWceqwQGk")
			.yCoordinate("ADSmRA43Z1DSNx_RvcLI87cdL07l6jQyyBXMoxVg_l2Th-x3S1WDhjDly79ajL4Kkd0AZMaZmh9ubmf63e3kyMj2")
			.build()
			.block();

		String jws = "eyJhbGciOiJFUzUxMiJ9.UGF5bG9hZA.AdwMgeerwtHoh-l192l60hp9wAHZFVJbLfD_UxMi70cwnZOYaRI1bKPWROc-mZZqwqT2SI-KGDKB34XO0aw_7XdtAG8GaSwFKdCAPZgoXD2YBJZCPEX3xKpRwcdOO8KpEHwJjyqOgzDO7iKvU8vcnwNrmxYbSW9ERBXukOXolLzeO_Jn";
		String[] jws_parts = jws.split("\\.");
		
		Assertions.assertTrue(jwk.signer("ES512").verify((jws_parts[0]+"."+jws_parts[1]).getBytes(), Base64.getUrlDecoder().decode(jws_parts[2])));
	}
	
	@Test
	public void testECGenerator() throws JsonProcessingException {
		String payload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
		String payload64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
		
		// ES256
		ECJWK jwk = new GenericECJWKGenerator().curve(ECCurve.P_256.getCurve()).generate().block();
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		String jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"ES256\"}";
		String jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		String m = jose64 + "." + payload64;
		
		byte[] signature = jwk.signer("ES256").sign(m.getBytes());
		Assertions.assertEquals(64, signature.length); // 2 * order size
		Assertions.assertTrue(jwk.signer("ES256").verify(m.getBytes(), signature));
		
		// ES384
		jwk = new GenericECJWKGenerator().curve(ECCurve.P_384.getCurve()).generate().block();
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"ES384\"}";
		jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		m = jose64 + "." + payload64;
		
		signature = jwk.signer("ES384").sign(m.getBytes());
		Assertions.assertEquals(96, signature.length); // 2 * order size
		Assertions.assertTrue(jwk.signer("ES384").verify(m.getBytes(), signature));
		
		// ES512
		jwk = new GenericECJWKGenerator().curve(ECCurve.P_521.getCurve()).generate().block();
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"ES512\"}";
		jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		m = jose64 + "." + payload64;
		
		signature = jwk.signer("ES512").sign(m.getBytes());
		Assertions.assertEquals(132, signature.length); // 2 * order size
		Assertions.assertTrue(jwk.signer("ES512").verify(m.getBytes(), signature));
	}
	
	@Test
	public void testECX5c() throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, InvalidAlgorithmParameterException {
//		displayCertAndKeyInfo(Path.of("src/test/resources/ec/private_key.der"), Path.of("src/test/resources/ec/cert.der"));
		
		String x = "Y4XGmjbqCMzBEvqd1emId89o9BY4m8v0qla90wdOgFU";
		String y = "E1YlPDekf_PDoHgp3Tcc4kIHVRNZZNwj5nJbg0oORAs";
		String d = "TAGDgB_CWOKKSrIBeglfHHvq0h92OQvE9U-hsslc74c";
		String[] x5c = new String[] {
			"MIIBejCCAR+gAwIBAgIUBJ9kB4wdVLOhl2AwHc5IT66x8C4wCgYIKoZIzj0EAwIwEjEQMA4GA1UEAwwHVEVTVF9FQzAeFw0yMjA1MDYxMjU5NDlaFw0zMjA1MDMxMjU5NDlaMBIxEDAOBgNVBAMMB1RFU1RfRUMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARjhcaaNuoIzMES+p3V6Yh3z2j0Fjiby/SqVr3TB06AVRNWJTw3pH/zw6B4Kd03HOJCB1UTWWTcI+ZyW4NKDkQLo1MwUTAdBgNVHQ4EFgQU/N9EreBMLqUG50OSgin08cNNv2gwHwYDVR0jBBgwFoAU/N9EreBMLqUG50OSgin08cNNv2gwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEAlPEBfYCImPoaS+0Xle2nxHmAlFOJWFd+EhUQNsR5Vk4CIQDutuGsuz5tylXKYUxGanWyGluG2Y+AnbupzyMH/J2HIA=="
		};
		
		Path x5p = Path.of("src/test/resources/ec/cert.der");
		
		ECJWK jwk = ecJWKBuilder(x5p.toUri())
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate(x)
			.yCoordinate(y)
			.x509CertificateChain(x5c)
			.build()
			.block();
		
		Assertions.assertEquals(x, jwk.getXCoordinate());
		Assertions.assertEquals(y, jwk.getYCoordinate());
		Assertions.assertEquals(x5c, jwk.getX509CertificateChain());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		 jwk = ecJWKBuilder(x5p.toUri())
			.curve(ECCurve.P_256.getCurve())
			.x509CertificateChain(x5c)
			.build()
			.block();
		
		Assertions.assertEquals(x, jwk.getXCoordinate());
		Assertions.assertEquals(y, jwk.getYCoordinate());
		Assertions.assertEquals(x5c, jwk.getX509CertificateChain());
		 
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		// Make it fail
		Assertions.assertThrows(JWKProcessingException.class,
			() -> {
				ecJWKBuilder(x5p.toUri())
					.curve(ECCurve.P_256.getCurve())
					.xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
					.yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
					.x509CertificateChain(x5c)
					.build()
					.block();
			},
			"Certificate key does not match JWK parameters"
		);
	}
	
	@Test
	public void testECX5u() throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, InvalidAlgorithmParameterException {
//		displayCertAndKeyInfo(Path.of("src/test/resources/ec/private_key.der"), Path.of("src/test/resources/ec/cert.der"));
		
		String x = "Y4XGmjbqCMzBEvqd1emId89o9BY4m8v0qla90wdOgFU";
		String y = "E1YlPDekf_PDoHgp3Tcc4kIHVRNZZNwj5nJbg0oORAs";
		String d = "TAGDgB_CWOKKSrIBeglfHHvq0h92OQvE9U-hsslc74c";
		URI x5u = Path.of("src/test/resources/ec/cert.der").toUri();
		
		ECJWK jwk = ecJWKBuilder(x5u)
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate(x)
			.yCoordinate(y)
			.x509CertificateURL(x5u)
			.build()
			.block();
		
		Assertions.assertEquals(x, jwk.getXCoordinate());
		Assertions.assertEquals(y, jwk.getYCoordinate());
		Assertions.assertEquals(x5u, jwk.getX509CertificateURL());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		jwk = ecJWKBuilder(x5u)
			.curve(ECCurve.P_256.getCurve())
			.x509CertificateURL(x5u)
			.build()
			.block();
		
		Assertions.assertEquals(x, jwk.getXCoordinate());
		Assertions.assertEquals(y, jwk.getYCoordinate());
		Assertions.assertEquals(x5u, jwk.getX509CertificateURL());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		// Make it fail
		Assertions.assertThrows(JWKProcessingException.class,
			() -> {
				ecJWKBuilder(x5u)
					.curve(ECCurve.P_256.getCurve())
					.xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
					.yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
					.x509CertificateURL(x5u)
					.build()
					.block();
			},
			"Certificate key does not match JWK parameters"
		);
	}
	
	/**
	 * Private key must be in PKCS8 format.
	 * 
	 * @param privateKeyPath
	 * @param certPath 
	 */
	private static void displayCertAndKeyInfo(Path privateKeyPath, Path certPath) throws FileNotFoundException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		try (FileInputStream in = new FileInputStream(certPath.toFile())) {
			X509Certificate certificate = (X509Certificate)cf.generateCertificate(in);
			ECPublicKey publicKey = (ECPublicKey)certificate.getPublicKey();
			
			System.out.println("x5c=" + Base64.getEncoder().encodeToString(certificate.getEncoded()));
			System.out.println("alg=" + certificate.getSigAlgName());
			System.out.println("x=" + Base64.getUrlEncoder().withoutPadding().encodeToString(JOSEUtils.toPaddedUnsignedBytes(publicKey.getW().getAffineX(), ECCurve.P_256.getKeyLength())));
			System.out.println("y=" + Base64.getUrlEncoder().withoutPadding().encodeToString(JOSEUtils.toPaddedUnsignedBytes(publicKey.getW().getAffineY(), ECCurve.P_256.getKeyLength())));
		}
		
		KeyFactory kf = KeyFactory.getInstance("EC");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(privateKeyPath));
		ECPrivateKey privateKey = (ECPrivateKey)kf.generatePrivate(keySpec);
		System.out.println("d=" + Base64.getUrlEncoder().withoutPadding().encodeToString(JOSEUtils.toPaddedUnsignedBytes(privateKey.getS(), ECCurve.P_256.getKeyLength())));
	}
	
	private static GenericECJWKBuilder ecJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private static GenericECJWKBuilder ecJWKBuilder(URI x5u) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, InvalidAlgorithmParameterException {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		Mockito.when(configuration.resolve_x5u()).thenReturn(true);
		PKIXParameters pkixParameters = pkixParameters(Path.of(x5u));
		GenericX509JWKCertPathValidator certPathValidator = new GenericX509JWKCertPathValidator(pkixParameters, WORKER_POOL);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		Mockito.when(resourceService.getResource(x5u)).thenReturn(new FileResource(x5u));
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, certPathValidator);
	}
	
	private static PKIXParameters pkixParameters(Path certPath) throws FileNotFoundException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		try (FileInputStream in = new FileInputStream(certPath.toFile())) {
			X509Certificate certificate = (X509Certificate)cf.generateCertificate(in);
			
			PKIXParameters parameters = new PKIXParameters(Set.of(new TrustAnchor(certificate, null)));
			parameters.setRevocationEnabled(false);
			
			return parameters;
		}
	}
}
