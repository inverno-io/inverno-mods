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
package io.winterframework.mod.commons.resource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
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
	
	public UrlResource(URI uri) throws IOException {
		this(uri, null);
	}
	
	public UrlResource(URL url) throws IOException, URISyntaxException {
		this(url, null);
	}
	
	protected UrlResource(URI uri, MediaTypeService mediaTypeService) throws IOException {
		super(mediaTypeService);
		this.uri = Objects.requireNonNull(uri.normalize());
		this.url = this.uri.toURL();
	}
	
	protected UrlResource(URL url, MediaTypeService mediaTypeService) throws IOException, URISyntaxException {
		super(mediaTypeService);
		this.url = Objects.requireNonNull(url);
		this.uri = url.toURI();
	}

	private Optional<URLConnection> resolve() throws IOException {
		if(this.connection == null) {
			this.connection = Optional.of(this.url.openConnection());
		}
		return this.connection;
	}
	
	@Override
	public String getFilename() throws IOException {
		String path = this.uri.getSchemeSpecificPart();
		int lastSlashIndex = this.uri.getPath().lastIndexOf("/");
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
	public Boolean exists() throws IOException {
		return null;
	}
	
	@Override
	public boolean isFile() throws IOException {
		return false;
	}

	@Override
	public FileTime lastModified() throws IOException {
		Optional<URLConnection> c = this.resolve();
		if(c.isPresent()) {
			long lastModified = c.get().getLastModified();
			if(lastModified > 0) {
				return FileTime.fromMillis(lastModified);
			}
		}
		return null;
	}
	
	@Override
	public Long size() throws IOException {
		Optional<URLConnection> c = this.resolve();
		if(c.isPresent()) {
			long contentLength = c.get().getContentLengthLong();
			if(contentLength >= 0) {
				return contentLength;
			}
		}
		return null;
	}

	@Override
	public Optional<ReadableByteChannel> openReadableByteChannel() throws IOException {
		Optional<URLConnection> c = this.resolve();
		if(c.isPresent()) {
			try {
				return Optional.of(Channels.newChannel(c.get().getInputStream()));
			}
			catch (UnknownServiceException e) {
				// The URL is not readable
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<WritableByteChannel> openWritableByteChannel(boolean append, boolean createParents) throws IOException {
		Optional<URLConnection> c = this.resolve();
		if(c.isPresent()) {
			try {
				return Optional.of(Channels.newChannel(c.get().getOutputStream()));
			}
			catch (UnknownServiceException e) {
				// The URL is not writable
				return Optional.empty();
			}
		}
		return Optional.empty();
	}
	
	@Override
	public boolean delete() throws IOException {
		return false;
	}

	@Override
	public void close() throws IOException {
	}
	
	@Override
	public Resource resolve(URI uri) throws IOException {
		return new UrlResource(this.uri.resolve(uri.normalize()), this.getMediaTypeService());
	}
}
