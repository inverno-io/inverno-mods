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
package io.inverno.mod.security.accesscontrol;

import io.inverno.mod.base.resource.ClasspathResource;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.source.CPropsFileConfigurationSource;
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ConfigurationSourcePermissionBasedAccessControllerTest {
	
	
	@Test
	public void test() {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/permissions.cprops")));
		src = src.withDefaultingStrategy(DefaultingStrategy.wildcard());
		
		PermissionBasedAccessController pbac = new ConfigurationSourcePermissionBasedAccessController(src, "jsmith");
		
		Assertions.assertFalse(pbac.hasPermission("query").block());
		Assertions.assertFalse(pbac.hasPermission("query", "domain", "printer").block());
		Assertions.assertTrue(pbac.hasPermission("print", "domain", "printer", "printer", "lp1200").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "lp1200").block());
		Assertions.assertFalse(pbac.hasPermission("print", "domain", "other", "printer", "lp1200").block());
		Assertions.assertFalse(pbac.hasPermission("query", "domain", "printer", "printer", "epsoncolor").block());
		Assertions.assertTrue(pbac.hasPermission("manage", "domain", "printer", "printer", "epsoncolor").block());
		Assertions.assertTrue(pbac.hasPermission("manage", "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("invalidPermission", "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("query", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasPermission("anyPermission", "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertFalse(pbac.hasPermission("anyPermission", "domain", "other", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertTrue(pbac.hasPermission("print", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertFalse(pbac.hasPermission("manage", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertFalse(pbac.hasPermission("print", "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertFalse(pbac.hasPermission("query", "domain", "printer", "printer", "HL-L6400DW").block());
		
		Assertions.assertFalse(pbac.hasAnyPermission(Set.of("query", "print"), "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertTrue(pbac.hasAnyPermission(Set.of("query", "print"), "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertFalse(pbac.hasAnyPermission(Set.of("query", "manage"), "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertFalse(pbac.hasAnyPermission(Set.of("manage"), "domain", "printer", "printer", "C400V_DN").block());
		
		Assertions.assertFalse(pbac.hasAllPermissions(Set.of("query", "print"), "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print", "manage"), "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print"), "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertFalse(pbac.hasAllPermissions(Set.of("query", "print", "manage"), "domain", "printer", "printer", "C400V_DN").block());
		
		// should be the same results except that now we should have the query permission in the printer domain and some different permissions for HL-L6400DW
		pbac = new ConfigurationSourcePermissionBasedAccessController(src, "jsmith", Set.of("user"));
		
		Assertions.assertFalse(pbac.hasPermission("query").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer").block());
		Assertions.assertTrue(pbac.hasPermission("print", "domain", "printer", "printer", "lp1200").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "lp1200").block());
		Assertions.assertFalse(pbac.hasPermission("print", "domain", "other", "printer", "lp1200").block());
		Assertions.assertFalse(pbac.hasPermission("query", "domain", "printer", "printer", "epsoncolor").block());
		Assertions.assertTrue(pbac.hasPermission("manage", "domain", "printer", "printer", "epsoncolor").block());
		Assertions.assertTrue(pbac.hasPermission("manage", "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("invalidPermission", "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("query", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasPermission("anyPermission", "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertFalse(pbac.hasPermission("anyPermission", "domain", "other", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertTrue(pbac.hasPermission("print", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertFalse(pbac.hasPermission("manage", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertTrue(pbac.hasPermission("print", "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "HL-L6400DW").block());
		
		Assertions.assertFalse(pbac.hasAnyPermission(Set.of("query", "print"), "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertTrue(pbac.hasAnyPermission(Set.of("query", "print"), "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasAnyPermission(Set.of("query", "manage"), "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertFalse(pbac.hasAnyPermission(Set.of("manage"), "domain", "printer", "printer", "C400V_DN").block());
		
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print"), "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print", "manage"), "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print"), "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertFalse(pbac.hasAllPermissions(Set.of("query", "print", "manage"), "domain", "printer", "printer", "C400V_DN").block());
		
		// This is a user with role "user" with no specific permissions
		pbac = new ConfigurationSourcePermissionBasedAccessController(src, "user", Set.of("user"));
		
		Assertions.assertFalse(pbac.hasPermission("query").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer").block());
		Assertions.assertFalse(pbac.hasPermission("print", "domain", "printer", "printer", "lp1200").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "lp1200").block());
		Assertions.assertFalse(pbac.hasPermission("query", "domain", "other", "printer", "lp1200").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("manage", "domain", "printer", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("manage", "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("invalidPermission", "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("query", "printer", "XP-4100").block());
		Assertions.assertFalse(pbac.hasPermission("anyPermission", "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertFalse(pbac.hasPermission("anyPermission", "domain", "other", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertFalse(pbac.hasPermission("print", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertFalse(pbac.hasPermission("manage", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertTrue(pbac.hasPermission("print", "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "HL-L6400DW").block());
		
		Assertions.assertTrue(pbac.hasAnyPermission(Set.of("query", "print"), "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertTrue(pbac.hasAnyPermission(Set.of("query", "print"), "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasAnyPermission(Set.of("query", "manage"), "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertFalse(pbac.hasAnyPermission(Set.of("manage"), "domain", "printer", "printer", "C400V_DN").block());
		
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print"), "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertFalse(pbac.hasAllPermissions(Set.of("query", "print", "manage"), "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertFalse(pbac.hasAllPermissions(Set.of("query", "print"), "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertFalse(pbac.hasAllPermissions(Set.of("query", "print", "manage"), "domain", "printer", "printer", "C400V_DN").block());
		
		// This is a user with role "admin" with all permissions in domain printer
		pbac = new ConfigurationSourcePermissionBasedAccessController(src, "admin", Set.of("admin"));
		
		Assertions.assertFalse(pbac.hasPermission("query").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer").block());
		Assertions.assertTrue(pbac.hasPermission("print", "domain", "printer", "printer", "lp1200").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "lp1200").block());
		Assertions.assertFalse(pbac.hasPermission("query", "domain", "other", "printer", "lp1200").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "epsoncolor").block());
		Assertions.assertTrue(pbac.hasPermission("manage", "domain", "printer", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("manage", "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("invalidPermission", "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertFalse(pbac.hasPermission("query", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasPermission("anyPermission", "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertFalse(pbac.hasPermission("anyPermission", "domain", "other", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertTrue(pbac.hasPermission("print", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertTrue(pbac.hasPermission("manage", "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertTrue(pbac.hasPermission("print", "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertTrue(pbac.hasPermission("query", "domain", "printer", "printer", "HL-L6400DW").block());
		
		Assertions.assertFalse(pbac.hasAnyPermission(Set.of("query", "print"), "domain", "other", "printer", "epsoncolor").block());
		Assertions.assertTrue(pbac.hasAnyPermission(Set.of("query", "print"), "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasAnyPermission(Set.of("query", "manage"), "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertTrue(pbac.hasAnyPermission(Set.of("manage"), "domain", "printer", "printer", "C400V_DN").block());
		
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print"), "domain", "printer", "printer", "HL-L6400DW").block());
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print", "manage"), "domain", "printer", "printer", "XP-4100").block());
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print"), "domain", "printer", "printer", "C400V_DN").block());
		Assertions.assertTrue(pbac.hasAllPermissions(Set.of("query", "print", "manage"), "domain", "printer", "printer", "C400V_DN").block());
	}
}
