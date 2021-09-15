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
package io.inverno.mod.boot.internal.resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class PathPatternResolverTest {

	@Test
	public void test() throws IOException {
		Path rootTestPath = Paths.get("target/pathPatternResolver");
		
		if(Files.exists(rootTestPath)) {
			Files.walk(rootTestPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
		
		Files.createDirectories(rootTestPath);
		Files.createFile(rootTestPath.resolve("tata"));
		Files.createFile(rootTestPath.resolve("tati"));
		
		Files.createDirectory(rootTestPath.resolve("empty"));
		Files.createDirectory(rootTestPath.resolve("foo"));
		Files.createFile(rootTestPath.resolve("foo/teta"));
		Files.createFile(rootTestPath.resolve("foo/tutu"));
		
		Files.createDirectory(rootTestPath.resolve("foo/bar"));
		Files.createFile(rootTestPath.resolve("foo/bar/titi"));
		
		Set<String> paths = PathPatternResolver.resolve(Paths.get("*"), rootTestPath).map(Object::toString).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of("target/pathPatternResolver/tati", "target/pathPatternResolver/tata", "target/pathPatternResolver/foo", "target/pathPatternResolver/empty"), paths);
		
		paths = PathPatternResolver.resolve(Paths.get("**/*"), rootTestPath).map(Object::toString).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of("target/pathPatternResolver/tati", "target/pathPatternResolver/foo/tutu", "target/pathPatternResolver/tata", "target/pathPatternResolver/foo/teta", "target/pathPatternResolver/foo/bar/titi"), paths);
		
		paths = PathPatternResolver.resolve(Paths.get("foo/**/*"), rootTestPath).map(Object::toString).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of("target/pathPatternResolver/foo/tutu","target/pathPatternResolver/foo/teta", "target/pathPatternResolver/foo/bar/titi"), paths);
		
		paths = PathPatternResolver.resolve(Paths.get("**/bar/*"), rootTestPath).map(Object::toString).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of("target/pathPatternResolver/foo/bar/titi"), paths);
		
		paths = PathPatternResolver.resolve(Paths.get("**/*ti"), rootTestPath).map(Object::toString).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of("target/pathPatternResolver/tati", "target/pathPatternResolver/foo/bar/titi"), paths);
		
		paths = PathPatternResolver.resolve(Paths.get("*/*"), rootTestPath).map(Object::toString).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of("target/pathPatternResolver/foo/bar", "target/pathPatternResolver/foo/tutu", "target/pathPatternResolver/foo/teta"), paths);
		
		paths = PathPatternResolver.resolve(Paths.get("**/t?ta"), rootTestPath).map(Object::toString).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of("target/pathPatternResolver/tata", "target/pathPatternResolver/foo/teta"), paths);
	}

}
