/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.irt;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * A {@link TemplateSet} base implementation template sets which renders data in a {@link StringBuilder}
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public abstract class AbstractStringTemplateSet extends AbstractTemplateSet {

	/**
	 * The string output
	 */
	private final StringBuilder output;
	
	/**
	 * <p>
	 * Creates a String template set.
	 * </p>
	 * 
	 * @param charset the charset to use to encode data
	 */
	public AbstractStringTemplateSet(Charset charset) {
		super(charset);
		this.output = new StringBuilder();
	}
	
	/**
	 * <p>
	 * Returns the string containing the rendered data.
	 * </p>
	 * 
	 * @return a string
	 */
	public String getOutput() {
		return output.toString();
	}

	@Override
	public CompletableFuture<Void> render(String value) {
		this.output.append(value);
		return COMPLETED_FUTURE;
	}

	@Override
	public CompletableFuture<Void> render(byte[] value) {
		return this.render(new String(value, this.charset));
	}

}
