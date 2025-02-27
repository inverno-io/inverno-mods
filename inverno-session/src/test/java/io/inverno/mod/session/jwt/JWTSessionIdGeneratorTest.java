/*
 * Copyright 2025 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.inverno.mod.session.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.boot.converter.JacksonStringConverter;
import io.inverno.mod.boot.converter.JsonStringMediaTypeConverter;
import io.inverno.mod.boot.converter.TextStringMediaTypeConverter;
import io.inverno.mod.security.jose.JOSEHeader;
import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.JOSEObjectReader;
import io.inverno.mod.security.jose.Jose;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.InMemoryJWKStore;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwt.InvalidJWTException;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.session.internal.jwt.JWTESessionIdGenerator;
import io.inverno.mod.session.internal.jwt.JWTSSessionIdGenerator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class JWTSessionIdGeneratorTest {

	private static final String ISSUER = "test";

	private static Jose joseModule;

	private static JWTSessionIdGenerator<Void, Map<String, String>> jwtsSessionIdGenerator;
	private static JWTSessionIdGenerator<Void, Map<String, String>> jwtsSessionIdGeneratorWithIssuer;
	private static JWTSessionIdGenerator<Void, Map<String, String>> jwteSessionIdGenerator;
	private static JWTSessionIdGenerator<Void, Map<String, String>> jwteSessionIdGeneratorWithIssuer;

	@BeforeAll
	public static void init() {
		JsonStringMediaTypeConverter jsonConverter = new JsonStringMediaTypeConverter(new JacksonStringConverter(new ObjectMapper()));
		TextStringMediaTypeConverter textConverter = new TextStringMediaTypeConverter(new StringConverter());

		joseModule = new Jose.Builder(List.of(jsonConverter, textConverter)).setJwkStore(new InMemoryJWKStore()).build();
		joseModule.start();

		String jwsKeyId = UUID.randomUUID().toString();
		joseModule.jwkService().oct().generator()
			.keyId(jwsKeyId)
			.algorithm(OCTAlgorithm.HS256.getAlgorithm())
			.generate()
			.map(JWK::trust)
			.flatMap(joseModule.jwkService().store()::set)
			.block();

		jwtsSessionIdGenerator = JWTSessionIdGenerator.jws(joseModule.jwtService(), headers -> headers.keyId(jwsKeyId).algorithm(OCTAlgorithm.HS256.getAlgorithm()));
		jwtsSessionIdGeneratorWithIssuer = JWTSessionIdGenerator.jws(joseModule.jwtService(), headers -> headers.keyId(jwsKeyId).algorithm(OCTAlgorithm.HS256.getAlgorithm()), ISSUER);

		String jweKeyId = UUID.randomUUID().toString();
		joseModule.jwkService().ec().generator()
			.keyId(jweKeyId)
			.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
			.curve(ECCurve.P_256.getCurve())
			.generate()
			.map(JWK::trust)
			.flatMap(joseModule.jwkService().store()::set)
			.block();

		jwteSessionIdGenerator = JWTSessionIdGenerator.jwe(joseModule.jwtService(), header -> header.keyId(jweKeyId).algorithm(ECAlgorithm.ECDH_ES.getAlgorithm()).encryptionAlgorithm(OCTAlgorithm.A256GCM.getAlgorithm()));
		jwteSessionIdGeneratorWithIssuer = JWTSessionIdGenerator.jwe(joseModule.jwtService(), header -> header.keyId(jweKeyId).algorithm(ECAlgorithm.ECDH_ES.getAlgorithm()).encryptionAlgorithm(OCTAlgorithm.A256GCM.getAlgorithm()), ISSUER);
	}

	@AfterAll
	public static void destroy() {
		joseModule.stop();
	}

	public static Stream<Arguments> provideJWTSessionIdGeneratorsAndJWTReaders() {
		return Stream.of(
			Arguments.of(jwtsSessionIdGenerator, joseModule.jwtService().jwsReader()),
			Arguments.of(jwteSessionIdGenerator, joseModule.jwtService().jweReader())
		);
	}

	public static Stream<Arguments> provideJWTSessionIdGeneratorsWithAndWithoutIssuer() {
		return Stream.of(
			Arguments.of(jwtsSessionIdGenerator, jwtsSessionIdGeneratorWithIssuer),
			Arguments.of(jwteSessionIdGenerator, jwteSessionIdGeneratorWithIssuer)
		);
	}

	@SuppressWarnings("unchecked")
	private static JWTSession<Void, Map<String, String>> mockJWTSession(Long maxInactiveInterval, Long expirationTime, Map<String, String> statelessData) {
		JWTSession<Void, Map<String, String>> jwtSessionMock = Mockito.mock(JWTSession.class);

		Mockito.when(jwtSessionMock.getStatelessData()).thenReturn(statelessData);
		Mockito.when(jwtSessionMock.getCreationTime()).thenReturn(System.currentTimeMillis());
		if(maxInactiveInterval != null) {
			Mockito.when(jwtSessionMock.getMaxInactiveInterval()).thenReturn(maxInactiveInterval);
		}
		else if(expirationTime != null) {
			Mockito.when(jwtSessionMock.getMaxInactiveInterval()).thenReturn(null);
			Mockito.when(jwtSessionMock.getExpirationTime()).thenReturn(expirationTime);
		}
		else {
			throw new IllegalArgumentException("One of maxInactiveInterval or expirationTime must be provided");
		}
		return jwtSessionMock;
	}

	@ParameterizedTest
	@MethodSource("provideJWTSessionIdGeneratorsAndJWTReaders")
	public void generateJWT_should_set_jwt_id_claim(JWTSessionIdGenerator<Void, Map<String, String>> jwtSessionIdGenerator, JOSEObjectReader<JWTClaimsSet, ? extends JOSEHeader, JOSEObject<JWTClaimsSet, ? extends JOSEHeader>, ?> jwtReader) {
		JWTSession<Void, Map<String, String>> jwtSessionMock = mockJWTSession(300000L, null, null);

		String sessionId = jwtSessionIdGenerator.generate(jwtSessionMock).block();
		Assertions.assertNotNull(sessionId);

		JOSEObject<JWTClaimsSet, ? extends JOSEHeader> jwtSessionId = jwtReader.read(sessionId).block();
		Assertions.assertNotNull(jwtSessionId.getPayload().getJWTId());
		Assertions.assertTrue(jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).isEmpty());
	}

	@ParameterizedTest
	@MethodSource("provideJWTSessionIdGeneratorsAndJWTReaders")
	public void generateJWT_should_set_issued_at_claim(JWTSessionIdGenerator<Void, Map<String, String>> jwtSessionIdGenerator, JOSEObjectReader<JWTClaimsSet, ? extends JOSEHeader, JOSEObject<JWTClaimsSet, ? extends JOSEHeader>, ?> jwtReader) {
		JWTSession<Void, Map<String, String>> jwtSessionMock = mockJWTSession(300000L, null, null);

		String sessionId = jwtSessionIdGenerator.generate(jwtSessionMock).block();
		Assertions.assertNotNull(sessionId);

		JOSEObject<JWTClaimsSet, ? extends JOSEHeader> jwtSessionId = jwtReader.read(sessionId).block();
		Assertions.assertNotNull(jwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(Math.floorDiv(jwtSessionMock.getCreationTime(), 1000), jwtSessionId.getPayload().getIssuedAt());
		Assertions.assertTrue(jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).isEmpty());
	}

	@ParameterizedTest
	@MethodSource("provideJWTSessionIdGeneratorsAndJWTReaders")
	public void generateJWT_should_set_max_inactive_interval_claim(JWTSessionIdGenerator<Void, Map<String, String>> jwtSessionIdGenerator, JOSEObjectReader<JWTClaimsSet, ? extends JOSEHeader, JOSEObject<JWTClaimsSet, ? extends JOSEHeader>, ?> jwtReader) {
		JWTSession<Void, Map<String, String>> jwtSessionMock = mockJWTSession(300000L, null, null);

		String sessionId = jwtSessionIdGenerator.generate(jwtSessionMock).block();
		Assertions.assertNotNull(sessionId);

		JOSEObject<JWTClaimsSet, ? extends JOSEHeader> jwtSessionId = jwtReader.read(sessionId).block();
		Assertions.assertNotNull(jwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(Math.floorDiv(jwtSessionMock.getCreationTime(), 1000), jwtSessionId.getPayload().getIssuedAt());
		Assertions.assertEquals(jwtSessionMock.getMaxInactiveInterval(), jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).map(JWTClaimsSet.Claim::asLong).orElse(null));
		Assertions.assertNull(jwtSessionId.getPayload().getExpirationTime());
		Assertions.assertTrue(jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).isEmpty());
	}

	@ParameterizedTest
	@MethodSource("provideJWTSessionIdGeneratorsAndJWTReaders")
	public void generateJWT_should_set_expiration_time_claim(JWTSessionIdGenerator<Void, Map<String, String>> jwtSessionIdGenerator, JOSEObjectReader<JWTClaimsSet, ? extends JOSEHeader, JOSEObject<JWTClaimsSet, ? extends JOSEHeader>, ?> jwtReader) {
		JWTSession<Void, Map<String, String>> jwtSessionMock = mockJWTSession(null, System.currentTimeMillis() + 300000L, null);

		String sessionId = jwtSessionIdGenerator.generate(jwtSessionMock).block();
		Assertions.assertNotNull(sessionId);

		JOSEObject<JWTClaimsSet, ? extends JOSEHeader> jwtSessionId = jwtReader.read(sessionId).block();
		Assertions.assertNotNull(jwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(Math.floorDiv(jwtSessionMock.getCreationTime(), 1000), jwtSessionId.getPayload().getIssuedAt());
		Assertions.assertTrue(jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).isEmpty());
		Assertions.assertEquals(Math.floorDiv(jwtSessionMock.getExpirationTime(), 1000), jwtSessionId.getPayload().getExpirationTime());
		Assertions.assertTrue(jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).isEmpty());
	}

	@ParameterizedTest
	@MethodSource("provideJWTSessionIdGeneratorsAndJWTReaders")
	public void generateJWT_should_set_stateless_data(JWTSessionIdGenerator<Void, Map<String, String>> jwtSessionIdGenerator, JOSEObjectReader<JWTClaimsSet, ? extends JOSEHeader, JOSEObject<JWTClaimsSet, ? extends JOSEHeader>, ?> jwtReader) {
		JWTSession<Void, Map<String, String>> jwtSessionMock = mockJWTSession(300000L, null, Map.of("a", "1", "b", "2"));

		String sessionId = jwtSessionIdGenerator.generate(jwtSessionMock).block();
		Assertions.assertNotNull(sessionId);

		JOSEObject<JWTClaimsSet, ? extends JOSEHeader> jwtSessionId = jwtReader.read(sessionId).block();
		Assertions.assertNotNull(jwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(Map.of("a", "1", "b", "2"), jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).map(claim -> claim.<Map<String, String>>as(Map.class)).orElse(null));
	}

	@ParameterizedTest
	@MethodSource("provideJWTSessionIdGeneratorsAndJWTReaders")
	public void readJWT_should_return_jwt_session_id(JWTSessionIdGenerator<Void, Map<String, String>> jwtSessionIdGenerator, JOSEObjectReader<JWTClaimsSet, ? extends JOSEHeader, JOSEObject<JWTClaimsSet, ? extends JOSEHeader>, ?> jwtReader) {
		JWTSession<Void, Map<String, String>> jwtSessionMock = mockJWTSession(300000L, null, Map.of("a", "1", "b", "2"));

		String sessionId = jwtSessionIdGenerator.generate(jwtSessionMock).block();

		JOSEObject<JWTClaimsSet, ?> jwtSessionId = jwtSessionIdGenerator.readJWT(sessionId).block();

		Assertions.assertNotNull(jwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(Math.floorDiv(jwtSessionMock.getCreationTime(), 1000), jwtSessionId.getPayload().getIssuedAt());
		Assertions.assertEquals(jwtSessionMock.getMaxInactiveInterval(), jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).map(JWTClaimsSet.Claim::asLong).orElse(null));
		Assertions.assertNull(jwtSessionId.getPayload().getExpirationTime());
		Assertions.assertEquals(Map.of("a", "1", "b", "2"), jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).map(claim -> claim.<Map<String, String>>as(Map.class)).orElse(null));
	}

	@ParameterizedTest
	@MethodSource("provideJWTSessionIdGeneratorsWithAndWithoutIssuer")
	public void given_issuer_readJWT_should_validate_issuer(JWTSessionIdGenerator<Void, Map<String, String>> jwtSessionIdGenerator, JWTSessionIdGenerator<Void, Map<String, String>> jwtSessionIdGeneratorWithIssuer) {
		JWTSession<Void, Map<String, String>> jwtSessionMock = mockJWTSession(300000L, null, Map.of("a", "1", "b", "2"));

		String sessionIdWithoutIssuer = jwtSessionIdGenerator.generate(jwtSessionMock).block();

		Assertions.assertThrows(InvalidJWTException.class, () -> jwtSessionIdGeneratorWithIssuer.readJWT(sessionIdWithoutIssuer).block(), "Invalid issuer: null");

		String sessionIdWithIssuer =jwtSessionIdGeneratorWithIssuer.generate(jwtSessionMock).block();

		JOSEObject<JWTClaimsSet, ?> jwtSessionId = jwtSessionIdGeneratorWithIssuer.readJWT(sessionIdWithIssuer).block();
		Assertions.assertEquals(ISSUER, jwtSessionId.getPayload().getIssuer());
	}
}
