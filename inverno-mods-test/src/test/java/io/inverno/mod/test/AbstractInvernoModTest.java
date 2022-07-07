/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.test;

import io.inverno.test.AbstractInvernoTest;
import java.io.File;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class AbstractInvernoModTest extends AbstractInvernoTest {

	public AbstractInvernoModTest() {
		super((Function<File, File>)file -> {
			if(file.getName().startsWith("inverno-configuration-compiler")) {
				if(new File("../inverno-configuration-compiler").exists()) {
					return Optional.of(new File("../inverno-configuration-compiler/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-configuration")) {
				if(new File("../inverno-configuration").exists()) {
					return Optional.of(new File("../inverno-configuration/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-web-compiler")) {
				if(new File("../inverno-web-compiler").exists()) {
					return Optional.of(new File("../inverno-web-compiler/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-web")) {
				if(new File("../inverno-web").exists()) {
					return Optional.of(new File("../inverno-web/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-http-server")) {
				if(new File("../inverno-http-server").exists()) {
					return Optional.of(new File("../inverno-http-server/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-http-base")) {
				if(new File("../inverno-http-base").exists()) {
					return Optional.of(new File("../inverno-http-base/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-boot")) {
				if(new File("../inverno-boot").exists()) {
					return Optional.of(new File("../inverno-boot/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-base")) {
				if(new File("../inverno-base").exists()) {
					return Optional.of(new File("../inverno-base/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-core-annotation")) {
				if(new File("../inverno-core-annotation").exists()) {
					return Optional.of(new File("../inverno-core-annotation/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-core-compiler")) {
				if(new File("../inverno-core-compiler").exists()) {
					return Optional.of(new File("../inverno-core-compiler/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-core")) {
				if(new File("../inverno-core").exists()) {
					return Optional.of(new File("../inverno-core/target/classes")).filter(File::exists).orElse(file);
				}
			}
			return file;
		},
		file -> {
			if(file.getName().startsWith("inverno-core-compiler")) {
				if(new File("../inverno-core-compiler").exists()) {
					return Optional.of(new File("../inverno-core-compiler/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-core-annotation")) {
				if(new File("../inverno-core-annotation").exists()) {
					return Optional.of(new File("../inverno-core-annotation/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-configuration-compiler")) {
				if(new File("../inverno-configuration-compiler").exists()) {
					return Optional.of(new File("../inverno-configuration-compiler/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-configuration")) {
				if(new File("../inverno-configuration").exists()) {
					return Optional.of(new File("../inverno-configuration/target/classes")).filter(File::exists).orElse(file);
				}
			}
			else if(file.getName().startsWith("inverno-base")) {
				if(new File("../inverno-base").exists()) {
					return Optional.of(new File("../inverno-base/target/classes")).filter(File::exists).orElse(file);
				}
			}
			return null;
		});
	}
}