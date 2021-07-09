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
import reactor.core.publisher.Sinks;

/**
 * <p>
 * A {@link TemplateSet} base implementation template sets which renders data in
 * a reactive way using a ByteBuf sink.
 * </p>
 * 
 * <p>
 * This implementation allows to process rendered data without waiting for the
 * whole data set to be rendered or even available following reactive
 * programming principles.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public abstract class AbstractByteBufPublisherTemplateSet extends AbstractTemplateSet implements PublisherTemplateSet<ByteBuf>, ByteBufTemplateSet {

	/**
	 * The ByteBuf sink where rendered data are published
	 */
	protected final Sinks.Many<ByteBuf> sink;

	/**
	 * <p>
	 * Creates a ByteBuf publisher template set.
	 * </p>
	 * 
	 * @param charset the charset to use to encode data
	 */
	protected AbstractByteBufPublisherTemplateSet(Charset charset) {
		super(charset);
		this.sink = Sinks.many().unicast().onBackpressureBuffer();
	}
	
	@Override
	public Sinks.Many<ByteBuf> getSink() {
		return this.sink;
	}
	
	@Override
	public CompletableFuture<Void> render(Object value) {
		if(value instanceof ByteBuf) {
			return this.render((ByteBuf)value);
		}
		else {
			return super.render(value);
		}
	}
	
	@Override
	public CompletableFuture<Void> render(ByteBuf value) {
		this.sink.tryEmitNext(value);
		return COMPLETED_FUTURE;
	}
	
	@Override
	public CompletableFuture<Void> render(String value) {
		return this.render(value.getBytes(this.charset));
	}
	
	@Override
	public CompletableFuture<Void> render(byte[] value) {
		this.sink.tryEmitNext(Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(value)));
		return COMPLETED_FUTURE;
	}
}
