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
package io.inverno.mod.http.server.internal.multipart;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.Headers.ContentType;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SignalType;

/**
 * <p>
 * An application/x-www-form-urlencoded payload decoder implementation as defined by <a href="https://url.spec.whatwg.org/#application/x-www-form-urlencoded">application/x-www-form-urlencoded</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Visibility.PRIVATE)
public class UrlEncodedBodyDecoder implements MultipartDecoder<Parameter> {

	private ObjectConverter<String> parameterConverter;

	/**
	 * <p>
	 * Creates an application/x-www-form-urlencoded body decoder.
	 * </p>
	 * 
	 * @param parameterConverter a string object converter
	 */
	public UrlEncodedBodyDecoder(ObjectConverter<String> parameterConverter) {
		this.parameterConverter = parameterConverter;
	}
	
	@Override
	public Flux<Parameter> decode(Flux<ByteBuf> data, ContentType contentType) {
		if(contentType == null || !contentType.getMediaType().equalsIgnoreCase(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)) {
			throw new IllegalArgumentException("Content type is not " + MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED);
		}
		
		return Flux.create(emitter -> {
			data.subscribe(new BodyDataSubscriber(contentType, emitter));
		});
	}

	private UrlEncodedParameter readParameter(ByteBuf buffer, Charset charset) throws MalformedBodyException {
		if (charset == null) {
			charset = HttpConstants.DEFAULT_CHARSET;
		}
		int readerIndex = buffer.readerIndex();

		Integer startIndex = null;
		Integer endIndex = null;
		String parameterName = null;
		while (buffer.isReadable()) {
			byte nextByte = buffer.readByte();
			if (nextByte == HttpConstants.CR) {
				if(buffer.isReadable()) {
					if (buffer.readByte() == HttpConstants.LF) {
						endIndex = buffer.readerIndex() - 2;
						if (parameterName != null) {
							return new UrlEncodedParameter(this.parameterConverter, this.decodeComponent(parameterName, charset), this.decodeComponent(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), charset), false, true);
						} 
						else if (startIndex != null) {
							return new UrlEncodedParameter(this.parameterConverter, this.decodeComponent(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), charset), "", false, true);
						}
						return new UrlEncodedParameter(this.parameterConverter, "", "", true, true);
					} 
					else {
						buffer.readerIndex(readerIndex);
						throw new MalformedBodyException("Bad end of line");
					}
				}
			} 
			else if (nextByte == HttpConstants.LF) {
				endIndex = buffer.readerIndex() - 1;
				if (parameterName != null) {
					return new UrlEncodedParameter(this.parameterConverter, this.decodeComponent(parameterName, charset), this.decodeComponent(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), charset), false, true);
				} 
				else if (startIndex != null) {
					return new UrlEncodedParameter(this.parameterConverter, this.decodeComponent(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), charset), "", false, true);
				}
				return new UrlEncodedParameter(this.parameterConverter, "", "", true, true);
			}
			else {
				if (parameterName == null) {
					if (startIndex == null) {
						startIndex = buffer.readerIndex() - 1;
					}
	
					if (nextByte == '=') {
						endIndex = buffer.readerIndex() - 1;
						parameterName = buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString();
						startIndex = endIndex = null;
					}
					else if (nextByte == '&') {
						if(startIndex < buffer.readerIndex() - 1) {
							endIndex = buffer.readerIndex() - 1;
							return new UrlEncodedParameter(this.parameterConverter, this.decodeComponent(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), charset), "", false, false);
						}
						else {
							startIndex = null;
						}
					}
				}
				else {
					if (startIndex == null) {
						startIndex = buffer.readerIndex() - 1;
					}
					if (nextByte == '&') {
						endIndex = buffer.readerIndex() - 1;
						return new UrlEncodedParameter(this.parameterConverter, this.decodeComponent(parameterName, charset), this.decodeComponent(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), charset), false, false);
					}
				}
			}
		}
		
		if(parameterName != null) {
			if(startIndex == null) {
				UrlEncodedParameter partialParameter = new UrlEncodedParameter(this.parameterConverter, this.decodeComponent(parameterName, charset), "", true, false);
				buffer.readerIndex(readerIndex);
				return partialParameter;
			}
			else {
				endIndex = buffer.readerIndex();
				UrlEncodedParameter partialParameter = new UrlEncodedParameter(this.parameterConverter, this.decodeComponent(parameterName, charset), this.decodeComponent(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), charset), true, false);
				buffer.readerIndex(readerIndex);
				return partialParameter;
			}
		}
		else {
			if(startIndex != null && startIndex < buffer.readerIndex()) {
				endIndex = buffer.readerIndex();
				UrlEncodedParameter partialParameter = new UrlEncodedParameter(this.parameterConverter, this.decodeComponent(buffer.getCharSequence(startIndex, endIndex - startIndex, charset).toString(), charset), "", true, false);
				buffer.readerIndex(readerIndex);
				return partialParameter;
			}
		}
		buffer.readerIndex(readerIndex);
		return null;
	}
	
	private String decodeComponent(String value, Charset charset) throws MalformedBodyException {
		try {
			return URLDecoder.decode(value, charset.toString()); // RFC-3986 2
		} 
		catch (IllegalArgumentException | UnsupportedEncodingException e) {
			throw new MalformedBodyException(e);
		}
	}
	
	/**
	 * <p>
	 * Request data publisher subscriber.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private class BodyDataSubscriber extends BaseSubscriber<ByteBuf> {
		
		private final Charset charset;
		
		private final FluxSink<Parameter> emitter;
		
		private ByteBuf keepBuffer;
		private UrlEncodedParameter partialParameter;
		
		private boolean canceling;
		
		public BodyDataSubscriber(Headers.ContentType contentType, FluxSink<Parameter> emitter) {
			this.charset = Charsets.orDefault(contentType.getCharset());
			this.emitter = emitter;
			this.emitter.onCancel(() -> {
				this.canceling = true;
				if(this.partialParameter == null) {
					// Otherwise we need to consume until we reach the next part or if we complete
					this.cancel();
				}
			});
		}
		
		private void emitPartialParameter() {
			if(this.partialParameter != null && !this.partialParameter.isLast()) {
				this.partialParameter.setPartial(false);
				this.partialParameter.setLast(true);
				this.emitter.next(this.partialParameter);
			}
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			final ByteBuf buffer;
			if(this.keepBuffer != null && this.keepBuffer.isReadable()) {
				buffer = Unpooled.wrappedBuffer(this.keepBuffer, value);
			}
			else {
				buffer = value;
			}

			try {
				UrlEncodedParameter nextParameter = null;
				while( (nextParameter = UrlEncodedBodyDecoder.this.readParameter(buffer, this.charset)) != null && !this.isDisposed()) {
					if(nextParameter.isLast()) {
						if(!nextParameter.isPartial()) {
							this.emitter.next(nextParameter);
						}
						if(buffer.isReadable()) {
							// Let's be strict: if there is more data after end of line then this is a bad request
							// Note that we'll cancel this subscriber so if the body data flux has more
							// chunk we won't be able to notify such error and if there are more data they
							// will be ignored
							this.emitter.error(new MalformedBodyException("Data received after body was fully decoded"));
						}
						else {
							this.emitter.complete();
						}
						this.cancel();
						return;
					}
					else if(nextParameter.isPartial()) {
						this.partialParameter = nextParameter;
						break;
					}
					else {
						this.emitter.next(nextParameter);
						if(this.canceling) {
							this.cancel();
						}
					}
				}
				
				if(!this.isDisposed()) {
					if(buffer.isReadable()) {
						if(this.keepBuffer != null) {
							this.keepBuffer.clear();
							this.keepBuffer.writeBytes(buffer);
						}
						else {
							this.keepBuffer = buffer.alloc().buffer(buffer.readableBytes());
							this.keepBuffer.writeBytes(buffer);
						}
					}
				}
			}
			catch(Throwable e) {
				this.emitter.error(e);
				this.cancel();
			}
			finally {
				value.release();
			}
		}
		
		@Override
		protected void hookOnError(Throwable throwable) {
			this.emitPartialParameter();
			this.emitter.error(throwable);
		}
		
		@Override
		protected void hookOnComplete() {
			this.emitPartialParameter();
			if(this.keepBuffer != null && this.keepBuffer.isReadable() && this.keepBuffer.getByte(this.keepBuffer.readableBytes() - 1) == HttpConstants.CR) {
				this.emitter.error(new MalformedBodyException("Bad end of line"));
			}
			this.emitter.complete();
		}
		
		@Override
		protected void hookFinally(SignalType type) {
			if(this.keepBuffer != null) {
				this.keepBuffer.release();
				this.keepBuffer = null;
			}
		}
	}
}
