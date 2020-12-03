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

import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.server.GenericResponse;
import io.winterframework.mod.web.internal.server.GenericResponseBody;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
class Http1xResponseBody extends GenericResponseBody {

	private boolean supportsFileRegion;
	
	private FileRegion fileRegion;
	
	/**
	 * @param response
	 */
	public Http1xResponseBody(GenericResponse response, boolean supportsFileRegion) {
		super(response);
		this.supportsFileRegion = supportsFileRegion;
	}
	
	public Optional<FileRegion> getFileRegion() {
		return Optional.ofNullable(this.fileRegion);
	}
	
	@Override
	public Resource resource() {
		if(!supportsFileRegion) {
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
			// Http2 doesn't support FileRegion so we have to read the resource and send it to the response data flux
			
//			fileregion is supported when we are not using ssl and we do not compress content
			try {
				if(resource.isFile()) {
					// We need to create the file region and then send an empty response
					// The Http1xServerExchange should then complete and check whether there is a file region or not
					this.populateHeaders(resource);
					Http1xResponseBody.this.fileRegion = new DefaultFileRegion((FileChannel)resource.openReadableByteChannel().orElseThrow(() -> new InternalServerErrorException("Resource " + resource + " is not readable")), 0l, resource.size().longValue());
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
			
			/*try {
				Http1xResponseBody.this.response.headers(h -> {
					if(Http1xResponseBody.this.response.getHeaders().getSize() == null) {
						Long size;
						try {
							size = resource.size();
							if(size != null) {
								h.size(size);
							}
						} 
						catch (IOException e) {
							// TODO maybe a debug log?
						}
					}
					
					if(Http1xResponseBody.this.response.getHeaders().getContentType() == null) {
						try {
							String mediaType = resource.getMediaType();
							if(mediaType != null) {
								h.contentType(mediaType);
							}
						} 
						catch (IOException e) {
							// TODO maybe a debug log? 
						}
					}
				});
				Http1xResponseBody.this.setData(resource.read().orElseThrow(() -> new InternalServerErrorException("Resource " + resource + " is not readable")));
			} 
			catch (IOException e) {
				throw new InternalServerErrorException("Error while reading resource " + resource, e);
			}
			return Http1xResponseBody.this.response.<ResponseBody.Resource>map(responseBody -> responseBody.resource());*/
		}
	}
}
