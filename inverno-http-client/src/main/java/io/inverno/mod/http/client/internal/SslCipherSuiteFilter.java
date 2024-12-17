/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.client.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.netty.handler.ssl.CipherSuiteFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Ssl cipher suite filter.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
@Bean(visibility = Visibility.PRIVATE)
public class SslCipherSuiteFilter implements CipherSuiteFilter {

	private final HttpClientConfiguration configuration;
	
	/**
	 * <p>
	 * Creates a SSL cipher suite filter
	 * </p>
	 * 
	 * @param configuration the HTTP client configuration
	 */
	public SslCipherSuiteFilter(HttpClientConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String[] filterCipherSuites(Iterable<String> ciphers, List<String> defaultCiphers, Set<String> supportedCiphers) {
		Set<String> filteredCiphers = new HashSet<>();
		for(String cipher : ciphers != null ? ciphers : supportedCiphers) {
			filteredCiphers.add(cipher);
		}
		
		if(this.configuration.tls_ciphers_includes() != null) {
			filteredCiphers.retainAll(Arrays.stream(configuration.tls_ciphers_includes()).collect(Collectors.toSet()));
		}
		if(this.configuration.tls_ciphers_excludes() != null) {
			filteredCiphers.removeAll(Arrays.stream(configuration.tls_ciphers_excludes()).collect(Collectors.toSet()));
		}
		return filteredCiphers.toArray(String[]::new);
	}
}
