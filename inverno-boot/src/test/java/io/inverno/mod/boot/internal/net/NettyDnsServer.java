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
package io.inverno.mod.boot.internal.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsQueryDecoder;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DatagramDnsResponseEncoder;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsSection;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class NettyDnsServer implements AutoCloseable {

	private final int port;
	private final long recordTtl;
	private final Map<String, List<Inet4Address>> ipv4Entries;
	private final Map<String, List<Inet6Address>> ipv6Entries;

	private final Channel channel;

	private NettyDnsServer(int port, long recordTtl, Map<String, List<Inet4Address>> ipv4Entries, Map<String, List<Inet6Address>> ipv6Entries) {
		this.port = port;
		this.recordTtl = recordTtl;
		this.ipv4Entries = ipv4Entries;
		this.ipv6Entries = ipv6Entries;

		Bootstrap bootstrap = new Bootstrap()
			.group(new NioEventLoopGroup())
			.channel(NioDatagramChannel.class)
			.handler(new ChannelInitializer<Channel>() {

				@Override
				protected void initChannel(Channel channel) throws Exception {
					channel.pipeline().addLast(
						new DatagramDnsQueryDecoder(),
						new DatagramDnsResponseEncoder(),
						new SimpleChannelInboundHandler<DatagramDnsQuery>() {

							@Override
							protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsQuery query) throws Exception {
								DnsQuestion question = query.recordAt(DnsSection.QUESTION);

								DatagramDnsResponse response = new DatagramDnsResponse(query.recipient(), query.sender(), query.id());
								response.addRecord(DnsSection.QUESTION, question);

								if(question.type().equals(DnsRecordType.A)) {
									for(Inet4Address address : ipv4Entries.getOrDefault(question.name(), List.of())) {
										response.addRecord(DnsSection.ANSWER, new DefaultDnsRawRecord(question.name(), DnsRecordType.A, recordTtl, Unpooled.wrappedBuffer(address.getAddress())));
									}
								}
								else if(question.type().equals(DnsRecordType.AAAA)) {
									for(Inet6Address address : ipv6Entries.getOrDefault(question.name(), List.of())) {
										response.addRecord(DnsSection.ANSWER, new DefaultDnsRawRecord(question.name(), DnsRecordType.AAAA, recordTtl, Unpooled.wrappedBuffer(address.getAddress())));
									}
								}

								ctx.writeAndFlush(response);
							}
						}
					);
				}
			});

		this.channel = bootstrap.bind(port).channel();
		System.out.println("DNS server started on port: " + port);
	}

	public static NettyDnsServer.Starter starter() {
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			return new NettyDnsServer.Starter(serverSocket.getLocalPort());
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static NettyDnsServer.Starter starter(int port) {
		return new NettyDnsServer.Starter(port);
	}

	private static int getFreePort() {
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			return serverSocket.getLocalPort();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public int getPort() {
		return port;
	}

	public long getRecordTtl() {
		return recordTtl;
	}

	public Map<String, List<Inet4Address>> getIpv4Entries() {
		return ipv4Entries;
	}

	public Map<String, List<Inet6Address>> getIpv6Entries() {
		return ipv6Entries;
	}

	@Override
	public void close() {
		try {
			this.channel.close().sync();
		}
		catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Starter {

		private final int port;

		private final Map<String, List<Inet4Address>> ipv4Entries;
		private final Map<String, List<Inet6Address>> ipv6Entries;

		private long recordTtl = 600;

		public Starter(int port) {
			this.port = port;
			this.ipv4Entries = new HashMap<>();
			this.ipv6Entries = new HashMap<>();
		}

		public Starter recordTtl(long recordTtl) {
			this.recordTtl = recordTtl;
			return this;
		}

		public Starter ipv4Entry(String hostName, List<Inet4Address> addresses) {
			this.ipv4Entries.put(hostName, addresses);
			return this;
		}

		public Starter ipv6Entry(String hostName, List<Inet6Address> addresses) {
			this.ipv6Entries.put(hostName, addresses);
			return this;
		}

		public NettyDnsServer start() {
			return new NettyDnsServer(this.port, this.recordTtl, this.ipv4Entries, this.ipv6Entries);
		}
	}

	public static void main(String[] args) throws IOException {
		Inet4Address inverno_io_1 = (Inet4Address)InetAddress.getByAddress(new byte[]{(byte) 1, (byte) 2, 3, 4});
		try(NettyDnsServer dnsServer = NettyDnsServer.starter().ipv4Entry("inverno.io.", List.of(inverno_io_1)).start()) {

			System.in.read();
		}
	}
}
