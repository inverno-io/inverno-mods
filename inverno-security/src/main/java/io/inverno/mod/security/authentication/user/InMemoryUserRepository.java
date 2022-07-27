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
package io.inverno.mod.security.authentication.user;

import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.password.PBKDF2Password;
import io.inverno.mod.security.authentication.password.Password;
import io.inverno.mod.security.authentication.password.PasswordException;
import io.inverno.mod.security.authentication.password.PasswordPolicy;
import io.inverno.mod.security.authentication.password.PasswordPolicyException;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.password.SimplePasswordPolicy;
import io.inverno.mod.security.identity.Identity;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link UserRepository} implementation that stores users in memory.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the identity type
 * @param <B> the user type
 */
public class InMemoryUserRepository<A extends Identity, B extends User<A>> implements UserRepository<A, B> {

	/**
	 * The default password encoder
	 */
	private static final PBKDF2Password.Encoder DEFAULT_PASSWORD_ENCODER = new PBKDF2Password.Encoder();
	
	/**
	 * The users map.
	 */
	private final Map<String, B> users;
	
	/**
	 * The password policy.
	 */
	private final PasswordPolicy<B, ?> passwordPolicy;
	
	/**
	 * The password encoder.
	 */
	private final Password.Encoder<?, ?> passwordEncoder;
	
	/**
	 * <p>
	 * Creates an in-memory user repository with default password encoder and policy.
	 * </p>
	 */
	public InMemoryUserRepository() {
		this(List.of(), DEFAULT_PASSWORD_ENCODER, new SimplePasswordPolicy<>());
	}
	
	/**
	 * <p>
	 * Creates an in-memory user repository initialized with the specified list of users with default password encoder and policy.
	 * </p>
	 * 
	 * @param users a collection of users
	 */
	public InMemoryUserRepository(Collection<B> users) {
		this(users, DEFAULT_PASSWORD_ENCODER, new SimplePasswordPolicy<>());
	}
	
	/**
	 * <p>
	 * Creates an in-memory user repository initialized with the specified list of users using the specified password encoder and policy.
	 * </p>
	 *
	 * @param users           a collection of users
	 * @param passwordEncoder the password encoder
	 * @param passwordPolicy  the password policy
	 */
	public InMemoryUserRepository(Collection<B> users, Password.Encoder<?, ?> passwordEncoder, PasswordPolicy<B, ?> passwordPolicy) throws UserRepositoryException {
		this.users = new ConcurrentHashMap<>();
		this.passwordEncoder = passwordEncoder != null ? passwordEncoder : DEFAULT_PASSWORD_ENCODER;
		this.passwordPolicy = passwordPolicy != null ? passwordPolicy : new SimplePasswordPolicy<>();
		if(users != null) {
			users.stream().forEach(user -> this.createUser(user).block());
		}
	}
	
	/**
	 * <p>
	 * Returns an in-memory user repository builder.
	 * </p>
	 * 
	 * @param <A> the identity type
	 * @param <B> the user type
	 * 
	 * @return an in-memory user repository builder.
	 */
	public static <A extends Identity, B extends User<A>> InMemoryUserRepository.Builder<A, B> of() {
		return new InMemoryUserRepository.Builder<>();
	}
	
	/**
	 * <p>
	 * Returns an in-memory user repository builder initialized with the specified list of users.
	 * </p>
	 * 
	 * @param <A> the identity type
	 * @param <B> the user type
	 * @param users a collection of users
	 * 
	 * @return an in-memory user repository builder.
	 */
	public static <A extends Identity, B extends User<A>> InMemoryUserRepository.Builder<A, B> of(Collection<B> users) {
		return new InMemoryUserRepository.Builder<A, B>().users(users);
	}

	/**
	 * <p>
	 * Returns the password encoder used to encode passwords.
	 * </p>
	 * 
	 * @return the password encoder
	 */
	public Password.Encoder<?, ?> getPasswordEncoder() {
		return passwordEncoder;
	}

	/**
	 * <p>
	 * Returns the password policy used to verify passwords.
	 * </p>
	 * 
	 * @return the password policy
	 */
	public PasswordPolicy<B, ?> getPasswordPolicy() {
		return passwordPolicy;
	}
	
	@Override
	public Mono<B> createUser(B user) throws UserRepositoryException {
		Objects.requireNonNull(user);
		if(!(user.getPassword() instanceof RawPassword)) {
				throw new UserRepositoryException("User password must be a raw password");
			}
		return Mono.fromSupplier(() -> {
			B previousUser = this.users.get(user.getUsername());
			if(previousUser != null) {
				throw new UserRepositoryException("User already exists: " + user.getUsername());
			}
			
			this.passwordPolicy.verify(user, user.getPassword().getValue());
			user.setPassword(this.passwordEncoder.encode(user.getPassword().getValue()));
			this.users.put(user.getUsername(), user);
			
			return user;
		});
	}

	@Override
	public Mono<B> updateUser(B user) {
		Objects.requireNonNull(user);
		return Mono.fromSupplier(() -> {
			B previousUser = this.users.get(user.getUsername());
			if(previousUser == null) {
				return null;
			}
			
			user.setPassword(previousUser.getPassword());
			user.setGroups(previousUser.getGroups());
			user.setLocked(previousUser.isLocked());
			
			this.users.put(user.getUsername(), user);
			
			return user;
		});
	}

	@Override
	public Mono<B> getUser(String username) throws UserRepositoryException {
		Objects.requireNonNull(username);
		return Mono.fromSupplier(() -> this.users.get(username));
	}

	@Override
	public Flux<B> listUsers() throws UserRepositoryException {
		return Flux.fromStream(() -> this.users.values().stream());
	}
	
	@Override
	public Mono<B> lockUser(String username) throws UserRepositoryException {
		Objects.requireNonNull(username);
		return Mono.fromSupplier(() -> {
			B previousUser = this.users.get(username);
			if(previousUser == null) {
				return null;
			}
			previousUser.setLocked(true);
			this.users.put(previousUser.getUsername(), previousUser);
			
			return previousUser;
		});
	}

	@Override
	public Mono<B> unlockUser(String username) throws UserRepositoryException {
		Objects.requireNonNull(username);
		return Mono.fromSupplier(() -> {
			B previousUser = this.users.get(username);
			if(previousUser == null) {
				return null;
			}
			previousUser.setLocked(false);
			this.users.put(previousUser.getUsername(), previousUser);
			
			return previousUser;
		});
	}
	
	@Override
	public Mono<B> changePassword(LoginCredentials credentials, String rawPassword) throws AuthenticationException, PasswordPolicyException, PasswordException, UserRepositoryException {
		Objects.requireNonNull(credentials);
		Objects.requireNonNull(rawPassword);
		return Mono.fromSupplier(() -> {
			B previousUser = this.users.get(credentials.getUsername());
			if(previousUser == null) {
				return null;
			}
			
			// Here we simply check password from credentials matches what we have in the repository
			// This is what is done in the UserAuthenticator which is fine for this particular use case
			// We can imagine more complex implementations where a full authentication is performed with the provied credentials (including two factors authentication...)
			// This would basically require to inject a UserAuthenticator
			if(!previousUser.getPassword().matches(credentials.getPassword())) {
				throw new AuthenticationException("Invalid credentials");
			}
			
			this.passwordPolicy.verify(previousUser, rawPassword);
			previousUser.setPassword(this.passwordEncoder.encode(rawPassword));
			this.users.put(previousUser.getUsername(), previousUser);
			
			return previousUser;
		});
	}

	@Override
	public Mono<B> addUserToGroups(String username, String... groups) throws UserRepositoryException {
		Objects.requireNonNull(username);
		Objects.requireNonNull(groups);
		return Mono.fromSupplier(() -> {
			B previousUser = this.users.get(username);
			if(previousUser == null) {
				return null;
			}
			
			Set<String> newGroups = new HashSet<>(previousUser.getGroups());
			newGroups.addAll(Arrays.asList(groups));
			previousUser.setGroups(newGroups);
			this.users.put(previousUser.getUsername(), previousUser);
			
			return previousUser;
		});
	}

	@Override
	public Mono<B> removeUserFromGroups(String username, String... groups) throws UserRepositoryException {
		Objects.requireNonNull(username);
		Objects.requireNonNull(groups);
		return Mono.fromSupplier(() -> {
			B previousUser = this.users.get(username);
			if(previousUser == null) {
				return null;
			}
			
			Set<String> newGroups = new HashSet<>(previousUser.getGroups());
			newGroups.removeAll(Arrays.asList(groups));
			previousUser.setGroups(newGroups);
			this.users.put(previousUser.getUsername(), previousUser);
			
			return previousUser;
		});
	}
	
	@Override
	public Mono<B> deleteUser(String username) throws UserRepositoryException {
		Objects.requireNonNull(username);
		return Mono.fromSupplier(() -> {
			B previousUser = this.users.remove(username);
			if(previousUser == null) {
				return null;
			}
			return previousUser;
		});
	}

	@Override
	public Mono<B> resolveCredentials(String id) throws SecurityException {
		Objects.requireNonNull(id);
		return this.getUser(id);
	}
	
	/**
	 * <p>
	 * A builder used to build in-memory user reposities.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the identity type
	 * @param <B> the user type
	 */
	public static class Builder<A extends Identity, B extends User<A>> {
	
		/**
		 * The initial set of users.
		 */
		private Set<B> users;
	
		/**
		 * The password policy.
		 */
		private PasswordPolicy<B, ?> passwordPolicy;
	
		/**
		 * The password encoder.
		 */
		private Password.Encoder<?, ?> passwordEncoder;
		
		/**
		 * Creates the builder.
		 */
		private Builder() {}
		
		/**
		 * <p>
		 * Adds a user to create when initializing the repository.
		 * </p>
		 * 
		 * @param user a user
		 * 
		 * @return this builder
		 */
		public Builder<A, B> user(B user) {
			if(user != null) {
				this.users.add(user);
			}
			return this;
		}
		
		/**
		 * <p>
		 * Adds users to create when initializing the repository.
		 * </p>
		 * 
		 * @param users a collection of users
		 * 
		 * @return this builder
		 */
		public Builder<A, B> users(Collection<B> users) {
			if(users != null) {
				if(this.users == null) {
					this.users = new HashSet<>();
				}
				this.users.addAll(users);
			}
			return this;
		}
		
		/**
		 * <p>
		 * Specifies the password policy.
		 * </p>
		 * 
		 * @param passwordPolicy a password policy
		 * 
		 * @return this builder
		 */
		public Builder<A, B> passwordPolicy(PasswordPolicy<B, ?> passwordPolicy) {
			this.passwordPolicy = passwordPolicy;
			return this;
		}
		
		/**
		 * <p>
		 * Specifies the password encoder.
		 * </p>
		 * 
		 * @param passwordEncoder a password encoder
		 * 
		 * @return this builder
		 */
		public Builder<A, B> passwordEncoder(Password.Encoder<?, ?> passwordEncoder) {
			this.passwordEncoder = passwordEncoder;
			return this;
		}
		
		/**
		 * <p>
		 * Builds an in-memory user repository.
		 * </p>
		 * 
		 * @return an new in-memory user repository
		 */
		public InMemoryUserRepository<A, B> build() {
			return new InMemoryUserRepository<>(this.users, this.passwordEncoder, this.passwordPolicy);
		}
	}
}
