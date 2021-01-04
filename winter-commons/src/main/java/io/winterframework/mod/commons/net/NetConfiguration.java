/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.mod.commons.net;

/**
 * @author jkuhn
 *
 */
public interface NetConfiguration {

	default boolean reuse_address() {
		return true;
	};
	
	default boolean reuse_port() {
		return true;
	}
	
	default boolean keep_alive() {
		return false;
	}
	
	default boolean tcp_no_delay() {
		return true;
	}
	
	default boolean tcp_quickack() {
		return false;
	}
	
	default boolean tcp_cork() {
		return false;
	}
	
	default boolean prefer_native_transport() {
		return true;
	}
	
	default int root_event_loop_group_size() {
		return Runtime.getRuntime().availableProcessors();
	}
}
