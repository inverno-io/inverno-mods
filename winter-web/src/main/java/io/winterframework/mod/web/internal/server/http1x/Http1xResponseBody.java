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
package io.winterframework.mod.web.internal.server.http1x;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Optional;

import org.reactivestreams.Publisher;

import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.server.GenericResponseBody;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
class Http1xResponseBody extends GenericResponseBody {

	private static final int MAX_FILE_REGION_SIZE = 1024 * 1024;
	
	private Publisher<FileRegion> fileRegionData;
	
	/**
	 * @param response
	 */
	public Http1xResponseBody(Http1xResponse response) {
		super(response);
	}
	
	public Optional<Publisher<FileRegion>> getFileRegionData() {
		return Optional.ofNullable(this.fileRegionData);
	}
	
	@Override
	public Resource resource() {
		if(!((Http1xResponse)this.response).supportsFileRegion()) {
			return super.resource();
		}
		else {
			if(this.resourceBody == null) {
				this.resourceBody = new Http1xResourceResponseBody();
			}
			return this.resourceBody;
		}
	}

	private class Http1xResourceResponseBody extends GenericResponseBody.GenericResourceResponseBody {

		@Override
		public Response<Resource> data(io.winterframework.mod.commons.resource.Resource resource) {
//			fileregion is supported when we are not using ssl and we do not compress content
			try {
				if(resource.isFile()) {
					// We need to create the file region and then send an empty response
					// The Http1xServerExchange should then complete and check whether there is a file region or not
					this.populateHeaders(resource);
					
					FileChannel fileChannel = (FileChannel)resource.openReadableByteChannel().orElseThrow(() -> new InternalServerErrorException("Resource " + resource + " is not readable"));
					long size = resource.size();
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
								Exceptions.propagate(e);
							}
						});
					Http1xResponseBody.this.setData(Flux.empty());
				}
				else {
					super.data(resource);
				}
			} 
			catch (IOException e) {
				throw new InternalServerErrorException("Error while reading resource " + resource, e);
			}
			return Http1xResponseBody.this.response.<ResponseBody.Resource>map(responseBody -> responseBody.resource());
		}
	}
}
