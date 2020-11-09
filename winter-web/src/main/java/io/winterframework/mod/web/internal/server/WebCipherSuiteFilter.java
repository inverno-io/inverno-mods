package io.winterframework.mod.web.internal.server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.netty.handler.ssl.CipherSuiteFilter;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.WebConfiguration;

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
