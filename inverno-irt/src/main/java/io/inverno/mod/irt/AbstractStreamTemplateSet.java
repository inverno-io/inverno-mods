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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * A {@link TemplateSet} base implementation template sets which renders data in
 * an {@link OutputStream}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 * @param <T> The type of {@link OutputStream}
 */
public abstract class AbstractStreamTemplateSet<T extends OutputStream> extends AbstractTemplateSet {

	/**
	 * The output stream where rendered data are written
	 */
	private final T output;
	
	/**
	 * <p>
	 * Creates a Stream template set.
	 * </p>
	 * 
	 * @param charset the charset to use to encode data
	 * @param output the output stream where to write rendered data
	 */
	public AbstractStreamTemplateSet(Charset charset, T output) {
		super(charset);
		this.output = output;
	}
	
	/**
	 * <p>
	 * Returns the output stream where rendered data are written.
	 * </p>
	 * 
	 * @return an output stream
	 */
	public T getOutput() {
		return output;
	}

	@Override
	public CompletableFuture<Void> render(String value) {
		return this.render(value.getBytes(this.charset));
	}

	@Override
	public CompletableFuture<Void> render(byte[] value) {
		try {
			this.output.write(value);
		} 
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return COMPLETED_FUTURE;
	}

}
