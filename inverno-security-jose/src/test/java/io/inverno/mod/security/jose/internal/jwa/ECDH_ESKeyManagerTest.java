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
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWK;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWKBuilder;
import io.inverno.mod.security.jose.jwa.DirectJWAKeyManager;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
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
public class ECDH_ESKeyManagerTest {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	@Test
	public void test_RFC7517_C() throws Exception {
		String expectedDerivedKey = "VqqN6vgjbSBcIijNcacQGg";
		Map<String, Object> parameters = Map.of("apu", "QWxpY2U", "apv", "Qm9i");
		
		// Alice (EPK)
		// {
		//   "kty":"EC",
		//   "crv":"P-256",
		//   "x":"gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0",
		//   "y":"SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps",
		//   "d":"0_NxaRPUMQoAJt50Gz8YiTr8gRTwyEaCumd-MToTmIo"
		// }
		GenericECJWK aliceJWK = ecJWKBuilder()
			.curve("P-256")
			.xCoordinate("gI0GAILBdu7T53akrFmMyGcsF3n5dO7MmwNBHKW5SV0")
			.yCoordinate("SLW_xSffzlPWrHEVI30DHM_4egVwt3NQqeUD7nMFpps")
			.eccPrivateKey("0_NxaRPUMQoAJt50Gz8YiTr8gRTwyEaCumd-MToTmIo")
			.build()
			.block();
		
		// Bob
		// {
		//   "kty":"EC",
        //   "crv":"P-256",
        //   "x":"weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ",
        //   "y":"e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck",
		//   "d":"VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw"
        // }
		GenericECJWK bobJWK = ecJWKBuilder()
			.curve("P-256")
			.xCoordinate("weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ")
			.yCoordinate("e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck")
			.eccPrivateKey("VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw")
			.build()
			.block();
		
		// producer
		ECDH_ESKeyManager producerKeyManager = new ECDH_ESKeyManager(bobJWK, ECAlgorithm.ECDH_ES);
		
		Map<String, Object> producerParameters = new HashMap<>();
		producerParameters.putAll(parameters);
		producerParameters.put("epk", aliceJWK); // we fix the EPK which contains a private key
		
		DirectJWAKeyManager.DirectCEK producerDeriveCEK = producerKeyManager.deriveCEK("A128GCM", producerParameters);
		Assertions.assertEquals(expectedDerivedKey, producerDeriveCEK.getEncryptionKey().getKeyValue());
		
		// make sure we have a public minified epk
		ECJWK epk = (ECJWK)producerDeriveCEK.getMoreHeaderParameters().get("epk");
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
		
		// consumer
		ECDH_ESKeyManager consumerKeyManager = new ECDH_ESKeyManager(bobJWK, ECAlgorithm.ECDH_ES);
		
		Map<String, Object> consumerParameters = new HashMap<>();
		consumerParameters.putAll(parameters);
		consumerParameters.putAll(producerDeriveCEK.getMoreHeaderParameters());

		DirectJWAKeyManager.DirectCEK consumerDeriveCEK = consumerKeyManager.deriveCEK("A128GCM", consumerParameters);
		Assertions.assertEquals(expectedDerivedKey, consumerDeriveCEK.getEncryptionKey().getKeyValue());
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
}
