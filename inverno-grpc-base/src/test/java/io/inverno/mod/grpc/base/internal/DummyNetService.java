/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.grpc.base.internal;

import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.base.net.NetServerConfiguration;
import io.inverno.mod.base.net.NetService;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import java.net.SocketAddress;

/**
 * <p>
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class DummyNetService implements NetService {

	@Override
	public TransportType getTransportType() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Bootstrap createClient(SocketAddress socketAddress) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Bootstrap createClient(SocketAddress socketAddress, NetClientConfiguration clientConfiguration) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Bootstrap createClient(SocketAddress socketAddress, int nThreads) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Bootstrap createClient(SocketAddress socketAddress, NetClientConfiguration clientConfiguration, int nThreads) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress, NetServerConfiguration serverConfiguration) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress, int nThreads) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress, NetServerConfiguration serverConfiguration, int nThreads) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public ByteBufAllocator getByteBufAllocator() {
		return ByteBufAllocator.DEFAULT;
	}

	@Override
	public ByteBufAllocator getDirectByteBufAllocator() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}
}
