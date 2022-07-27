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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.redis.RedisClient;
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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link UserRepository} implementation that stores users in a Redis data store.
 * </p>
 * 
 *
 * 
 * <p>
 * Users are stored as string entries serialized as JSON, the user key is of the form: {@code keyPrefix ":USER:" username}. Groups are stored as set entries, the group key is of the form:
 * {@code keyPrefix ":GROUP:" group}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the identity type
 * @param <B> the user type
 */
public class RedisUserRepository<A extends Identity, B extends User<A>> implements UserRepository<A, B> {
	
	/**
	 * The default key prefix.
	 */
	private static final String DEFAULT_KEY_PREFIX = "SEC";
	
	/**
	 * The default password encoder.
	 */
	private static final PBKDF2Password.Encoder DEFAULT_PASSWORD_ENCODER = new PBKDF2Password.Encoder();

	/**
	 * The redis client.
	 */
	private final RedisClient<String, String> redisClient;
	
	/**
	 * The object mapper.
	 */
	private final ObjectMapper mapper;
	
	/**
	 * The password policy.
	 */
	private final PasswordPolicy<B, ?> passwordPolicy;
	
	/**
	 * The password encoder.
	 */
	private final Password.Encoder<?, ?> passwordEncoder;
	
	/**
	 * The key prefix.
	 */
	private String keyPrefix;
	
	/**
	 * The user key prefix.
	 */
	private String userKeyPrefix;
	
	/**
	 * The group key prefix.
	 */
	private String groupKeyPrefix;
	
	/**
	 * <p>
	 * Creates a Redis user repository with the specified Redis client and mapper.
	 * </p>
	 *
	 * @param redisClient a Redis client
	 * @param mapper      an object mapper
	 */
	public RedisUserRepository(RedisClient<String, String> redisClient, ObjectMapper mapper) {
		this(redisClient, mapper, DEFAULT_PASSWORD_ENCODER, new SimplePasswordPolicy<>());
	}
	
	/**
	 * <p>
	 * Creates a Redis user repository with the specified Redis client, mapper and password policy.
	 * </p>
	 *
	 * @param redisClient    a Redis client
	 * @param mapper         an object mapper
	 * @param passwordPolicy a password policy
	 */
	public RedisUserRepository(RedisClient<String, String> redisClient, ObjectMapper mapper, PasswordPolicy<B, ?> passwordPolicy) {
		this(redisClient, mapper, DEFAULT_PASSWORD_ENCODER, passwordPolicy);
	}
	
	/**
	 * <p>
	 * Creates a Redis user repository with the specified Redis client mapper and password encoder.
	 * </p>
	 *
	 * @param redisClient     a Redis client
	 * @param mapper          an object mapper
	 * @param passwordEncoder a password encoder
	 */
	public RedisUserRepository(RedisClient<String, String> redisClient, ObjectMapper mapper, Password.Encoder<?, ?> passwordEncoder) {
		this(redisClient, mapper, passwordEncoder, new SimplePasswordPolicy<>());
	}
	
	/**
	 * <p>
	 * Creates a Redis user repository with the specified Redis client, mapper, password encoder and password policy.
	 * </p>
	 * 
	 * @param redisClient     a Redis client
	 * @param mapper          an object mapper
	 * @param passwordEncoder a password encoder
	 * @param passwordPolicy  a password policy
	 */
	public RedisUserRepository(RedisClient<String, String> redisClient, ObjectMapper mapper, Password.Encoder<?, ?> passwordEncoder, PasswordPolicy<B, ?> passwordPolicy) {
		this.redisClient = redisClient;
		this.mapper = mapper;
		this.passwordPolicy = passwordPolicy;
		this.passwordEncoder = passwordEncoder;
		this.setKeyPrefix(DEFAULT_KEY_PREFIX);
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

	/**
	 * <p>
	 * Returns the prefix used to format user and group entry keys.
	 * </p>
	 * 
	 * <ul>
	 * <li>A user key is of the form: {@code keyPrefix ":USER:" username}.</li>
	 * <li>A group key is of the form: {@code keyPrefix ":GROUP:" group}.</li>
	 * </ul>
	 * 
	 * @return the key prefix
	 */
	public final String getKeyPrefix() {
		return keyPrefix;
	}

	/**
	 * <p>
	 * Sets the prefix used to format user and group entry keys.
	 * </p>
	 * 
	 * <ul>
	 * <li>A user key is of the form: {@code keyPrefix ":USER:" username}.</li>
	 * <li>A group key is of the form: {@code keyPrefix ":GROUP:" group}.</li>
	 * </ul>
	 * 
	 * @param keyPrefix the key prefix to set
	 */
	public final void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = StringUtils.isNotBlank(keyPrefix) ? keyPrefix  : DEFAULT_KEY_PREFIX;
		this.userKeyPrefix = this.keyPrefix + ":USER:";
		this.groupKeyPrefix = this.keyPrefix + ":GROUP:";
	}
	
	@Override
	public Mono<B> createUser(B user) throws UserRepositoryException {
		Objects.requireNonNull(user);
		if(!(user.getPassword() instanceof RawPassword)) {
			throw new UserRepositoryException("User password must be a raw password");
		}
		return Mono.from(this.redisClient.connection(operations -> {
			try {
				this.passwordPolicy.verify(user, user.getPassword().getValue());
				user.setPassword(this.passwordEncoder.encode(user.getPassword().getValue()));
				return operations.set().nx().build(this.userKeyPrefix + user.getUsername(), this.mapper.writeValueAsString(user))
					.switchIfEmpty(Mono.error(() -> new UserRepositoryException("User already exists: " + user.getUsername())))
					.flatMapMany(result -> {
						if(result.equals("OK")) {
							if(!user.getGroups().isEmpty()) {
								return Flux.fromIterable(user.getGroups()).flatMap(group -> operations.sadd(this.groupKeyPrefix + group, user.getUsername()));
							}
							return Mono.empty();
						}
						else {
							throw new UserRepositoryException("Error setting user: " + user.getUsername());
						}
					})
					.then(Mono.just(user));
			} 
			catch (JsonProcessingException e) {
				throw new UserRepositoryException(e);
			}
		}));
	}

	@Override
	public Mono<B> updateUser(B user) throws UserRepositoryException {
		Objects.requireNonNull(user);
		return this.getUser(user.getUsername())
			.flatMap(previousUser -> {
				// this doesn't update password, groups or locked
				user.setPassword(previousUser.getPassword());
				user.setGroups(previousUser.getGroups());
				user.setLocked(previousUser.isLocked());
				try {
					return this.redisClient.set(this.userKeyPrefix + user.getUsername(), this.mapper.writeValueAsString(user))
						.map(result -> {
							if(result.equals("OK")) {
								return user;
							}
							else {
								throw new UserRepositoryException("Error setting user: " + user.getUsername());
							}
						});
				}
				catch(JsonProcessingException e) {
					throw new UserRepositoryException(e);
				}
			});
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Mono<B> getUser(String username) throws UserRepositoryException {
		Objects.requireNonNull(username);
		return this.redisClient.get(this.userKeyPrefix + username)
			.map(jsonUser -> {
				try {
					return (B)this.mapper.readValue(jsonUser, User.class);
				}
				catch (JsonProcessingException e) {
					throw new UserRepositoryException("Malformed user: " + username, e);
				}
			});
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<B> listUsers() throws UserRepositoryException {
		String usersPattern = this.userKeyPrefix + "*";
		return Flux.from(this.redisClient.connection(operations -> operations.scan()
			.pattern(usersPattern)
			.count(100)
			.build("0")
			.expand(result -> {
				if(result.isFinished()) {
					return Mono.empty();
				}
				return operations.scan()
					.pattern(usersPattern)
					.count(100)
					.build(result.getCursor());
			})
			.flatMapIterable(result -> result.getKeys())
			.flatMap(username -> operations.get(username)
				.map(jsonUser -> {
					try {
						return (B)this.mapper.readValue(jsonUser, User.class);
					}
					catch (JsonProcessingException e) {
						throw new UserRepositoryException("Malformed user: " + username, e);
					}
				})
			)
		));
	}

	@Override
	public Mono<B> changePassword(LoginCredentials credentials, String rawPassword) throws AuthenticationException, PasswordPolicyException, PasswordException, UserRepositoryException {
		Objects.requireNonNull(credentials); // current credentials
		Objects.requireNonNull(rawPassword); // new password
		return this.getUser(credentials.getUsername())
			.flatMap(previousUser -> {
				// Here we simply check password from credentials matches what we have in the repository
				// This is what is done in the UserAuthenticator which is fine for this particular use case
				// We can imagine more complex implementations where a full authentication is performed with the provided credentials (including two factors authentication...)
				// This would basically require to inject a UserAuthenticator
				if(!previousUser.getPassword().matches(credentials.getPassword())) {
					throw new AuthenticationException("Invalid credentials");
				}
				
				this.passwordPolicy.verify(previousUser, rawPassword);
				previousUser.setPassword(this.passwordEncoder.encode(rawPassword));
				
				try {
					return this.redisClient.set(this.userKeyPrefix + credentials.getUsername(), this.mapper.writeValueAsString(previousUser))
						.map(result -> {
							if(result.equals("OK")) {
								return previousUser;
							}
							else {
								throw new UserRepositoryException("Error setting user: " + credentials.getUsername());
							}
						});
				}
				catch(JsonProcessingException e) {
					throw new UserRepositoryException(e);
				}
			});
	}

	@Override
	public Mono<B> lockUser(String username) throws UserRepositoryException {
		Objects.requireNonNull(username);
		return this.getUser(username)
			.flatMap(previousUser -> {
				previousUser.setLocked(true);
				try {
					return this.redisClient.set(this.userKeyPrefix + username, this.mapper.writeValueAsString(previousUser))
						.map(result -> {
							if(result.equals("OK")) {
								return previousUser;
							}
							else {
								throw new UserRepositoryException("Error setting user: " + username);
							}
						});
				}
				catch(JsonProcessingException e) {
					throw new UserRepositoryException(e);
				}
			});
	}

	@Override
	public Mono<B> unlockUser(String username) throws UserRepositoryException {
		Objects.requireNonNull(username);
		return this.getUser(username)
			.flatMap(previousUser -> {
				previousUser.setLocked(false);
				try {
					return this.redisClient.set(this.userKeyPrefix + username, this.mapper.writeValueAsString(previousUser))
						.map(result -> {
							if(result.equals("OK")) {
								return previousUser;
							}
							else {
								throw new UserRepositoryException("Error setting user: " + username);
							}
						});
				}
				catch(JsonProcessingException e) {
					throw new UserRepositoryException(e);
				}
			});
	}

	@Override
	public Mono<B> addUserToGroups(String username, String... groups) throws UserRepositoryException {
		Objects.requireNonNull(username);
		Objects.requireNonNull(groups);
		return this.getUser(username)
			.flatMap(previousUser -> {
				Set<String> updatedGroups = new HashSet<>(previousUser.getGroups());
				Set<String> addedGroups = Arrays.stream(groups).filter(updatedGroups::add).collect(Collectors.toSet());
				if(updatedGroups.size() != previousUser.getGroups().size()) {
					previousUser.setGroups(updatedGroups);
					return Mono.from(this.redisClient.connection(operations -> {
						try {
							return operations.set().xx().build(this.userKeyPrefix + username, this.mapper.writeValueAsString(previousUser))
								.switchIfEmpty(Mono.error(() -> new UserRepositoryException("User already exists: " + username)))
								.flatMapMany(result -> {
									if(result.equals("OK")) {
										return Flux.fromIterable(addedGroups).flatMap(group -> operations.sadd(this.groupKeyPrefix + group, username));
									}
									else {
										throw new UserRepositoryException("Error setting user: " + username);
									}
								})
								.then(Mono.just(previousUser));
						} 
						catch (JsonProcessingException e) {
							throw new UserRepositoryException(e);
						}
					}));
				}
				else {
					return Mono.just(previousUser);
				}
			});
	}

	@Override
	public Mono<B> removeUserFromGroups(String username, String... groups) throws UserRepositoryException {
		Objects.requireNonNull(username);
		Objects.requireNonNull(groups);
		return this.getUser(username)
			.flatMap(previousUser -> {
				Set<String> updatedGroups = new HashSet<>(previousUser.getGroups());
				Set<String> removedGroups = Arrays.stream(groups).filter(updatedGroups::remove).collect(Collectors.toSet());
				if(updatedGroups.size() != previousUser.getGroups().size()) {
					previousUser.setGroups(updatedGroups);
					return Mono.from(this.redisClient.connection(operations -> {
						try {
							return operations.set().xx().build(this.userKeyPrefix + username, this.mapper.writeValueAsString(previousUser))
								.switchIfEmpty(Mono.error(() -> new UserRepositoryException("User already exists: " + username)))
								.flatMapMany(result -> {
									if(result.equals("OK")) {
										return Flux.fromIterable(removedGroups).flatMap(group -> operations.srem(this.groupKeyPrefix + group, username));
									}
									else {
										throw new UserRepositoryException("Error setting user: " + username);
									}
								})
								.then(Mono.just(previousUser));
						} 
						catch (JsonProcessingException e) {
							throw new UserRepositoryException(e);
						}
					}));
				}
				else {
					return Mono.just(previousUser);
				}
			});
	}

	@Override
	public Mono<B> deleteUser(String username) throws UserRepositoryException {
		Objects.requireNonNull(username);
		return this.getUser(username)
			.flatMap(previousUser -> Mono.from(this.redisClient.connection(operations -> Flux.fromIterable(previousUser.getGroups())
				.flatMap(group -> operations.srem(this.groupKeyPrefix + group, username))
				.then(operations.del(this.userKeyPrefix + username))
				.thenReturn(previousUser)
			)));
	}

	@Override
	public Mono<B> resolveCredentials(String id) throws SecurityException {
		return this.getUser(id);
	}
}

