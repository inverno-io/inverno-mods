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
package io.inverno.mod.redis.operations;

import java.nio.charset.Charset;

/**
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> builder type
 */
public interface AbstractScanBuilder<A extends AbstractScanBuilder<A>> {

	/**
	 * 
	 * @param count
	 * @return 
	 */
	A count(long count);
	
	/**
	 * 
	 * @param pattern
	 * @return 
	 */
	A pattern(String pattern);
	
	/**
	 * 
	 * @param pattern
	 * @param charset
	 * @return 
	 */
	A pattern(String pattern, Charset charset);
}
