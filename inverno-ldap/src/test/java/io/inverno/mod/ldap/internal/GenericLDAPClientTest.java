/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.ldap.internal;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.inverno.mod.ldap.LDAPAttribute;
import io.inverno.mod.ldap.LDAPClient;
import io.inverno.mod.ldap.LDAPClientConfiguration;
import io.inverno.mod.ldap.LDAPEntry;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@EnabledIf( value = "isEnabled", disabledReason = "Failed to connect to test Ldap server" )
public class GenericLDAPClientTest {

	private static LDAPClient createClient() {
		GenericLDAPClient client = new GenericLDAPClient(
			new LDAPClientConfiguration() {
			
				@Override
				public URI uri() {
					return URI.create("ldap://127.0.0.1:1389");
				}
				
				@Override
				public String admin_dn() {
					return "cn=admin,dc=inverno,dc=io";
				}
				
				@Override
				public String admin_credentials() {
					return "adminpassword";
				}
			}, 
			Executors.newCachedThreadPool()
		);
		client.init();
		return client;
	}
	
	public static boolean isEnabled() {
		try {
			LDAPClient client = createClient();
			Mono.from(client.bind(null, null, ops -> Mono.empty())).block();
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}
	
	@Test
	public void test_get() {
		LDAPClient client = createClient();
		try {
			LDAPEntry result = client.get("cn={0},ou=users,dc=inverno,dc=io", new String[] {"cn", "uid", "mail", "userPassword"}, "jsmith").block();
			
			Assertions.assertEquals("cn=jsmith,ou=users,dc=inverno,dc=io", result.getDN());
			Assertions.assertEquals(4, result.getAll().size());
			Assertions.assertEquals("jsmith", result.get("cn").get());
			Assertions.assertEquals("jsmith", result.get("uid").get());
			Assertions.assertEquals("jsmith@inverno.io", result.get("mail").get());
		}
		finally {
			client.close().block();
		}
	}
	
	@Test
	public void test_search() {
		LDAPClient client = createClient();
		try {
			List<LDAPEntry> result = client.search("ou=users,dc=inverno,dc=io", new String[] {"cn", "uid"}, "(objectClass=inetOrgPerson)")
				.collectList()
				.block();
			
			Assertions.assertEquals(2, result.size());
			
			LDAPEntry entry = result.get(0);
			
			Assertions.assertEquals("cn=jsmith,ou=users,dc=inverno,dc=io", entry.getDN());
			Assertions.assertEquals(2, entry.getAll().size());
			Assertions.assertEquals(Set.of("jsmith"), new HashSet<>(entry.getAll("cn")));
			Assertions.assertEquals("jsmith", entry.get("uid").get());
		}
		finally {
			client.close().block();
		}
	}
	
	@Test
	public void test_authenticate() {
		LDAPClient client = createClient();
		try {
			String uid = "jsmith";
			String userDN = "cn=jsmith,ou=users,dc=inverno,dc=io";
			DummyUser user = Mono.from(client.bind(
					"cn={0},ou=users,dc=inverno,dc=io",
					new Object[] {uid},
					"password", 
					ops -> ops.search(userDN, new String[] {"uid"}, "(&(objectClass=inetOrgPerson)(uid={0}))", uid)
						.flatMap(userEntry -> ops.search("dc=inverno,dc=io", new String[]{ "cn" }, "(&(objectClass=groupOfNames)(member={0}))", userEntry.getDN())
							.map(groupEntry -> groupEntry.getAttribute("cn").map(LDAPAttribute::asString).get())
							.collectList()
							.map(groups -> new DummyUser(userEntry.getDN(), userEntry.getAttribute("uid").map(LDAPAttribute::asString).get(), groups)))
						)
				)
				.block();
			
			Assertions.assertEquals(userDN, user.getDn());
			Assertions.assertEquals("jsmith", user.getName());
			Assertions.assertEquals(List.of("readers", "writers"), user.getGroups());
		}
		finally {
			client.close().block();
		}
	}
	
	@Test
	public void test_identity() {
		LDAPClient client = createClient();
		try {
			client.search("cn=jsmith,ou=users,dc=inverno,dc=io", new String[] {"uid", "givenName", "sn", "displayName", "mail", "title", "telephoneNumber", "mobile", "jpegPhoto"}, "(&(objectClass=inetOrgPerson)(uid={0}))", "jsmith")
				.single()
				.doOnNext(userEntry -> {
					Assertions.assertEquals("jsmith", userEntry.getAttribute("uid").map(LDAPAttribute::asString).orElse(null));
					Assertions.assertEquals("John", userEntry.getAttribute("givenName").map(LDAPAttribute::asString).orElse(null));
					Assertions.assertEquals("Smith", userEntry.getAttribute("sn").map(LDAPAttribute::asString).orElse(null));
					Assertions.assertEquals("John Smith", userEntry.getAttribute("displayName").map(LDAPAttribute::asString).orElse(null));
					Assertions.assertEquals("jsmith@inverno.io", userEntry.getAttribute("mail").map(LDAPAttribute::asString).orElse(null));
					Assertions.assertEquals("Chief", userEntry.getAttribute("title").map(LDAPAttribute::asString).orElse(null));
					Assertions.assertEquals("+1 408 555 1862", userEntry.getAttribute("telephoneNumber").map(LDAPAttribute::asString).orElse(null));
					Assertions.assertEquals("+1 408 555 1941", userEntry.getAttribute("mobile").map(LDAPAttribute::asString).orElse(null));
					try {
						Assertions.assertArrayEquals(Files.readAllBytes(Path.of("src/test/resources/user.jpeg")), userEntry.getAttribute("jpegPhoto").map(attr -> attr.as(byte[].class)).orElse(null));
					} 
					catch (IOException e) {
						Assertions.fail(e);
					}
				})
				.block();
		}
		finally {
			client.close().block();
		}
	}
	
	/*
	 * LDAPTIVE example
	 */
	/*@Test
	public void test_ldaptive() throws LdapException, IOException {
		PooledConnectionFactory cf = null;
		try {
			cf = PooledConnectionFactory.builder(new NettyConnectionFactoryTransport(io.netty.channel.epoll.EpollSocketChannel.class, new io.netty.channel.epoll.EpollEventLoopGroup())) // depends on: Reactor and TransportType
				.config(ConnectionConfig.builder()
					.url("ldap://127.0.0.1:1389")
					.build()
				)
				.min(1)
				.max(4)
				.build();
			cf.initialize();
			
			try(Connection connection = cf.getConnection()) {
				PooledConnectionProxy invocationHandler = (PooledConnectionProxy)Proxy.getInvocationHandler(connection);
				
				DefaultOperationHandle<BindRequest, BindResponse> bindOp = new DefaultOperationHandle<>(
					SimpleBindRequest.builder()
						.dn("cn=user01,ou=users,dc=example,dc=org")
						.password("bitnami1")
						.controls(new PasswordPolicyControl())
						.build(), 
					(TransportConnection) invocationHandler.getConnection(), 
					cf.getConnectionConfig().getConnectTimeout()
				);
				
				bindOp
					.onResult(bindResult -> {
						if(bindResult.isSuccess()) {
							connection
								.operation(SearchRequest.builder().dn("ou=users,dc=example,dc=org").filter("(userid=user01)").returnAttributes("*").build())
								.onEntry(entry -> {
									entry.getAttributes().stream().forEach(attr -> {
										System.out.println(attr);
									});
									return entry;
								})
								.onResult(result -> {
									System.out.println("= complete =");
								})
								.send();
							
							connection
								.operation(SearchRequest.builder().dn("dc=example,dc=org").filter("(&(objectClass=groupOfNames)(member=cn=user01,ou=users,dc=example,dc=org))").returnAttributes("*").build())
								.onEntry(entry -> {
									entry.getAttributes().stream().forEach(attr -> {
										System.out.println(attr);
									});
									return entry;
								})
								.onResult(result -> {
									System.out.println("= complete =");
								})
								.send();
						}
						else {
							System.out.println(bindResult.getControls().length);
						}
					}).send();
				
				System.in.read();
			}
		}
		finally {
			if(cf != null) {
				cf.close();
			}
		}
	}*/
}

