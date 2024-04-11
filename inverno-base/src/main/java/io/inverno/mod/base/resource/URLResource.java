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
package io.inverno.mod.base.resource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A {@link Resource} implementation that identifies resources by a URL that looks up data by opening a {@link URLConnection}.
 * </p>
 * 
 * <p>
 * A typical usage is:
 * </p>
 * 
 * <pre>{@code
 * URLResource resource = new URLResource(URI.create("http://host/path/to/resource"));
 * ...
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AsyncResource
 */
public class URLResource extends AbstractAsyncResource {

	private final URI uri;
	
	private final URL url;
	
	private String filename;
	
	private Optional<URLConnection> connection;
	
	private ResourceException resolutionError;
	
	/**
	 * <p>
	 * Creates a URL resource with the specified URI.
	 * </p>
	 * 
	 * @param uri the resource URI
	 * 
	 * throws {@link IllegalArgumentException} if the URI can't be converted to a URL
	 */
	public URLResource(URI uri) throws IllegalArgumentException {
		this(uri, null);
	}
	
	/**
	 * <p>
	 * Creates a URL resource with the specified URL.
	 * </p>
	 * 
	 * @param url the resource URL
	 * 
	 * throws {@link IllegalArgumentException} if the URL can't be converted to a URI
	 */
	public URLResource(URL url) throws IllegalArgumentException {
		this(url, null);
	}
	
	/**
	 * <p>
	 * Creates a URL resource with the specified URI and media type service.
	 * </p>
	 * 
	 * @param uri the resource URI
	 * @param mediaTypeService a media type service
	 * 
	 * throws {@link IllegalArgumentException} if the URI can't be converted to a URL
	 */
	public URLResource(URI uri, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		super(mediaTypeService);
		this.uri = Objects.requireNonNull(uri.normalize());
		try {
			this.url = this.uri.toURL();
		} 
		catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid URI", e);
		}
	}
	
	/**
	 * <p>
	 * Creates a URL resource with the specified URL and media type service.
	 * </p>
	 * 
	 * @param url the resource URL
	 * @param mediaTypeService a media type service
	 * 
	 * throws {@link IllegalArgumentException} if the URL can't be converted to a URI
	 */
	public URLResource(URL url, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		super(mediaTypeService);
		this.url = Objects.requireNonNull(url);
		try {
			this.uri = url.toURI();
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL", e);
		}
	}

	private Optional<URLConnection> resolve() {
		if(this.resolutionError != null) {
			throw this.resolutionError;
		}
		if(this.connection == null) {
			try {
				this.connection = Optional.of(this.url.openConnection());
			}
			catch (IOException e) {
				this.resolutionError = new ResourceException(e);
				throw this.resolutionError;
			}
		}
		return this.connection;
	}
	
	@Override
	public URI getURI() {
		return this.uri;
	}
	
	@Override
	public String getFilename() throws ResourceException {
		if(this.filename == null) {
			String path = this.uri.getPath();
			int lastSlashIndex = path.lastIndexOf("/");
			if(lastSlashIndex != -1) {
				this.filename = path.substring(lastSlashIndex + 1);
			}
		}
		return this.filename;
	}
	
	@Override
	public Optional<Boolean> exists() throws ResourceException {
		return Optional.empty();
	}
	
	@Override
	public Optional<Boolean> isFile() throws ResourceException {
		return Optional.empty();
	}

	@Override
	public Optional<FileTime> lastModified() throws ResourceException {
		return this.resolve().map(c -> {
			long lastModified = c.getLastModified();
			if(lastModified > 0) {
				return FileTime.fromMillis(lastModified);
			}
			return null;
		});
	}
	
	@Override
	public Optional<Long> size() throws ResourceException {
		return this.resolve().map(c -> {
			long contentLength = c.getContentLengthLong();
			if(contentLength >= 0) {
				return contentLength;
			}
			return null;
		});
	}

	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() throws ResourceException {
		return this.resolve().map(c -> {
			try {
				return Channels.newChannel(c.getInputStream());
			}
			catch (IOException e) {
				throw new ResourceException(e);
			}
			finally {
				// We must reset the connection to allow for future read since 1 connection is for 1 request
				this.connection = null;
			}
		});
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) throws ResourceException {
		return this.resolve().map(c -> {
			try {
				return Channels.newChannel(c.getOutputStream());
			}
			catch (IOException e) {
				throw new ResourceException(e);
			}
			finally {
				// We must reset the connection to allow for future write since 1 connection is for 1 request
				this.connection = null;
			}
		});
	}
	
	@Override
	public boolean delete() throws ResourceException {
		return false;
	}

	@Override
	public URLResource resolve(Path path) throws ResourceException {
		try {
			String resolvedPath;
			if(IS_WINDOWS_PATH && "file".equalsIgnoreCase(this.uri.getScheme())) {
				if(this.uri.getRawQuery() != null || this.uri.getRawFragment() != null) {
					resolvedPath = "/" + pathToSanitizedString(Path.of(new URI("file", Path.of(this.uri).resolve(path).toUri().getRawSchemeSpecificPart(), null)).resolve(path));
				}
				else {
					resolvedPath = "/" + pathToSanitizedString(Path.of(this.uri).resolve(path));
				}
			}
			else {
				resolvedPath = pathToSanitizedString(Path.of(this.uri.getRawPath()).resolve(path));
			}
			URI resolvedUri = new URI(this.uri.getScheme(), this.uri.getAuthority(), resolvedPath, this.uri.getQuery(), this.uri.getFragment());
			URLResource resolvedResource = new URLResource(resolvedUri, this.getMediaTypeService());
			resolvedResource.setExecutor(this.getExecutor());
			return resolvedResource;
		} 
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid path", e);
		}
	}
	
	@Override
	public void close() {
	}
}
