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

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.BeanSocket;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * <p>
 * PKIX Parameters used to validate X.509 certificate path.
 * </p>
 * 
 * <p>
 * This is an overridable wrapper bean which provides parameters that allows to validate certificates paths against a trust store. It can be overridden by injecting a custom instance when building the
 * JOSE module.
 * </p>
 * 
 * <p>
 * Parameters are built from a trust store containing the trusted root certificates. The default Java trust store is used by default.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Overridable
@Wrapper
@Bean( name= "jwkPKIXParameters", visibility = Bean.Visibility.PRIVATE )
public class JWKPKIXParameters implements Supplier<PKIXParameters> {

	private final CertStore trustStore;
	
	private PKIXParameters instance;

	/**
	 * <p>
	 * Creates JWK PKIX parameters wrapper using the default Java trust store.
	 * </p>
	 */
	@BeanSocket
	public JWKPKIXParameters() {
		this(null);
	}
	
	/**
	 * <p>
	 * Creates JWK PKIX parameters wrapper with the specified trust store.
	 * </p>
	 * 
	 * @param trustStore a trust store.
	 */
	public JWKPKIXParameters(CertStore trustStore) {
		this.trustStore = trustStore != null ? trustStore : this.getDefaultTrustStore();
	}
	
	/**
	 * <p>
	 * Creates and returns the default trust store.
	 * </p>
	 * 
	 * <p>
	 * The default trust store comprises the trusted certificates stored in the Java trust store.
	 * </p>
	 * 
	 * @return the default trust store
	 */
	private CertStore getDefaultTrustStore() {
		try {
			List<X509Certificate> trustedCertificates = new ArrayList<>();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore) null);
			for (TrustManager tm : tmf.getTrustManagers()) {
				if (tm instanceof X509TrustManager) {
					trustedCertificates.addAll(Arrays.asList(((X509TrustManager) tm).getAcceptedIssuers()));
				}
			}
			return CertStore.getInstance("collection", new CollectionCertStoreParameters(trustedCertificates));
		} 
		catch(NoSuchAlgorithmException | KeyStoreException | InvalidAlgorithmParameterException e) {
			throw new RuntimeException("Error initializing JWK trust store", e);
		}
	}
	
	@Override
	public PKIXParameters get() {
		if(this.instance == null) {
			// List trusted certificates
			try {
				this.instance = new PKIXParameters(this.trustStore.getCertificates(new X509CertSelector()).stream().map(trustedCert -> new TrustAnchor((X509Certificate)trustedCert, null)).collect(Collectors.toSet()));

				// let's disable revocation check for now as it makes synchronous calls to retrieve CRL or use OCSP
				// This is actually less secure but faster but more performant as this looks for revocation for all certificates in the chain
				// it is still possible to define untrusted certificates in "lib/security/blocked.certs" properties file, each key is a certificate fingerprint of a certificate we don't want to trust
				// if custom chain validation configuration is required it is still possible to override this bean.
				// PKIXRevocationChecker rc = (PKIXRevocationChecker)cpv.getRevocationChecker();
				// rc.setOptions(EnumSet.of(PKIXRevocationChecker.Option.PREFER_CRLS, PKIXRevocationChecker.Option.NO_FALLBACK, PKIXRevocationChecker.Option.SOFT_FAIL));
				// params.addCertPathChecker(rc);
				this.instance.setRevocationEnabled(false);

				return this.instance;
			} 
			catch(CertStoreException | InvalidAlgorithmParameterException e) {
				throw new RuntimeException("Error initializing certificate path validator parameters", e);
			}
		}
		return this.instance;
	}
}
