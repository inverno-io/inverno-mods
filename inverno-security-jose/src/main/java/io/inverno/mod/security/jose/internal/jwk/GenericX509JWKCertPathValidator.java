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
package io.inverno.mod.security.jose.internal.jwk;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutorService;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.X509JWKCertPathValidator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * Generic X.509 certificates path validator implementation.
 * </p>
 * 
 * <p>
 * This is an overridable bean which can be overriden by injecting a custom {@link X509JWKCertPathValidator} instance when building the JOSE module.
 * </p>
 * 
 * <p>
 * It requires an executor service to be able to execute certificate path validation, which might be blocking, asynchronously.
 * </p>
 * 
 * <p>
 * Certificates path validation can be disabled by configuration (see {@link JOSEConfiguration#validate_certificate()}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Overridable
@Bean( name = "jwkX509CertPathValidator", visibility = Bean.Visibility.PRIVATE )
public class GenericX509JWKCertPathValidator implements @Provide X509JWKCertPathValidator {

	private final JOSEConfiguration configuration;
	private final PKIXParameters pkixParameters;
	private final Scheduler scheduler;

	/**
	 * <p>
	 * Creates an X.509 certificates path validator.
	 * </p>
	 * 
	 * @param configuration the JOSE module configuration
	 * @param pkixParameters PKIX parameters
	 * @param executor an executor service
	 */
	public GenericX509JWKCertPathValidator(JOSEConfiguration configuration, PKIXParameters pkixParameters, ExecutorService executor) {
		this.configuration = configuration;
		this.pkixParameters = pkixParameters;
		this.scheduler = Schedulers.fromExecutor(executor);
		
	}
	
	@Override
	public Mono<X509Certificate> validate(List<X509Certificate> certificates) throws JWKResolveException {
		if(!this.configuration.validate_certificate()) {
			return Mono.just(certificates.get(0));
		}
		return Mono.fromSupplier(() -> {
			try {
				CertificateFactory cf =  CertificateFactory.getInstance("X.509");
				CertPath certChain = cf.generateCertPath(certificates);

				// Let's validate the chain and return the first certificate
				CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
				cpv.validate(certChain, this.pkixParameters);

				return (X509Certificate)certificates.get(0);
			}
			catch(CertificateException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertPathValidatorException e) {
				throw new JWKResolveException("Error validating X.509 certificate: ", e);
			}
		})
		.subscribeOn(this.scheduler);
	}
}
