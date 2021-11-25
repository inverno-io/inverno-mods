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
package io.inverno.mod.boot.internal;

import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.net.NetService.TransportType;
import io.inverno.mod.boot.BootConfiguration;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;

import java.util.function.Supplier;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.netty.incubator.channel.uring.IOUring;

/**
 * <p>
 * Determines the transport type based on configuration and software/hardware
 * capabilities.
 * </p>
 * 
 * <p>
 * Note that this bean is private, the resulting transport type is exposed
 * within the NetService bean.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
@Wrapper @Bean( name = "transportType", visibility = Visibility.PRIVATE )
public class Transport implements Supplier<TransportType> {

	private final BootConfiguration configuration;
	
	/**
	 * <p>
	 * Creates a transport type wrapper bean.
	 * </p>
	 * 
	 * @param configuration the boot module configuration
	 */
	public Transport(BootConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public TransportType get() {
		if(this.configuration.prefer_native_transport()) {
			if(isKQueueAvailable()) {
				return TransportType.KQUEUE;
			}
			else if(isEpollAvailable()) {
				return TransportType.EPOLL;
			}
			else if(isIo_uringAvailable()) {
				return TransportType.IO_URING;
			}
			else {
				return TransportType.NIO;
			}
		}
		else {
			return TransportType.NIO;
		}
	}
	
	private static boolean isKQueueAvailable() {
		try {
			return KQueue.isAvailable();
		}
		catch(NoClassDefFoundError e) {
			return false;
		}
	}
	
	private static boolean isEpollAvailable() {
		try {
			return Epoll.isAvailable();
		}
		catch(NoClassDefFoundError e) {
			return false;
		}
	}
	
	private static boolean isIo_uringAvailable() {
		try {
			return IOUring.isAvailable();
		}
		catch(NoClassDefFoundError e) {
			return false;
		}
	}

}
