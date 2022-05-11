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

import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKKeyResolver;
import io.inverno.mod.security.jose.internal.jwk.NoOpJWKStore;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWK;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWK;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWKBuilder;
import io.inverno.mod.security.jose.jwa.PBES2Algorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.util.Base64;
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
public class PBES2KeyManagerTest {
	
	@Test
	public void testRFC7517_C() throws Exception {
		byte[] expectedEncryptedKey = Base64.getUrlDecoder().decode("TrqXOwuNUfDV9VPTNbyGvEJ9JMjefAVn-TR1uIxR9p6hsRQh9Tk7BA");
		
		byte[] p2s = new byte[] { (byte)217, (byte)96, (byte)147, (byte)112, (byte)150, (byte)117, (byte)70, (byte)247, (byte)127, (byte)8, (byte)155, (byte)137, (byte)174, (byte)42, (byte)80, (byte)215 };
		int p2c = 4096;
		byte[] cek = new byte[] { (byte)111, (byte)27, (byte)25, (byte)52, (byte)66, (byte)29, (byte)20, (byte)78, (byte)92, (byte)176, (byte)56, (byte)240, (byte)65, (byte)208, (byte)82, (byte)112, (byte)161, (byte)131, (byte)36, (byte)55, (byte)202, (byte)236, (byte)185, (byte)172, (byte)129, (byte)23, (byte)153, (byte)194, (byte)195, (byte)48, (byte)253, (byte)182 };
		String password = "Thus from my lips, by yours, my sin is purged.";

		GenericOCTJWK cekJWK = octJWKBuilder()
			.keyValue(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(cek))
			.build()
			.block();
		
		GenericPBES2JWK passwordJWK = pbesJWKBuilder()
			.password(JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(password.getBytes()))
			.build()
			.block();
		
		Map<String, Object> parameters = Map.of(
			"p2s", JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(p2s),
			"p2c", p2c
		);
		
		PBES2KeyManager keyManager = new PBES2KeyManager(passwordJWK, PBES2Algorithm.PBES2_HS256_A128KW);
		
		AbstractEncryptingJWAKeyManager.EncryptedCEK encryptedCEK = keyManager.encryptCEK(cekJWK, parameters);
		
		Assertions.assertArrayEquals(expectedEncryptedKey, encryptedCEK.getEncryptedKey());
		
		JWK decryptedCEK = keyManager.decryptCEK(expectedEncryptedKey, "A128CBC-HS256", parameters);
		
		Assertions.assertInstanceOf(OCTJWK.class, decryptedCEK);
		Assertions.assertArrayEquals(cek, Base64.getUrlDecoder().decode(((OCTJWK)decryptedCEK).getKeyValue()));
	}
	
	private GenericOCTJWKBuilder octJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		
		return new GenericOCTJWKBuilder(configuration, jwkStore, keyResolver);
	}
	
	private GenericPBES2JWKBuilder pbesJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		
		return new GenericPBES2JWKBuilder(configuration, jwkStore, keyResolver);
	}
}
