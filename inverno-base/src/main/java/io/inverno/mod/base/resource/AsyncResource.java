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
package io.inverno.mod.base.resource;

import java.util.concurrent.ExecutorService;

/**
 * <p>
 * An async resource uses an executor service to be read and written asynchronously.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Resource
 */
public interface AsyncResource extends Resource {

	/**
	 * <p>
	 * Sets the executor service to use when reading or writing the resource asynchronously.
	 * </p>
	 *
	 * @param executor the executor service to set
	 */
	void setExecutor(ExecutorService executor);
	
	/**
	 * <p>
	 * Returns the executor service.
	 * </p>
	 * 
	 * @return an executor service
	 */
	ExecutorService getExecutor();
}
