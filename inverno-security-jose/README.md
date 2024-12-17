[rfc7515]: https://datatracker.ietf.org/doc/html/rfc7515
[rfc7516]: https://datatracker.ietf.org/doc/html/rfc7516
[rfc7517]: https://datatracker.ietf.org/doc/html/rfc7517
[rfc7518]: https://datatracker.ietf.org/doc/html/rfc7518
[rfc7519]: https://datatracker.ietf.org/doc/html/rfc7519
[rfc7638]: https://datatracker.ietf.org/doc/html/rfc7638
[rfc7797]: https://datatracker.ietf.org/doc/html/rfc7797
[rfc8037]: https://datatracker.ietf.org/doc/html/rfc8037
[rfc8812]: https://datatracker.ietf.org/doc/html/rfc8812


# JSON Object Signing and Encryption

The Inverno *security-jose* module is a complete implementation of JSON Object Signing and Encryption RFC specifications.

It allows to create, load or manipulate JSON Web Keys used to sign and verify JWS tokens or encrypt and decrypt JWE tokens. It also allows to manipulate so-called JSON Web Tokens (JWT) which are basically a set of claims wrapped inside a JWS or JWE token.

JWS and JWE tokens are using cryptographic signature and encryption algorithms which offer both payload integrity and/or privacy. The fact that they can be easily validated makes them an ideal choice for token credentials which do not necessarily require external systems for authentication.

Here is the complete list of RFCs implemented in the *security-jose* module:

- [RFC 7515][rfc7515] JSON Web Signature (JWS)
- [RFC 7516][rfc7516] JSON Web Encryption (JWE)
- [RFC 7517][rfc7517] JSON Web Key (JWK)
- [RFC 7518][rfc7518] JSON Web Algorithms (JWA)
- [RFC 7519][rfc7519] JSON Web Token (JWT)
- [RFC 7638][rfc7638] JSON Web Key (JWK) Thumbprint
- [RFC 7797][rfc7797] JSON Web Signature (JWS) Unencoded Payload Option
- [RFC 8037][rfc8037] CFRG Elliptic Curve Diffie-Hellman (ECDH) and Signatures in JSON Object Signing and Encryption (JOSE)
- [RFC 8812][rfc8812] CBOR Object Signing and Encryption (COSE) and JSON Object Signing and Encryption (JOSE) Registrations for Web Authentication (WebAuthn) Algorithms

The Inverno *security-jose* module requires media type converters to be able to convert JWS and JWE payloads (e.g. object to JSON...), media type converters are usually provided in the *boot* module, as a result in order to use the module, we need to declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.security.jose;
}
```

And also declare these dependencies in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
    <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-boot</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-security-jose</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
compile 'io.inverno.mod:inverno-security-jose:${VERSION_INVERNO_MODS}'
```

The *security-jose* module is an Inverno module which exposes four services:

- the **jwkService** used to manage JSON Web Keys.
- the **jwsService** used to sign and verify JSON Web signature tokens.
- the **jwsService** used to encrypt and decrypt JSON Web signature tokens.
- the **jwtService** used to create JSON Web tokens as JWS or JWE.

It also provides JOSE object media type converters (e.g. `application/jose`, `application/jose+json`, `application/jwk+json`...) which can be used to decode (parse, verify, decrypt) JWS, JWE or JWK.

It can be easily composed in another Inverno module, as shown above, to get these services injected where they are needed, but it can also be used in any other application which requires JOSE support. Media type converters might however be required to automatically convert payloads inside JWS or JWE token based on the content type, they can be provided explicitly when creating the module.

> Explicit encoders and decoders can also be used to convert payloads, it is then completely possible to run the module without specifying media type converters.

A `Jose` module instance embeddable in any Java application and able to handle `application/json` or `text/plain` payloads can be obtained as follows:

```java
// Exported in the 'boot' module
JsonStringMediaTypeConverter jsonConverter = new JsonStringMediaTypeConverter(new JacksonStringConverter(new ObjectMapper()));
TextStringMediaTypeConverter textConverter = new TextStringMediaTypeConverter(new StringConverter());

// Build Jose module
Jose jose = new Jose.Builder(List.of(jsonConverter, textConverter)).build();

// Initialize Jose module
jose.start();

// Create, load or store JSON Web keys
JWKService jwkService = jose.jwkService();
...

// Create, sign and verify JSON Web Signature tokens
JWSService jwsService = jose.jwsService();
...

// Create, encrypt and decrypt JSON Web encryption tokens
JWEService jweService = jose.jweService();
...

// Create JSON Web Token as JWS or JWE
JWTService jwtService = jose.jwtService();
...

// Destroy Jose module
jose.stop();
```

Although it is recommended to compose the *security-jose* module with the *boot* module inside an Inverno application so as not to have to deal with dependency injection or module's lifecycle, it is completely feasible to use JOSE services in any Java application as shown above, even those which do not use the Java module system.

The API is quite complete and supports advanced features such as automatic key resolution by JWK key id or X.509 thumbprints (from a Java key store or other trusted repositories), a JWK store to store frequently used keys, JWK certificate path validation, JWK Set resolution, JWE compression... Before seeing all this in details, let's quickly see how to create JSON Web Keys and use them to create and read JWS, JWE or JWT tokens.

A JSON Web Key (JWK) represents a cryptographic key used to sign/verify or encrypt/decrypt JWS or JWE tokens. The following example shows how to create a simple symmetric octet key using HS256 signature algorithm:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...

/*
 * {
 *   "alg":"HS512",
 *   "k":"h92UNTmd5NpTl5UUalbp03z4AygiLZrDYHOaSjwjYQ_fma8_aO6A8MwOUjJGJyFEGPLJ46ujcTLlKoO_AjK3UQ",
 *   "kty":"oct",
 *   "kid":"octKey"
 * }
 */
Mono<? extends OCTJWK> octKey = jwkService.oct().generator()
    .keyId("octKey")
    .algorithm(OCTAlgorithm.HS512.getAlgorithm())
    .generate()
    .cache();
```

The API is fully reactive, subscribing multiple times to the `Mono` returned by the key generator would result in multiple keys being generated which is why `cache()` was used to make sure one single key is generated and returned. The key thus obtained can then be used to sign or verify JWS tokens.

A JSON Web Signature token (JWS) is composed of a header, a payload and a payload signature. The header basically specifies the information needed to verify the payload signature. A JWS token then provides integrity protection since it is not possible to modify the payload, which is unencrypted and fully readable, without breaking the signature.

The following example shows how to create a JWS token with a simple text payload using previous symmetric key:

```java
// Injected or obtained from a 'Jose' instance
JWSService jwsService = ...

/*
 * {
 *   "header":{
 *     "alg":"HS512",
 *     "cty":"text/plain",
 *     "kid":"octKey"
 *   },
 *   "payload":"This is a simple payload",
 *   "signature":"mwq--Ke2Om3zA2y1F9cQlw5SyFPzhkvwoRaaezbzqifL5joJWuJEddPbtFDKLaBUD9Ufwi6R6IFbbOe-nxkr4w"
 * }
 */
Mono<JWS<String>> jws = jwsService.builder(String.class, octKey)
    .header(header -> header
        .keyId("octKey")
        .algorithm(OCTAlgorithm.HS512.getAlgorithm())
        .contentType(MediaTypes.TEXT_PLAIN)
    )
    .payload("This is a simple payload")
    .build();

// eyJjdHkiOiJ0ZXh0L3BsYWluIiwia2lkIjoib2N0S2V5IiwiYWxnIjoiSFM1MTIifQ.VGhpcyBpcyBhIHNpbXBsZSBwYXlsb2Fk.mwq--Ke2Om3zA2y1F9cQlw5SyFPzhkvwoRaaezbzqifL5joJWuJEddPbtFDKLaBUD9Ufwi6R6IFbbOe-nxkr4w
String jwsCompact = jws.block().toCompact();
```

> The JWS content type must be set in order to determine which media type converters to use to convert the payload. If you don't want to include the content type property (`cty`) in the resulting JWS, the content type can also be specified on the `build()` method. An explicit `Function<T, Mono<String>>` payload encoder can also be specified on the `build()` method in order to bypass media type converters.

The compact representation of the JWS token can then be used to communicate integrity protected data to a recipient sharing the same symmetric key. A JWS token compact representation is parsed and validated as follows:

```java
Mono<JWS<String>> jws = jwsService.reader(String.class, octKey)
    .read(compactJWS);

// Returns "This is a simple payload" or throw a JWSReadException if the token is invalid
jws.block().getPayload();
```

A JSON Web Encryption token (JWE) provides privacy in addition to integrity by encrypting the payload. It is composed of a header which specifies how to decrypt and verify the cipher text, an encrypted key (used for digital signature and encryption), an initialization vector, the cipher text and an authentication tag.

The following example shows how to load an RSA key pair into a JWK, use it to create a JWE token and read its compact representation:

```java
// Injected or obtained from a 'Jose' instance
JWEService jweService = ...

/*
 * From RFC7516 Section A.1:
 * {
 *   "n":"oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw",
 *   "e":"AQAB","d":"kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ",
 *   "p":"1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0",
 *   "q":"wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc",
 *   "dp":"ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE",
 *   "dq":"Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis",
 *   "qi":"VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY",
 *   "kty":"RSA",
 *   "kid":"rsaKey"
 * }
 */
Mono<? extends RSAJWK> rsaKey = jwkService.rsa().builder()
    .keyId("rsaKey")
    .modulus("oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw")
    .publicExponent("AQAB")
    .privateExponent("kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ")
    .firstPrimeFactor("1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0")
    .secondPrimeFactor("wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc")
    .firstFactorExponent("ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE")
    .secondFactorExponent("Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis")
    .firstCoefficient("VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY")
    .build()
    .cache();

/*
 * {
 *   "header":{
 *     "enc":"A256GCM",
 *     "alg":"RSA-OAEP",
 *     "cty":"text/plain",
 *     "kid":"rsaKey"
 *   },
 *   "payload":"This is a simple payload",
 *   "initializationVector":"97ZuhWEQOygN7T3g",
 *   "authenticationTag":"_e-vSUwj5LawcnXROqKvmQ",
 *   "encryptedKey":"VOk1HQDwucfkljliz8RzxvuKXX_B6sTMwZbwKJztZjL0Ga8i3yrRl_4jumBTKBIyWMDdZYxcbHtkzZQhQDFJVpvNcf1QxEryhe3OnFOEF2BGJDPwSYc-AVmAq01gHrUaTF02xvWntfvzu3ePq5vVHl4eiL72POVdoN9w8ck4HaOjeoooYcrkaV8l15cYurXsJ8oo_KQ40SBmKnK99CRrqR1QggPscTpE1QeVj2Z9tw5A3rqYGbCX2d2QwP-zc7w5o1bsuB5qE99i0iAKtMwEdaz6iC97nDry8Vo2uSPf3YviwpzmLbbwJlb_bHhl1aeTZaNQl9JLvxvqCDQehdAx7g",
 *   "cipherText":"YFPMGQXbmI5ZWZXkpH04vEWsBLCmBJ4G"
  * }
 */
Mono<JWE<String>> jwe = jweService.builder(String.class, rsaKey)
    .header(header -> header
        .keyId("rsaKey")
        .algorithm("RSA-OAEP")
        .encryptionAlgorithm("A256GCM")
    )
    .payload("This is a simple payload")
    .build(MediaTypes.TEXT_PLAIN);

// eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAiLCJjdHkiOiJ0ZXh0L3BsYWluIiwia2lkIjoicnNhS2V5In0.EG3dFsn0MAxWRadls1UHpmfNFspczXldNTwr9LfO8BZXsliEJJ8J9-Z25oFnpaI7q3lXazNg06C9upJW2ZiDg2hmmqoCzYD7xdFEz_YkgO7_92tPxCm0XSGZJUtX1d8gpJBoIQWmPCmO6vVveoCds-kmtTQEigokSewKmkIQyOQcyAhLT5y_gkL0JrKLTPjTKGept7dl9uTzuZenWi-5apdVynDhOkraOkCSu8ahVPPPSf5s9aHUS8th-pjWAtS7OFwMOrjLzYXmcqdNPAYM0Pcg88Fw_uI8J7I6tzDInV31rVZ9pDlVarmVSYhS9Rfa91gZaba-onCiFURceUaeOg.im9v2BnFnFp_uGtX.VItnFUA2xtrgrO-Fs-LukV0RZbRUrNkv.eFgbb8i1olfIkSHFM8IkXA
String jweCompact = jwe.block().toCompact();
```

```java
Mono<JWE<String>> jwe = jweService.reader(String.class, rsaKey)
    .read(jweCompact);

// Returns "This is a simple payload" or throw a JWEReadException if the token is invalid
jwe.block().getPayload();
```

In above example, the RSA public key was used to encrypt a generated symmetric key (using RSA-OAEP algorithm) which is used to encrypt the payload (using A256GCM algorithm) and the RSA private key was used to decrypt that encryption key and use it to decrypt and validate the token.

A JSON Web Token (JWT) can be a JWS or a JWE with a JWT Claims Set as payload.

The following example shows how to create and validate a JWT expiring in ten minutes from now using previous symmetric key:

```java
// Injected or obtained from a 'Jose' instance
JWTService jwtService = ...

/*
 * {
 *   "header":{
 *     "typ":"JWT",
 *     "kid":"octKey",
 *     "alg":"HS512"
 *   },
 *   "payload":{
 *     "iss":"john",
 *     "exp":1659346862,
 *     "http://example.com/is_root":true
 *   },
 *   "signature":"hX_m668usLB1DHGW4cD2NJ1UzCs3T6sGCa0ctvGTkresiZ87iIeKnY0-EoIvWmDy3SY69rGLMsbsEjsru1QdZw"
 * }
 */
Mono<JWS<JWTClaimsSet>> jwt = jwtService.jwsBuilder(octKey)
    .header(header -> header
        .keyId("octKey")
        .algorithm("HS512")
        .type("JWT")
    )
    .payload(JWTClaimsSet.of("john", ZonedDateTime.now().plusMinutes(10).toEpochSecond())
        .addCustomClaim("http://example.com/is_root", true)
        .build()
    )
    .build();

// eyJ0eXAiOiJKV1QiLCJraWQiOiJvY3RLZXkiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJqb2huIiwiZXhwIjoxNjU5MzQ2ODYyLCJodHRwOi8vZXhhbXBsZS5jb20vaXNfcm9vdCI6dHJ1ZX0.hX_m668usLB1DHGW4cD2NJ1UzCs3T6sGCa0ctvGTkresiZ87iIeKnY0-EoIvWmDy3SY69rGLMsbsEjsru1QdZw
String jwtCompact = jwt.block().toCompact();
```

```java
Mono<JWS<JWTClaimsSet>> jwt = jwtService.jwsReader(octKey)
    .read(compactJWT);

// Throw a JWSReadException if the signature is invalid or an InvalidJWTException if the JWT Claims set is invalid (e.g. expired, inactive...)
jwt.block().getPayload().ifInvalidThrow();
```

> Note that here we didn't have to specify the content type since a JWT payload is always `application/json`.

## JWK Service

The JWK service is used to build, generate or read JSON Web Keys (JWK) which represent cryptographic keys as specified by [RFC 7517][rfc7517]. A `JWK` is meant to be used to sign or verify the signature part in a JWS, derive, encrypt/decrypt or wrap/unwrap the content encryption key in a JWE or encrypt or decrypt a JWE. It is characterized by a set of properties:

- `kty` (key type) which identifies the cryptographic algorithm family used with the key (e.g. RSA, EC...).
- `use` (public use) which identifies the intended use of the public key (signature or encryption).
- `key_ops` (key operations) which identifies the operations for which the key is intended to be used (e.g. sign, verify, encrypt, decrypt...).
- `alg` (algorithm) which identifies the algorithm intended for use with the key (e.g. HS256).
- `kid` (key id) which identifies the key in issuer and recipient systems.
- `x5u` (X.509 URL) which is a URI pointing to a resource for an X.509 public key certificate or certificate chain (the public key when considering asymmetric JWK).
- `x5c` (X.509 certificate chain) which contains a chain of one or more PKIX certificates (the public key when considering asymmetric JWK).
- `x5t` an `x5t#S256` (X.509 thumbprints) which are Base64 encoded X.509 certificate thumbprint used to uniquely identifies a key (the public key when considering asymmetric JWK).

Depending on the key type and more particularly the cryptographic algorithm family, additional properties may be required (e.g. the name of an elliptic curve, the modulus of an RSA public key...).

A JWK can be symmetrical or asymmetrical composed of a public and private key pair and respectively used in symmetrical (e.g. HMAC, AES...) or asymmetrical (e.g. Elliptic Curve, RSA...) cryptographic algorithms as specified by [RFC 7518][rfc7518]. The specification differentiates three types of algorithms:

- *Digital Signatures and MACs* which are used to digitally sign or create a MAC of a JWS.
- *Key Management* which are used to derive or encrypt/decrypt the Content Encryption Key (CEK) used to encrypt a JWE.
- *Content Encryption* which are used to encrypt and identity-protect a JWE using a CEK.

The `JWK` interface exposes common JWK properties and provides `JWASigner`, `JWAKeyManager` or `JWACipher` instances for any of these cryptographic operations assuming they are supported by the JWK. For instance, an `ECJWK` which supports Elliptic-Curve algorithms cannot be used for content encryption, but it can be used to digitally sign content and decrypt or derive keys, a `JWKProcessingException` shall be thrown when trying to obtain a signer, a key manager or a cipher when the JWK does not support it, when JWK properties are not consistent with the requested algorithm or if the requested algorithm is not of the requested type.

```java
ECJWK ecJWK = jwkService.ec().generator()
    .curve(ECCurve.P_256.getCurve())
    .generate()
    .block();

// Throw a JWKProcessingException since Elliptic-curve algorithms cannot be used to encrypt data
ecJWK.cipher();

// Throw a JWKProcessingException since no algorithm was specified in the JWK
ecJWK.signer();

// Throw a JWKProcessingException since ES512 algorithm is not a key management algorithm
ecJWK.keyManager(ECAlgorithm.ES512.getAlgorithm());

// Throw a JWKProcessingException since ES512 algorithm is not consistent with curve P_256 (P_512 is expected)
ecJWK.signer(ECAlgorithm.ES512.getAlgorithm());

// Return a key manager using ECDH ES algorithm on curve P_256
ecJWK.keyManager(ECAlgorithm.ECDH_ES.getAlgorithm());

OCTJWK octJWK = jwkService.oct().generator()
    .algorithm(OCTAlgorithm.HS512.getAlgorithm())
    .generate()
    .block();

// Throw a JWKProcessingException since HS256 algorithm is requested which is not consistent with HS512 algorithm specified in the JWK
octJWK.signer(OCTAlgorithm.HS256.getAlgorithm());

// Return a signer using HS512 algorithm
octJWK.signer();
```

A `SymmetricJWK` exposes a symmetric secret key whereas an `AsymmetricJWK` exposes a public and private key pair.

A `JWK` can be minified using method `minified()` which returns a `JWK` containing required minimal properties as specified by [RFC 7638][rfc7638]. A JWK thumbprint can be created using method `toJWKThumbprint()` which allows to specify the message digest (defaults to SHA-256) to use to digest the minified `JWK`. A JWK thumbprint can be used as key id to uniquely identify a `JWK`.

A `JWK` can be converted to a public `JWK` using method `toPublicJWK()` which removes any sensitive properties: in case of a `SymmetricJWK` the secret key value is removed and in case of an `AsymmetricJWK` the private key value and any related information are removed.

> Private `JWK` containing sensitive data shall never be communicated unprotected, most of the time the public representation shall be enough for a recipient to resolve the key to use to verify or decrypt a JWS or a JWE.

A `JWK` can be trusted or untrusted depending on how the key was resolved by the JWK service. For instance, a `JWK` built from an X.509 certificate chain (`x5c` or `x5u`) whose path could not be validated will be considered untrusted. Digital signature or content decryption will eventually fail in JWS and JWE services when using an untrusted key. It is possible to explicitly trust a key using method `trust()` when its authenticity could be determined using external means.

The `JWKService` bean uses `JWKFactory` implementations to generate, build or read JWKs, they are injected into the service when the module is initialized. Standard implementations supporting Elliptic-curve, RSA, Octet, Edward-Curve, extended Elliptic-Curve and PBES2 keys are provided and injected by default as defined by [RFC 7518][rfc7518] and [RFC 8037][rfc8037]. Additional `JWKFactory` implementations can be added when building the module to extend the module's capabilities and support extra signature, encryption or key management algorithms.

Standard built-in factories are directly exposed on the `JWKService` in order to quikly generate or build specific JWK:

```java
// Return the ECJWKFactory
jwkService.ec()...

// Return the RSAJWKFactory
jwkService.rsa()...

// Return the OCTJWKFactory
jwkService.oct()...

// Return the EdECJWKFactory
jwkService.edec()...

// Return the XECJWKFactory
jwkService.xec()...

// Return the PBES2JWKFactory
jwkService.pbes2()...
```

> External factories cannot be exposed explicitly by the `JWKService` interface. When reading or generating a `JWK`, The JWK service basically retains all factories that supports the requested key type and algorithm, including external ones. Multiple JWKs built by different factories might then be returned by `read()` and `generate()` methods.

The `JWKService` interface also exposes methods for reading JWK JSON representations. For instance the following example shows how to resolve and read a JWK Set JSON resource located at a specific URIs as defined by [RFC 7517 Section 5][rfc7517]:

```java
// Return one or more JWKs
Publisher<? extends JWK> read = jwkService.read(URI.create("https://host/jwks.json"));
```

### JWK Factory

A `JWKFactory` allows to generate a `JWK` using a `JWKGenerator`, build a `JWK` using a `JWKBuilder` and read a `JWK` from a JSON representation.

#### Generating JWK

A `JWKGenerator` is used to generate a new `JWK`. Depending on the type (symmetric or asymmetric) this results in the creation of a secret key or a public and private key pair matching the key type and algorithm specified in the generator instance.

For instance, a symmetric octet key can be generated as follows:

```java
JWKService jwkService = ...

OCTJWK mySymmetricKey = jwkService.oct().generator()
    .keyId("mySymmetricKey")
    .algorithm(OCTAlgorithm.HS512.getAlgorithm())
    .keySize(24)
    .generate()
    .block();
```

An asymmetric RSA key pair can be generated as follows:

```java
JWKService jwkService = ...

Mono<? extends RSAJWK> myAsymmetricKey = jwkService.rsa().generator()
    .keyId("myAsymmetricKey")
    .algorithm(RSAAlgorithm.PS256.getAlgorithm())
    .generate()
    .cache();
```

> Note how `cache()` was used to transform the resulting `Mono` into a hot source and prevent generating a new key each time it is being subscribed.

#### Building JWK

A `JWKBuilder` is used to build a `JWK` from a set of properties as defined by [RFC 7517][rfc7517]. A JWK builder does not simply create a `JWK` instance filled with the provided properties, it can also directly resolve the JWK from a `JWKStore` or resolve keys (secret, public or private) using a `JWKKeyResolver` and determines whether the resulting `JWK` is consistent and can be trusted.

> The default `JWKKeyResolver` implementation uses a Java Key Store to resolve keys corresponding to the key id or X.509 thumbprints properties in that order. The Java Key Store location is specified in the module's configuration (`JOSEConfiguration`).

In practice, a `JWK` is resolved as follows:

1. The builder first tries to get a matching `JWK` in the module's `JWKStore` from the key id, the X.509 SHA-1 or the X.509 SHA-256 thumbprints in that order. If a matching `JWK` is found the process stops and the `JWK` returned.
2. If no matching `JWK` was found, it tries to resolve the secret key or the public and private key pair from the key id, X.509 SHA-1 or X.509 SHA-256 thumbprints in that order using the module's `JWKKeyResolver`.
3. X.509 certificates chain (`x5c`), if any, is validated using module's `X509JWKCertPathValidator` and corresponding public key value is extracted.
4. X.509 certificates chain URI (`x5u`), if any, is resolved using module's `JWKURLResolver` and validated using module's `X509JWKCertPathValidator` and corresponding public key value is extracted.
5. It then checks that all information are consistent (i.e. specified key values match the ones resolved with the `JWKKeyResolver`, and the ones extracted from X.509 certificates).
6. It finally returns a consistent JWK which is trusted when key values were resolved with the `JWKKeyResolver` (which is assumed to be trusted) or when the X.509 certificate path have been validated (i.e. a certificate in the chain is trusted).

Any issue detected during that process results in a `JWKProcessingException`. X.509 certificates chain resolution as well as certificate path validation are disabled by default (`x5c` and `x5u` are simply ignored) and can be activated by setting properties `resolve_x5u` and `validate_certificate` to `true` in the module's configuration (`JOSEConfiguration`).

> Automatic resolution of X.509 certificates URI can be dangerous and might be considered as a threat which is why this is disabled by default.

The following example shows how to build an `RSAJWK` with a public and private key pair by specifying each property:

```java
RSAJWK rsaKey = jwkService.rsa().builder()
    .keyId("rsaKey")
    .modulus("oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw")
    .publicExponent("AQAB")
    .privateExponent("kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ")
    .firstPrimeFactor("1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0")
    .secondPrimeFactor("wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc")
    .firstFactorExponent("ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE")
    .secondFactorExponent("Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis")
    .firstCoefficient("VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY")
    .build()
    .block();
```

If we assumed that `rsaKey` is not stored in the module's `JWKStore` and that public and private keys are also not stored in the module's Java Key Store, the resulting `RSAKey` is therefore untrusted since the provided information could not be authenticated.

An untrusted `JWK` cannot be used to digitally sign, encrypt or derive keys. If we know by external means that the provided information can be trusted after all, we can explicitly trust the `JWK` as follows:

```java
rsaKey.trust();

// The JWK is now trusted
...
```

> Note that this can be considered unsafe and should be used with extra care.

Now if we assume that `rsaKey` is stored in the module's `JWKStore`, the key can be built, or in that case simply loaded, as follows:

```java
RSAJWK rsaKey = jwkService.rsa().builder()
    .keyId("rsaKey")
    .build()
    .block();
```

In that case, the returned `JWK` is trusted as it comes from a trusted `JWKStore`.

Finally, if the `rsaKey` is not stored in the module's `JWKStore`, but a public and private key pair is stored in the module's Java Key Store, the `JWK` can be loaded in the exact same way:

```java
RSAJWK rsaKey = jwkService.rsa().builder()
    .keyId("rsaKey")
    .build()
    .block();
```

There is however a noticeable difference between the two, when a `JWK` is resolved from the module's `JWKStore`, properties specified in the builder other than the key id or X.509 thumbprints are simply ignored and no further consistency check is performed. On the other hand, when keys are resolved using the module's `JWKKeyResolver`, the properties specified in the builder must be consistent. The purpose of the `JWKStore` is to optimize the resolution of frequently used keys which is incompatible with systematic consistency check.

Please refer to [JWK Store](#jwk-store) and [JWK Key Resolution](#jwk-key-resolution) to better understand how JWK and key resolution work.

#### Reading JWK

A `JWK` is read from a JSON representation in a similar way as the one described for the JWK builder. The JSON object is basically parsed in a map of properties which are then injected in a `JWKBuilder` which is used to build the resulting `JWK`.

The following example shows how to parse the JSON representation of the `RSAJWK` built in previous section:

```java
String rsaJwkJSON = "{\n"
    + "   \"n\":\"oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUWcJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3Spsk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2asbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMStPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2djYgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw\",\n"
    + "   \"e\":\"AQAB\",\"d\":\"kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5NWV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD93Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghkqDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vlt3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSndVTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ\",\n"
    + "   \"p\":\"1r52Xk46c-LsfB5P442p7atdPUrxQSy4mti_tZI3Mgf2EuFVbUoDBvaRQ-SWxkbkmoEzL7JXroSBjSrK3YIQgYdMgyAEPTPjXv_hI2_1eTSPVZfzL0lffNn03IXqWF5MDFuoUYE0hzb2vhrlN_rKrbfDIwUbTrjjgieRbwC6Cl0\",\n"
    + "   \"q\":\"wLb35x7hmQWZsWJmB_vle87ihgZ19S8lBEROLIsZG4ayZVe9Hi9gDVCOBmUDdaDYVTSNx_8Fyw1YYa9XGrGnDew00J28cRUoeBB_jKI1oma0Orv1T9aXIWxKwd4gvxFImOWr3QRL9KEBRzk2RatUBnmDZJTIAfwTs0g68UZHvtc\",\n"
    + "   \"dp\":\"ZK-YwE7diUh0qR1tR7w8WHtolDx3MZ_OTowiFvgfeQ3SiresXjm9gZ5KLhMXvo-uz-KUJWDxS5pFQ_M0evdo1dKiRTjVw_x4NyqyXPM5nULPkcpU827rnpZzAJKpdhWAgqrXGKAECQH0Xt4taznjnd_zVpAmZZq60WPMBMfKcuE\",\n"
    + "   \"dq\":\"Dq0gfgJ1DdFGXiLvQEZnuKEN0UUmsJBxkjydc3j4ZYdBiMRAy86x0vHCjywcMlYYg4yoC4YZa9hNVcsjqA3FeiL19rk8g6Qn29Tt0cj8qqyFpz9vNDBUfCAiJVeESOjJDZPYHdHY8v1b-o-Z2X5tvLx-TCekf7oxyeKDUqKWjis\",\n"
    + "   \"qi\":\"VIMpMYbPf47dT1w_zDUXfPimsSegnMOA1zTaX7aGk_8urY6R8-ZW1FxU7AlWAyLWybqq6t16VFd7hQd0y6flUK4SlOydB61gwanOsXGOAOv82cHq0E3eL4HrtZkUuKvnPrMnsUUFlfUdybVzxyjz9JF_XyaY14ardLSjf4L_FNY\",\n"
    + "   \"kty\":\"RSA\",\n"
    + "   \"kid\":\"rsaKey\"\n"
    + "}";

RSAJWK rsaKey = jwkService.rsa().read(rsaJwkJSON).block();
```

The same rules as the ones described for the JWK builder apply. In above code the resulting `JWK` is untrusted. Assuming a `rsaKey` JWK is stored in the module's `JWKStore`, the following code shall return a workable `JWK`:

```java
String rsaJwkJSON = "{\n"
    + "   \"kid\":\"rsaKey\"\n"
    + "}";

RSAJWK rsaKey = jwkService.rsa().read(rsaJwkJSON).block();
```

> Note that we did not have to specify the key type here since we are directly using the `RSAJWKFactory` to read the JSON representation. We could have invoked the `read()` method on the `JWKService` instead but the key type would then have been required in order to determine which JWK factory to use.

### JWK Store

The *security-jose* module uses a `JWKStore` to store and load frequently used keys. By default, the module uses a no-op implementation but more effective implementations can be injected when creating the module.

The purpose of the `JWKStore` is to optimize key resolution when loading keys while creating or reading JWS or JWE. As soon as a key is matched by a key id, an X.509 SHA-1 or X.509 SHA-256 thumbprint, the key shall be returned and no further processing performed, including consistency checks.

> Note that this actually goes a bit against [RFC 7517][rfc7517] for which inconsistent JWK must be rejected but this is a fair optimization as the returned `JWK` shall always be consistent.

The `JWKStore` interface exposes methods `getByKeyId()`, `getBy509CertificateSHA1Thumbprint()` and `getByX509CertificateSHA256Thumbprint()` which are respectively used by `JWKBuilder` implementation to resolve `JWK` by key id, X.509 SHA-1 and X.509 SHA-256 thumbprints. The `set()` and `remove()` methods are used to add or remove `JWK` instances.

The `InMemoryJWKStore` is a simple implementation that stores keys in concurrent hash maps, the following wrapper bean can be defined in a module to override the default no-op implementation:

```java
@Wrapper
@Bean
public class JWKStoreWrapper implements Supplier<JWKStore> {

    @Override
    public JWKStore get() {
        return new InMemoryJWKStore();
    }
}
```

Or it can be injected directly in the module's builder if the module is created and initialized explicitly:

```java
Jose jose = new Jose.Builder(List.of(jsonConverter, textConverter)).setJwkStore(new InMemoryJWKStore()).build();

jose.start();
...
jose.stop();
```

The `JWKStore` is exposed in the `JWKService`, a `JWK` can be stored as follows:

```java
jwkService.oct().generator()
    .keyId("octKey")
    .algorithm(OCTAlgorithm.HS512.getAlgorithm())
    .generate()
    .map(JWK::trust)
    .flatMap(jwkService.store()::set)
    .block();
```

Since keys resolved from the `JWKStore` are usually used when validating or decrypting JWS or JWE, they should all be trusted to avoid errors.

> The `InMemoryJWKStore` is a basic implementation that does not check this condition before storing an instance but more advanced implementations should definitely consider rejecting untrusted keys. Whatever the solution, processing will eventually fail when using an untrusted key.

### JWK Key resolution

When building or reading a `JWK`, actual keys (secret, public and private) can be resolved by key id, X.509 SHA-1 or X.509 SHA-256 thumbprints in a `JWKBuilder` implementation using the module's `JWKKeyResolver`.

The module provides a default implementation that look up keys in a Java Key Store whose location is specified in the module's configuration. Key resolution will be disabled if the key store configuration is missing.

Let's assume we have a Java Key Store `keystore.jks` accessible with password `password`, the following configuration allows the default `JWKKeyResolver` implementation to resolve keys from that key store:

```plaintext
# configuration.cprops
io.inverno.example.app_jose.appConfiguration {
    jose {
        key_store = "file:/path/to/keytstore.jks"
        key_store_password = "password"
    }
}
```

Unlike the `JWKStore`, a `JWKProcessingException` is thrown when resolved keys are not consistent with the properties specified in the JWK builder.

Custom `JWKKeyResolver` implementation can be provided to override the default behaviour by defining a bean in the module or by directly injecting the instance in the module's builder when the module is created and initialized explicitly:

```java
Jose jose = new Jose.Builder(List.of(jsonConverter, textConverter)).setJwkKeyResolver(new CustomJWKKeyResolver()).build();
```

### JWK Set resolution

The `JWKService` can be used to resolve multiple keys from a URI pointing to a JWK Set resource as defined by [RFC 7517 Section 5][rfc7517].

For instance, the keys defined in a JWK Set at location `https://server.example.com/keys.jwks` can be resolved as follows:

```java
Publisher<? extends JWK> read = jwkService.read(URI.create("https://server.example.com/keys.jwks"));
```

The `JWKService` delegates to the module's `JWKURLResolver` to resolve the resource as a map of properties, the default implementation uses a `ResourceService` which must be injected into the module for the feature to be activated.

> A complete `ResourceService` implementation supporting common URI schemes (`file:/`, `http://`, `classpath:`...) is provided in the *boot* module.

JWK set resolution is also used as a last resort to resolve keys when building or reading JWS or JWE with property `jku`, this behaviour is disabled by default and must be activated explicitly in the module's configuration (`JOSEConfiguration`) by setting `resolve_jku` property to `true`:

```
# configuration.cprops
io.inverno.example.app_jose.appConfiguration {
    jose {
        resolve_jku = true
    }
}
```

> Automatic resolution of JWK Set URL can be dangerous and might be considered as a threat which is why this is disabled by default.

`JWK` instances obtained that way from external JWK Set resources are considered untrusted by default, and therefore cannot be used to build or read JWS or JWE, unless locations (i.e. URIs) are explicitly as a trusted listed in the module's  configuration (`JOSEConfiguration`) in `trusted_jku` property.

For instance, the following configuration can be set to trust keys resolved from `https://server.example.com/keys.jwks`:

```
# configuration.cprops
io.inverno.example.app_jose.appConfiguration {
    jose {
        trusted_jku = "https://server.example.com/keys.jwks"
    }
}
```

### Certificate path validation

When building or reading a `JWK` with an X.509 certificates chain or X.509 certificates chain URI, it is possible to validate the certificates chain in order to determine whether the resulting `JWK` can be trusted.

An X.509 certificate is considered trusted if any of the certificate in the chain is trusted. An `X509JWKCertPathValidator` is used in `JWKBuilder` implementations to validate resolved certificates chains.

The default implementation uses a PKIX `CertPathValidator` with `PKIXParameters` defining the trusted certificates, these parameters are provided by the `JWKPKIXParameters` wrapper bean which uses the trust store of the JDK by default. This bean is overridable and custom `PKIXParameters` can be provided as well by defining a bean in the module or by directly injecting the instance in the module's builder when the module is created and initialized explicitly:

```java
CertStore customTrustStore = ...

Jose jose = new Jose.Builder(List.of(jsonConverter, textConverter)).setJwkPKIXParameters(new JWKPKIXParameters(customTrustStore).get()).build();
```

Certificate path resolution is disabled by default and must be activated explicitly in the module's configuration (`JOSEConfiguration`) by setting `validate_certificate` property to `true`:

```
# configuration.cprops
io.inverno.example.app_jose.appConfiguration {
    jose {
        validate_certificate = true
    }
}
```

### JSON Web Algorithms

The *security-jose* module fully supports algorithms specified in [RFC 7518 JSON Web Algorithm (JWA)][rfc7518], [RFC 8037][rfc8037] and [RFC 8812][rfc8812] and used to sign/verify, encrypt/decrypt and derive content encryption keys. They are grouped into categories with associated `JWK` implementations and `JWAAlgorithm` enum listing the algorithms and defining the parameters required to create corresponding `JWASigner`, `JWACipher` and `JWAKeyManager`.

The `JWA` interface is the base type extended by all JWA algorithms including `JWASigner` for digital signature algorithms, `JWACipher` for encryption algorithms and `JWAKeyManager` for key management algorithms.

The `JWASigner` interface exposes methods `sign()` and `verify()` used to respectively sign and verify some arbitrary data.

```java
byte[] payload = "This is a payload".getBytes();

JWASigner signer = ...

byte[] signature = signer.sign(payload);

if(signer.verify(payload, signature)) {
    ...
}
```

The `JWACipher` interface exposes methods `encrypt()` and `decrypt()` to respectively encrypt and decrypt some arbitrary data. Encryption requires additional authentication data and a `SecureRandom` for random number generation and returns encrypted data composed of a cipher text, an initialization vector and an authentication tag. Decryption requires the additional authentication data, the cipher text, the initialization vector and the authentication tag (which are basically the components of a JWE).

```java
byte[] payload = "This is a payload".getBytes();
// Specified in RFC 7516
byte[] aad = ...

JWACipher cipher = ...

JWACipher.EncryptedData encryptedData = cipher.encrypt(payload, aad);

byte[] decryptedPayload = cipher.decrypt(encryptedData.getCipherText(), aad, encryptedData.getInitializationVector(), encryptedData.getAuthenticationTag());
```

Key management algorithms are used to determine the Content Encryption Key (CEK) used to encrypt a JWE, they are further divided into `DirectJWAKeyManager` for algorithms that derives the content encryption key which is not encrypted, `EncryptingJWAKeyManager` for algorithms that encrypt/decrypt the content encryption key and `WrappingJWAKeyManager` for algorithms that wrap/unwrap the content encryption key.

Key management algorithm usually requires specific parameters passed in the JOSE header, as a result methods exposed by key managers usually require the algorithm and a map of parameters.

A `DirectJWAKeyManager` is used to derive the CEK on both ends using parameters specified in a JOSE header.

```java
// e.g. Ephemeral public key (epk), Agreement PartyUInfo (apu), Agreement PartyVInfo (apv) when using ECDH-ES algorithm
Map<String, Object> parameters = ...

DirectJWAKeyManager directKeyManager = ...

DirectJWAKeyManager.DirectCEK directCEK = directKeyManager.deriveCEK("ECDH-ES", parameters);
OCTJWK cek = directCEK.getEncryptionKey();
```

> When using a direct key management algorithm, the encrypted key part of the JWE is empty since the CEK is derived and not encrypted or wrapped.

An `EncryptingJWAKeyManager` is used to encrypt and decrypt the CEK.

```java
// e.g. PBES2 Salt Input (p2s), PBES2 Count (p2c) when using PBES2-HS256+A128KW algorithm
Map<String, Object> parameters = ...
// Generated when building a JWE
JWK cek = ...

EncryptingJWAKeyManager encryptingKeyManager = ...

EncryptingJWAKeyManager.EncryptedCEK encryptedCEK = encryptingKeyManager.encryptCEK(cek, parameters);
byte[] encryptedKey = encryptedCEK.getEncryptedKey();

JWK decryptedCEK = encryptingKeyManager.decryptCEK(encryptedKey, "PBES2-HS256+A128KW", parameters);
```

A `WrappingJWAKeyManager` is used to wrap and unwrap the CEK.

```java
Map<String, Object> parameters = ...
// Generated when building a JWE
JWK cek = ...

WrappingJWAKeyManager wrappingKeyManager = ...

WrappingJWAKeyManager.WrappedCEK wrappedCEK = wrappingKeyManager.wrapCEK(cek, parameters);
byte[] wrappedKey = wrappedCEK.getWrappedKey();

JWK unwrappedCEK = wrappingKeyManager.unwrapCEK(wrappedKey, "A192KW", parameters);
```

> Although signers, ciphers and key managers are usually used indirectly when building or reading JWS or JWE, but they can also be used directly as shown above.

#### Octet

Octet algorithms are based on a shared secret key, they are listed in the `OCTAlgorithm` enum.

The following example shows how to obtain an A128GCM `JWACipher` from a generated `OCTJWK`:

```java
JWACipher cipher = jwkService.oct().generator()
    .algorithm(OCTAlgorithm.A128GCM.getAlgorithm())
    .generate()
    .block()
    .cipher();
```

#### Elliptic Curve

Elliptic-curve algorithms are based on a public and private key pair and using a specific Elliptic curve (P-256, P-384, P-521 defined in `ECCurve` enum), they are listed in the `ECAlgorithm` enum.

Elliptic-curve cryptography has the advantage of producing smaller signatures than RSA for the same level of protection.

The following example shows how to obtain an ES384 `JWASigner` from a generated `ECJWK` using default P-256 curve:

```java
JWAsigner signer = jwkService.ec().generator()
    .algorithm(ECAlgorithm.ES384.getAlgorithm())
    .generate()
    .block()
    .signer();
```

#### RSA

RSA algorithms are based on a public and private key pair, they are listed in the `RSAAlgorithm` enum.

The following example shows how to obtain an RSA_OAEP `JWAKeyManager` (`EncryptingJWAKeyManager`) from a generated `RSAWK`:

```java
JWAKeyManager keyManager = jwkService.rsa().generator()
    .algorithm(RSAAlgorithm.RSA_OAEP.getAlgorithm())
    .generate()
    .block()
    .keyManager();
```

#### PBES2

PBES2 algorithms are based on a shared secret key, namely a password, they are listed in the `PBES2Algorithm` enum.

They are usually used for the password-based encryption of the CEK in a JWE.

The following example shows how to obtain a PBES2-HS256+A128KW `JWAKeyManager` (`EncryptingJWAKeyManager`) from a generated `PBES2JWK`:

```java
JWAKeyManager keyManager = jwkService.pbes2().generator()
    .algorithm(PBES2Algorithm.PBES2_HS256_A128KW.getAlgorithm())
    .length(32) // generate a 32 characters long password
    .generate()
    .block()
    .keyManager();
```

#### Edward-Curve

Edward-curve algorithms are based on a public and private key pair and using a specific Edward-curve (Ed25519, Ed448, X25519, X448 defined in `OKPCurve`), they are listed in the `EdECalgorithm` enum.

The following example shows how to obtain an Ed25519 `JWASigner` from a generated `EdECJWK`:

```java
JWAsigner signer = jwkService.edec().generator()
    .algorithm(EdECAlgorithm.EDDSA_ED25519.getAlgorithm())
    .generate()
    .block()
    .signer();
```

#### Extended Elliptic Curve

Extended elliptic-curve algorithms are based on a public and private key pair, they are listed in the `XECAlgorithm` enum.

These algorithms basically combine ECDH_ES algorithms with elliptic-curve algorithms to wrap the CEK in a JWE.

The following example shows how to obtain an ECDH-ES+A128KW `JWAKeyManager` (`WrappingJWAKeyManager`) from a generated `XECJWK`:

̀```java
jwkService.xec().generator()
    .algorithm(XECAlgorithm.ECDH_ES_A128KW.getAlgorithm())
    .curve(OKPCurve.X25519.getCurve())
    .generate()
    .block()
    .keyManager();
̀```

## JWS Service

The JWS service is used to build or read JWS represented using the compact or the JSON notation as defined by [RFC 7515][rfc7515].

The `JWSService` bean is used to create `JWSBuilder` or `JsonJWSBuilder` instances to build JWS using the compact or the JSON notation and `JWSReader` or `JsonJWSReader` instances to read JWS serialized using the compact or JSON notation.

A JWS allows to communicate integrity protected content using digital signatures or message authentication codes (MACs). It is composed of three parts:

- a JOSE header which specifies how to understand (i.e. type, content type...), sign or verify the JWS.
- a payload which is digitally signed in the JWS.
- a signature which is essentially the digital signature of the concatenation of the header and the payload.

A `JWS` is obtained from a `JWSBuilder` or a `JWSReader`, the `JWS` interface exposes the header, the payload and the signature. It can be serialized using the compact notation as follows:

```java
JWS<?> jws = ...

// <header>.<payload>.<signature>
// e.g. eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
String jwsCompact = jws.toCompact();
```

A `JsonJWS` is obtained from a `JsonJWSBuilder` or a `JsonJWSReader`, the `JsonJWS` interface exposes the payload and the list of signatures. It can be serialized using the JSON notation as follows:

```java
JsonJWS<?, ?> jsonJWS = ...

/*
 * RFC 7515 Appendix A.6
 *
 * {
 *   "payload": "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ",
 *   "signatures": [
 *     {
 *       "protected":"eyJhbGciOiJSUzI1NiJ9",
 *       "header": {
 *         "kid":"2010-12-29"
 *       },
 *       "signature": "cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZ
 *                     mh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjb
 *                     KBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHl
 *                     b1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZES
 *                     c6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AX
 *                     LIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw"
 *     },
 *     {
 *       "protected":"eyJhbGciOiJFUzI1NiJ9",
 *       "header": {
 *         "kid":"e9bc097a-ce51-4036-9562-d2ade882db0d"
 *       },
 *       "signature": "DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8IS
 *                     lSApmWQxfKTUJqPP3-Kg6NU1Q"
 *     }
 *   ]
 * }
 */
String jwsJson = jsonJWS.toJson();
```

The detached compact representation as specified by [RFC 7797][rfc7797] is also supported and can be used when large payloads communicated by external means are considered.

```java
JWS<?> jws = ...

// <header>..<signature>
// e.g. eyJhbGciOiJFUzUxMiJ9..AdwMgeerwtHoh-l192l60hp9wAHZFVJbLfD_UxMi70cwnZOYaRI1bKPWROc-mZZqwqT2SI-KGDKB34XO0aw_7XdtAG8GaSwFKdCAPZgoXD2YBJZCPEX3xKpRwcdOO8KpEHwJjyqOgzDO7iKvU8vcnwNrmxYbSW9ERBXukOXolLzeO_Jn
String jwsDetachedCompact = jws.toDetachedCompact();
```

> The most common representation is by far the compact representation which can be safely used in URLs. On the other hand, the JSON notation can be used to target multiple systems with various JWKs.

A JWS offers integrity protection of its content using a digital signature, as a result, building or reading a JWS requires a `JWK` supporting digital signature algorithms.

### Building JWS

A `JWSBuilder` is used to create `JWS`, it is obtained by invoking one of the `builder()` methods on the `JWSService` bean. The actual payload type can be specified explicitly in the method as well as the `JWK` to use to digitally sign the `JWS`.

The `builder()` method actually accepts a publisher of `JWK` which means multiple keys can be considered when building the JWS. If keys are not specified, they are resolved from the JOSE header parameters using the [JWK service](#jwk-service). When building a JWS, the `JWSBuilder` basically retains the first trusted `JWK` that was able to sign the JWS. The retained `JWK` is exposed in the resulting `JWSHeader`. It is important to note that untrusted `JWK` are filtered out. A `JOSEObjectBuildException` is thrown if no suitable keys could be found.

A `JWSbuilder` uses media type converters injected in the module to encode the JWS payload based on the content type which can be either specified in the JOSE header (`cty`), or when invoking the `build()` method. An explicit `Function<T, Mono<String>>` encoder can also be specified in order to bypass media type converters.

> A specific encoder basically overrides the content type specified in `build()` method which overrides the content type specified in the JOSE header.

The digital signature is computed by applying a signature algorithm to the JWS signing input composed of the JWS header and the serialized payload.

The following example shows how to build a `JWS` with a generated `JWK` and a payload serialized as `application/json` using corresponding media type converter:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWSService jwsService = ...

Mono<? extends OCTJWK> key = jwkService.oct().generator()
    .keyId("keyId")
    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
    .generate()
    .cache();

JWS<Message> jws = jwsService.builder(Message.class, key)
    .header(header -> header
        .keyId("keyId")
        .algorithm(OCTAlgorithm.HS256.getAlgorithm())
    )
    .payload(new Message("John", "Hello world!"))
    .build(MediaTypes.APPLICATION_JSON)
    .block();

// eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIn0.eyJhdXRob3IiOiJKb2huIiwibWVzc2FnZSI6IkhlbGxvIHdvcmxkISJ9.aSRmKH3ZiTGm2MrKBLqBJH-d-rBEt5bWPY6TEC15B7s
String jwsCompact = jws.toCompact();
```

Assuming the `JWK` can be resolved by the `JWKService` using the key id (from module's `JWKStore` or `JWKKeyResolver`), the key can be omitted when creating the builder:

```java
// Using an 'InMemoryJWKStore', we can store the key so it can be resolved by key id by the 'JWKService'
key.map(JWK::trust).map(jwkService.store()::set).block();

// Key 'keyId' is then automatically resolved
JWS<Message> jws = jwsService.builder(Message.class)
    ...
```

The JWS JSON representation as defined by [RFC 7515 Section 7.2][rfc7515] is a JWS representation that is neither optimized nor URL-safe. This notation can hardly be compared to the compact notation, and it shall be used for very different purposes, for instance to communicate digitally signed or MACed content in JSON using different keys and algorithms to one or more recipients.

A `JsonJWSBuilder` is used to create `JsonJWS` with multiple signatures following the JSON representation specification, it is obtained by invoking one of the `jsonBuilder()` methods on the `JWSService` bean. Since a `JsonJWS` might have multiple signatures using different keys and algorithms, only the payload type can be specified when creating the builder, keys will be provided or resolved later in the process.

A `JsonJWS` is created in a similar way as for a `JWS` with one payload but multiple JOSE headers to create multiple signatures. The JOSE header is then divided into an unprotected header and a protected headers which, unlike the unprotected header, is included in the digital signature. Protected and unprotected headers must be disjoint and content related parameters such as the type (`typ`) or the content type (`cty`) must be consistent across all signature headers. Some sensitive parameters such as the algorithm (`alg`) must also be integrity protected and therefore specified exclusively in the protected header. A `JWSBuildException` shall be thrown in case of invalid or inconsistent signature headers. Keys must be provided explicitly or resolved automatically for each signature to be able to compute the digital signature.

The following example shows how to build a `JsonJWS` with two signatures using generated keys and a payload encoded using an explicit encoder:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWSService jwsService = ...

Mono<? extends OCTJWK> key1 = jwkService.oct().generator()
    .keyId("key1")
    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
    .generate()
    .cache();

Mono<? extends RSAJWK> key2 = jwkService.rsa().generator()
    .keyId("key2")
    .algorithm(RSAAlgorithm.RS256.getAlgorithm())
    .generate()
    .cache();

JsonJWS<Message, BuiltSignature<Message>> jsonJWS = jwsService.jsonBuilder(Message.class)
    .signature(
        protectedHeader -> protectedHeader
            .keyId("key1")
            .algorithm(OCTAlgorithm.HS256.getAlgorithm()),
        unprotectedHeader -> {},
        key1
    )
    .signature(
        protectedHeader -> protectedHeader
            .keyId("key2")
            .algorithm(RSAAlgorithm.RS256.getAlgorithm()),
        unprotectedHeader -> {},
        key2
    )
    .payload(new Message("Alice", "Hi John!"))
    .build(message -> Mono.just(message.getAuthor() + " > " + message.getMessage()))
    .block();

/*
 * {
 *   "signatures": [
 *     {
 *       "header": {
 *         "kid": "key1",
 *         "cty": "text/plain"
 *       },
 *       "signature": "u38wYs0v1M-zgw0lr2Gw3PKRALPxWH6I4wfpLFF_E3I",
 *       "protected": "eyJhbGciOiJIUzI1NiJ9"
 *     },
 *     {
 *       "header": {
 *         "kid": "key2",
 *         "cty": "text/plain"
 *       },
 *       "signature": "X6J77kf7sXW_7j7tLvgwJR2hy2kvDjuEGdT-1WU_Po2Z0sMPvHJd9LRdgYWUCn10V6
 *                     xgNatDQuwEnegOrIOVTI2yN6_T74rQY1-VWO8kESg_MyGRoieC3s6beQAt0JdWKgSs
 *                     xNZjCbRLTu_bxTpIl90j2MgPNHiL8ox2uDwA3pg-6cgEzswMQx6x_KQ-e3VPuqdiSd
 *                     6PNeFNiYN-s9xBTlN_m-0k8MDHSzQ612Ms3Q1ox2gONdpVG3wcoIPX63zaRmt-a3r6
 *                     KReL9bPBs1hCRHxp6ermxwJRf0yjKfo2KH2fWV_wMiPsCdbJSlIL3MPreR0yi5iVDu
 *                     iXK-yWoJ2XOg",
 *       "protected": "eyJhbGciOiJSUzI1NiJ9"
 *     }
 *   ],
 *   "payload": "QWxpY2UgPiBIaSBKb2huIQ"
 * }
 */
String jwsJson = jsonJWS.toJson();
```

In above code, we can see that the payload is common to all signatures which explains why content related parameters must be consistent across all signatures and to make this clear the content type was specified in the common unprotected header. Each resulting unprotected headers then contain the key id and the JWS content type whereas protected headers, encoded in Base64, contain the algorithms that were used to digitally sign the JWS.

The `JsonJWS` interface exposes the payload as well as the `JWS` instances corresponding to each signature.

```java
Message message = jsonJWS.getPayload();

List<JWS> jwsSignatures = jsonJWS.getSignatures().stream()
    .map(signature -> signature.getJWS())
    .collect(Collectors.toList());
```

> Note that the `JWS` instances thus obtained are deduced from the JSON representation which makes a difference between protected and unprotected headers, as a result the actual header used in the signature corresponds to the protected header but the `JWSHeader` exposed in the `JWS` results from the merge of the protected and unprotected headers.

### Reading JWS

A `JWSReader` is used to read JWS compact representations, it is obtained by invoking one of the `reader()` methods on the `JWSService` bean. The expected payload type must be specified explicitly in the method and the `JWK` to use to verify the JWS signature can be specified as well.

As for the `JWSBuilder`, a `JWSReader` can consider multiple keys to verify a JWS signature. If keys are not specified, they are resolved from the JOSE header parameters using the [JWK service](#jwk-service). When reading a `JWS`, the `JWSReader` basically uses provided or resolved trusted `JWK` in sequence to verify the signature and stops when the signature could be verified. As for the `JWSBuilder`, untrusted `JWK` are filtered out and a `JOSEObjectReadException` is thrown if no suitable keys could be found. A `JWSReadException` with aggregated errors (`getSuppressed()`) is thrown when reading an invalid JWS.

A `JWSReader` also uses media type converters injected in the module to decode the JWS payload based on the JWS content type defined in the JOSE header (`cty`) or explicitly specified when invoking the `read()` method. An explicit `Function<String, Mono<T>>` decoder can also be specified in order to bypass media type converters.

> A specific decoder basically overrides the content type specified in `read()` method which overrides the content type in the JOSE header.

The following example shows how to read a JWS compact representation by decoding the `application/json` payload as specified in the JOSE header using the corresponding media type converter:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWSService jwsService = ...

Mono<? extends OCTJWK> key = jwkService.oct().builder()
    .keyId("keyId")
    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
    .keyValue("xqf1haCsSJGuueZivcq4YafdWw6n5CH2BTT6vDwUSaM")
    .build()
    .cache();

String jwsCompact = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIiwiY3R5IjoiYXBwbGljYXRpb24vanNvbiJ9."
    + "eyJhdXRob3IiOiJCaWxsIiwibWVzc2FnZSI6IkhleSEifQ."
    + "pNS2tZmB20ezMA-twecOhobDk3H5AgWyh-m5eV5xE14";

JWS<Message> jws = jwsService.reader(Message.class, key)
    .read(jwsCompact)
    .block();

// Bill says Hey!
Message message = jws.getPayload();
```

Assuming the `JWK` can be resolved by the `JWKService` using the key id (from module's `JWKStore` or `JWKKeyResolver`), the key can be omitted when creating the reader:

```java
// Using an 'InMemoryJWKStore', we can store the key so it can be resolved by key id by the 'JWKService'
key.map(JWK::trust).map(jwkService.store()::set).block();

// Key 'keyId' is then automatically resolved
JWS<Message> jws = jwsService.reader(Message.class)
    ...
```

A `JsonJWSReader` is used to read JWS JSON representations as defined by [RFC 7515 Section 7.2][rfc7515], it is obtained by invoking one of the `jsonReader()` methods on the `JWSService` bean. Since a `JsonJWS` might have multiple signatures using different keys and algorithms, only the payload type must be specified when creating the reader. A `JsonJWS` is basically read without verifying signatures which must later be verified individually, keys can then be specified explicitly or automatically resolved. A `JsonJWS` can be considered valid if one signature could be verified.

> The `JsonJWS` instance returned by a `JsonJWSReader` actually differs from the one returned by a `JsonJWSBuilder`, a built `JsonJWS` exposes `JsonJWS.BuiltSignature` which exposes a valid `JWS` whereas a read `JsonJWS` exposes `JsonJWS.ReadSignature` which exposes `readJWS()` methods to actually verify the signature and return the corresponding `JWS`.

The following example shows how to read and verify a JWS JSON representation with two signatures, the payload being decoded using an explicit decoder:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWSService jwsService = ...

Mono<? extends ECJWK> key2 = jwkService.ec().builder()
    .keyId("key2")
    .algorithm(ECAlgorithm.ES256.getAlgorithm())
    .curve(ECCurve.P_256.getCurve())
    .xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
    .yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
    .eccPrivateKey("jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI")
    .build()
    .cache();

String jwsJson = "{"
    + "  \"signatures\": ["
    + "    {"
    + "      \"header\": {"
    + "        \"cty\": \"text/plain\","
    + "        \"kid\": \"key1\""
    + "      },"
    + "      \"signature\": \"PxhpMkmTep5obGFZv50OsRGA-e7-fxhUmWdUyLC74ms\","
    + "      \"protected\": \"eyJhbGciOiJIUzI1NiJ9\""
    + "    },"
    + "    {"
    + "      \"header\": {"
    + "        \"cty\": \"text/plain\","
    + "        \"kid\": \"key2\""
    + "      },"
    + "      \"signature\": \"KqjGSxiBD5GhwFhLs8H_RBg8nXsKtp4nj5PsdxCzd0ZqMed874ZAxTgnyd0KmQEZwmYvvgM-o8NC9VdIWalMvw\","
    + "      \"protected\": \"eyJhbGciOiJFUzI1NiJ9\""
    + "    }"
    + "  ],"
    + "  \"payload\": \"TGluZGEgPiBTaGFsbCB3ZSBiZWdpbj8\""
    + "}";

JsonJWS<Message, ReadSignature<Message>> jsonJWS = jwsService.jsonReader(Message.class)
    .read(jwsJson, p ->
        Mono.fromSupplier(() -> {
            int separatorIndex = p.indexOf(">");
            return new Message(p.substring(0, separatorIndex - 1), p.substring(separatorIndex + 2));
        })
    )
    .block();

// Return as soon as one of the signatures could have been verified with key2
JWS<Message> verifiedJWS = Flux.fromIterable(jsonJWS.getSignatures())
    .flatMap(signature -> signature.readJWS(key2).onErrorResume(e -> Mono.empty()))
    .blockFirst();

if(verifiedJWS != null) {
    // Linda says Shall we begin?
    Message message = verifiedJWS.getPayload();
}
```

In above code, the verified `JWS` should correspond to the second signature since we used `key2` to verify the `JsonJWS` signatures.

As defined by [RFC 7515][rfc7515], custom parameters listed in the critical header parameter (`crit`) and present in the JOSE header must be fully understood by the application for the JWS to be valid. The parameters actually processed by and application and therefore understood can be specified on the `JWSReader` which throws a `JOSEObjectReadException` when encountering unknown critical parameters.

In the following example, the `JWSReader` is set up to understand custom parameter `http://example.com/application_parameter` which allows it to read the specified JWS:

```java
Mono<? extends OCTJWK> key = jwkService.oct().builder()
    .keyId("keyId")
    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
    .keyValue("xqf1haCsSJGuueZivcq4YafdWw6n5CH2BTT6vDwUSaM")
    .build()
    .cache();

/*
 * {
 *   "header": {
 *     "alg": "HS256",
 *     "kid": "keyId",
 *     "crit": [
 *       "http://example.com/application_parameter"
 *     ],
 *     "http://example.com/application_parameter": true
 *   },
 *   "payload": "Lorem ipsum",
 *   "signature": "aQMWohoxZWOcpYVm04FBJwGc7fBO4xzUKVJz9qfjpxc"
 * }
 */
String jwsCompact = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIiwiY3JpdCI6WyJodHRwOi8vZXhhbXBsZS5jb20vYXBwbGljYXRpb25fcGFyYW1ldGVyIl0sImh0dHA6Ly9leGFtcGxlLmNvbS9hcHBsaWNhdGlvbl9wYXJhbWV0ZXIiOnRydWV9."
    + "TG9yZW0gaXBzdW0."
    + "aQMWohoxZWOcpYVm04FBJwGc7fBO4xzUKVJz9qfjpxc";

JWS<String> jws = jwsService.reader(String.class, key)
    .processedParameters("http://example.com/application_parameter")
    .read(jwsCompact, MediaTypes.TEXT_PLAIN)
    .block();
```

## JWE Service

The JWE service is used to build or read JWE represented using the compact or the JSON notation as defined by [RFC 7516][rfc7516].

The `JWEService` bean is used to create `JWEBuilder` or `JsonJWEBuilder` instances to build JWE using the compact or the JSON notation and `JWEReader` or `JsonJWEReader` instances to read JWE serialized using the compact or JSON notation.

A JWE allows to communicate encrypted content using cryptographic algorithms that guarantees both integrity and confidentiality. It is composed of five parts:

- a JOSE header which specifies how to understand (i.e. type, content type...), encrypt or decrypt the JWE content.
- an encrypted key which corresponds to the content encryption key used to encrypt the JWE content.
- an initialization vector used when encrypting the JWE content.
- a cipher text which results from the authenticated encryption of the JWE content.
- an authentication tag which ensures the integrity of the cipher text.

A `JWE` is obtained from a `JWEBuilder` or a `JWEReader`, the `JWE` interface exposes the header, the encrypted key, the initialization vector, the cipher text, the authentication tag and the payload. It can be serialized using the compact notation as follows:

```java
JWE<?> jwe = ...

// <header>.<encrypted_key>.<initialization_vector>.<cipher_text>.<authentication_tag>
// e.g. eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ.AxY8DCtDaGlsbGljb3RoZQ.KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY.U0m_YmjN04DJvceFICbCVQ
String jweCompact = jwe.toCompact();
```

A `JsonJWE` is obtained from a `JsonJWEBuilder` or a `JsonJWEReader`, the `JsonJWE` interface exposes protected and unprotected headers, the initialization vector, the additional authentication data, the cipher text, the authentication tag and the list of recipients. It can be serialized using the JSON notation as follows:

```java
JsonJWE<?, ?> jsonJWE = ...

/*
 * RFC 7516 Appendix A.4
 *
 * {
 *   "protected": "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0",
 *   "unprotected": {
 *     "jku": "https://server.example.com/keys.jwks"
 *   },
 *   "recipients": [
 *     {
 *       "header": {
 *         "alg":"RSA1_5",
 *         "kid":"2011-04-29"
 *       },
 *       "encrypted_key": "UGhIOguC7IuEvf_NPVaXsGMoLOmwvc1GyqlIKOK1nN94nHPoltGRhWhw7Zx0-
 *                         kFm1NJn8LE9XShH59_i8J0PH5ZZyNfGy2xGdULU7sHNF6Gp2vPLgNZ__deLKx
 *                         GHZ7PcHALUzoOegEI-8E66jX2E4zyJKx-YxzZIItRzC5hlRirb6Y5Cl_p-ko3
 *                         YvkkysZIFNPccxRU7qve1WYPxqbb2Yw8kZqa2rMWI5ng8OtvzlV7elprCbuPh
 *                         cCdZ6XDP0_F8rkXds2vE4X-ncOIM8hAYHHi29NX0mcKiRaD0-D-ljQTP-cFPg
 *                         wCp6X-nZZd9OHBv-B3oWh2TbqmScqXMR4gp_A"
 *     },
 *     {
 *       "header": {
 *         "alg":"A128KW",
 *         "kid":"7"
 *       },
 *       "encrypted_key": "6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ"
 *     }
 *   ],
 *   "iv": "AxY8DCtDaGlsbGljb3RoZQ",
 *   "ciphertext": "KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY",
 *   "tag": "Mz-VPPyU4RlcuYv1IwIvzw"
 * }
 */
String jweJson = jsonJWE.toJson();
```

> The most common representation is by far the compact representation which can be safely used in URLs. On the other hand, the JSON notation can be used to target multiple systems with various JWKs.

A JWE offers integrity and confidentiality of its content using authenticated encryption, it requires two algorithms:

- the algorithm (`alg`) used to encrypt/decrypt, wrap/unwrap or derive a content encryption key (CEK)
- the encryption algorithm used to actually encrypt/decrypt the content using the CEK.

The CEK is either generated or derived when building a JWE and resolved when reading a JWE using a key management algorithm. As a result, building or reading a JWE requires a `JWK` supporting key management algorithms.

### Building JWE

A `JWEBuilder` is used to create `JWE`, it is obtained by invoking one of the `builder()` methods on the `JWEService` bean. The actual payload type can be specified explicitly in the method as well as the `JWK` to use to encrypt, wrap or derive the content encryption key used to encrypt the `JWE`.

The `builder()` method actually accepts a publisher of `JWK` which means multiple keys can be considered when building the JWE. If keys are not specified, they are resolved from the JOSE header parameters using the [JWK service](#jwk-service). When building a JWE, the `JWEBuilder` basically retains the first trusted `JWK` that was able to encrypt the content encryption key. The retained `JWK` is exposed in the resulting `JWEHeader`. It is important to note that untrusted `JWK` are filtered out. A `JOSEObjectBuildException` is thrown if no suitable keys could be found.

A `JWEbuilder` uses media type converters injected in the module to encode the JWE payload based on the content type which can be either specified in the JOSE header (`cty`), or when invoking the `build()` method. An explicit `Function<T, Mono<String>>` encoder can also be specified in order to bypass media type converters.

> A specific encoder basically overrides the content type specified in `build()` method which overrides the content type specified in the JOSE header.

The JWE content are encrypted using a generated content encryption key (CEK) or directly using the provided or resolved `JWK` in case of direct encryption (i.e. `alg=dir`). The CEK (if any) is encrypted, wrapped or derived using the provided or resolved `JWK` and included in the resulting JWE with the initialization vector that was generated and used during the authenticated encryption and the resulting authentication tag so that a recipient has all the information required to decrypt the CEK and eventually verify and decrypt the JWE.

The following example shows how to build a `JWE` with a generated `JWK` and a payload serialized as `application/json` using corresponding media type converter:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWEService jweService = ...

Mono<? extends OCTJWK> key = jwkService.oct().generator()
    .keyId("keyId")
    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
    .generate()
    .cache();

JWE<Message> jwe = jweService.builder(Message.class, key)
    .header(header -> header
        .keyId("keyId")
        .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
        .encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
    )
    .payload(new Message("John", "Hello world!"))
    .build(MediaTypes.APPLICATION_JSON)
    .block();

// eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoia2V5SWQiLCJ0YWciOiJ3b2RKcDJSbThPOEdGWG9PUWZvaTdnIiwiaXYiOiJpYmFfakkzSDRyWUdfcUQtIn0.Barv9ju_JgIBugTD3TtKGA6OyqadZ635rkw6rfpeR7s.QH1HhZKhKWrPzJtfSLRjUQ.gUXtGvVzvwopFh0ZgUlZGB2zOdsFjUG0u2Rih_JNsryDIAkpD_LMDDNYTh2ZRgm1.EgQt9XxCfFDRho5mPAXQRQ
String jweCompact = jwe.toCompact();
```

Assuming the `JWK` can be resolved by the `JWKService` using the key id (from module's `JWKStore` or `JWKKeyResolver`), the key can be omitted when creating the builder:

```java
// Using an 'InMemoryJWKStore', we can store the key so it can be resolved by key id by the 'JWKService'
key.map(JWK::trust).map(jwkService.store()::set).block();

// Key 'keyId' is then automatically resolved
JWE<Message> jwe = jweService.builder(Message.class)
    ...
```

The JWE JSON representation as defined by [RFC 7516 Section 7.2][rfc7516] is a JWE representation that is neither optimized nor URL-safe. This notation can hardly be compared to the compact notation, and it shall be used for very different purposes, for instance to communicate encrypted content in JSON using different keys and algorithms to one or more recipients.

A `JsonJWEBuilder` is used to create `JsonJWE` with multiple recipients following the JSON representation specification, it is obtained by invoking one of the `jsonBuilder()` methods on the `JWEService` bean. Since a `JsonJWE` might have multiple recipients with different encrypted content using different keys and algorithms, only the payload type can be specified when creating the builder, keys will be provided or resolved later in the process.

A `JsonJWE` is created from common protected and unprotected headers, one payload and multiple recipients with unprotected headers used to encrypt the JWE using different keys. Unlike unprotected headers, the common protected header is included in the additional authentication data used during the authenticated encryption of the JWE. Common headers and per recipient header must be disjoint and content related parameters such as the type (`typ`) or the content type (`cty`) must be consistent across all recipient headers. A `JWEBuildException` shall be thrown in case of invalid or inconsistent recipient headers. The encryption algorithm parameter (`enc`) must also be consistent across all recipients since the cipher text, the initialization vector, the authentication tag and the content encryption key used to encrypt the JWE are common to all recipients (the JWE is actually encrypted once), it is however encrypted, wrapped or derived per recipient using different keys explicitly provided or automatically resolved for each recipient. In case of a direct encryption or direct key agreement algorithm, the algorithm parameter (`alg`) must also be consistent across all recipients.

> In the particular case of a direct encryption, a `JsonJWE` is really not different from a regular JWE since all recipients have then to share the same encryption key.

The following example shows how to build a `JsonJWE` with two recipients using generated keys and a payload encoded as `text/plain` using an explicit encoder:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWEService jweService = ...

Mono<? extends RSAJWK> key1 = jwkService.rsa().generator()
    .keyId("key1")
    .algorithm(RSAAlgorithm.RSA1_5.getAlgorithm())
    .generate()
    .cache();

Mono<? extends OCTJWK> key2 = jwkService.oct().generator()
    .keyId("key2")
    .algorithm(OCTAlgorithm.A128KW.getAlgorithm())
    .generate()
    .cache();

JsonJWE<Message, BuiltRecipient<Message>> jsonJWE = jweService.jsonBuilder(Message.class)
    .headers(
        protectedHeader -> protectedHeader
            .encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm()),
        unprotectedHeader -> {}
    )
    .payload(new Message("Alice", "Hi John!"))
    .recipient(
        header -> header
            .keyId("key1")
            .algorithm(RSAAlgorithm.RSA1_5.getAlgorithm()),
        key1
    )
    .recipient(
        header -> header
            .keyId("key2")
            .algorithm(OCTAlgorithm.A128KW.getAlgorithm()),
        key2
    )
    .build(message -> Mono.just(message.getAuthor() + " > " + message.getMessage()))
    .block();

/*
 * {
 *   "unprotected": {
 *   },
 *   "ciphertext": "n8hpXBhxZ9brlm465Ipey9kpCHyOxDfR-qNzRh32KQM",
 *   "recipients": [
 *     {
 *       "header": {
 *         "alg": "RSA1_5",
 *         "kid": "key1"
 *       },
 *       "encrypted_key": "ItwxvAJqMh_kGeJ9jmHPm1NJ1Kod-TmAwsm5IbZDy54uB6U1eGQZKQzzLTMGMM
 *                         UUf6G96kT35Vv__L2fr6k8INlGOi3ae5YDnRmVwOpD74pffQn3FFcoxx68_xSu
 *                         DWDHMRbyEqHFur-DZy2O-yb0Odna7qg7kmAz0wv9VSOHpfRWj8wB4w7g4zg4jI
 *                         5IztiTX587fCtw7YuiBYnNEUzCrddUoBAAphWHiilez25lvOdhjvyyMNAT-j_5
 *                         8FDIQGgqUY0uLE48-gKF2alnrIkjk_9H9Cg_99mBEyls5EAnRq3aGiJz7wPJR3
 *                         1Qtl54c8IUDLtqNXKaB8qsk5taYV5hlQ"
 *     },
 *     {
 *       "header": {
 *         "alg": "A128KW",
 *         "kid": "key2"
 *       },
 *       "encrypted_key": "srvZC3EPaEYkfHkTp21-mzBHA17gjuof6-NTWdg7unHsPK1rnp1eFQ"
 *     }
 *   ],
 *   "iv": "bBb7jcsxoRcPpKahEPCvwA",
 *   "tag": "u7dD-MwLkfA4SfuRjvVmdQ",
 *   "protected": "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0"
 * }
 */
String jweJson = jsonJWE.toJson();
```

In above code, we can see that the cipher text is common to all recipients which explains why content related parameters and the encryption algorithm must be consistent across all recipients and to make this clear the encryption algorithm was specified in the common protected header, encoded in Base64. Unprotected headers in each recipient then specify the key id and the algorithm to use to resolve the content encryption key in order to decrypt the JWE.

The `JsonJWE` interface exposes the common protected and unprotected headers, the cipher text, the initialization vector, the additional authentication data and the authentication tag as well as the `JWE` instances corresponding to each recipient.

```java
List<JWE<Message>> jweRecipients = jsonJWE.getRecipients().stream()
    .map(recipient -> recipient.getJWE())
    .collect(Collectors.toList());
```

> Note that the `JWE` instances thus obtained are deduced from the JSON representation which makes a difference between protected and unprotected headers, as a result the actual header used in the additional authentication data corresponds to the protected header but the `JWEHeader` exposed in the `JWE` results from the merge of the common protected and unprotected headers and the recipient unprotected header.

### Reading JWE

A `JWEReader` is used to read JWE compact representations, it is obtained by invoking one of the `reader()` methods on the `JWEService` bean. The expected payload type must be specified explicitly in the method and the `JWK` to use to decrypt, unwrap or derive the content encryption key, actually used to decrypt the `JWE`, can be specified as well.

As for the `JWEBuilder`, a `JWEReader` can consider multiple keys to decrypt, unwrap or derive the content encryption key used to encrypt the JWE. If keys are not specified, they are resolved from the JOSE header parameters using the [JWK service](#jwk-service). When reading a `JWE`, the `JWEReader` basically uses provided or resolved trusted `JWK` in sequence to resolve the content encryption key and stops when the CEK could be resolved. As for the `JWEBuilder`, untrusted `JWK` are filtered out and a `JOSEObjectReadException` is thrown if no suitable keys could be found. A `JWEReadException` with aggregated errors (`getSuppressed()`) is thrown when reading an invalid JWE.

A `JWEReader` also uses media type converters injected in the module to decode the JWE payload based on the JWE content type defined in the JOSE header (`cty`) or explicitly specified when invoking the `read()` method. An explicit `Function<String, Mono<T>>` decoder can also be specified in order to bypass media type converters.

> A specific decoder basically overrides the content type specified in `read()` method which overrides the content type in the JOSE header.

The following example shows how to read a JWE compact representation by decoding the `application/json` payload as specified in the JOSE header using the corresponding media type converter:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWEService jweService = ...

Mono<? extends OCTJWK> key = jwkService.oct().builder()
    .keyId("keyId")
    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
    .keyValue("GkilETj3L4jpinuRiaNq6zd7-_1JPbfU9DY3xHl9HEE")
    .build()
    .cache();

String jweCompact = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoia2V5SWQiLCJjdHkiOiJhcHBsaWNhdGlvbi9qc29uIiwidGFnIjoiYUtlc2VBelZoenh5Vk9pRVNvVEdoQSIsIml2IjoieEJFSTlYeHBDVTZwcVVSaCJ9."
    + "MNYqpQCQPrUSZTwP-C7kUCGOFqFGGciUU2qW54jc3NM."
    + "_nfKSroUwjqzdJcPETt-ow."
    + "1dL8rLmhKF7hqVNzQf5oWPOSZN7Z_V46w0UvIBDuFjH5pqvhbs4ltrTsk6E_NF-y."
    + "RJ8QOGLuT2fz5VrzG1EHbg";

JWE<Message> jwe = jweService.reader(Message.class, key)
    .read(jweCompact)
    .block();

// Bill says Hey!
Message message = jwe.getPayload();
```

Assuming the `JWK` can be resolved by the `JWKService` using the key id (from module's `JWKStore` or `JWKKeyResolver`), the key can be omitted when creating the reader:

```java
// Using an 'InMemoryJWKStore', we can store the key so it can be resolved by key id by the 'JWKService'
key.map(JWK::trust).map(jwkService.store()::set).block();

// Key 'keyId' is then automatically resolved
JWE<Message> jwe = jweService.reader(Message.class)
    ...
```

A `JsonJWEReader` is used to read JWE JSON representations as defined by [RFC 7516 Section 7.2][rfc7516], it is obtained by invoking one of the `jsonReader()` methods on the `JWEService` bean. Since a `JsonJWE` might have multiple recipients using different keys and algorithms, only the payload type must be specified when creating the reader. A `JsonJWE` is basically read without decrypting the JWE content which must be decrypted for each recipient individually, keys can then be specified explicitly or automatically resolved. A `JsonJWE` can be considered valid if the content could be verified and decrypted for at least one recipient.

> The `JsonJWE` instance returned by a `JsonJWEReader` actually differs from the one returned by a `JsonJWEBuilder`, a built `JsonJWE` exposes `JsonJWE.BuiltRecipient` which exposes a valid `JWE` whereas a read `JsonJWE` exposes `JsonJWE.ReadRecipient` which exposes `readJWE()` methods to actually verify and decrypt the JWE content and return the corresponding `JWE`.

The following example shows how to read and decrypt a JWE JSON representation with two recipients, the payload being decoded using an explicit decoder:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWEService jweService = ...

Mono<? extends OCTJWK> key2 = jwkService.oct().builder()
    .keyValue("GawgguFyGrWKav7AX4VKUg")
    .build()
    .cache();

String jweJson = "{"
    + "  \"unprotected\": {"
    + "  },"
    + "  \"ciphertext\": \"2jtWSZdL-TJGyktUwldH4sphYuz2VbseUS9el_vh_tU\","
    + "  \"recipients\": ["
    + "    {"
    + "      \"header\": {"
    + "        \"alg\": \"RSA1_5\","
    + "        \"kid\": \"key1\""
    + "      },"
    + "      \"encrypted_key\": \"kIHuM-OZU1wvmb6ocdDsn1ljF11kIbfvv9y7XpTPGfdYeaz2AhJvpHfPZ6LKk5-yDfHAVWTXz_RbgjPATURNKyu0hdogfWBWXEpQEk8WaBafI8kSk0GzhJrR2tcXhrxs0xWPMthjfZ38zNql1oZuL9pzUZ3PicNhcCXD2XN52kw7VGMvPus8r89orY4q2l_xA65wkxHtG3JDG9Je_CidYuX_PXHqMkrbszsUPbyCspPIRTP5yWMeFmMp8KiEnyGaQITt0vZuea4u3tWuhX0Wa2AN74qesuArMhx81NWxaMnuDNrF6eQFIQw4QJ41MqVchHRAoXYKQvB8DYce9fHhPQ\""
    + "    },"
    + "    {"
    + "      \"header\": {"
    + "        \"alg\": \"A128KW\","
    + "        \"kid\": \"key2\""
    + "      },"
    + "      \"encrypted_key\": \"OSMIf3Elx-NmfzP1Y_aZbae6k6yU2rl7o2uHd7v3lHgS4UjJURVYTQ\""
    + "    }"
    + "  ],"
    + "  \"iv\": \"vrCX8Yr9oOs--KiBtkQ6kw\","
    + "  \"tag\": \"gHpLPXRRDjUNJ1HDivaSTg\","
    + "  \"protected\": \"eyJlbmMiOiJBMTI4Q0JDLUhTMjU2In0\""
    + "}";

JsonJWE<Message, ReadRecipient<Message>> readJsonJWE = jweService.jsonReader(Message.class)
    .read(jweJson, payload ->
        Mono.fromSupplier(() -> {
            int separatorIndex = payload.indexOf(">");
            return new Message(payload.substring(0, separatorIndex - 1), payload.substring(separatorIndex + 2));
        })
    )
    .block();

// Return as soon as one of the recipients could have been verified and decrypted with key2
JWE<Message> decryptedJWE = Flux.fromIterable(readJsonJWE.getRecipients())
    .flatMap(recipient -> recipient.readJWE(key2).onErrorResume(e -> Mono.empty()))
    .blockFirst();

if(decryptedJWE != null) {
    // Linda says Shall we begin?
    Message message = decryptedJWE.getPayload();
}
```

In above code, the decrypted `JWE` should correspond to the second recipient since we used `key2` to resolve the content encryption key.

As defined by [RFC 7516][rfc7516], custom parameters listed in the critical header parameter (`crit`) and present in the JOSE header must be fully understood by the application for the JWE to be valid. The parameters actually processed by and application and therefore understood can be specified on the `JWEReader` which throws a `JOSEObjectReadException` when encountering unknown critical parameters.

In the following example, the `JWEReader` is set up to understand custom parameter `http://example.com/application_parameter` which allows it to read the specified JWE:

```java
Mono<? extends OCTJWK> key = jwkService.oct().builder()
    .keyId("keyId")
    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
    .keyValue("GkilETj3L4jpinuRiaNq6zd7-_1JPbfU9DY3xHl9HEE")
    .build()
    .cache();

/*
* {
*   "header": {
*     "enc": "A128CBC-HS256",
*     "alg": "A256GCMKW",
*     "kid": "keyId",
*     "crit": [
*       "http://example.com/application_parameter"
*     ],
*     "http://example.com/application_parameter": true,
*     "tag": "pq1OChvU6GZcMDLZqTEo0Q",
*     "iv": "VcuwU871tvGMGOHB"
*   },
*   "payload": "Lorem ipsum",
*   "initializationVector": "i1GTQ9xyOL89vza7hNCiAQ",
*   "authenticationTag": "5EiKTUS272wTHd978QOuHQ",
*   "encryptedKey": "Aq7NWm_h4LmGjJynbUYOO7O9juKlUMFWXS_HMpAAR1g",
*   "cipherText": "mDeuwt3QO199_h6diPwu_w"
* }
*/
String jweCompact = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoia2V5SWQiLCJjcml0IjpbImh0dHA6Ly9leGFtcGxlLmNvbS9hcHBsaWNhdGlvbl9wYXJhbWV0ZXIiXSwiaHR0cDovL2V4YW1wbGUuY29tL2FwcGxpY2F0aW9uX3BhcmFtZXRlciI6dHJ1ZSwidGFnIjoicHExT0NodlU2R1pjTURMWnFURW8wUSIsIml2IjoiVmN1d1U4NzF0dkdNR09IQiJ9."
    + "Aq7NWm_h4LmGjJynbUYOO7O9juKlUMFWXS_HMpAAR1g."
    + "i1GTQ9xyOL89vza7hNCiAQ."
    + "mDeuwt3QO199_h6diPwu_w."
    + "5EiKTUS272wTHd978QOuHQ";

JWE<String> jwe = jweService.reader(String.class, key)
    .processedParameters("http://example.com/application_parameter")
    .read(jweCompact, MediaTypes.TEXT_PLAIN)
    .block();
```

## JWT Service

The JWT service is used to build or read JWT represented using a URL-safe compact notation as defined by [RFC 7519][rfc7519]. A JSON Web Token is a particular type of JWS or JWE that is used to securely transfer claims between two parties.

In practice, a JWT is created or read just like a `JWS` or a `JWE` with type `JWT` and a JSON payload of type `JWTClaimsSet` representing a set of claims.

The `JWTService` bean is used to create specific `JWSBuilder` or `JWEBuilder` instances for building JWT as `JWS` or `JWE` and specific `JWSReader` or `JWEReader` instances for reading `JWS` and `JWE` with `JWTClaimsSet` payloads serialized using the compact notation.

### JWT claims set

A JWT claims set represents a JSON object whose members are the claims conveyed by the JWT as defined by [RFC 7519][rfc7519] which also specifies registered claim names. For instance, the issuer (`iss`) claim identifies the principal that issued the JWT, the expiration time claim (`exp`) identifies the expiration time on or after which the JWT must not be accepted for processing... A JWT is therefore validated by first verifying or decrypting the enclosing JWS or JWE and then by validating the JWT claims set, a JWT must be rejected if for instance the expiration time has passed.

The `JWTClaimsSet` interface is used to represent the JWT payload in a JWS or a JWE, it exposes the registered claims and allows to specify custom claims.

The following example shows how to create a `JWTClaimsSet` with an issuer and a custom claim and which expires in a day:

```java
JWTClaimsSet jwtClaimsSet = JWTClaimsSet.of("joe", ZonedDateTime.now().plusDays(1).toEpochSecond())
    .addCustomClaim("http://example.com/is_root", true)
    .build();
```

A `JWTClaimsSet` can be validated in multiple ways:

```java
if(jwtClaimsSet.isValid()) {

}

// Run an action only if the JWT claims set is valid
jwtClaimsSet.ifValid(() -> {
    ...
});

// Run an action the JWT claims set is valid and another action if it is not
jwtClaimsSet.ifValidOrElse(
    () -> {
        // Valid
        ...
    },
    () -> {
        // Invalid
        ...
    }
);

// Throws an InvalidJWTException if the JWT claims set is invalid
jwtClaimsSet.ifInvalidThrow();

// Throws the provided exception if the JWT claims set is invalid
jwtClaimsSet.ifInvalidThrow(() -> new CustomException("Invalid credentials"));
```

A `JWTClaimsSet` validates expiration time and not before claims by default, additional `JWTClaimsSetValidator` can be added as well by invoking `validate()` or `setValidators()` methods.

In the following example, a validator is added to check that the issuer is `iss`, an `InvalidJWTException` is thrown if the issuer claim does not match:

```java
jwtClaimsSet.validate(JWTClaimsSetValidator.issuer("iss"));

// Throws an InvalidJWTException since issuer 'joe' does not match the expected 'iss'
jwtClaimsSet.ifInvalidThrow();
```

> It is then possible to provide custom validation logic using multiple `JWTClaimsSetValidator`, but the `JWTClaimsSet` interface can also be itself extended to better reflect application specificities by exposing application specific claims or specific validation logic.

### Building JWT

The `JWTService` bean exposes `jwsBuilder()` and `jweBuilder()` methods used to obtain specific `JWSBuilder` or `JWEBuilder` for creating JWT as `JWS` or `JWE` with `JWTClaimsSet` payloads. The builders thus obtained follow the exact same rules as defined by the [JWS service](#jws-service) and the [JWE service](#jwe-service) with the following exceptions: the type (`typ`) and the content type (`cty`) are always considered to be `JWT` and `application/json` since the JWT claims set is defined as a JSON object. A `JWTBuildException` is thrown when a type other than `JWT` (the type can be omitted) or a content type (no content type is allowed) are specified in the JOSE header.

The following example shows how to create a JWT as a `JWS` using a generated key:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWTService jwtService = ...

Mono<? extends OCTJWK> key = jwkService.oct().generator()
    .generate()
    .cache();

/*
 * {
 *   "iss":"joe",
 *   "exp":1691133731,
 *   "http://example.com/is_root":true
 * }
 */
JWTClaimsSet claims = JWTClaimsSet.of("joe", ZonedDateTime.now().plusYears(1).toEpochSecond())
    .addCustomClaim("http://example.com/is_root", true)
    .build();

JWS<JWTClaimsSet> jwts = jwtService.jwsBuilder(key)
    .header(header -> header
        .algorithm(OCTAlgorithm.HS256.getAlgorithm())
        .type("JWT")
    )
    .payload(claims)
    .build()
    .block();

// eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLCJleHAiOjE2OTExMzM3NzQsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.4fEhUpbK4aNhgZB0XL_UiJV9k5pLw35MT1zIjq4oCro
String jwtsCompact = jwts.toCompact();
```

The following example shows how to create a JWT as a `JWE` using a generated key:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWTService jwtService = ...

Mono<? extends ECJWK> key = jwkService.ec().generator()
    .keyId("keyId")
    .algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
    .curve(ECCurve.P_256.getCurve())
    .generate()
    .cache();

/*
 * {
 *   "iss":"joe",
 *   "exp":1691133731,
 *   "http://example.com/is_root":true
 * }
 */
JWTClaimsSet claims = JWTClaimsSet.of("joe", ZonedDateTime.now().plusYears(1).toEpochSecond())
    .addCustomClaim("http://example.com/is_root", true)
    .build();

JWE<JWTClaimsSet> jwte = jwtService.jweBuilder(key)
    .header(header -> header
        .algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
        .encryptionAlgorithm(OCTAlgorithm.A256GCM.getAlgorithm())
        .type("JWT")
    )
    .payload(claims)
    .build()
    .block();

// eyJlbmMiOiJBMjU2R0NNIiwidHlwIjoiSldUIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJjcnYiOiJQLTI1NiIsIngiOiIxdVc4VlAxVzhDazZ6dERIMWRjYnk3NzRfVXU4X1RvalNKZEJSMVpRaFRNIiwieSI6InBGRG1KZDJXTS1jZGcxVHdMR0FkaldUSURrRW1xc2lmMWJfV0tKMWRWSnciLCJrdHkiOiJFQyJ9fQ..zhhYytTdGNvPajvU.j-Edyx9DpIdHGrCYiH20cjLKORhw95bXBJSEQPVjDe7wRfYFUvfch43X4HI3fKYSxIWgjIACM3ynqQwu7Ta3cQ.3PDSOt-SdNyCEqYRD8P0hA
String jwteCompact = jwte.toCompact();
```

> By default, the JWT service creates `JWSBuilder` and `JWEBuilder` for building JWT with `JWTClaimsSet` payload type, in order to obtain builders for custom `JWTClaimsSet` types, the type must be explicitly specified when creating the builder.

### Reading JWT

The `JWTService` bean exposes `jwsReader()` and `jweReader()` methods used to obtain specific `JWSReader` or `JWEReader` for reading JWT as `JWS` or `JWE` with `JWTClaimsSet` payloads. The builders thus obtained follow the exact same rules as defined by the [JWS service](#jws-service) and the [JWE service](#jwe-service) with the following exceptions: the type (`typ`) must be `JWT` and no content type (`cty`) is allowed. A `JWTReadException` is thrown when a type other than `JWT` or a content type are specified in the JOSE header.

The following example shows how to read a JWT as a `JWS`:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWTService jwtService = ...

Mono<? extends OCTJWK> key = jwkService.oct().builder()
    .keyId("keyId")
    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
    .keyValue("xqf1haCsSJGuueZivcq4YafdWw6n5CH2BTT6vDwUSaM")
    .build()
    .cache();

String jwtsCompact = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."
    + "eyJpc3MiOiJqb2UiLCJleHAiOjE2OTExMzMyMTMsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ."
    + "4p0_3W8DBrjTpw2e2KI1__v-6QOT_5dWIMKbfsSvTo0";

JWTClaimsSet validClaims = jwtService.jwsReader(key)
    .read(jwtsCompact)
    .map(JWS::getPayload)
    .filter(JWTClaimsSet::isValid)
    .block();
```

The following example shows how to read a JWT as a `JWE`:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWTService jwtService = ...

Mono<? extends ECJWK> key = jwkService.ec().builder()
    .keyId("keyId")
    .algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
    .curve(ECCurve.P_256.getCurve())
    .xCoordinate("a9HrKi7kwXR0EumziK_B5ZRlsk7QbXGPJfx_c3OGoZs")
    .yCoordinate("fixJ3kr2abu0huetFyhs0OMqd3_M6xMIKE8hr3FggOM")
    .eccPrivateKey("VCSeZseVoZ1E4TyWmRqD0nt5I_ipSbKfXcRHQSTPqUw")
    .build()
    .cache();

// The encrypted key is empty since ECDH_ES is a direct key agreement
jwteCompact = "eyJlbmMiOiJBMjU2R0NNIiwidHlwIjoiSldUIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJjcnYiOiJQLTI1NiIsIngiOiJ6bEczQzVwUEtZVG4yVHpiZlJZYm5KOTZTai0yNDJGeTlwVVRmUWN0MUlzIiwieSI6IkUyeE9hNnNlb0dJVHpkRHdxVjZlT2NIc2dzNmI2M082NlJVWXlsV2N6LTgiLCJrdHkiOiJFQyJ9fQ."
        + "."
        + "_1eQRi8ukFZDwa27."
        + "WjPLHYGHu1zpg3QSbhB9ciraoRU7UXpeJJXz76UZAkwJ-rxEXwkimnflTnEymG_oK1i7hKwCANRhqWwr22GqNg."
        + "Zos43NFBxdh_brO1ae-7vA";

JWTClaimsSet validClaims = jwtService.jweReader(key)
    .read(jwteCompact)
    .map(JWE::getPayload)
    .filter(JWTClaimsSet::isValid)
    .block();
```

> By default, the JWT service creates `JWSReader` and `JWEReader` for reading JWT with `JWTClaimsSet` payload type, in order to obtain readers for custom `JWTClaimsSet` types, the type must be explicitly specified when creating the reader.

## JOSE Media Type Converters

The module also exposes a set of `MediaTypeConverter<String>` for converting JOSE media types as defined by [RFC 7515 Section 9.2][rfc7515], [RFC 7517 Section 8.5][rfc7517] and [RFC 7519 Section 10.3][rfc7519]. It currently supports: `application/jose`, `application/jose+json`, `application/jwk+json`, `application/jwk-set+json` and `application/jwt`.

JOSE media type converters are basically used to convert JWK, JWS, JWE or JWT serialized using the compact or the JSON notation. They rely on the module's services to decode an input into corresponding JOSE object (`JWK`, `JWS`, `JWE` or `JWT`), as a result a JWS or a JWE are verified and decrypted by the converters which throw `ConverterException` in case of invalid inputs. In the specific case of a JWT, the validation of the decoded `JWTClaimsSet` is not performed and left to the application.

These media types converters are also used by module services when converting JOSE payloads. It is then possible to wrap any JOSE object in a `JWS` or a `JWE` using compact or JSON serialization. A typical use cases consist in wrapping a `JWK` or a `JWKSet` in a `JWE` to securely communicate keys.

The following example shows how to create a `JWE` conveying multiple `JWK`:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = ...
JWEService jweService = ...

OCTJWK key1 = jwkService.oct().builder()
    .keyId("key1")
    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
    .keyValue("GkilETj3L4jpinuRiaNq6zd7-_1JPbfU9DY3xHl9HEE")
    .build()
    .block();

OCTJWK key2 = jwkService.oct().builder()
    .keyId("key2")
    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
    .keyValue("xqf1haCsSJGuueZivcq4YafdWw6n5CH2BTT6vDwUSaM")
    .build()
    .block();

JWKSet jwkSet = new JWKSet(key1, key2);

Mono<? extends ECJWK> key = jwkService.ec().builder()
    .keyId("keyId")
    .algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
    .curve(ECCurve.P_256.getCurve())
    .xCoordinate("a9HrKi7kwXR0EumziK_B5ZRlsk7QbXGPJfx_c3OGoZs")
    .yCoordinate("fixJ3kr2abu0huetFyhs0OMqd3_M6xMIKE8hr3FggOM")
    .eccPrivateKey("VCSeZseVoZ1E4TyWmRqD0nt5I_ipSbKfXcRHQSTPqUw")
    .build()
    .cache();

JWE<JWKSet> jwe = jweService.builder(JWKSet.class, key)
    .header(header -> header
        .algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
        .encryptionAlgorithm(OCTAlgorithm.A128GCM.getAlgorithm())
        .contentType(MediaTypes.APPLICATION_JWK_SET_JSON)
    )
    .payload(jwkSet)
    .build()
    .block();

// eyJlbmMiOiJBMTI4R0NNIiwiY3R5IjoiYXBwbGljYXRpb24vandrLXNldCtqc29uIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJjcnYiOiJQLTI1NiIsIngiOiJPcW5NbjBKcDNQcGZ6VlFCQW1ZanU2MVEwWUNkUHJuMkI3eW5ZdlRLN3FJIiwieSI6ImhZXzI2am9tS1QzX2QzaGQ2VVNRSm1zSjV5blBtaDN5QmRkZVdHbEs5ZDgiLCJrdHkiOiJFQyJ9fQ..XvpO0GyH44d8GeWc.5aV-epA4DaoWAD84EyYqFnaFv2HtQJlNF33jwSIuxHaMG0nK1Cm6yKcdzzC4e1pG1FNY7wg9SI_JlkFDYqjp6EuMe64vFUOiPCj28QtPaafEx7jOt5nbGNRvzBZJdDWQbhlZomXL7cKzLjfYpv8Y4SWPzcua6FJMSH7DoZwUZfKZDzDk_-2fpXvE_LLw7rTbi8Vltm9AClzmy2QS1tu5R4hY5E9Ew5QIWC06IErtldHF_y_oZIy7iSxf55GjgBVs0roFkA.OujlTScT9qOM6wWsFJMUlA
String jweCompact = jwe.toCompact();
```

The resulting compact JWE containing encrypted keys can then be conveyed to a recipient which can decrypt the keys with the shared secret key.

```java
// Injected or obtained from a 'Jose' instance
JWEService jweService = ...

jweCompact = "eyJlbmMiOiJBMTI4R0NNIiwiY3R5IjoiYXBwbGljYXRpb24vandrLXNldCtqc29uIiwiYWxnIjoiRUNESC1FUyIsImVwayI6eyJjcnYiOiJQLTI1NiIsIngiOiJPcW5NbjBKcDNQcGZ6VlFCQW1ZanU2MVEwWUNkUHJuMkI3eW5ZdlRLN3FJIiwieSI6ImhZXzI2am9tS1QzX2QzaGQ2VVNRSm1zSjV5blBtaDN5QmRkZVdHbEs5ZDgiLCJrdHkiOiJFQyJ9fQ."
    + "."
    + "XvpO0GyH44d8GeWc."
    + "5aV-epA4DaoWAD84EyYqFnaFv2HtQJlNF33jwSIuxHaMG0nK1Cm6yKcdzzC4e1pG1FNY7wg9SI_JlkFDYqjp6EuMe64vFUOiPCj28QtPaafEx7jOt5nbGNRvzBZJdDWQbhlZomXL7cKzLjfYpv8Y4SWPzcua6FJMSH7DoZwUZfKZDzDk_-2fpXvE_LLw7rTbi8Vltm9AClzmy2QS1tu5R4hY5E9Ew5QIWC06IErtldHF_y_oZIy7iSxf55GjgBVs0roFkA."
    + "OujlTScT9qOM6wWsFJMUlA";

jwkSet = jweService.reader(JWKSet.class, key)
    .read(jweCompact)
    .map(JWE::getPayload)
    .block();
```

The following example shows how to wrap a received `JWS` in a `JWE` in order to add confidentiality protection:

```java
// Injected or obtained from a 'Jose' instance
JWKService jwkService = null;
JWSService jwsService = null;
JWEService jweService = null;

jwkService.oct().builder()
    .keyId("jwsKey")
    .algorithm(OCTAlgorithm.HS256.getAlgorithm())
    .keyValue("xqf1haCsSJGuueZivcq4YafdWw6n5CH2BTT6vDwUSaM")
    .build()
    .map(JWK::trust)
    .flatMap(jwkService.store()::set)
    .block();

jwkService.oct().builder()
    .keyId("jweKey")
    .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
    .keyValue("GkilETj3L4jpinuRiaNq6zd7-_1JPbfU9DY3xHl9HEE")
    .build()
    .map(JWK::trust)
    .flatMap(jwkService.store()::set)
    .block();

String jwsCompact = "eyJhbGciOiJIUzI1NiIsImtpZCI6Imp3c0tleSIsImN0eSI6ImFwcGxpY2F0aW9uL2pzb24ifQ."
    + "eyJhdXRob3IiOiJNYXJjZWwiLCJtZXNzYWdlIjoiRmluYWxseSEifQ."
    + "wjnBucCNvQXHtL8QBWuXbutRECtIhazISQhR0NfYOQs";

JWS<Message> block = jwsService.reader(Message.class)
    .read(jwsCompact)
    .block();

JWE<JWS<Message>> jwe = jwsService.reader(Message.class)
    .read(jwsCompact)
    .flatMap(jws -> jweService.<JWS<Message>>builder(Types.type(JWS.class).type(Message.class).and().build())
        .header(header -> header
            .keyId("jweKey")
            .algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
            .encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
            .contentType(MediaTypes.APPLICATION_JOSE)
        )
        .payload(jws)
        .build()
    )
    .block();

// eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoiandlS2V5IiwiY3R5IjoiYXBwbGljYXRpb24vam9zZSIsInRhZyI6IlBjT2tjZWNsNUswaW92a2hnMEhwUEEiLCJpdiI6InFNMUtfeHhIcmZocXFuRFMifQ.LLn2scpDiAdRRSFIrvTXTXsVwQp9mSH4dPv1I-IruFM.LfCNkDe5r3eE2Kjadmpkww.5AjCbDExRhRsLy-iXX2RAavfXVWFEcinKcXu3t_BObnC4mzgxmaqvfwUC8QMu8KM8C3gjt36Qa89nqajVYmJwRrZ0ZMoH68JgXvp2npIEdJSruL3CqTHm3ObK5-7TbYLP1K3t9v995wOIAajUsXaHfpNODqAsFlc83A6wwxv37WVq4mWy-WZ7ZwIpwHY5semqMxv0FbpNMPtkLaG0JzqYLnzH7yaT2DSBQKIxlCZ0hc.ZML3thQjah7dtXdv17LJXA
String jweCompact = jwe.toCompact();
```

In above example, we choose to store the `jwsKey` and `jweKey` in the module's `JWKStore`, although we could have specified keys explicitly to read the JWS and build the JWE, converters can only rely on key resolution based on the JOSE header parameters and as a result a recipient which would like to decode above compact JWE must make sure keys can be resolved using the `JWKStore`, the `JWKKeyResolver` or the `JWKURLResolver`.

```java
// Injected or obtained from a 'Jose' instance
JWSService jwsService = null;
JWEService jweService = null;

jweCompact = "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiQTI1NkdDTUtXIiwia2lkIjoiandlS2V5IiwiY3R5IjoiYXBwbGljYXRpb24vam9zZSIsInRhZyI6IlBjT2tjZWNsNUswaW92a2hnMEhwUEEiLCJpdiI6InFNMUtfeHhIcmZocXFuRFMifQ."
    + "LLn2scpDiAdRRSFIrvTXTXsVwQp9mSH4dPv1I-IruFM."
    + "LfCNkDe5r3eE2Kjadmpkww."
    + "5AjCbDExRhRsLy-iXX2RAavfXVWFEcinKcXu3t_BObnC4mzgxmaqvfwUC8QMu8KM8C3gjt36Qa89nqajVYmJwRrZ0ZMoH68JgXvp2npIEdJSruL3CqTHm3ObK5-7TbYLP1K3t9v995wOIAajUsXaHfpNODqAsFlc83A6wwxv37WVq4mWy-WZ7ZwIpwHY5semqMxv0FbpNMPtkLaG0JzqYLnzH7yaT2DSBQKIxlCZ0hc."
    + "ZML3thQjah7dtXdv17LJXA";

/// Here we assume keys 'jwsKey' and 'jweKey' can be resolved by the 'JWSService' and the 'JWEService'

// Marcel says Finally!
Message message = jweService.<JWS<Message>>reader(Types.type(JWS.class).type(Message.class).and().build())
    .read(jweCompact)
    .map(JWE::getPayload)
    .map(JWS::getPayload)
    .block();
```
