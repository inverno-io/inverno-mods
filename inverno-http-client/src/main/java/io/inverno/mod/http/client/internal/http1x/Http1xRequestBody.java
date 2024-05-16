/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ZipResource;
import io.inverno.mod.http.client.internal.EndpointRequestBody;
import io.netty.buffer.ByteBuf;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

/**
 * <p>
 * Http/1.x request body with support for {@link FileRegion} data.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xRequestBody {
	
	private static final int MAX_FILE_REGION_SIZE = 1024 * 1024;
	
	private final boolean supportsFileRegion;
	
	private final Publisher<ByteBuf> data;
	private final Publisher<FileRegion> fileRegionData;

	/**
	 * <p>
	 * Creates an Http1x request body.
	 * </p>
	 *
	 * @param endpointRequestBody the originating endpoint request body
	 * @param supportsFileRegion  true if the connection supports file region, false otherwise
	 */
	public Http1xRequestBody(EndpointRequestBody endpointRequestBody, boolean supportsFileRegion) {
		this.supportsFileRegion = supportsFileRegion;
		
		if(this.supportsFileRegion && endpointRequestBody.getResource() != null) {
			Resource resource = endpointRequestBody.getResource();
			
			// We know resource exists, this has been checked in EndpointRequestBody
			// Only regular file resources supports zero-copy
			// It seems FileRegion does not support Zip files, I saw different behavior between JDK<15 and above
			if(resource.isFile().orElse(false) && !(resource instanceof ZipResource)) {
				this.data = Flux.empty();
				// We need to create the file region and then send an empty response
				// The Http1xServerExchange should then complete and check whether there is a file region or not
				FileChannel fileChannel = (FileChannel)resource.openReadableByteChannel().orElseThrow(() -> new IllegalArgumentException("Resource is not readable: " + resource.getURI()));

				long size = resource.size().get();
				int count = (int)Math.ceil((float)size / (float)MAX_FILE_REGION_SIZE);

				// We need to add an extra element in order to control when the flux terminates so we can properly close the file channel
				this.fileRegionData = Flux.range(0, count + 1) 
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
			}
			else {
				this.data = endpointRequestBody.getData();
				this.fileRegionData = null;
			}
		}
		else {
			this.data = endpointRequestBody.getData();
			this.fileRegionData = null;
		}
	}

	/**
	 * <p>
	 * Returns the response body data publisher.
	 * </p>
	 * 
	 * <p>
	 * The data publisher MUST be subscribed to produce a request body.
	 * </p>
	 * 
	 * @return the response body data publisher
	 */
	public Publisher<ByteBuf> getData() {
		return data;
	}
	
	/**
	 * <p>
	 * Returns the file region data publisher.
	 * </p>
	 * 
	 * <p>
	 * The file region data publisher has priority over the request body data publisher and shall be subscribed first when present to produce the request body.
	 * </p>
	 * 
	 * @return the file region data publisher or null
	 */
	public Publisher<FileRegion> getFileRegionData() {
		return this.fileRegionData;
	}
}
