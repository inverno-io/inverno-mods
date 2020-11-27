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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractAsyncResource extends AbstractResource implements AsyncResource {

	private static ExecutorService defaultExecutor;
	
	private ExecutorService executor;
	
	public AbstractAsyncResource() {
	}

	protected AbstractAsyncResource(MediaTypeService mediaTypeService) {
		super(mediaTypeService);
	}

	private static ExecutorService getDefaultExecutor() {
		if(defaultExecutor == null) {
			// TODO using such thread pool might be dangerous since there are no thread limit...
			defaultExecutor = Executors.newCachedThreadPool();
			
			/*defaultExecutor = new ThreadPoolExecutor(0, 20,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>());*/
		}
		return defaultExecutor;
	}
	
	@Override
	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}
	
	@Override
	public ExecutorService getExecutor() {
		return this.executor != null ? this.executor : getDefaultExecutor();
	}
}
