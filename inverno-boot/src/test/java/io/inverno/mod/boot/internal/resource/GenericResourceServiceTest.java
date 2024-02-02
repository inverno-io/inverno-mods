/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.boot.internal.resource;

import io.inverno.mod.base.resource.Resource;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jkuhn
 */
public class GenericResourceServiceTest {

	@Test
	public void test_getResources_webjar() {
		GenericResourceService service = new GenericResourceService();
		service.setProviders(List.of(new JarResourceProvider()));
		
		Path jarPath = Path.of("src/test/resources/test.jar").toAbsolutePath();
		URI uri = URI.create("jar:" + jarPath.toUri()+"!/*/*");
		
		Set<URI> resources = service.getResources(uri).map(Resource::getURI).map(URI::normalize).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(URI.create("jar:" + jarPath.toUri() +"!/ign/test.txt").normalize()), resources);
		
	}
}
