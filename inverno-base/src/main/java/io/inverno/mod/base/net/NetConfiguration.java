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

package io.inverno.mod.base.net;

/**
 * <p>
 * Base network configuration.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface NetConfiguration {

	/**
	 * <p>
	 * Enables/Disables socket address re-use .
	 * </p>
	 * 
	 * <p>
	 * Defaults to true.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean reuse_address() {
		return true;
	};

	/**
	 * <p>
	 * Enables/Disables socket port re-use.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean reuse_port() {
		return false;
	}

	/**
	 * <p>
	 * Enables/Disables socket keep alive.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean keep_alive() {
		return false;
	}

	/**
	 * <p>
	 * Enables/Disables TCP no delay.
	 * </p>
	 * 
	 * <p>
	 * Defaults to true.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean tcp_no_delay() {
		return true;
	}

	/**
	 * <p>
	 * Enables/Disables TCP quick ack.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean tcp_quickack() {
		return false;
	}

	/**
	 * <p>
	 * Enables/Disables TCP cork.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean tcp_cork() {
		return false;
	}
	
	/**
	 * <p>
	 * The linger-on-close timeout in milliseconds.
	 * </p>
	 * 
	 * @return the linger-on-close timeout or null
	 */
	Integer linger();
	
	/**
	 * <p>
	 * The receive buffer size.
	 * </p>
	 * 
	 * @return the receive buffer size
	 */
	Integer rcv_buffer();
	
	/**
	 * <p>
	 * The send buffer size.
	 * </p>
	 * 
	 * @return the send buffer size
	 */
	Integer snd_buffer();
	
	/**
	 * <p>
	 * The IP type of service.
	 * </p>
	 * 
	 * @return the IP type of service
	 */
	Integer ip_tos();
	
	/**
	 * <p>
	 * The channel idle timeout in milliseconds.
	 * </p>
	 * 
	 * <p>
	 * {@code null} or {@code <= 0} means no timeout.
	 * </p>
	 * 
	 * @return the idle timeout
	 */
	Long idle_timeout();
	
	/**
	 * <p>
	 * The channel read idle timeout in milliseconds.
	 * </p>
	 * 
	 * <p>
	 * {@code null} or {@code <= 0} means no timeout.
	 * </p>
	 * 
	 * @return the read idle timeout
	 */
	Long idle_read_timeout();
	
	/**
	 * <p>
	 * The channel write idle timeout in milliseconds.
	 * </p>
	 * 
	 * <p>
	 * {@code null} or {@code <= 0} means no timeout.
	 * </p>
	 * 
	 * @return the write idle timeout
	 */
	Long idle_write_timeout();
}
