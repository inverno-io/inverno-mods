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
package io.inverno.mod.configuration;

import java.net.URISyntaxException;
import java.nio.file.Paths;

import io.inverno.mod.configuration.source.CPropsFileConfigurationSource;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class Test {

	@org.junit.jupiter.api.Test
	public void test() throws URISyntaxException {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test.cprops").toURI()));
		
		long t0 = System.nanoTime();
		System.out.println(src.get("db.url").withParameters("env", "prod", "zone", "us").execute().blockLast().getResult().get().asURI().get());
		System.out.println(System.nanoTime() - t0);
		
		t0 = System.nanoTime();
		System.out.println(src.get("db.url").withParameters("env", "prod", "zone", "us").execute().blockLast().getResult().get().asURI().get());
		System.out.println(System.nanoTime() - t0);
	}

}
