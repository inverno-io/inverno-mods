/*
 * Copyright 2024 Jeremy KUHN
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
package io.inverno.mod.base.resource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ReferenceCountedFileSystemsTest {
	
	@Test
	public void testFileSystemWithFileURI() throws IOException {
		URI uri = URI.create("jar:" + new File("src/test/resources/test.jar").toURI().toString());
		
		try(FileSystem fileSystem = ReferenceCountedFileSystems.getFileSystem(uri)) {
			Assertions.assertEquals("This is a test", Files.readString(fileSystem.getPath("/ign/test.txt")));
		}
	}
}
