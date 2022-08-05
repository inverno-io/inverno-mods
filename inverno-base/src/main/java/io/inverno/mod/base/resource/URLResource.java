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
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A {@link Resource} implementation that identifies resources by a URL that
 * looks up data by opening a {@link URLConnection}.
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

	private URI uri;
	
	private URL url;
	
	private Optional<URLConnection> connection;
	
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
		if(this.connection == null) {
			try {
				this.connection = Optional.of(this.url.openConnection());
			}
			catch (IOException e) {
				throw new ResourceException(e);
			}
		}
		return this.connection;
	}
	
	@Override
	public String getFilename() {
		String path = this.uri.getPath();
		int lastSlashIndex = path.lastIndexOf("/");
		if(lastSlashIndex != -1) {
			return path.substring(lastSlashIndex + 1);
		}
		else {
			return null;
		}
	}
	
	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public Optional<Boolean> exists() {
		return Optional.empty();
	}
	
	@Override
	public Optional<Boolean> isFile() {
		return Optional.empty();
	}

	@Override
	public Optional<FileTime> lastModified() {
		Optional<URLConnection> c = this.resolve();
		if(c.isPresent()) {
			long lastModified = c.get().getLastModified();
			if(lastModified > 0) {
				return Optional.of(FileTime.fromMillis(lastModified));
			}
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<Long> size() {
		Optional<URLConnection> c = this.resolve();
		if(c.isPresent()) {
			long contentLength = c.get().getContentLengthLong();
			if(contentLength >= 0) {
				return Optional.of(contentLength);
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() {
		Optional<URLConnection> c = this.resolve();
		if(c.isPresent()) {
			try {
				return Optional.of(Channels.newChannel(c.get().getInputStream()));
			}
			catch (IOException e) {
				// The URL is not readable
				// TODO log debug
				return Optional.empty();
			} 
		}
		return Optional.empty();
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) {
		Optional<URLConnection> c = this.resolve();
		if(c.isPresent()) {
			try {
				return Optional.of(Channels.newChannel(c.get().getOutputStream()));
			}
			catch (IOException e) {
				// The URL is not writable
				// TODO log debug
				return Optional.empty();
			}
		}
		return Optional.empty();
	}
	
	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public void close() {
	}
	
	@Override
	public URLResource resolve(Path path) {
		try {
			URI resolvedUri = new URI(this.uri.getScheme(), this.uri.getAuthority(), Paths.get(this.uri.getPath()).resolve(path).toString(), this.uri.getQuery(), this.uri.getFragment());
			URLResource resolvedResource = new URLResource(resolvedUri, this.getMediaTypeService());
			resolvedResource.setExecutor(this.getExecutor());
			return resolvedResource;
		} 
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid path", e);
		}
	}
}
