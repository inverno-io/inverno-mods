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
package io.inverno.mod.base.resource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Utility methods to optimally create {@link FileSystem} instances.
 * </p>
 *
 * <p>
 * File systems returned by these methods are reference counted which means that for a given file system URI the same instance is returned with an incremented reference count until all consumers close
 * the file system.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
final class ReferenceCountedFileSystems {

	private static final Map<URI, ReferenceCountedFileSystem> FILE_SYSTEMS = new HashMap<>();
	
	private ReferenceCountedFileSystems() {}
	
	/**
	 * <p>
	 * Creates or gets the file system identified by the specified URI.
	 * </p>
	 *
	 * @param uri the file system URI
	 *
	 * @return a file system
	 *
	 * @throws IOException if an I/O error occurs creating the file system
	 */
	public static FileSystem getFileSystem(URI uri) throws IOException {
		return getFileSystem(uri, Map.of());
	}
	
	/**
	 * <p>
	 * Creates or gets the file system identified by the specified URI.
	 * </p>
	 *
	 * @param uri the file system URI
	 * @param env a map of provider specific properties to configure the file system; may be empty
	 *
	 * @return a file system
	 *
	 * @throws IOException if an I/O error occurs creating the file system
	 */
	public static FileSystem getFileSystem(URI uri, Map<String,?> env) throws IOException {
		synchronized (FILE_SYSTEMS) {
			URI normalizedURI = uri.normalize();
			ReferenceCountedFileSystem fs = FILE_SYSTEMS.get(normalizedURI);
			if(fs == null) {
				fs = new ReferenceCountedFileSystem(normalizedURI, FileSystems.newFileSystem(uri, env));
				FILE_SYSTEMS.put(normalizedURI, fs);
			}
			fs.retain();
			return fs;
		}
	}
	
	/**
	 * <p>
	 * A reference counted file system wrapper.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private static class ReferenceCountedFileSystem extends FileSystem {

		private final URI uri;
		
		private final FileSystem fs;
		
		private volatile int refCount;
		
		private boolean closed;
		
		public ReferenceCountedFileSystem(URI uri, FileSystem fs) {
			this.uri = uri;
			this.fs = fs;
		}
		
		public ReferenceCountedFileSystem retain() {
			synchronized (this) {
				if(this.closed) {
					throw new ClosedFileSystemException();
				}
				this.refCount++;
			}
			return this;
		}
		
		public ReferenceCountedFileSystem release() throws IOException {
			synchronized(this) {
				if(this.closed) {
					throw new ClosedFileSystemException();
				}
				this.closed = this.refCount-- == 0;
			}
			if(this.closed) {
				FILE_SYSTEMS.remove(this.uri);
				this.fs.close();
			}
			return this;
		}
		
		@Override
		public FileSystemProvider provider() {
			return this.fs.provider();
		}

		@Override
		public void close() throws IOException {
			this.release();
		}

		@Override
		public boolean isOpen() {
			return !this.closed;
		}

		@Override
		public boolean isReadOnly() {
			return this.fs.isReadOnly();
		}

		@Override
		public String getSeparator() {
			return this.fs.getSeparator();
		}

		@Override
		public Iterable<Path> getRootDirectories() {
			return this.fs.getRootDirectories();
		}

		@Override
		public Iterable<FileStore> getFileStores() {
			return this.fs.getFileStores();
		}

		@Override
		public Set<String> supportedFileAttributeViews() {
			return this.fs.supportedFileAttributeViews();
		}

		@Override
		public Path getPath(String first, String... more) {
			return this.fs.getPath(first, more);
		}

		@Override
		public PathMatcher getPathMatcher(String syntaxAndPattern) {
			return this.fs.getPathMatcher(syntaxAndPattern);
		}

		@Override
		public UserPrincipalLookupService getUserPrincipalLookupService() {
			return this.fs.getUserPrincipalLookupService();
		}

		@Override
		public WatchService newWatchService() throws IOException {
			return this.fs.newWatchService();
		}
	}
}
