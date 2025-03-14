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

import io.inverno.test.InvernoCompilerExtension;
import io.inverno.test.InvernoCompilerTest;
import io.inverno.test.InvernoTestCompiler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@ExtendWith(InvernoCompilerExtension.class)
public class AbstractInvernoModTest implements InvernoCompilerTest {
	
	public static final Function<Path, Path> MODULE_OVERRIDE = path -> {
		// !!! order is important when a common prefix exists between two modules
		if(path.getFileName().toString().startsWith("inverno-base")) {
			return Optional.of(Path.of("../inverno-base/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-boot")) {
			return Optional.of(Path.of("../inverno-boot/target/classes")).filter(Files::exists).orElse(path);
		}
		if(path.getFileName().toString().startsWith("inverno-configuration-compiler")) {
			return Optional.of(Path.of("../inverno-configuration-compiler/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-configuration")) {
			return Optional.of(Path.of("../inverno-configuration/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-discovery-http-k8s")) {
			return Optional.of(Path.of("../inverno-discovery-http-k8s/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-discovery-http-meta")) {
			return Optional.of(Path.of("../inverno-discovery-http-meta/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-discovery-http")) {
			return Optional.of(Path.of("../inverno-discovery-http/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-discovery")) {
			return Optional.of(Path.of("../inverno-discovery/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-grpc-base")) {
			return Optional.of(Path.of("../inverno-grpc-base/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-grpc-client")) {
			return Optional.of(Path.of("../inverno-grpc-client/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-grpc-server")) {
			return Optional.of(Path.of("../inverno-grpc-server/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-http-base")) {
			return Optional.of(Path.of("../inverno-http-base/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-http-client")) {
			return Optional.of(Path.of("../inverno-http-client/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-http-server")) {
			return Optional.of(Path.of("../inverno-http-server/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-irt-compiler")) {
			return Optional.of(Path.of("../inverno-irt-compiler/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-irt")) {
			return Optional.of(Path.of("../inverno-irt/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-ldap")) {
			return Optional.of(Path.of("../inverno-ldap/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-redis-lettuce")) {
			return Optional.of(Path.of("../inverno-redis-lettuce/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-redis")) {
			return Optional.of(Path.of("../inverno-redis/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-security-http")) {
			return Optional.of(Path.of("../inverno-security-http/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-security-jose")) {
			return Optional.of(Path.of("../inverno-security-jose/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-security-ldap")) {
			return Optional.of(Path.of("../inverno-security-ldap/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-security")) {
			return Optional.of(Path.of("../inverno-security/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-sql-vertx")) {
			return Optional.of(Path.of("../inverno-sql-vertx/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-sql")) {
			return Optional.of(Path.of("../inverno-sql/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-web-base")) {
			return Optional.of(Path.of("../inverno-web-base/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-web-client")) {
			return Optional.of(Path.of("../inverno-web-client/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-web-server")) {
			return Optional.of(Path.of("../inverno-web-server/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-web-compiler")) {
			return Optional.of(Path.of("../inverno-web-compiler/target/classes")).filter(Files::exists).orElse(path);
		}
		return path;
	};
	
	public static final Function<Path, Path> ANNOTATION_PROCESSOR_MODULE_OVERRIDE = path -> {
		// !!! order is important when a common prefix exists between two modules
		if(path.getFileName().toString().startsWith("inverno-base")) {
			return Optional.of(Path.of("../inverno-base/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-configuration-compiler")) {
			return Optional.of(Path.of("../inverno-configuration-compiler/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-configuration")) {
			return Optional.of(Path.of("../inverno-configuration/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-http-base")) {
			return Optional.of(Path.of("../inverno-http-base/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-http-client")) {
			return Optional.of(Path.of("../inverno-http-client/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-http-server")) {
			return Optional.of(Path.of("../inverno-http-server/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-irt-compiler")) {
			return Optional.of(Path.of("../inverno-irt-compiler/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-irt")) {
			return Optional.of(Path.of("../inverno-irt/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-web-base")) {
			return Optional.of(Path.of("../inverno-web-base/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-web-client")) {
			return Optional.of(Path.of("../inverno-web-client/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-web-server")) {
			return Optional.of(Path.of("../inverno-web-server/target/classes")).filter(Files::exists).orElse(path);
		}
		else if(path.getFileName().toString().startsWith("inverno-web-compiler")) {
			return Optional.of(Path.of("../inverno-web-compiler/target/classes")).filter(Files::exists).orElse(path);
		}
		return null;
	};
	
	private InvernoTestCompiler invernoCompiler;

	@Override
	public void setInvernoCompiler(InvernoTestCompiler invernoCompiler) {
		this.invernoCompiler = invernoCompiler;
	}

	public InvernoTestCompiler getInvernoCompiler() {
		return invernoCompiler;
	}
	
	@Override
	public Function<Path, Path> getModuleOverride() {
		return MODULE_OVERRIDE;
	}

	@Override
	public Function<Path, Path> getAnnotationProcessorModuleOverride() {
		return ANNOTATION_PROCESSOR_MODULE_OVERRIDE;
	}
}
