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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * <p>
 * A {@link TemplateSet} which can render {@link ByteBuf}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public interface ByteBufTemplateSet extends TemplateSet {

	/**
	 * <p>
	 * Creates a direct {@link ByteBuf} with the specified data encoded using the
	 * specified charset.
	 * </p>
	 * 
	 * @param data    the data
	 * @param charset the charset
	 * 
	 * @return a direct ByteBuf
	 */
	static ByteBuf directByteBuf(String data, Charset charset) {
		byte[] bytes = data.getBytes(charset);
		ByteBuf buf = Unpooled.directBuffer(bytes.length);
		buf.writeBytes(bytes);
		return Unpooled.unreleasableBuffer(buf);
	}
	
	/**
	 * <p>
	 * Renders a ByteBuf to the output.
	 * </p>
	 * 
	 * @param value the ByteBuf to render
	 * 
	 * @return a future which completes once the value is rendered
	 */
	CompletableFuture<Void> render(ByteBuf value);
}
