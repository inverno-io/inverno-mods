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
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class PathPatternResolverTest {

	public static final Path ROOT_TEST_PATH = Path.of("target/pathPatternResolver");
	
	@BeforeAll
	public static void init() throws IOException {
		if(Files.exists(ROOT_TEST_PATH)) {
			Files.walk(ROOT_TEST_PATH).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
		
		Files.createDirectories(ROOT_TEST_PATH);
		Files.createFile(ROOT_TEST_PATH.resolve("tata"));
		Files.createFile(ROOT_TEST_PATH.resolve("tati"));
		
		Files.createDirectory(ROOT_TEST_PATH.resolve("empty"));
		Files.createDirectory(ROOT_TEST_PATH.resolve("foo"));
		Files.createFile(ROOT_TEST_PATH.resolve("foo/teta"));
		Files.createFile(ROOT_TEST_PATH.resolve("foo/tutu"));
		
		Files.createDirectory(ROOT_TEST_PATH.resolve("foo/bar"));
		Files.createFile(ROOT_TEST_PATH.resolve("foo/bar/titi"));
	}
	
	@Test
	public void test_relativePattern() throws IOException {
		Path basePath = ROOT_TEST_PATH;
		
		Set<Path> paths = PathPatternResolver.resolve("*", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/tati").toAbsolutePath(), Path.of("target/pathPatternResolver/tata").toAbsolutePath(), Path.of("target/pathPatternResolver/foo").toAbsolutePath(), Path.of("target/pathPatternResolver/empty").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve("**/*", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/tati").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/tutu").toAbsolutePath(), Path.of("target/pathPatternResolver/tata").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/teta").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/bar/titi").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve("foo/**/*", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/foo/tutu").toAbsolutePath(),Path.of("target/pathPatternResolver/foo/teta").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/bar/titi").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve("**/bar/*", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/foo/bar/titi").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve("**/*ti", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/tati").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/bar/titi").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve("*/*", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/foo/bar").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/tutu").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/teta").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve("**/t?ta", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/tata").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/teta").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve("tati", basePath.resolve("tati")).collect(Collectors.toSet());
		Assertions.assertEquals(Set.<Path>of(), paths);
	}

	@Test
	public void test_absolutePattern() throws IOException {
		Path basePath = ROOT_TEST_PATH;
		String basePattern = ROOT_TEST_PATH.toAbsolutePath().toString().replace('\\', '/') + "/";
		
		Set<Path> paths = PathPatternResolver.resolve(basePattern + "*", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/tati").toAbsolutePath(), Path.of("target/pathPatternResolver/tata").toAbsolutePath(), Path.of("target/pathPatternResolver/foo").toAbsolutePath(), Path.of("target/pathPatternResolver/empty").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve(basePattern + "tati", basePath.resolve("tati")).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/tati").toAbsolutePath()), paths);
		
		basePath = ROOT_TEST_PATH.resolve("foo");
		
		paths = PathPatternResolver.resolve(basePattern + "foo", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/foo").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve(basePattern + "foo/*", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/foo/bar").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/teta").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/tutu").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve(basePattern + "**/*", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/foo/bar/titi").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/teta").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/tutu").toAbsolutePath()), paths);
		
		paths = PathPatternResolver.resolve(basePattern + "**/t?t?", basePath).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(Path.of("target/pathPatternResolver/foo/bar/titi").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/teta").toAbsolutePath(), Path.of("target/pathPatternResolver/foo/tutu").toAbsolutePath()), paths);
	}
}
