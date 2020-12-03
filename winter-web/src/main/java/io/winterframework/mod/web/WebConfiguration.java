package io.winterframework.mod.web;

import io.winterframework.mod.configuration.Configuration;

@Configuration
public interface WebConfiguration {

	default String server_host() {
		return "localhost";
	}

	default int server_port() {
		return 8080;
	}
	
	default String key_store_type() {
		return "JKS";
	}

	default int accept_backlog() {
		return 1024;
	}
	
	default boolean tcp_keep_alive() {
		return false;
	}
	
	default boolean tcp_no_delay() {
		return true;
	}
	
	default boolean ssl_enabled() {
		return false;
	}
	
	default boolean h2c_enabled() {
		return false;
	}
	
	String key_store();

	String key_store_password();

	String key_alias();
	
	String[] ssl_ciphers_includes();
	
	String[] ssl_ciphers_excludes();
	
	
	default Long http2_header_table_size() {
		return null;
	}
	
	default Boolean http2_push_enabled() {
		return null;
	}
	
	default Integer http2_max_concurrent_streams() {
		return 100;
	}
	
	default Integer http2_initial_window_size() {
		return null;
	}
	
	default Integer http2_max_frame_size() {
		return null;
	}
	
	default Integer http2_max_header_list_size() {
		return null;
	}
}