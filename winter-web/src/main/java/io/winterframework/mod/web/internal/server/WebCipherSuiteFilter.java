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
package io.winterframework.mod.web.internal.server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.netty.handler.ssl.CipherSuiteFilter;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.WebConfiguration;

/**
 * 
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class WebCipherSuiteFilter implements CipherSuiteFilter {

	private WebConfiguration configuration;
	
	public WebCipherSuiteFilter(WebConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String[] filterCipherSuites(Iterable<String> ciphers, List<String> defaultCiphers, Set<String> supportedCiphers) {
		Set<String> filteredCiphers = new HashSet<>();
		for(String cipher : ciphers != null ? ciphers : supportedCiphers) {
			filteredCiphers.add(cipher);
		}
		
		if(configuration.ssl_ciphers_includes() != null) {
			filteredCiphers.retainAll(Arrays.asList(configuration.ssl_ciphers_includes()));
		}
		if(configuration.ssl_ciphers_excludes() != null) {
			filteredCiphers.removeAll(Arrays.asList(configuration.ssl_ciphers_excludes()));
		}
		
		return filteredCiphers.stream().toArray(String[]::new);
	}

}
