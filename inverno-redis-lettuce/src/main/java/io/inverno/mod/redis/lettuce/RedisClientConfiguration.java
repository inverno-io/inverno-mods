/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.lettuce;

import io.inverno.mod.configuration.Configuration;

/**
 *
 * @author jkuhn
 */
@Configuration
public interface RedisClientConfiguration {

	default int event_loop_group_size() {
		return Runtime.getRuntime().availableProcessors();
	}
	
	String uri();
	
	String username();
	
	String password();
	
	default String host() {
		return "localhost";
	}
	
	default int port() {
		return 6379;
	}
	
	default boolean ssl() {
		return false;
	}
	
	default int database() {
		return 0;
	}
	
	String client_name();
	
	default long timeout() {
		return 60000l;
	}
	
	default int pool_max_active() {
		return 8;
	}
	
	default int pool_min_idle() {
		return 0;
	}
	
	default int pool_max_idle() {
		return 8;
	}
}
