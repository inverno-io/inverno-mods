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
package io.inverno.mod.http.server.internal.http1x;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Optional;

import org.reactivestreams.Publisher;

import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.inverno.mod.base.resource.ZipResource;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.http.server.internal.GenericResponseBody;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

/**
 * <p>
 * HTTP1.x {@link ResponseBody} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see GenericResponseBody
 */
class Http1xResponseBody extends GenericResponseBody {

	private static final int MAX_FILE_REGION_SIZE = 1024 * 1024;
	
	private Publisher<FileRegion> fileRegionData;
	
	/**
	 * <p>
	 * Creates a HTTP1.x server response body for the specified response.
	 * </p>
	 * 
	 * @param response the HTTP1.x response
	 */
	public Http1xResponseBody(Http1xResponse response) {
		super(response);
	}
	
	/**
	 * <p>
	 * Returns the file region data publisher to send when present instead of the
	 * regular payload data publisher.
	 * </p>
	 * 
	 * @return an optional returning a file region publisher or an empty optional if
	 *         no file region has been set in the response
	 */
	public Publisher<FileRegion> getFileRegionData() {
		return this.fileRegionData;
	}
	
	@Override
	public Resource resource() {
		// fileregion is supported when we are not using ssl and we do not compress content
		if(((Http1xResponse)this.response).supportsFileRegion()) {
			if(this.resourceData == null) {
				this.resourceData = new Http1xResponseBodyResourceData();
			}
			return this.resourceData;
		}
		return super.resource();
	}

	/**
	 * <p>
	 * {@link ResponseBody.Resource} implementation that uses file region when
	 * available.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see FileRegion
	 */
	private class Http1xResponseBodyResourceData extends GenericResponseBody.GenericResponseBodyResourceData {

		@Override
		public void value(io.inverno.mod.base.resource.Resource resource) {
			// In case of file resources we should always be able to determine existence
			// For other resources with a null exists we can still try, worst case scenario: 
			// internal server error
			if(resource.exists().orElse(true)) {
				this.populateHeaders(resource);

				// Only regular file resources supports zero-copy
				// It seems FileRegion does not support Zip files, I saw different behavior between JDK<15 and above
				if(resource.isFile().orElse(false) && !(resource instanceof ZipResource)) {
					// We need to create the file region and then send an empty response
					// The Http1xServerExchange should then complete and check whether there is a file region or not
					FileChannel fileChannel = (FileChannel)resource.openReadableByteChannel().orElseThrow(() -> new InternalServerErrorException("Resource is not readable: " + resource.getURI()));
					
					long size = resource.size().get();
					int count = (int)Math.ceil((float)size / (float)MAX_FILE_REGION_SIZE);
					
					// We need to add an extra element in order to control when the flux terminates so we can properly close the file channel
					Http1xResponseBody.this.fileRegionData = Flux.range(0, count + 1) 
						.filter(index -> index < count)
						.map(index -> {
							long position = index * MAX_FILE_REGION_SIZE;
							FileRegion region = new DefaultFileRegion(fileChannel, position, Math.min(size - position, MAX_FILE_REGION_SIZE));
							region.retain();
							return region;
						})
						.doFinally(sgn -> {
							try {
								fileChannel.close();
							} 
							catch (IOException e) {
								throw Exceptions.propagate(e);
							}
						});
					Http1xResponseBody.this.setData(Flux.empty());
				}
				else {
					Http1xResponseBody.this.setData(resource.read().orElseThrow(() -> new InternalServerErrorException("Resource is not readable: " + resource.getURI())));
				}
			}
			else {
				throw new NotFoundException();
			}
		}
	}
}
