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
package io.inverno.mod.security.jose.jwk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link JWKStore} implementation that stores and read JWK from concurrent maps in memory.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class InMemoryJWKStore implements JWKStore {

	/**
	 * JWKs by key id.
	 */
	private Map<String, JWK> jwksByKid;
	/**
	 * JWKs by X.509 SHA1 thumbprint.
	 */
	private Map<String, JWK> jwksByX5t;
	/**
	 * JWKs by X.509 SHA256 thumbprint.
	 */
	private Map<String, JWK> jwksByX5t_S256;
	/**
	 * JWKs by JWK thumbprint.
	 */
	private Map<String, JWK> jwksByThumbprint;
	
	@Override
	@SuppressWarnings("unchecked")
	public <T extends JWK> Mono<T> getByKeyId(String kid) throws JWKStoreException {
		if(this.jwksByKid != null && kid != null) {
			return Mono.fromSupplier(() -> (T)this.jwksByKid.get(kid));
		}
		return Mono.empty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends JWK> Mono<T> getBy509CertificateSHA1Thumbprint(String x5t) throws JWKStoreException {
		if(this.jwksByX5t != null && x5t != null) {
			return Mono.fromSupplier(() -> (T)this.jwksByX5t.get(x5t));
		}
		return Mono.empty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends JWK> Mono<T> getByX509CertificateSHA256Thumbprint(String x5t_S256) throws JWKStoreException {
		if(this.jwksByX5t_S256 != null && x5t_S256 != null) {
			return Mono.fromSupplier(() -> (T)this.jwksByX5t_S256.get(x5t_S256));
		}
		return Mono.empty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends JWK> Mono<T> getByJWKThumbprint(String thumbprint) throws JWKStoreException {
		if(this.jwksByThumbprint != null && thumbprint != null) {
			return Mono.fromSupplier(() -> (T)this.jwksByThumbprint.get(thumbprint));
		}
		return Mono.empty();
	}

	@Override
	public Mono<Void> set(JWK jwk) throws JWKStoreException {
		return Mono.fromRunnable(() -> {
			if(StringUtils.isNotBlank(jwk.getKeyId())) {
				if(this.jwksByKid == null) {
					this.jwksByKid = new ConcurrentHashMap<>();
				}
				this.jwksByKid.put(jwk.getKeyId(), jwk);
			}
			if(jwk instanceof X509JWK) {
				X509JWK<?, ?> x509JWK = (X509JWK<?, ?>)jwk;
				if(StringUtils.isNotBlank(x509JWK.getX509CertificateSHA1Thumbprint())) {
					if(this.jwksByX5t == null) {
						this.jwksByX5t = new ConcurrentHashMap<>();
					}
					this.jwksByX5t.put(x509JWK.getX509CertificateSHA1Thumbprint(), x509JWK);
				}
				if(StringUtils.isNotBlank(x509JWK.getX509CertificateSHA256Thumbprint())) {
					if(this.jwksByX5t_S256 == null) {
						this.jwksByX5t_S256 = new ConcurrentHashMap<>();
					}
					this.jwksByX5t_S256.put(x509JWK.getX509CertificateSHA256Thumbprint(), x509JWK);
				}
			}
			if(this.jwksByThumbprint == null) {
				this.jwksByThumbprint = new ConcurrentHashMap<>();
			}
			this.jwksByThumbprint.put(jwk.toJWKThumbprint(JWK.DEFAULT_THUMBPRINT_DIGEST), jwk);
		});
	}
	
	@Override
	public Mono<Void> remove(JWK jwk) throws JWKStoreException {
		return Mono.fromRunnable(() -> {
			if(StringUtils.isNotBlank(jwk.getKeyId()) && this.jwksByKid != null) {
				this.jwksByKid.remove(jwk.getKeyId());
			}
			if(jwk instanceof X509JWK) {
				X509JWK<?, ?> x509JWK = (X509JWK<?, ?>)jwk;
				if(StringUtils.isNotBlank(x509JWK.getX509CertificateSHA1Thumbprint()) && this.jwksByX5t != null) {
					this.jwksByX5t.remove(x509JWK.getX509CertificateSHA1Thumbprint());
				}
				if(StringUtils.isNotBlank(x509JWK.getX509CertificateSHA256Thumbprint()) && this.jwksByX5t_S256 != null) {
					this.jwksByX5t_S256.remove(x509JWK.getX509CertificateSHA256Thumbprint());
				}
			}
			if(this.jwksByThumbprint != null) {
				this.jwksByThumbprint.remove(jwk.toJWKThumbprint(JWK.DEFAULT_THUMBPRINT_DIGEST));
			}
		});
	}
}
