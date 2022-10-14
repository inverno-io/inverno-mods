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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.ZipResource;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.internal.GenericRequestBodyConfigurator;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Objects;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
class Http1xRequestBodyConfigurer extends GenericRequestBodyConfigurator<Http1xRequestBody> {

	private static final int MAX_FILE_REGION_SIZE = 1024 * 1024;

	private final boolean supportsFileRegion;
	
	public Http1xRequestBodyConfigurer(
			Http1xRequestHeaders requestHeaders,
			Http1xRequestBody requestBody, 
			ObjectConverter<String> parameterConverter, 
			MultipartEncoder<Parameter> urlEncodedBodyEncoder, 
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory,
			boolean supportsFileRegion) {
		super(requestHeaders, requestBody, parameterConverter, urlEncodedBodyEncoder, multipartBodyEncoder, partFactory);
		this.supportsFileRegion = supportsFileRegion;
	}

	@Override
	public Resource resource() {
		if(this.supportsFileRegion) {
			if(this.resourceData == null) {
				this.resourceData = new Http1xResourceData();
			}
			return this.resourceData;
		}
		else {
			return super.resource();
		}
	}
	
	protected class Http1xResourceData extends GenericRequestBodyConfigurator.ResourceData {

		@Override
		public void value(io.inverno.mod.base.resource.Resource resource) throws IllegalStateException {
			Objects.requireNonNull(resource);
			if(resource.exists().orElse(true)) {
				this.populateHeaders(resource);
				
				// Only regular file resources supports zero-copy
				// It seems FileRegion does not support Zip files, I saw different behavior between JDK<15 and above
				if(resource.isFile().orElse(false) && !(resource instanceof ZipResource)) {
					// We need to create the file region and then send an empty response
					// The Http1xServerExchange should then complete and check whether there is a file region or not
					FileChannel fileChannel = (FileChannel)resource.openReadableByteChannel().orElseThrow(() -> new IllegalArgumentException("Resource is not readable: " + resource.getURI()));
					
					long size = resource.size().get();
					int count = (int)Math.ceil((float)size / (float)MAX_FILE_REGION_SIZE);
					
					// We need to add an extra element in order to control when the flux terminates so we can properly close the file channel
					Http1xRequestBodyConfigurer.this.requestBody.setFileRegionData(Flux.range(0, count + 1) 
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
					Http1xRequestBodyConfigurer.this.requestBody.setData(Flux.empty());
				}
				else {
					Http1xRequestBodyConfigurer.this.requestBody.setData(resource.read().orElseThrow(() -> new InternalServerErrorException("Resource " + resource + " is not readable")));
				}
			}
			else {
				throw new NotFoundException();
			}
		}
	}
}
