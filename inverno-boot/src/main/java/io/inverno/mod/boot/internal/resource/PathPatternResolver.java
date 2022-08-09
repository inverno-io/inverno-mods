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

import io.inverno.mod.base.resource.ResourceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * <p>
 * A path pattern resolvers which can resolve paths matching a given path
 * pattern from a base path.
 * </p>
 * 
 * <p>Paths are resolved using the following rules:</p>
 * <ul>
 *   <li>? matches one character</p>
 *   <li>* matches zero or more characters</p>
 *   <li>* matches zero or more characters</p>
 *   <li>** matches zero or more directories in a path</p>
 * </ul>
 * 
 * <p>For instance:</p>
 * 
 * <pre>{@code
 * // Returns: /base/test1/a, /base/test1/a/b, /base/test2/c...
 * Stream<Path> paths = PathPatternResolver.resolve(Path.of("/test?/{@literal **}/*"), Path.of("/base"));
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
final class PathPatternResolver {
	
	private PathPatternResolver() {}
	
	/**
	 * <p>
	 * Resolves paths against the specified path pattern.
	 * </p>
	 * 
	 * <p>
	 * The base path is deduced from the path pattern: if it is absolute, the base
	 * path is the root directory (ie. <code>'/'</code>), otherwise it is the
	 * current directory (ie. <code>'./'</code>).
	 * </p>
	 * 
	 * @param pathPattern a path pattern
	 * 
	 * @return a stream of paths
	 */
	public static Stream<Path> resolve(Path pathPattern) {
		return resolve(pathPattern, null, Function.identity());
	}

	/**
	 * <p>
	 * Resolves paths against the specified path pattern mapping resolved paths
	 * using the specified mapper.
	 * </p>
	 * 
	 * <p>
	 * The base path is deduced from the path pattern: if it is absolute, the base
	 * path is the root directory (ie. <code>'/'</code>), otherwise it is the
	 * current directory (ie. <code>'./'</code>).
	 * </p>
	 * 
	 * @param <T>         the type of object returned by the mapper
	 * @param pathPattern a path pattern
	 * @param mapper      a path mapper
	 * 
	 * @return a stream of T
	 */
	public static <T> Stream<T> resolve(Path pathPattern, Function<? super Path, ? extends T> mapper) {
		return resolve(pathPattern, null, mapper);
	}
	
	/**
	 * <p>
	 * Resolves paths against the specified path pattern from the specified base path.
	 * </p>
	 * 
	 * @param pathPattern a path pattern
	 * @param basePath a base path
	 * 
	 * @return a stream of paths
	 */
	public static Stream<Path> resolve(Path pathPattern, Path basePath) {
		return resolve(pathPattern, basePath, Function.identity());
	}
	
	/**
	 * <p>
	 * Resolves paths against the specified path pattern from the specified base
	 * path mapping resolved paths using the specified mapper.
	 * </p>
	 * 
	 * @param <T>         the type of object returned by the mapper
	 * @param pathPattern a path pattern
	 * @param basePath    a base path
	 * @param mapper      a path mapper
	 * 
	 * @return a stream of T
	 */
	public static <T> Stream<T> resolve(Path pathPattern, Path basePath, Function<? super Path, ? extends T> mapper) {
		Objects.requireNonNull(pathPattern, "path");
		if(basePath == null) {
			basePath = pathPattern.isAbsolute() ? Path.of("/") : Path.of("./");
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
