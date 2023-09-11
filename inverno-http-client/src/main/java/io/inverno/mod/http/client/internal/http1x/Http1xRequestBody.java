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

import io.inverno.mod.http.client.internal.GenericRequestBody;
import io.netty.channel.FileRegion;
import org.reactivestreams.Publisher;

/**
 * <p>
 * HTTP/1.x request body with support for {@link FileRegion} data.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http1xRequestBody extends GenericRequestBody {

	private Publisher<FileRegion> fileRegionData;

	/**
	 * <p>
	 * Sets the request body data as file region data.
	 * </p>
	 * 
	 * @param fileRegionData a publisher of file region
	 */
	public void setFileRegionData(Publisher<FileRegion> fileRegionData) {
		this.fileRegionData = fileRegionData;
	}
	
	/**
	 * <p>
	 * Returns the request body file region data publisher.
	 * </p>
	 * 
	 * @return a publisher of file region data or null if the body is not made of file region data.
	 */
	public Publisher<FileRegion> getFileRegionData() {
		return this.fileRegionData;
	}
}
