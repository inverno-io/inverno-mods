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
package io.inverno.mod.security.jose.internal.jwk.rsa;

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
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
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
public class RSAJWKTest {
	
	private static final ExecutorService WORKER_POOL = Executors.newCachedThreadPool();
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7515#appendix-A.2">RFC7515 Appendix A.2</a> Example JWS Using RSASSA-PKCS1-v1_5 SHA-256
	 * 
	 * {
	 *     "kty":"RSA",
	 *     "n":"ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ",
	 *     "e":"AQAB",
	 *     "d":"Eq5xpGnNCivDflJsRQBXHx1hdR1k6Ulwe2JZD50LpXyWPEAeP88vLNO97IjlA7_GQ5sLKMgvfTeXZx9SE-7YwVol2NXOoAJe46sui395IW_GO-pWJ1O0BkTGoVEn2bKVRUCgu-GjBVaYLU6f3l9kJfFNS3E0QbVdxzubSu3Mkqzjkn439X0M_V51gfpRLI9JYanrC4D4qAdGcopV_0ZHHzQlBjudU2QvXt4ehNYTCBr6XCLQUShb1juUO1ZdiYoFaFQT5Tw8bGUl_x_jTj3ccPDVZFD9pIuhLhBOneufuBiB4cS98l2SR_RQyGWSeWjnczT0QU91p1DhOVRuOopznQ",
	 *     "p":"4BzEEOtIpmVdVEZNCqS7baC4crd0pqnRH_5IB3jw3bcxGn6QLvnEtfdUdiYrqBdss1l58BQ3KhooKeQTa9AB0Hw_Py5PJdTJNPY8cQn7ouZ2KKDcmnPGBY5t7yLc1QlQ5xHdwW1VhvKn-nXqhJTBgIPgtldC-KDV5z-y2XDwGUc",
	 *     "q":"uQPEfgmVtjL0Uyyx88GZFF1fOunH3-7cepKmtH4pxhtCoHqpWmT8YAmZxaewHgHAjLYsp1ZSe7zFYHj7C6ul7TjeLQeZD_YwD66t62wDmpe_HlB-TnBA-njbglfIsRLtXlnDzQkv5dTltRJ11BKBBypeeF6689rjcJIDEz9RWdc",
	 *     "dp":"BwKfV3Akq5_MFZDFZCnW-wzl-CCo83WoZvnLQwCTeDv8uzluRSnm71I3QCLdhrqE2e9YkxvuxdBfpT_PI7Yz-FOKnu1R6HsJeDCjn12Sk3vmAktV2zb34MCdy7cpdTh_YVr7tss2u6vneTwrA86rZtu5Mbr1C1XsmvkxHQAdYo0",
	 *     "dq":"h_96-mK1R_7glhsum81dZxjTnYynPbZpHziZjeeHcXYsXaaMwkOlODsWa7I9xXDoRwbKgB719rrmI2oKr6N3Do9U0ajaHF-NKJnwgjMd2w9cjz3_-kyNlxAr2v4IKhGNpmM5iIgOS1VZnOZ68m6_pbLBSp3nssTdlqvd0tIiTHU",
	 *     "qi":"IYd7DHOhrWvxkwPQsRM2tOgrjbcrfvtQJipd-DlcxyVuuM9sQLdgjVk2oy26F0EmpScGLq2MowX7fhd_QJQ3ydy5cY7YIBi87w93IKLEdfnbJtoOPLUW0ITrJReOgo1cq9SbsxYawBgfp_gh6A5603k2-ZQwVK0JKSHuLFkuQ3U"
	 *	}
	 */
	@Test
	public void testRFC7515_A2() {
		RSAJWK jwk = rsaJWKBuilder()
			.modulus("ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ")
			.publicExponent("AQAB")
			.privateExponent("Eq5xpGnNCivDflJsRQBXHx1hdR1k6Ulwe2JZD50LpXyWPEAeP88vLNO97IjlA7_GQ5sLKMgvfTeXZx9SE-7YwVol2NXOoAJe46sui395IW_GO-pWJ1O0BkTGoVEn2bKVRUCgu-GjBVaYLU6f3l9kJfFNS3E0QbVdxzubSu3Mkqzjkn439X0M_V51gfpRLI9JYanrC4D4qAdGcopV_0ZHHzQlBjudU2QvXt4ehNYTCBr6XCLQUShb1juUO1ZdiYoFaFQT5Tw8bGUl_x_jTj3ccPDVZFD9pIuhLhBOneufuBiB4cS98l2SR_RQyGWSeWjnczT0QU91p1DhOVRuOopznQ")
			.firstPrimeFactor("4BzEEOtIpmVdVEZNCqS7baC4crd0pqnRH_5IB3jw3bcxGn6QLvnEtfdUdiYrqBdss1l58BQ3KhooKeQTa9AB0Hw_Py5PJdTJNPY8cQn7ouZ2KKDcmnPGBY5t7yLc1QlQ5xHdwW1VhvKn-nXqhJTBgIPgtldC-KDV5z-y2XDwGUc")
			.secondPrimeFactor("uQPEfgmVtjL0Uyyx88GZFF1fOunH3-7cepKmtH4pxhtCoHqpWmT8YAmZxaewHgHAjLYsp1ZSe7zFYHj7C6ul7TjeLQeZD_YwD66t62wDmpe_HlB-TnBA-njbglfIsRLtXlnDzQkv5dTltRJ11BKBBypeeF6689rjcJIDEz9RWdc")
			.firstFactorExponent("BwKfV3Akq5_MFZDFZCnW-wzl-CCo83WoZvnLQwCTeDv8uzluRSnm71I3QCLdhrqE2e9YkxvuxdBfpT_PI7Yz-FOKnu1R6HsJeDCjn12Sk3vmAktV2zb34MCdy7cpdTh_YVr7tss2u6vneTwrA86rZtu5Mbr1C1XsmvkxHQAdYo0")
			.secondFactorExponent("h_96-mK1R_7glhsum81dZxjTnYynPbZpHziZjeeHcXYsXaaMwkOlODsWa7I9xXDoRwbKgB719rrmI2oKr6N3Do9U0ajaHF-NKJnwgjMd2w9cjz3_-kyNlxAr2v4IKhGNpmM5iIgOS1VZnOZ68m6_pbLBSp3nssTdlqvd0tIiTHU")
			.firstCoefficient("IYd7DHOhrWvxkwPQsRM2tOgrjbcrfvtQJipd-DlcxyVuuM9sQLdgjVk2oy26F0EmpScGLq2MowX7fhd_QJQ3ydy5cY7YIBi87w93IKLEdfnbJtoOPLUW0ITrJReOgo1cq9SbsxYawBgfp_gh6A5603k2-ZQwVK0JKSHuLFkuQ3U")
			.build()
			.block();
		
		String jws = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw";
		String[] jws_parts = jws.split("\\.");
		
		Assertions.assertTrue(jwk.signer("RS256").verify((jws_parts[0]+"."+jws_parts[1]).getBytes(), Base64.getUrlDecoder().decode(jws_parts[2])));
	}
	
	@Test
	public void testRSAGenerator() throws JsonProcessingException {
		RSAJWK jwk = new GenericRSAJWKGenerator().generate().block();
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		String payload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
		String payload64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
		
		// RS256
		String jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"RS256\"}";
		String jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		String m = jose64 + "." + payload64;
		
		byte[] signature = jwk.signer("RS256").sign(m.getBytes());
		Assertions.assertTrue(jwk.signer("RS256").verify(m.getBytes(), signature));
		
		// RS384
		jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"RS384\"}";
		jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		m = jose64 + "." + payload64;
		
		signature = jwk.signer("RS384").sign(m.getBytes());
		Assertions.assertEquals(256, signature.length); // RSA key size: 2048/8=256
		Assertions.assertTrue(jwk.signer("RS384").verify(m.getBytes(), signature));
		
		// RS512
		jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"RS512\"}";
		jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		m = jose64 + "." + payload64;
		
		signature = jwk.signer("RS512").sign(m.getBytes());
		Assertions.assertEquals(256, signature.length); // RSA key size: 2048/8=256
		Assertions.assertTrue(jwk.signer("RS512").verify(m.getBytes(), signature));
		
		// PS256
		jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"PS256\"}";
		jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		m = jose64 + "." + payload64;
		
		signature = jwk.signer("PS256").sign(m.getBytes());
		Assertions.assertEquals(256, signature.length); // RSA key size: 2048/8=256
		Assertions.assertTrue(jwk.signer("PS256").verify(m.getBytes(), signature));
		
		// PS384
		jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"PS384\"}";
		jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		m = jose64 + "." + payload64;
		
		signature = jwk.signer("PS384").sign(m.getBytes());
		Assertions.assertEquals(256, signature.length); // RSA key size: 2048/8=256
		Assertions.assertTrue(jwk.signer("PS384").verify(m.getBytes(), signature));
		
		// PS512
		jose = "{\"typ\":\"JWT\",\r\n \"alg\":\"PS512\"}";
		jose64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jose.getBytes());
		m = jose64 + "." + payload64;
		
		signature = jwk.signer("PS512").sign(m.getBytes());
		Assertions.assertEquals(256, signature.length); // RSA key size: 2048/8=256
		Assertions.assertTrue(jwk.signer("PS512").verify(m.getBytes(), signature));
	}
	
	@Test
	public void testRSAX5c() throws FileNotFoundException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException {
//		displayCertAndKeyInfo(Path.of("src/test/resources/rsa/private_key.der"), Path.of("src/test/resources/rsa/cert.der"), "RSA");
		
		String n = "yiuP-607_c5vnBM5dXwtZtdE8Da_gxGN4DxLrstoXD0fUZ1J6HbeTY0_-zcyR61d06irprt1RJ9nlbTJCs1P_ZffLHYOAVhEjzp0pyPjUCztxXw0MdMpWblA4U0aIS6BN2cdaKojBQY7yZM9NY5G0OWovBWOhvCHLmznxmK54wCdpq2QIpXLT_DTC68brlvCVoij1IdwydQmdioScN47VySUAaqlim3jv4GjSkDZd6ecnwpzbYpCMC7qhR2iFE-PgThgWlZ_iil9Su4qsWmUWjPSkfSgOAO73bUSg-PQAa3Np_AlF4fCihXg2V6FJxTd77KcZ13IQrXs2AiZ7Anm-Q";
		String e = "AQAB";
		String d = "rvyXqnpeI6fL2OaW5EawMYSASf7JMtQ93emyhD_RO404D3c54nkIn010JKe44GuBe9NRh4ZX0Sa8DMsm_C-LXe9XHu-r3aQd627oS0b32Iya3UVNFBc2gk-jhZ8rz66l72NUBCTHHPExTJ8h6roUN3mg2_M4ozmLeDaRQphvVrfE_xUGoITJt03C0u1ACF_ELoEUhx0DzMFFe6zeHcgYD4okljqRi_NT8Tvl8rQ2PxM5CoEqe9AV35VdJ1mARw4YQ9MiFmBXOrJ-FG2SflVLy5zCnb2hJ36tZFFjDJpKTd2nVXFXFNXa7tIbPx4nqwBiwBtEcNE1OerD06qImbQxdQ";
		String[] x5c = new String[] {
			"MIIDBzCCAe+gAwIBAgIUG3wX8Pn8BHcjtKiM27EFs/5driMwDQYJKoZIhvcNAQELBQAwEzERMA8GA1UEAwwIVEVTVF9SU0EwHhcNMjIwNTA2MTMwMDE2WhcNMzIwNTAzMTMwMDE2WjATMREwDwYDVQQDDAhURVNUX1JTQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMorj/utO/3Ob5wTOXV8LWbXRPA2v4MRjeA8S67LaFw9H1GdSeh23k2NP/s3MketXdOoq6a7dUSfZ5W0yQrNT/2X3yx2DgFYRI86dKcj41As7cV8NDHTKVm5QOFNGiEugTdnHWiqIwUGO8mTPTWORtDlqLwVjobwhy5s58ZiueMAnaatkCKVy0/w0wuvG65bwlaIo9SHcMnUJnYqEnDeO1cklAGqpYpt47+Bo0pA2XennJ8Kc22KQjAu6oUdohRPj4E4YFpWf4opfUruKrFplFoz0pH0oDgDu921EoPj0AGtzafwJReHwooV4NlehScU3e+ynGddyEK17NgImewJ5vkCAwEAAaNTMFEwHQYDVR0OBBYEFNpuLeAl5rXNAt1gFmms7IQ+9PdpMB8GA1UdIwQYMBaAFNpuLeAl5rXNAt1gFmms7IQ+9PdpMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBACODLbcJ/Q/JBubliJBLU40wjnmSkfE2O4ZtnaSkTWzbeaFWMPK/DXe1YcRGR786HxQWQPekRv+aDCeftp9kHjFyzZCmhNrAoKega5J1fVo9OfXVWx58/xLtnbSAuDWllfCzTiDnVO60i4Lelh5U+YpQ3PrOMtgzAzbJ0tFSAHjZJzE7DvGes0ce/Kd79fTM0rzBYEBFbBdPctXLne5U8n1DuCcJp4gTZl4uo1ILbYMb4bAq3Ndz8/5Ri1U9NAu0RllwPtVmcKg6Xgo8clBm+TnoCCSDnxkkI0B6rPXPL+vcjZbg+SIpfHapAHViai3OSxePzRJIBMmmWCsIOKqb55Q="
		};
		Path x5p = Path.of("src/test/resources/rsa/cert.der");
		
		RSAJWK jwk = rsaJWKBuilder(x5p.toUri())
			.modulus(n)
			.publicExponent(e)
			.x509CertificateChain(x5c)
			.build()
			.block();
		
		Assertions.assertEquals(n, jwk.getModulus());
		Assertions.assertEquals(e, jwk.getPublicExponent());
		Assertions.assertEquals(x5c, jwk.getX509CertificateChain());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		jwk = rsaJWKBuilder(x5p.toUri())
			.x509CertificateChain(x5c)
			.build()
			.block();
		
		Assertions.assertEquals(n, jwk.getModulus());
		Assertions.assertEquals(e, jwk.getPublicExponent());
		Assertions.assertEquals(x5c, jwk.getX509CertificateChain());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		// Make it fail
		Assertions.assertThrows(JWKProcessingException.class,
			() -> {
				rsaJWKBuilder(x5p.toUri())
					.modulus("ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ")
					.publicExponent("AQAB")
					.x509CertificateChain(x5c)
					.build()
					.block();
			},
			"Certificate key does not match JWK parameters"
		);
	}
	
	@Test
	public void testRSAX5u() throws FileNotFoundException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException {
//		displayCertAndKeyInfo(Path.of("src/test/resources/rsa/pss/private_key.der"), Path.of("src/test/resources/rsa/pss/cert.der"), "RSASSA-PSS");
		
		String n = "9NC1kMNercoyoSwx5wzTVlUm2XadzUqAKoz37RB2mpzsGVoqmyixyrLQSLm9nzlEqpuDA5sKxELV1A0C0rCVsOv69lYmMCep11GvwmjVPstGk3oXBn9fh6y4IDf5JrJIfUGlYcIh4BuDjF_tMmmcE0ZlkUq7EqS99PF0IQA7KWsDAFvpa48TjKCxrR6_tdny-WtQYyvBd8f0LTwGcPfZG-aeEVzFd_gPtlxkPGQ1MTCyo3h38jsRJn5Rhg0yEtOeWGf9DEYBHNny2TfgqZlizHHzu2lTDZJfMGgT9p_G2Q59GY3kZZX2LazwDsRXvqdR1gp_5HwOEa1KlmuKPtArQQ";
		String e = "AQAB";
		String d = "s7GeE6vTiuynTPYLivQ3C19lLKmMGmtct97Q_AjhhYs5IUK1kz3DgmzNxRPQw1ZduHx9JeBffr8wBH2oXM2QklQj2TxSu3XhjFJBGAmqvHSoUQeEbxh_Hi8A12U-U9D4tDfDFIZSJxUK8bZXfHFYRi2dz49y0LRrWacA_lgVFMgv1rvVYJNtlIIrv-QHieXJvDlN3jKdgo3B4C8J2KrXkEB89rtMUGvq89qeoQH0YfRv-6z2cGVrX6pHCuD3orrKmAdfHk5OrtVSVjBbtSL1TzN4QxzNJJEXMjAi-ZjdgOJ5wyKBD6zKFhojwh_3maa-BdeLiTAb523lZNyuTUugAQ";
		Path x5p = Path.of("src/test/resources/rsa/pss/cert.der");
		URI x5u = x5p.toUri();
		
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		Mockito.when(resourceService.getResource(x5u)).thenReturn(new FileResource(x5u));
		
		RSAJWK jwk = rsaJWKBuilder(x5u)
			.modulus(n)
			.publicExponent(e)
			.x509CertificateURL(x5u)
			.build()
			.block();
		
		Assertions.assertEquals(n, jwk.getModulus());
		Assertions.assertEquals(e, jwk.getPublicExponent());
		Assertions.assertEquals(x5u, jwk.getX509CertificateURL());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		jwk = rsaJWKBuilder(x5u)
			.x509CertificateURL(x5u)
			.build()
			.block();
		
		Assertions.assertEquals(n, jwk.getModulus());
		Assertions.assertEquals(e, jwk.getPublicExponent());
		Assertions.assertEquals(x5u, jwk.getX509CertificateURL());
		
//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jwk));
		
		// Make it fail
		Assertions.assertThrows(JWKProcessingException.class,
			() -> {
				rsaJWKBuilder(x5u)
					.modulus("ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ")
					.publicExponent("AQAB")
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
	private void displayCertAndKeyInfo(Path privateKeyPath, Path certPath, String algorithm) throws FileNotFoundException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		try (FileInputStream in = new FileInputStream(certPath.toFile())) {
			X509Certificate certificate = (X509Certificate)cf.generateCertificate(in);
			RSAPublicKey publicKey = (RSAPublicKey)certificate.getPublicKey();
			
			System.out.println("x5c=" + Base64.getEncoder().encodeToString(certificate.getEncoded()));
			System.out.println("alg=" + certificate.getSigAlgName());
			System.out.println("n=" + Base64.getUrlEncoder().withoutPadding().encodeToString(JOSEUtils.toUnsignedBytes(publicKey.getModulus())));
			System.out.println("e=" + Base64.getUrlEncoder().withoutPadding().encodeToString(JOSEUtils.toUnsignedBytes(publicKey.getPublicExponent())));
		}
		
		KeyFactory kf = KeyFactory.getInstance(algorithm);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(privateKeyPath));
		RSAPrivateKey privateKey = (RSAPrivateKey)kf.generatePrivate(keySpec);
		System.out.println("d=" + Base64.getUrlEncoder().withoutPadding().encodeToString(JOSEUtils.toUnsignedBytes(privateKey.getPrivateExponent())));
	}
	
	private static GenericRSAJWKBuilder rsaJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericRSAJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private static GenericRSAJWKBuilder rsaJWKBuilder(URI x5u) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, FileNotFoundException, InvalidAlgorithmParameterException {
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
		
		return new GenericRSAJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, certPathValidator);
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
