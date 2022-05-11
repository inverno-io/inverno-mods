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
package io.inverno.mod.security.jose.internal.jwk.okp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKKeyResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKURLResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericX509JWKCertPathValidator;
import io.inverno.mod.security.jose.internal.jwk.NoOpJWKStore;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.okp.EdECJWK;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
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
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
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
public class OKPJWKTest {
	
	private static final ExecutorService WORKER_POOL = Executors.newCachedThreadPool();
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	@Test
	public void testRFC8037_A4() {
		EdECJWK jwk = edecJWKBuilder()
			.curve(OKPCurve.ED25519.getCurve())
			.publicKey("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo")
			.privateKey("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A")
			.build()
			.block();
		
		String jws = "eyJhbGciOiJFZERTQSJ9.RXhhbXBsZSBvZiBFZDI1NTE5IHNpZ25pbmc.hgyY0il_MGCjP0JzlnLWG1PPOt7-09PGcvMg3AIbQR6dWbhijcNR4ki4iylGjg5BhVsPt9g7sVvpAr_MuM0KAg";
		String[] jws_parts = jws.split("\\.");
		
		String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(jwk.signer("EdDSA").sign((jws_parts[0]+"."+jws_parts[1]).getBytes()));
		
		Assertions.assertEquals(jws_parts[2], signature);
	}
	
	@Test
	public void testRFC8037_A5() {
		EdECJWK jwk = edecJWKBuilder()
			.curve(OKPCurve.ED25519.getCurve())
			.publicKey("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo")
			.build()
			.block();
		
		String jws = "eyJhbGciOiJFZERTQSJ9.RXhhbXBsZSBvZiBFZDI1NTE5IHNpZ25pbmc.hgyY0il_MGCjP0JzlnLWG1PPOt7-09PGcvMg3AIbQR6dWbhijcNR4ki4iylGjg5BhVsPt9g7sVvpAr_MuM0KAg";
		String[] jws_parts = jws.split("\\.");
		
		Assertions.assertTrue(jwk.signer("EdDSA").verify((jws_parts[0]+"."+jws_parts[1]).getBytes(), Base64.getUrlDecoder().decode(jws_parts[2])));
	}
	
	@Test
	public void testOKPGenerator() throws JsonProcessingException {
		String payload = "Example of signing";
		String payload64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());

		String jose = "{\"alg\":\"EdDSA\"}";
		String jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		String m = jose64 + "." + payload64;
		
		// Ed25519
		EdECJWK jwk = new GenericEdECJWKGenerator().curve(OKPCurve.ED25519.getCurve()).generate().block();
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));

		byte[] signature = jwk.signer("EdDSA").sign(m.getBytes());
		Assertions.assertEquals(64, signature.length); // 32*2
		Assertions.assertTrue(jwk.signer("EdDSA").verify(m.getBytes(), signature));
		
		// Ed448
		jwk = new GenericEdECJWKGenerator().curve(OKPCurve.ED448.getCurve()).generate().block();
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));

		signature = jwk.signer("EdDSA").sign(m.getBytes());
		Assertions.assertEquals(114, signature.length); // 57*2
		Assertions.assertTrue(jwk.signer("EdDSA").verify(m.getBytes(), signature));
	}
	
	@Test
	public void testECX5c() throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, InvalidAlgorithmParameterException {
//		displayEdECCertAndKeyInfo(Path.of("src/test/resources/okp/edec/private_key.der"), Path.of("src/test/resources/okp/edec/cert.der"));
		
		String x = "Uguw_mZ4K9vvkpHxu4Wcm7tnGJg8RANt1YmkGm6v8v4";
		String d = "xpHPy9ha5ARGSL4Ob_9xhI2ko0r8zdozQJ1tCHfTOS0";
		String[] x5c = new String[] {
			"MIIBPDCB76ADAgECAhRDVBGESlcludrbWXR3EWHfl6N3CDAFBgMrZXAwFDESMBAGA1UEAwwJVEVTVF9FZEVDMB4XDTIyMDUwNjEzMDAwM1oXDTMyMDUwMzEzMDAwM1owFDESMBAGA1UEAwwJVEVTVF9FZEVDMCowBQYDK2VwAyEAUguw/mZ4K9vvkpHxu4Wcm7tnGJg8RANt1YmkGm6v8v6jUzBRMB0GA1UdDgQWBBQnYpi+JNH9OUta79DTdaf0BSPkPTAfBgNVHSMEGDAWgBQnYpi+JNH9OUta79DTdaf0BSPkPTAPBgNVHRMBAf8EBTADAQH/MAUGAytlcANBAJDg6DDcOfuG/YeLKH6cdTpPOMsMZkmmO9fAMdED9KyoySdG6uWAXwG3/6c431vxl2g1AndDzo0CKn+p3u9PEg0="
		};
		
		Path x5p = Path.of("src/test/resources/okp/edec/cert.der");
		
		EdECJWK jwk = edecJWKBuilder(x5p.toUri())
			.curve(OKPCurve.ED25519.getCurve())
			.publicKey(x)
			.privateKey(d)
			.x509CertificateChain(x5c)
			.build()
			.block();
		
		Assertions.assertEquals(x, jwk.getPublicKey());
		Assertions.assertEquals(x5c, jwk.getX509CertificateChain());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		 jwk = edecJWKBuilder(x5p.toUri())
			.curve(OKPCurve.ED25519.getCurve())
			.x509CertificateChain(x5c)
			.build()
			.block();
		
		Assertions.assertEquals(x, jwk.getPublicKey());
		Assertions.assertEquals(x5c, jwk.getX509CertificateChain());
		 
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		// Make it fail
		Assertions.assertThrows(JWKProcessingException.class,
			() -> {
				edecJWKBuilder(x5p.toUri())
					.curve(OKPCurve.ED25519.getCurve())
					.publicKey("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo")
					.privateKey("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A")
					.x509CertificateChain(x5c)
					.build()
					.block();
			},
			"Certificate key does not match JWK parameters"
		);
	}
	
	@Test
	public void testECX5u() throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, InvalidAlgorithmParameterException {
//		displayXECCertAndKeyInfo(Path.of("src/test/resources/okp/xec/private_key.der"), Path.of("src/test/resources/okp/xec/cert.der"));
		
		String x = "5ysteKhT2dTy5wU9Sjb2KIS80djtz5XybrGGNbZ6y0N-hbF7Pbs-cc_ce-udmP07UUFWpKLz0eI";
		String d = "oLNkPvCsu3St4uJcRI5XjyNOv2qhV-wAuOlr81s-RASNUQTXMzKSOEslbvvsg6_5GZPus2YJEvg";
		URI x5u = Path.of("src/test/resources/okp/xec/cert.der").toUri();
		
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		Mockito.when(resourceService.getResource(x5u)).thenReturn(new FileResource(x5u));
		
		XECJWK jwk = xecJWKBuilder(x5u)
			.curve(OKPCurve.X448.getCurve())
			.publicKey(x)
			.privateKey(d)
			.x509CertificateURL(x5u)
			.build()
			.block();
		
		Assertions.assertEquals(x, jwk.getPublicKey());
		Assertions.assertEquals(x5u, jwk.getX509CertificateURL());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		jwk = xecJWKBuilder(x5u)
			.curve(OKPCurve.X448.getCurve())
			.x509CertificateURL(x5u)
			.build()
			.block();
		
		Assertions.assertEquals(x, jwk.getPublicKey());
		Assertions.assertEquals(x5u, jwk.getX509CertificateURL());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		// Make it fail
		Assertions.assertThrows(JWKProcessingException.class,
			() -> {
				edecJWKBuilder(x5u)
					.curve(OKPCurve.X448.getCurve())
					.publicKey("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo")
					.privateKey("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A")
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
	private static void displayEdECCertAndKeyInfo(Path privateKeyPath, Path certPath) throws FileNotFoundException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		try (FileInputStream in = new FileInputStream(certPath.toFile())) {
			X509Certificate certificate = (X509Certificate)cf.generateCertificate(in);
			EdECPublicKey publicKey = (EdECPublicKey)certificate.getPublicKey();
			
			OKPCurve curve = OKPCurve.fromCurve(publicKey.getParams().getName());
			
			byte[] xBytes = new byte[curve.getKeyLength()];
			byte[] encodedKeyBytes = publicKey.getEncoded();
			System.arraycopy(encodedKeyBytes, encodedKeyBytes.length - xBytes.length, xBytes, 0, xBytes.length);
			
			System.out.println("x5c=" + Base64.getEncoder().encodeToString(certificate.getEncoded()));
			System.out.println("alg=" + certificate.getSigAlgName());
			System.out.println("x=" + Base64.getUrlEncoder().withoutPadding().encodeToString(xBytes));
			
			KeyFactory kf = KeyFactory.getInstance(publicKey.getParams().getName());
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(privateKeyPath));
			EdECPrivateKey privateKey = (EdECPrivateKey)kf.generatePrivate(keySpec);
			System.out.println("d=" + Base64.getUrlEncoder().withoutPadding().encodeToString(privateKey.getBytes().get()));
		}
	}
	
	/**
	 * Private key must be in PKCS8 format.
	 * 
	 * @param privateKeyPath
	 * @param certPath 
	 */
	private static void displayXECCertAndKeyInfo(Path privateKeyPath, Path certPath) throws FileNotFoundException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		try (FileInputStream in = new FileInputStream(certPath.toFile())) {
			X509Certificate certificate = (X509Certificate)cf.generateCertificate(in);
			XECPublicKey publicKey = (XECPublicKey)certificate.getPublicKey();
			
			OKPCurve curve = OKPCurve.fromCurve(((NamedParameterSpec)publicKey.getParams()).getName());
			
			byte[] xBytes = new byte[curve.getKeyLength()];
			byte[] encodedKeyBytes = publicKey.getEncoded();
			System.arraycopy(encodedKeyBytes, encodedKeyBytes.length - xBytes.length, xBytes, 0, xBytes.length);
			
			System.out.println("x5c=" + Base64.getEncoder().encodeToString(certificate.getEncoded()));
			System.out.println("alg=" + certificate.getSigAlgName());
			System.out.println("x=" + Base64.getUrlEncoder().withoutPadding().encodeToString(xBytes));
			
			KeyFactory kf = KeyFactory.getInstance(curve.getJCAName());
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(privateKeyPath));
			XECPrivateKey privateKey = (XECPrivateKey)kf.generatePrivate(keySpec);
			
			System.out.println("d=" + Base64.getUrlEncoder().withoutPadding().encodeToString(privateKey.getScalar().get()));
		}
	}
	
	private static GenericEdECJWKBuilder edecJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericEdECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private static GenericEdECJWKBuilder edecJWKBuilder(URI x5u) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, InvalidAlgorithmParameterException {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		Mockito.when(configuration.resolve_x5u()).thenReturn(true);
		PKIXParameters pkixParameters = pkixParameters(Path.of(x5u));
		GenericX509JWKCertPathValidator certPathValidator = new GenericX509JWKCertPathValidator(configuration, pkixParameters, WORKER_POOL);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		Mockito.when(resourceService.getResource(x5u)).thenReturn(new FileResource(x5u));
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, certPathValidator, MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericEdECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, certPathValidator);
	}
	
	private static GenericXECJWKBuilder xecJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericXECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private static GenericXECJWKBuilder xecJWKBuilder(URI x5u) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, InvalidAlgorithmParameterException {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		Mockito.when(configuration.resolve_x5u()).thenReturn(true);
		PKIXParameters pkixParameters = pkixParameters(Path.of(x5u));
		GenericX509JWKCertPathValidator certPathValidator = new GenericX509JWKCertPathValidator(configuration, pkixParameters, WORKER_POOL);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		Mockito.when(resourceService.getResource(x5u)).thenReturn(new FileResource(x5u));
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, certPathValidator, MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericXECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, certPathValidator);
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
