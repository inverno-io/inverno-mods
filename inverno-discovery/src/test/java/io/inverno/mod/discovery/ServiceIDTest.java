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
package io.inverno.mod.discovery;

import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ServiceIDTest {

	@Test
	public void testGetRequestTarget() {
		Assertions.assertEquals("/", ServiceID.getRequestTarget(URI.create("http://example.org")));
		Assertions.assertEquals("/", ServiceID.getRequestTarget(URI.create("http://example.org/")));
		Assertions.assertEquals("/test", ServiceID.getRequestTarget(URI.create("http://example.org/test")));

		Assertions.assertEquals("/", ServiceID.getRequestTarget(URI.create("urn:test:abc")));
		Assertions.assertEquals("/", ServiceID.getRequestTarget(URI.create("urn:test:abc#")));
		Assertions.assertEquals("/", ServiceID.getRequestTarget(URI.create("urn:test:abc#/")));
		Assertions.assertEquals("/test", ServiceID.getRequestTarget(URI.create("urn:test:abc#/test")));
	}

	@Test
	public void testServiceURIValidation() {
		Assertions.assertDoesNotThrow(() -> ServiceID.of(URI.create("scheme://authority/path/to/resource")));
		Assertions.assertDoesNotThrow(() -> ServiceID.of(URI.create("scheme:ssp#/path/to/resource")));

		Assertions.assertEquals("URI must be absolute: /path/to/resource", Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceID.of(URI.create("/path/to/resource"))).getMessage());
		Assertions.assertEquals("URI must have an authority component: scheme:///path/to/resource", Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceID.of(URI.create("scheme:///path/to/resource"))).getMessage());
		Assertions.assertEquals("Opaque URI fragment path must be absolute: scheme:ssp#path/to/resource", Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceID.of(URI.create("scheme:ssp#path/to/resource"))).getMessage());

		Assertions.assertDoesNotThrow(() -> ServiceID.getRequestTarget(URI.create("scheme://authority/path/to/resource")));
		Assertions.assertDoesNotThrow(() -> ServiceID.getRequestTarget(URI.create("scheme:ssp#/path/to/resource")));

		Assertions.assertEquals("URI must be absolute: /path/to/resource", Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceID.getRequestTarget(URI.create("/path/to/resource"))).getMessage());
		Assertions.assertEquals("URI must have an authority component: scheme:///path/to/resource", Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceID.getRequestTarget(URI.create("scheme:///path/to/resource"))).getMessage());
		Assertions.assertEquals("Opaque URI fragment path must be absolute: scheme:ssp#path/to/resource", Assertions.assertThrows(IllegalArgumentException.class, () -> ServiceID.getRequestTarget(URI.create("scheme:ssp#path/to/resource"))).getMessage());
	}
}
