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
package io.winterframework.mod.boot.internal.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import io.winterframework.mod.base.resource.ResourceException;

/**
 * @author jkuhn
 *
 */
final class PathPatternResolver {
	
	private PathPatternResolver() {}
	
	public static Stream<Path> resolve(Path pathPattern) {
		return resolve(pathPattern, null, Function.identity());
	}
	
	public static <T> Stream<T> resolve(Path pathPattern, Function<? super Path, ? extends T> mapper) {
		return resolve(pathPattern, null, mapper);
	}

	public static Stream<Path> resolve(Path pathPattern, Path basePath) {
		return resolve(pathPattern, basePath, Function.identity());
	}
	
	public static <T> Stream<T> resolve(Path pathPattern, Path basePath, Function<? super Path, ? extends T> mapper) {
		Objects.requireNonNull(pathPattern, "path");
		if(basePath == null) {
			basePath = pathPattern.isAbsolute() ? Paths.get("/") : Paths.get("./");
		}
		if(!Files.isDirectory(basePath) || !Files.exists(basePath)) {
			return Stream.of();
		}
		
		int pathNameCount = pathPattern.getNameCount();
		String segment = pathPattern.getName(0).toString();

		final Path subPath;
		final String segmentPattern;
		final String nextSegmentPattern;
		if(segment.equals("**")) {
			segmentPattern = null;
			// we match everything AND if the segment match the next segment then we move on
			String nextSegment = null;
			int nextSegmentIndex = 1;
			while(nextSegmentIndex < pathNameCount) {
				nextSegment = pathPattern.getName(nextSegmentIndex).toString();
				if(!nextSegment.equals("**")) {
					break;
				}
				nextSegmentIndex++;
			}
			
			if(nextSegmentIndex < pathNameCount) {
				nextSegmentPattern = nextSegment.replace("*", ".*").replace("?", ".?");
				subPath = pathPattern.subpath(nextSegmentIndex - 1, pathNameCount);
			}
			else {
				nextSegmentPattern = null;
				subPath = pathPattern;
			}
		}
		else {
			nextSegmentPattern = null;
			segmentPattern = segment.replace("*", ".*").replace("?", ".?");
			if(pathNameCount > 1) {
				subPath = pathPattern.subpath(1, pathNameCount);
			}
			else {
				subPath = null;
			}
		}
		
		try {
			return Files.list(basePath)
				.flatMap(nextPath -> {
					String fileName = nextPath.getFileName().toString();
					if(segmentPattern != null) {
						if(fileName.matches(segmentPattern)) {
							if(subPath != null) {
								return resolve(subPath, nextPath, mapper);
							}
							else /*if(!Files.isDirectory(nextPath))*/ {
								return Stream.of(mapper.apply(nextPath));
							}
						}
					}
					else if(nextSegmentPattern != null) {
						if(fileName.matches(nextSegmentPattern)) {
							int subPathNameCount = subPath.getNameCount();
							Path nextSubPath = subPathNameCount > 2 ? subPath.subpath(2, subPathNameCount) : null;
							if(nextSubPath != null) {
								return resolve(nextSubPath, nextPath, mapper);
							}
							else {
								if(!Files.isDirectory(nextPath)) {
									return Stream.of(mapper.apply(nextPath));
								}
								else {
									return resolve(subPath, nextPath, mapper);
								}
								
								/*try {
									if(!Files.isDirectory(nextPath) || Files.list(nextPath).count() == 0) {
										return Stream.of(mapper.apply(nextPath));
									}
									else {
										return resolve(subPath, nextPath, mapper);
									}
								} 
								catch (IOException e) {
									throw new ResourceException("Error resolving paths from pattern: " + pathPattern, e);
								}*/
							}
							
						}
						else if(Files.isDirectory(nextPath)) {
							return resolve(subPath, nextPath, mapper);
						}
					}
					else {
						if(!Files.isDirectory(nextPath)) {
							return Stream.of(mapper.apply(nextPath));
						}
						else {
							return resolve(subPath, nextPath, mapper);
						}
					}
					return Stream.of();
				});
		} 
		catch (IOException e) {
			throw new ResourceException("Error resolving paths from pattern: " + pathPattern, e);
		}
	}
}
