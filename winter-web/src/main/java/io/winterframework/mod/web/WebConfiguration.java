package io.winterframework.mod.web;

import io.winterframework.mod.configuration.Configuration;

@Configuration
public interface WebConfiguration {

	default String server_host() {
		return "localhost";
	}

	default int server_port() {
		return 8443;
	}
	
	default String key_store_type() {
		return "JKS";
	}

	String key_store();

	String key_store_password();

	String key_alias();
	
	String[] ssl_ciphers_includes();
	
	String[] ssl_ciphers_excludes();
}