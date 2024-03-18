/*
 * Copyright 2022 Jeremy KUHN
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
import io.inverno.mod.http.client.internal.HttpConnectionRequestBody;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Optional;
import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

/**
 * <p>
 * HTTP/1.x request body with support for {@link FileRegion} data.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http1xRequestBody extends HttpConnectionRequestBody {
	
	private static final int MAX_FILE_REGION_SIZE = 1024 * 1024;

	private final EndpointRequestBody endpointRequestBody;
	private final boolean supportsFileRegion;
	
	private Optional<Publisher<FileRegion>> fileRegionData;

	/**
	 * <p>
	 * Creates an Http1x request body.
	 * </p>
	 *
	 * @param endpointRequestBody the original endpoint request body
	 * @param supportsFileRegion  true if the connection supports file region, false otherwise
	 */
	public Http1xRequestBody(EndpointRequestBody endpointRequestBody, boolean supportsFileRegion) {
		super(endpointRequestBody.getData());
		this.endpointRequestBody = endpointRequestBody;
		this.supportsFileRegion = supportsFileRegion;
	}
	
	/**
	 * <p>
	 * Returns the request body file region data publisher.
	 * </p>
	 * 
	 * @return a publisher of file region data or null if the body is not made of file region data.
	 */
	public Optional<Publisher<FileRegion>> getFileRegionData() {
		if(!this.supportsFileRegion || this.endpointRequestBody.getResource() == null) {
			return Optional.empty();
		}
		
		if(this.fileRegionData == null) {
			Resource resource = this.endpointRequestBody.getResource();
		
			// We know resource exists, this has been checked in EndpointRequestBody
			// Only regular file resources supports zero-copy
			// It seems FileRegion does not support Zip files, I saw different behavior between JDK<15 and above
			if(resource.isFile().orElse(false) && !(resource instanceof ZipResource)) {
				// We need to create the file region and then send an empty response
				// The Http1xServerExchange should then complete and check whether there is a file region or not
				FileChannel fileChannel = (FileChannel)resource.openReadableByteChannel().orElseThrow(() -> new IllegalArgumentException("Resource is not readable: " + resource.getURI()));

				long size = resource.size().get();
				int count = (int)Math.ceil((float)size / (float)MAX_FILE_REGION_SIZE);

				// We need to add an extra element in order to control when the flux terminates so we can properly close the file channel

				this.fileRegionData = Optional.of(Flux.range(0, count + 1) 
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
					}));
			}
			else {
				this.fileRegionData = Optional.empty();
			}
		}
		return this.fileRegionData;
	}
}
