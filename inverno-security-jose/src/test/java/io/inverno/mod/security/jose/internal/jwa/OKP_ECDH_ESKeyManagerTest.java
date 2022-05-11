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
package io.inverno.mod.security.jose.internal.jwa;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKKeyResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKURLResolver;
import io.inverno.mod.security.jose.internal.jwk.NoOpJWKStore;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWK;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWKBuilder;
import io.inverno.mod.security.jose.jwa.DirectJWAKeyManager;
import io.inverno.mod.security.jose.jwa.XECAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import java.util.HashMap;
import java.util.Map;
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
public class OKP_ECDH_ESKeyManagerTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	@Test
	public void testRFC8037_A6() {
		String expectedDerivedKey = "puNU_fyJsAZgc2tvEFlREg";
		Map<String, Object> parameters = Map.of("apu", "QWxpY2U", "apv", "Qm9i");
		
		// Alice (EPK)
		// {
		//   "kty":"OKP",
		//   "crv":"X25519",
		//   "x":"hSDwCYkwp1R0i33ctD73Wg2_Og0mOBr066SpjqqbTmo",
		//   "d":"dwdtCnMYpX08FsFyUbJmRd9ML4frwJkqsXf7pR25LCo"
		// }
		GenericXECJWK aliceJWK = xecJWKBuilder()
			.curve("X25519")
			.publicKey("hSDwCYkwp1R0i33ctD73Wg2_Og0mOBr066SpjqqbTmo")
			.privateKey("dwdtCnMYpX08FsFyUbJmRd9ML4frwJkqsXf7pR25LCo")
			.build()
			.block();
		
		// Bob (EPK)
		// {
		//   "kty":"OKP",
		//   "crv":"X25519",
		//   "x":"3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08",
		//   "d":"XasIfmJKikt54X-Lg4AO5m87sSkmGLb9HC-LJ_-I4Os"
		// }
		GenericXECJWK bobJWK = xecJWKBuilder()
			.curve("X25519")
			.publicKey("3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08")
			.privateKey("XasIfmJKikt54X-Lg4AO5m87sSkmGLb9HC-LJ_-I4Os")
			.build()
			.block();
		
		// producer
		OKP_ECDH_ESKeyManager producerKeyManager = new OKP_ECDH_ESKeyManager(bobJWK, XECAlgorithm.ECDH_ES);
		
		Map<String, Object> producerParameters = new HashMap<>();
		producerParameters.putAll(parameters);
		producerParameters.put("epk", aliceJWK); // we fix the EPK which contains a private key
		
		DirectJWAKeyManager.DirectCEK producerDeriveCEK = producerKeyManager.deriveCEK("A128GCM", producerParameters);
		Assertions.assertEquals(expectedDerivedKey, producerDeriveCEK.getEncryptionKey().getKeyValue());
		
		// make sure we have a public minified epk
		XECJWK epk = (XECJWK)producerDeriveCEK.getMoreHeaderParameters().get("epk");
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
		
		// consumer
		OKP_ECDH_ESKeyManager consumerKeyManager = new OKP_ECDH_ESKeyManager(bobJWK, XECAlgorithm.ECDH_ES);
		
		Map<String, Object> consumerParameters = new HashMap<>();
		consumerParameters.putAll(parameters);
		consumerParameters.putAll(producerDeriveCEK.getMoreHeaderParameters());

		DirectJWAKeyManager.DirectCEK consumerDeriveCEK = consumerKeyManager.deriveCEK("A128GCM", consumerParameters);
		Assertions.assertEquals(expectedDerivedKey, consumerDeriveCEK.getEncryptionKey().getKeyValue());
	}
	
	@Test
	public void testRFC8037_A7() {
		String expectedDerivedKey = "AyTVtwvefN5G2lbd4v9mrQ";
		Map<String, Object> parameters = Map.of("apu", "QWxpY2U", "apv", "Qm9i");
		
		// Alice (EPK)
		// {
		//   "kty":"OKP",
		//   "crv":"X448",
		//   "x":"hSDwCYkwp1R0i33ctD73Wg2_Og0mOBr066SpjqqbTmo",
		//   "d":"dwdtCnMYpX08FsFyUbJmRd9ML4frwJkqsXf7pR25LCo"
		// }
		GenericXECJWK aliceJWK = xecJWKBuilder()
			.curve("X448")
			.publicKey("mwj3zDG34-Z9ItWuoSEHSic70rg94Jxj-qc9LCLF2bvINmRyQdlT1AxbEtqIEg1TF3-A5TLEH6A")
			.privateKey("mo9JJdFRn1d1z0awS1gA1O6e6LrovFVl1JjCjdnJuvV0qUGXRIlzkQBjgqbxJ6sdmsLYwKWYcms")
			.build()
			.block();
		
		// Bob (EPK)
		// {
		//   "kty":"OKP",
		//   "crv":"X448",
		//   "x":"3p7bfXt9wbTTW2HC7OQ1Nz-DQ8hbeGdNrfx-FG-IK08",
		//   "d":"XasIfmJKikt54X-Lg4AO5m87sSkmGLb9HC-LJ_-I4Os"
		// }
		GenericXECJWK bobJWK = xecJWKBuilder()
			.curve("X448")
			.publicKey("PreoKbDNIPW8_AtZm2_sz22kYnEHvbDU80W0MCfYuXL8PjT7QjKhPKcG3LV67D2uB73BxnvzNgk")
			.privateKey("HDBqesKg4uCZCylEcMujOeZFN3KwdYEdj60NHWknwSC7XuiXKw0-ITdMnJIbCdGwNm8QtlFzmS0")
			.build()
			.block();
		
		// producer
		OKP_ECDH_ESKeyManager producerKeyManager = new OKP_ECDH_ESKeyManager(bobJWK, XECAlgorithm.ECDH_ES);
		
		Map<String, Object> producerParameters = new HashMap<>();
		producerParameters.putAll(parameters);
		producerParameters.put("epk", aliceJWK); // we fix the EPK which contains a private key
		
		DirectJWAKeyManager.DirectCEK producerDeriveCEK = producerKeyManager.deriveCEK("A128GCM", producerParameters);
		Assertions.assertEquals(expectedDerivedKey, producerDeriveCEK.getEncryptionKey().getKeyValue());
		
		// make sure we have a public minified epk
		XECJWK epk = (XECJWK)producerDeriveCEK.getMoreHeaderParameters().get("epk");
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
		
		// consumer
		OKP_ECDH_ESKeyManager consumerKeyManager = new OKP_ECDH_ESKeyManager(bobJWK, XECAlgorithm.ECDH_ES);
		
		Map<String, Object> consumerParameters = new HashMap<>();
		consumerParameters.putAll(parameters);
		consumerParameters.putAll(producerDeriveCEK.getMoreHeaderParameters());

		DirectJWAKeyManager.DirectCEK consumerDeriveCEK = consumerKeyManager.deriveCEK("A128GCM", consumerParameters);
		Assertions.assertEquals(expectedDerivedKey, consumerDeriveCEK.getEncryptionKey().getKeyValue());
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
}
