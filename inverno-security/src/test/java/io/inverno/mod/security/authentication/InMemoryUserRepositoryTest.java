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
package io.inverno.mod.security.authentication;

import io.inverno.mod.security.authentication.password.PBKDF2Password;
import io.inverno.mod.security.authentication.password.Password;
import io.inverno.mod.security.authentication.password.PasswordPolicyException;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserRepository;
import io.inverno.mod.security.identity.PersonIdentity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class InMemoryUserRepositoryTest {
	
	@Test
	public void test() {
		UserRepository<PersonIdentity, User<PersonIdentity>> userRepository = new InMemoryUserRepository<>(Set.of(
			User.<PersonIdentity>of("reader").password(new RawPassword("password")).groups("readers").build(),
			User.<PersonIdentity>of("writer").password(new RawPassword("password")).groups("writers").build()
		));
		
		User<PersonIdentity> reader = userRepository.resolveCredentials("reader").block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of("readers"), reader.getGroups());
		Assertions.assertInstanceOf(PBKDF2Password.class, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertFalse(reader.isLocked());
		Assertions.assertNull(reader.getIdentity());
		
		User<PersonIdentity> writer = userRepository.resolveCredentials("writer").block();
		Assertions.assertNotNull(writer);
		Assertions.assertEquals("writer", writer.getUsername());
		Assertions.assertEquals(Set.of("writers"), writer.getGroups());
		Assertions.assertInstanceOf(PBKDF2Password.class, writer.getPassword());
		Assertions.assertTrue(writer.getPassword().matches("password"));
		Assertions.assertFalse(writer.isLocked());
		Assertions.assertNull(writer.getIdentity());
		
		List<User<PersonIdentity>> users = userRepository.listUsers().collectList().block();
		
		Assertions.assertEquals(Set.of(reader, writer), new HashSet<>(users));

		User<PersonIdentity> admin = User.<PersonIdentity>of("admin")
			.identity(new PersonIdentity("admin", "John", "Smith", "admin@inverno.io"))
			.password(new RawPassword("adminpassword"))
			.groups("admin")
			.build();
		
		admin = userRepository.createUser(admin).block();
		Assertions.assertNotNull(admin);
		Assertions.assertEquals("admin", admin.getUsername());
		Assertions.assertEquals(Set.of("admin"), admin.getGroups());
		Assertions.assertInstanceOf(PBKDF2Password.class, admin.getPassword());
		Assertions.assertTrue(admin.getPassword().matches("adminpassword"));
		Assertions.assertFalse(admin.isLocked());
		Assertions.assertNotNull(admin.getIdentity());
		Assertions.assertEquals("John", admin.getIdentity().getFirstName());
		Assertions.assertEquals("Smith", admin.getIdentity().getLastName());
		Assertions.assertEquals("admin@inverno.io", admin.getIdentity().getEmail());
		
		Assertions.assertEquals(3, userRepository.listUsers().collectList().block().size());
		
		User<PersonIdentity> updateReader = User.<PersonIdentity>of(reader.getUsername())
			.identity(new PersonIdentity(reader.getUsername(), "Bob", "Johnson", "reader@inverno.io"))
			.password(new RawPassword("newPassword"))
			.groups("group1", "group2")
			.build();
		
		Password<?,?> readerPassword = reader.getPassword();
		reader = userRepository.updateUser(updateReader).block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of("readers"), reader.getGroups());
		Assertions.assertEquals(readerPassword, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertFalse(reader.isLocked());
		Assertions.assertNotNull(reader.getIdentity());
		Assertions.assertEquals("Bob", reader.getIdentity().getFirstName());
		Assertions.assertEquals("Johnson", reader.getIdentity().getLastName());
		Assertions.assertEquals("reader@inverno.io", reader.getIdentity().getEmail());
		
		reader = userRepository.resolveCredentials("reader").block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of("readers"), reader.getGroups());
		Assertions.assertEquals(readerPassword, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertFalse(reader.isLocked());
		Assertions.assertNotNull(reader.getIdentity());
		Assertions.assertEquals("Bob", reader.getIdentity().getFirstName());
		Assertions.assertEquals("Johnson", reader.getIdentity().getLastName());
		Assertions.assertEquals("reader@inverno.io", reader.getIdentity().getEmail());
		
		reader = userRepository.lockUser("reader").block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of("readers"), reader.getGroups());
		Assertions.assertEquals(readerPassword, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertTrue(reader.isLocked());
		Assertions.assertNotNull(reader.getIdentity());
		Assertions.assertEquals("Bob", reader.getIdentity().getFirstName());
		Assertions.assertEquals("Johnson", reader.getIdentity().getLastName());
		Assertions.assertEquals("reader@inverno.io", reader.getIdentity().getEmail());
		
		reader = userRepository.resolveCredentials("reader").block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of("readers"), reader.getGroups());
		Assertions.assertEquals(readerPassword, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertTrue(reader.isLocked());
		Assertions.assertNotNull(reader.getIdentity());
		Assertions.assertEquals("Bob", reader.getIdentity().getFirstName());
		Assertions.assertEquals("Johnson", reader.getIdentity().getLastName());
		Assertions.assertEquals("reader@inverno.io", reader.getIdentity().getEmail());
		
		reader = userRepository.unlockUser("reader").block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of("readers"), reader.getGroups());
		Assertions.assertEquals(readerPassword, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertFalse(reader.isLocked());
		Assertions.assertNotNull(reader.getIdentity());
		Assertions.assertEquals("Bob", reader.getIdentity().getFirstName());
		Assertions.assertEquals("Johnson", reader.getIdentity().getLastName());
		Assertions.assertEquals("reader@inverno.io", reader.getIdentity().getEmail());
		
		reader = userRepository.resolveCredentials("reader").block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of("readers"), reader.getGroups());
		Assertions.assertEquals(readerPassword, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertFalse(reader.isLocked());
		Assertions.assertNotNull(reader.getIdentity());
		Assertions.assertEquals("Bob", reader.getIdentity().getFirstName());
		Assertions.assertEquals("Johnson", reader.getIdentity().getLastName());
		Assertions.assertEquals("reader@inverno.io", reader.getIdentity().getEmail());
		
		Assertions.assertEquals("Invalid credentials", Assertions.assertThrows(AuthenticationException.class, () -> userRepository.changePassword(LoginCredentials.of("writer", new RawPassword("invalid")), "newPassword").block()).getMessage());
		Assertions.assertEquals("Password must be at least 8 characters long", Assertions.assertThrows(PasswordPolicyException.class, () -> userRepository.changePassword(LoginCredentials.of("writer", new RawPassword("password")), "bad").block()).getMessage());
		Assertions.assertEquals("Password must be at most 64 characters long", Assertions.assertThrows(PasswordPolicyException.class, () -> userRepository.changePassword(LoginCredentials.of("writer", new RawPassword("password")), "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789").block()).getMessage());
		
		writer = userRepository.changePassword(LoginCredentials.of("writer", new RawPassword("password")), "newpassword").block();
		Assertions.assertNotNull(writer);
		Assertions.assertEquals("writer", writer.getUsername());
		Assertions.assertEquals(Set.of("writers"), writer.getGroups());
		Assertions.assertInstanceOf(PBKDF2Password.class, writer.getPassword());
		Assertions.assertTrue(writer.getPassword().matches("newpassword"));
		Assertions.assertFalse(writer.isLocked());
		Assertions.assertNull(writer.getIdentity());
		
		writer = userRepository.resolveCredentials("writer").block();
		Assertions.assertNotNull(writer);
		Assertions.assertEquals("writer", writer.getUsername());
		Assertions.assertEquals(Set.of("writers"), writer.getGroups());
		Assertions.assertInstanceOf(PBKDF2Password.class, writer.getPassword());
		Assertions.assertTrue(writer.getPassword().matches("newpassword"));
		Assertions.assertFalse(writer.isLocked());
		Assertions.assertNull(writer.getIdentity());
		
		writer = userRepository.addUserToGroups("writer", "readers", "writers").block();
		Assertions.assertNotNull(writer);
		Assertions.assertEquals("writer", writer.getUsername());
		Assertions.assertEquals(Set.of("readers", "writers"), writer.getGroups());
		Assertions.assertInstanceOf(PBKDF2Password.class, writer.getPassword());
		Assertions.assertTrue(writer.getPassword().matches("newpassword"));
		Assertions.assertFalse(writer.isLocked());
		Assertions.assertNull(writer.getIdentity());
		
		writer = userRepository.resolveCredentials("writer").block();
		Assertions.assertNotNull(writer);
		Assertions.assertEquals("writer", writer.getUsername());
		Assertions.assertEquals(Set.of("readers", "writers"), writer.getGroups());
		Assertions.assertInstanceOf(PBKDF2Password.class, writer.getPassword());
		Assertions.assertTrue(writer.getPassword().matches("newpassword"));
		Assertions.assertFalse(writer.isLocked());
		Assertions.assertNull(writer.getIdentity());
		
		reader = userRepository.removeUserFromGroups("reader", "readers", "other").block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of(), reader.getGroups());
		Assertions.assertEquals(readerPassword, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertFalse(reader.isLocked());
		Assertions.assertNotNull(reader.getIdentity());
		Assertions.assertEquals("Bob", reader.getIdentity().getFirstName());
		Assertions.assertEquals("Johnson", reader.getIdentity().getLastName());
		Assertions.assertEquals("reader@inverno.io", reader.getIdentity().getEmail());
		
		reader = userRepository.resolveCredentials("reader").block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of(), reader.getGroups());
		Assertions.assertEquals(readerPassword, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertFalse(reader.isLocked());
		Assertions.assertNotNull(reader.getIdentity());
		Assertions.assertEquals("Bob", reader.getIdentity().getFirstName());
		Assertions.assertEquals("Johnson", reader.getIdentity().getLastName());
		Assertions.assertEquals("reader@inverno.io", reader.getIdentity().getEmail());
		
		reader = userRepository.deleteUser("reader").block();
		Assertions.assertNotNull(reader);
		Assertions.assertEquals("reader", reader.getUsername());
		Assertions.assertEquals(Set.of(), reader.getGroups());
		Assertions.assertEquals(readerPassword, reader.getPassword());
		Assertions.assertTrue(reader.getPassword().matches("password"));
		Assertions.assertFalse(reader.isLocked());
		Assertions.assertNotNull(reader.getIdentity());
		Assertions.assertEquals("Bob", reader.getIdentity().getFirstName());
		Assertions.assertEquals("Johnson", reader.getIdentity().getLastName());
		Assertions.assertEquals("reader@inverno.io", reader.getIdentity().getEmail());
		
		Assertions.assertNull(userRepository.resolveCredentials("reader").block());
		
		Assertions.assertEquals(2, userRepository.listUsers().collectList().block().size());
	}
}
