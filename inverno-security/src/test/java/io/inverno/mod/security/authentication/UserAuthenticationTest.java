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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.authentication.password.PBKDF2Password;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthentication;
import io.inverno.mod.security.identity.PersonIdentity;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class UserAuthenticationTest {

	@Test
	public void testMapper() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		
		PersonIdentity identity = new PersonIdentity("jsmith", "John", "Smith", "john.smith@inverno.io");
		User<PersonIdentity> user = User.of("jsmith").identity(identity).groups("reader").password(new PBKDF2Password.Encoder().encode("password")).build();

		String wuser = mapper.writeValueAsString(user);
		User<PersonIdentity> ruser = mapper.readValue(wuser, new TypeReference<User<PersonIdentity>>() {});
		
		UserAuthentication<PersonIdentity> authentication = UserAuthentication.of(user);
		
		String wauthentication = mapper.writeValueAsString(authentication);
		UserAuthentication<PersonIdentity> rauthentication = mapper.readValue(wauthentication, new TypeReference<UserAuthentication<PersonIdentity>>() {});
	}
}
