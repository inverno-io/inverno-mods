/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.security.authentication;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.inverno.mod.security.internal.authentication.GenericUserAuthentication;
import java.util.Set;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class UserAuthenticationTest {

	@Test
	public void testMapper() throws JsonProcessingException {
		UserAuthentication auth = new GenericUserAuthentication("user01", Set.of("readers"), false);
		
		ObjectMapper mapper = new ObjectMapper();
		
		String authJson = mapper.writeValueAsString(auth);
		
		Assertions.assertEquals("{\"username\":\"user01\",\"groups\":[\"readers\"],\"authenticated\":false}", authJson);
		
		UserAuthentication authParsed = mapper.readValue(authJson, UserAuthentication.class);
		
		Assertions.assertEquals(auth, authParsed);
	}
}
