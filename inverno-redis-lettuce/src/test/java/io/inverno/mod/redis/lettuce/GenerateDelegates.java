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
package io.inverno.mod.redis.lettuce;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class used to generate delegates
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenerateDelegates {
	
	public static String removeGenerics(String input) {
		StringBuilder output = new StringBuilder();
		
		int depth = 0;
		for(int i=0;i<input.length();i++) {
			
			if(input.charAt(i) == '<') {
				depth++;
			}
			else if(input.charAt(i) == '>') {
				depth--;
			}
			
			if(depth == 0 && input.charAt(i) != '>') {
				output.append(input.charAt(i));
			}
		}
		return output.toString();
	}
	
	
	public static void main(String[] args) throws IOException {
		Set<String> excludedMethods = Set.of("transaction", "batch", "connection", "close");

		StringBuilder output = new StringBuilder();
		
		String methodName = null;
		List<String> methodParameters = null;
		boolean override = false;
		boolean mono = false;
		boolean flux = false;
		boolean excluded = false;
		for(String line : Files.readAllLines(Path.of("src/main/java/io/inverno/mod/redis/lettuce/internal/AbstractRedisClient.java"))) {
			if(line.equals("\t@Override")) {
				override = true;
			}
			else if(override) {
				String methodSignature = line;
				override = false;
				
				if(removeGenerics(methodSignature.substring(8)).trim().startsWith("Mono")) {
					mono = true;
					flux = false;
				}
				else if(removeGenerics(methodSignature.substring(8)).trim().startsWith("Flux")) {
					mono = false;
					flux = true;
				}
				else {
					mono = false;
					flux = false;
				}
				
				int pStart = methodSignature.indexOf("(") + 1;

				int mnStart = pStart;
				do {
					mnStart--;
				} while(line.charAt(mnStart) != ' ');
				
				methodName = line.substring(mnStart+1, pStart-1);
				if(excludedMethods.contains(methodName)) {
					excluded = true;
					methodName = null;
					continue;
				}
				
				String parameters = methodSignature.substring(pStart, methodSignature.lastIndexOf(")"));
				
				if(!parameters.isEmpty()) {
					methodParameters = Arrays.stream(removeGenerics(parameters).split(","))
						.map(String::trim)
						.map(s -> {
							return s.substring(s.lastIndexOf(" ")+1);
						})
						.collect(Collectors.toList());
				}
				else {
					methodParameters = List.of();
				}
				
				output.append("\t@Override").append(System.lineSeparator());
				output.append(methodSignature).append(System.lineSeparator());
			}
			else if(methodName != null) {
				output.append("\t\treturn ");
				if(mono) {
					output.append("Mono.from(");
				}
				else if(flux) {
					output.append("Flux.from(");
				}
				else {
					output.append("Mono.from(");
				}
				
				output.append("this.connection(o -> ");
				if(!mono && !flux) {
					output.append("Mono.just(");
				}
				output.append("o.").append(methodName).append("(").append(methodParameters.stream().collect(Collectors.joining(", "))).append(")))");
				
				if(!mono && !flux) {
					output.append(").block()");
				}
				output.append(";").append(System.lineSeparator());
				
				methodName = null;
				methodParameters = null;
				mono = flux = false;
			}
			else if(line.equals("\t}") || line.isBlank()) {
				if(!excluded) {
					output.append(line).append(System.lineSeparator());
				}
				excluded = false;
			}
		}
		
		Files.write(Path.of("delegates.txt"), output.toString().getBytes(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}
}
