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

import io.inverno.mod.boot.Boot;
import io.inverno.mod.boot.BootConfigurationLoader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericNetServiceTest {

	@Test
	public void test_resolve() throws UnknownHostException {
		Inet4Address inverno_io_ipv4 = (Inet4Address)InetAddress.getByAddress(new byte[]{1, 2, 3, 4});
		Inet6Address inverno_io_ipv6 = (Inet6Address)InetAddress.getByAddress(new byte[]{0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7, 0, 8 });

		try(NettyDnsServer dnsServer = NettyDnsServer.starter().ipv4Entry("inverno.io.", List.of(inverno_io_ipv4)).ipv6Entry("inverno.io.", List.of(inverno_io_ipv6)).start()) {
			Boot boot = new Boot.Builder()
				.setConfiguration(BootConfigurationLoader.load(bootConfig -> bootConfig
					.address_resolver(addressResolverConfig -> addressResolverConfig
						.name_servers(Set.of(new InetSocketAddress("127.0.0.1", dnsServer.getPort())))
					)
				))
				.build();

			boot.start();
			try {
				Assertions.assertEquals(inverno_io_ipv4, boot.netService().resolve("inverno.io").block());
				Assertions.assertEquals(new InetSocketAddress(inverno_io_ipv4, 1234), boot.netService().resolve("inverno.io", 1234).block());
				Assertions.assertEquals(new InetSocketAddress(inverno_io_ipv4, 1234), boot.netService().resolve(InetSocketAddress.createUnresolved("inverno.io", 1234)).block());
			}
			finally {
				boot.stop();
			}
		}
	}

	@Test
	public void test_resolveAll() throws UnknownHostException {
		Inet4Address inverno_io_ipv4_1 = (Inet4Address)InetAddress.getByAddress(new byte[]{1, 2, 3, 4});
		Inet4Address inverno_io_ipv4_2 = (Inet4Address)InetAddress.getByAddress(new byte[]{5, 6, 7, 8});
		Inet6Address inverno_io_ipv6 = (Inet6Address)InetAddress.getByAddress(new byte[]{0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7, 0, 8 });

		try(NettyDnsServer dnsServer = NettyDnsServer.starter().ipv4Entry("inverno.io.", List.of(inverno_io_ipv4_1, inverno_io_ipv4_2)).ipv6Entry("inverno.io.", List.of(inverno_io_ipv6)).start()) {
			Boot boot = new Boot.Builder()
				.setConfiguration(BootConfigurationLoader.load(bootConfig -> bootConfig
					.address_resolver(addressResolverConfig -> addressResolverConfig
						.name_servers(Set.of(new InetSocketAddress("127.0.0.1", dnsServer.getPort())))
					)
				))
				.build();

			boot.start();
			try {
				Assertions.assertEquals(List.of(inverno_io_ipv4_1, inverno_io_ipv4_2, inverno_io_ipv6), boot.netService().resolveAll("inverno.io").block());
				Assertions.assertEquals(List.of(inverno_io_ipv4_1, inverno_io_ipv4_2, inverno_io_ipv6).stream().map(address -> new InetSocketAddress(address, 1234)).collect(Collectors.toUnmodifiableList()), boot.netService().resolveAll("inverno.io", 1234).block());
				Assertions.assertEquals(List.of(inverno_io_ipv4_1, inverno_io_ipv4_2, inverno_io_ipv6).stream().map(address -> new InetSocketAddress(address, 1234)).collect(Collectors.toUnmodifiableList()), boot.netService().resolveAll(InetSocketAddress.createUnresolved("inverno.io", 1234)).block());
			}
			finally {
				boot.stop();
			}
		}
	}
}
