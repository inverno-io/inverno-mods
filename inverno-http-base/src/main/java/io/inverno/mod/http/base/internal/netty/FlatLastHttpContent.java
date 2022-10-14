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
package io.inverno.mod.http.base.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * <p>
 * Optimized {@link LastHttpContent} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class FlatLastHttpContent extends DefaultByteBufHolder implements LastHttpContent {

	private final HttpHeaders trailingHeaders;
	private DecoderResult result;

	public FlatLastHttpContent(ByteBuf content, HttpHeaders trailingHeaders) {
		this(content, trailingHeaders, DecoderResult.SUCCESS);
	}

	public FlatLastHttpContent(ByteBuf content, HttpHeaders trailingHeaders, DecoderResult result) {
		super(content);
		this.trailingHeaders = trailingHeaders;
		this.result = result;
	}

	@Override
	public HttpHeaders trailingHeaders() {
		return trailingHeaders;
	}

	@Override
	public LastHttpContent copy() {
		return this.replace(this.content().copy());
	}

	@Override
	public LastHttpContent duplicate() {
		return this.replace(this.content().duplicate());
	}
	
	@Override
	public LastHttpContent retainedDuplicate() {
		return this.replace(this.content().retainedDuplicate());
	}
	
	@Override
	public LastHttpContent replace(ByteBuf content) {
		return new FlatLastHttpContent(content, this.trailingHeaders, this.result);
	}
	
	@Override
	public LastHttpContent retain(int increment) {
		super.retain(increment);
		return this;
	}

	@Override
	public LastHttpContent retain() {
		super.retain();
		return this;
	}

	@Override
	public DecoderResult decoderResult() {
		return this.result;
	}

	@Override
	@Deprecated
	public DecoderResult getDecoderResult() {
		return this.result;
	}

	@Override
	public void setDecoderResult(DecoderResult result) {
		this.result = result;
	}

	@Override
	public FlatLastHttpContent touch() {
		super.touch();
		return this;
	}

	@Override
	public FlatLastHttpContent touch(Object hint) {
		super.touch(hint);
		return this;
	}
}