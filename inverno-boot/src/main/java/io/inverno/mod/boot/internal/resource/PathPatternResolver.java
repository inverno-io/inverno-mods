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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * A path pattern resolvers which can resolve paths matching a given path pattern from a base path.
 * </p>
 *
 * <p>
 * Paths are resolved using the following rules:</p>
 * <ul>
 * <li>? matches one character</li>
 * <li>* matches zero or more characters</li>
 * <li>* matches zero or more characters</li>
 * <li>** matches zero or more directories in a path</li>
 * </ul>
 *
 * <p>
 * For instance:</p>
 *
 * <pre>{@code
 * // Returns: /base/test1/a, /base/test1/a/b, /base/test2/c...
 * Stream<Path> paths = PathPatternResolver.resolve(Path.of("/test?/{@literal **}/*"), Path.of("/base"));
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
final class PathPatternResolver {
	
	private static final boolean WINDOWS_PATH = File.separatorChar == '\\';
	
	private PathPatternResolver() {}
	
	/**
	 * <p>
	 * Resolves paths against the specified path pattern.
	 * </p>
	 *
	 * <p>
	 * The base path is deduced from the path pattern: if it is absolute, the base path is the root directory (ie. <code>'/'</code>), otherwise it is the current directory (ie. <code>'./'</code>).
	 * </p>
	 *
	 * @param pathPattern a path pattern
	 *
	 * @return a stream of paths
	 */
	public static Stream<Path> resolve(String pathPattern) {
		return resolve(pathPattern, null, Function.identity());
	}

	/**
	 * <p>
	 * Resolves paths against the specified path pattern mapping resolved paths using the specified mapper.
	 * </p>
	 *
	 * <p>
	 * The base path is deduced from the path pattern: if it is absolute, the base path is the root directory (ie. <code>'/'</code>), otherwise it is the current directory (ie. <code>'./'</code>).
	 * </p>
	 *
	 * @param <T> the type of object returned by the mapper
	 * @param pathPattern a path pattern
	 * @param mapper a path mapper
	 *
	 * @return a stream of T
	 */
	public static <T> Stream<T> resolve(String pathPattern, Function<? super Path, ? extends T> mapper) {
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
	public static Stream<Path> resolve(String pathPattern, Path basePath) {
		return resolve(pathPattern, basePath, Function.identity());
	}
	
	/**
	 * <p>
	 * Resolves paths against the specified path pattern from the specified base path mapping resolved paths using the specified mapper.
	 * </p>
	 *
	 * @param <T> the type of object returned by the mapper
	 * @param pathPattern a path pattern
	 * @param basePath a base path
	 * @param mapper a path mapper
	 *
	 * @return a stream of T
	 */
	public static <T> Stream<T> resolve(String pathPattern, Path basePath, Function<? super Path, ? extends T> mapper) {
		return resolve(new PathPattern(Objects.requireNonNull(pathPattern, "path")), basePath, mapper);
	}
		
	/**
	 * <p>
	 * Resolves paths against the specified path pattern from the specified base path mapping resolved paths using the specified mapper.
	 * </p>
	 *
	 * @param <T> the type of object returned by the mapper
	 * @param pathPattern a path pattern
	 * @param basePath a base path
	 * @param mapper a path mapper
	 *
	 * @return a stream of T
	 */
	private static <T> Stream<T> resolve(PathPattern pathPattern, Path basePath, Function<? super Path, ? extends T> mapper) {
		if(pathPattern.getNameCount() == 0) {
			return Stream.of();
		}
		if(basePath == null) {
			basePath = pathPattern.isAbsolute() ? Path.of("/") : Path.of("./");
		}
		if(!Files.exists(basePath)) {
			return Stream.of();
		}
		basePath = basePath.toAbsolutePath().normalize();
		if(pathPattern.isAbsolute()) {
			if(pathPattern.getRoot().equalsIgnoreCase(basePath.getRoot().toString()) || pathPattern.getRoot().equals("\\")) {
				// if path pattern is absolute we must match from the absolute base path but only list files from the base path
				// we must advance as for the relative path matching one segment at a time from the root base path segment
				
				// objective is to reduce the path pattern up to the base path or return empty result if base path doesn't match
				
				// so we need: pathPattern, basePath and that's it
				pathPattern = relativizePathPattern(pathPattern, basePath);
				if(pathPattern == null) {
					return Stream.of();
				}
				else if(pathPattern.getNameCount() == 0) {
					return Stream.of(mapper.apply(basePath));
				}
			}
			else {
				return Stream.of();
			}
		}
		else {
			if(!Files.isDirectory(basePath)) {
				return Stream.of();
			}
			
			int upCount = 0;
			while(pathPattern.getNameCount() > 0 && pathPattern.getName(0).equals("..")) {
				pathPattern = pathPattern.subpath(1, pathPattern.getNameCount());
				upCount++;
			}
			
			if(upCount > 0) {
				int basePathNameCount = basePath.getNameCount();
				if(basePathNameCount < upCount) {
					return Stream.of();
				}
				basePath = basePath.subpath(0, basePathNameCount - upCount);
			}
		}
		
		int pathNameCount = pathPattern.getNameCount();
		String segment = pathPattern.getName(0);

		final PathPattern subPath;
		final String segmentPattern;
		final String nextSegmentPattern;
		if(segment.equals("**")) {
			segmentPattern = null;
			// we match everything AND if the segment match the next segment then we move on
			String nextSegment = null;
			int nextSegmentIndex = 1;
			while(nextSegmentIndex < pathNameCount) {
				nextSegment = pathPattern.getName(nextSegmentIndex);
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
							else {
								return Stream.of(mapper.apply(nextPath));
							}
						}
					}
					else if(nextSegmentPattern != null) {
						if(fileName.matches(nextSegmentPattern)) {
							int subPathNameCount = subPath.getNameCount(); // subPath can't be null when nextSegmentPattern is not null
							PathPattern nextSubPath = subPathNameCount > 2 ? subPath.subpath(2, subPathNameCount) : null;
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
	
	/**
	 * <p>
	 * Returns the path pattern relative to the specified base path.
	 * </p>
	 * 
	 * <p>
	 * This method only returns a value when the path pattern and the base path shares a common root i.e. when there's a match possibility otherwise {@code null} is returned.
	 * </p>
	 * 
	 * @param pathPattern
	 * @param basePath
	 * @return the relative path pattern or null if the specified pattern can't match the base path
	 */
	private static PathPattern relativizePathPattern(PathPattern pathPattern, Path basePath) {
		if(basePath.getNameCount() == 0) {
			return pathPattern.subpath(0, pathPattern.getNameCount());
		}
		int pathNameCount = pathPattern.getNameCount();
		String segment = pathPattern.getName(0);

		final PathPattern subPath;
		final String segmentPattern;
		final String nextSegmentPattern;
		if(segment.equals("**")) {
			segmentPattern = null;
			// we match everything AND if the segment match the next segment then we move on
			String nextSegment = null;
			int nextSegmentIndex = 1;
			while(nextSegmentIndex < pathNameCount) {
				nextSegment = pathPattern.getName(nextSegmentIndex);
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
				subPath = pathPattern.subpath(1, 1);
			}
		}
		
		// does it match
		String segmentName = basePath.getName(0).toString();
		if(segmentPattern != null) {
			if(segmentName.matches(segmentPattern)) {
				int basePathSize = basePath.getNameCount();
				if(basePathSize > 1) {
					return relativizePathPattern(subPath, basePath.subpath(1, basePathSize));
				}
				else {
					return subPath;
				}
			}
		}
		else if(nextSegmentPattern != null) {
			if(segmentName.matches(nextSegmentPattern)) {
				int subPathNameCount = subPath.getNameCount(); // subPath can't be null when nextSegmentPattern is not null
				PathPattern nextSubPath = subPathNameCount > 2 ? subPath.subpath(2, subPathNameCount) : null;
				if(nextSubPath != null) {
					int basePathSize = basePath.getNameCount();
					if(basePathSize > 1) {
						return relativizePathPattern(nextSubPath, basePath.subpath(1, basePathSize));
					}
					else {
						return nextSubPath;
					}
				}
				else {
					int basePathSize = basePath.getNameCount();
					if(basePathSize > 1) {
						return relativizePathPattern(subPath, basePath.subpath(1, basePathSize));
					}
					else {
						return subPath;
					}
				}
			}
			else {
				int basePathSize = basePath.getNameCount();
				if(basePathSize > 1) {
					return relativizePathPattern(subPath, basePath.subpath(1, basePathSize));
				}
				else {
					return subPath;
				}
			}
		}
		else {
			int basePathSize = basePath.getNameCount();
			if(basePathSize > 1) {
				return relativizePathPattern(subPath, basePath.subpath(1, basePathSize));
			}
			else {
				return subPath;
			}
		}
		return null;
	}
	
	/**
	 * <p>
	 * Represents a path pattern as described in {@link PathPatternResolver}.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private static class PathPattern {
		
		private final List<String> segments;
		
		private final String root;
		
		/**
		 * <p>
		 * Creates a path pattern from the specified pattern string.
		 * </p>
		 * 
		 * @param pattern a path pattern string
		 * 
		 * @throws IllegalArgumentException if the specified pattern is invalid
		 */
		public PathPattern(String pattern) throws IllegalArgumentException {
			String[] splitPattern = pattern.split("/");
			this.segments = new ArrayList<>(splitPattern.length);
			String root_tmp = null;
			for(int i=0;i<splitPattern.length;i++) {
				String segment = splitPattern[i].trim();
				if(segment.isEmpty()) {
					if(i == 0) {
						if(WINDOWS_PATH) {
							if(splitPattern.length > 1) {
								if(splitPattern[1].isEmpty()) {
									// UNC?
									String hostname = null;
									String sharename = null;
									if(splitPattern.length > 3) {
										hostname = splitPattern[2];
										sharename = splitPattern[3];
									}
									if(hostname == null || hostname.isEmpty()) {
										throw new IllegalArgumentException("UNC hostname is missing");
									}
									if(sharename == null || sharename.isEmpty()) {
										throw new IllegalArgumentException("UNC sharename is missing");
									}
									root_tmp = "\\\\" + splitPattern[2] + "\\" + splitPattern[3];
								}
								else if(splitPattern[1].length() == 2 && isLetter(splitPattern[1].charAt(0)) && splitPattern[1].charAt(1) == ':') {
									root_tmp = splitPattern[1] + "\\";
								}
								else {
									root_tmp = "\\";
								}
							}
						}
						else {
							root_tmp = "/";
						}
					}
				}
				else if(segment.equals("..")) {
					if(this.segments.isEmpty()) {
						if(root_tmp != null) {
							throw new IllegalArgumentException("Can't create path above root path");
						}
						this.segments.add(segment);
					}
					else if(this.segments.get(this.segments.size() - 1).equals("..")) {
						this.segments.add(segment);
					}
					else {
						this.segments.remove(this.segments.size() - 1);
					}
				}
				else if(!segment.equals(".")) {
					if(WINDOWS_PATH && i == 0 && segment.length() == 2 && isLetter(segment.charAt(0)) && segment.charAt(1) == ':') {
						root_tmp = segment + "\\";
					}
					else {
						this.segments.add(segment);
					}
				}
			}
			this.root = root_tmp;
		}
		
		/**
		 * <p>
		 * Creates a path pattern from the specified list of segments.
		 * </p>
		 * 
		 * @param segments a list of segments
		 */
		private PathPattern(List<String> segments) {
			this.segments = segments;
			this.root = null;
		}
		
		/**
		 * <p>
		 * Determines whether the specified character is a letter ({@code a-zA-Z}).
		 * </p>
		 * 
		 * @param c a character
		 * 
		 * @return true if the character is a letter, false otherwise
		 */
		private static boolean isLetter(char c) {
			return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
		}
		
		/**
		 * <p>
		 * Returns the path pattern root.
		 * </p>
		 * 
		 * @return the path pattern root or null if the path pattern is relative
		 */
		public String getRoot() {
			return this.root;
		}
		
		/**
		 * <p>
		 * Determines whether the path pattern is absolute.
		 * </p>
		 * 
		 * <p>
		 * A path pattern is absolute when the root is {@code null}.
		 * </p>
		 * 
		 * @return true if the path pattern is absolute, false otherwise
		 */
		public boolean isAbsolute() {
			return this.root != null;
		}
		
		/**
		 * <p>
		 * Returns the number of segments composing the path pattern.
		 * </p>
		 * 
		 * <p>
		 * Note that the root is not listed among the segments.
		 * </p>
		 * 
		 * @return the number of segments
		 */
		public int getNameCount() {
			return this.segments.size();
		}
		
		/**
		 * <p>
		 * Returns the segment at the specified index.
		 * </p>
		 * 
		 * @param index an index
		 * 
		 * @return a segment
		 */
		public String getName(int index) {
			return this.segments.get(index);
		}
		
		/**
		 * <p>
		 * Returns a relative path oattern that is a subsequence of the segments of this path pattern.
		 * </p>
		 * 
		 * @param beginIndex the index of the first segment (inclusive)
		 * @param endIndex   the index of the last segment (exclusive)
		 * 
		 * @return a new path pattern
		 */
		public PathPattern subpath(int beginIndex, int endIndex) {
			return new PathPattern(this.segments.subList(beginIndex, endIndex));
		}

		@Override
		public String toString() {
			return (this.isAbsolute() ? this.root.replace('\\', '/') : "") +this.segments.stream().collect(Collectors.joining("/"));
		}
	}
}
