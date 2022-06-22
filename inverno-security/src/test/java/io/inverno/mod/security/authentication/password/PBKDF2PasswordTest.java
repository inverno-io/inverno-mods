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
package io.inverno.mod.security.authentication.password;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class PBKDF2PasswordTest {
	
	@Test
	public void test() throws JsonProcessingException {
		byte[] salt = Base64.getUrlDecoder().decode("zXlZd6iTs_ypwN1TFB5P8g");
		
		SecureRandom secureRandom = new SecureRandom() {
			@Override
			public void nextBytes(byte[] bytes) {
				System.arraycopy(salt, 0, bytes, 0, bytes.length);
			}
		};
		
		PBKDF2Password.Encoder encoder = new PBKDF2Password.Encoder("HmacSHA256", 1000, 16, 32, secureRandom);
		
		PBKDF2Password pbkdf2Password = encoder.encode("password");
		
		Assertions.assertEquals(encoder, pbkdf2Password.getEncoder());
		Assertions.assertEquals("zXlZd6iTs_ypwN1TFB5P8rqoXrwvhJlRAIGXb8TaR_dqn8dePmBG8frQJ93t1l27", pbkdf2Password.getValue());

		Assertions.assertTrue(pbkdf2Password.matches("password"));
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonPassword = mapper.writeValueAsString(pbkdf2Password);
		Password<?, ?> readPassword = mapper.readValue(jsonPassword, Password.class);
		Assertions.assertEquals(pbkdf2Password, readPassword);
	}
}
