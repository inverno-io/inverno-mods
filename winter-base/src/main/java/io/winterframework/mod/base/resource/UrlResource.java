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
package io.winterframework.mod.base.resource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Highly non-performing
 * 
 * @author jkuhn
 *
 */
public class UrlResource extends AbstractAsyncResource {

	private URI uri;
	
	private URL url;
	
	private Optional<URLConnection> connection;
	
	public UrlResource(URI uri) {
		this(uri, null);
	}
	
	public UrlResource(URL url) throws IllegalArgumentException {
		this(url, null);
	}
	
	public UrlResource(URI uri, MediaTypeService mediaTypeService) throws IllegalArgumentException {
		super(mediaTypeService);
		this.uri = Objects.requireNonNull(uri.normalize());
		try {
			this.url = this.uri.toURL();
		} 
		catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid URI", e);
		}
	}
	
	public UrlResource(URL url, MediaTypeService mediaTypeService) throws IllegalArgumentException {
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
	public boolean isFile() {
		return false;
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
	public UrlResource resolve(URI uri) {
		return new UrlResource(this.uri.resolve(uri.normalize()), this.getMediaTypeService());
	}
}
