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
package io.inverno.mod.test.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.11
 */
public class DummyHttpProxyServer {

	private final int port;
	
	private ServerSocket proxyServer;
	private Socket clientSocket;
	private Socket serverSocket;
	
	private boolean clientConnected;

	public DummyHttpProxyServer(int port) {
		this.port = port;
	}

	public boolean isClientConnected() {
		return clientConnected;
	}
	
	public void start() {
		new Thread(new ServerAcceptTask()).start();
	}
	
	public void stop() {
		try {
			if(this.proxyServer != null) {
				this.proxyServer.close();
			}
			if(this.clientSocket != null) {
				this.clientSocket.close();
			}
			if(this.serverSocket != null) {
				this.serverSocket.close();
			}
		}
		catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private class ServerAcceptTask implements Runnable {
		
		@Override
		public void run() {
			try {
				ServerSocket proxyServer = new ServerSocket(DummyHttpProxyServer.this.port);

				DummyHttpProxyServer.this.clientSocket = proxyServer.accept();
				DummyHttpProxyServer.this.clientConnected = true;

				new Thread(new ClientReadTask()).start();
			}
			catch(IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
	
	private class ClientReadTask implements Runnable {
		
		@Override
		public void run() {
			ByteArrayOutputStream connectRequest = new ByteArrayOutputStream();
			try {
				InputStream clientInput = DummyHttpProxyServer.this.clientSocket.getInputStream();
				int b;
				while( (b = clientInput.read()) != -1) {
					if(DummyHttpProxyServer.this.serverSocket == null) {
						connectRequest.write(b);
						connectRequest.flush();
						
						byte[] conReqBytes = connectRequest.toByteArray();
						
						if(conReqBytes.length > 4 && conReqBytes[conReqBytes.length - 4] == '\r' && conReqBytes[conReqBytes.length - 3] == '\n' && conReqBytes[conReqBytes.length - 2] == '\r' && conReqBytes[conReqBytes.length - 1] == '\n') {
							// CONNECT 127.0.0.1:8443 HTTP/1.1
							int serverAddressIndex = 8;
							int serverAddressLength = 0;
							while( conReqBytes[serverAddressIndex + serverAddressLength] != ' ') {
								serverAddressLength++;
							}
							String serverAddress = new String(conReqBytes, serverAddressIndex, serverAddressLength);
							String[] serverAddressSplit = serverAddress.split(":");
							String serverHost = serverAddressSplit[0];
							int serverPort = Integer.parseInt(serverAddressSplit[1]);
							
							connectRequest = null;
							DummyHttpProxyServer.this.serverSocket = new Socket(serverHost, serverPort);
							
							new Thread(new DummyHttpProxyServer.ServerReadTask()).start();
							
							DummyHttpProxyServer.this.clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
							DummyHttpProxyServer.this.clientSocket.getOutputStream().flush();
						}
					}
					else {
						DummyHttpProxyServer.this.serverSocket.getOutputStream().write(b);
						DummyHttpProxyServer.this.serverSocket.getOutputStream().flush();
					}
				}
			}
			catch(Exception e) {
			}
			finally {
				DummyHttpProxyServer.this.stop();
			}
		}
	}
	
	private class ServerReadTask implements Runnable {

		@Override
		public void run() {
			try {
				InputStream serverInput = DummyHttpProxyServer.this.serverSocket.getInputStream();
				OutputStream clientOutput = DummyHttpProxyServer.this.clientSocket.getOutputStream();
				int b;
				while( (b = serverInput.read()) != -1) {
					clientOutput.write(b);
					clientOutput.flush();
				}
			}
			catch(Exception e) {
			}
			finally {
				DummyHttpProxyServer.this.stop();
			}
		}
	}
	
	public static void main(String[] args) {
		new DummyHttpProxyServer(3456).start();
	}
}
