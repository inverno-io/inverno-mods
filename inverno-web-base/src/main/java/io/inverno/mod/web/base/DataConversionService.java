/*
 * Copyright 2024 Jeremy KUHN
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
package io.inverno.mod.web.base;

import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.http.base.InboundData;
import io.inverno.mod.http.base.OutboundData;
import io.inverno.mod.http.base.ws.BaseWebSocketExchange;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;

/**
 * <p>
 * A data conversion that resolves the {@link MediaTypeConverter} to use to encode or decode content based on an HTTP message payload media type or a WebSocket subprotocol.
 * </p>
 *
 * <p>
 * It uses the list of {@link MediaTypeConverter media type converters} injected in the module.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see MediaTypeConverter
 */
public interface DataConversionService {

	/**
	 * <p>
	 * Returns the converter supporting the specified media type.
	 * </p>
	 *
	 * @param mediaType a media type
	 *
	 * @return a media type converter
	 *
	 * @throws MissingConverterException if no converter supporting the media type was found
	 */
	MediaTypeConverter<ByteBuf> getConverter(String mediaType) throws MissingConverterException;

	/**
	 * <p>
	 * Creates an inbound data decoder.
	 * </p>
	 *
	 * @param <T>       the Java content type
	 * @param rawData   the raw inbound data
	 * @param mediaType the media type
	 * @param type      the Java content type
	 *
	 * @return an inbound data decoder
	 *
	 * @throws MissingConverterException if no converter supporting the media type was found
	 */
	<T> InboundDataDecoder<T> createDecoder(InboundData<ByteBuf> rawData, String mediaType, Class<T> type) throws MissingConverterException;

	/**
	 * <p>
	 * Creates an inbound data decoder.
	 * </p>
	 *
	 * @param <T>       the Java content type
	 * @param rawData   the raw inbound data
	 * @param mediaType the media type
	 * @param type      the Java content type
	 *
	 * @return an inbound data decoder
	 *
	 * @throws MissingConverterException if no converter supporting the media type was found
	 */
	<T> InboundDataDecoder<T> createDecoder(InboundData<ByteBuf> rawData, String mediaType, Type type) throws MissingConverterException;

	/**
	 * <p>
	 * Creates an outbound data encoder.
	 * </p>
	 *
	 * @param <T>       the Java content type
	 * @param rawData   the raw outbound data
	 * @param mediaType the media type
	 *
	 * @return an outbound data encoder
	 *
	 * @throws MissingConverterException if no converter supporting the media type was found
	 */
	<T> OutboundDataEncoder<T> createEncoder(OutboundData<ByteBuf> rawData, String mediaType) throws MissingConverterException;

	/**
	 * <p>
	 * Creates an outbound data encoder.
	 * </p>
	 *
	 * @param <T>       the Java content type
	 * @param rawData   the raw outbound data
	 * @param mediaType the media type
	 * @param type      the Java content type
	 *
	 * @return an outbound data encoder
	 *
	 * @throws MissingConverterException if no converter supporting the media type was found
	 */
	<T> OutboundDataEncoder<T> createEncoder(OutboundData<ByteBuf> rawData, String mediaType, Class<T> type) throws MissingConverterException;

	/**
	 * <p>
	 * Creates an outbound data encoder.
	 * </p>
	 *
	 * @param <T>       the Java content type
	 * @param rawData   the raw outbound data
	 * @param mediaType the media type
	 * @param type      the Java content type
	 *
	 * @return an outbound data encoder
	 *
	 * @throws MissingConverterException if no converter supporting the media type was found
	 */
	<T> OutboundDataEncoder<T> createEncoder(OutboundData<ByteBuf> rawData, String mediaType, Type type) throws MissingConverterException;

	/**
	 * <p>
	 * Creates a decoding WebSocket inbound.
	 * </p>
	 *
	 * <p>
	 * In order to resolve a matching {@link MediaTypeConverter}, the subprotocol is considered as an application media type normalized with
	 * {@link io.inverno.mod.base.resource.MediaTypes#normalizeApplicationMediaType(String)}.
	 * </p>
	 *
	 * @param inbound     the raw WebSocket inbound
	 * @param subprotocol the WebSocket subprotocol
	 *
	 * @return a decoding WebSocket inbound
	 */
	BaseWeb2SocketExchange.Inbound createWebSocketDecodingInbound(BaseWebSocketExchange.Inbound inbound, String subprotocol);

	/**
	 * <p>
	 * Creates an encoding WebSocket outbound.
	 * </p>
	 *
	 * <p>
	 * In order to resolve a matching {@link MediaTypeConverter}, the subprotocol is considered as an application media type normalized with
	 * {@link io.inverno.mod.base.resource.MediaTypes#normalizeApplicationMediaType(String)}.
	 * </p>
	 *
	 * @param outbound    the raw WebSocket outbound
	 * @param subprotocol the WebSocket subprotocol
	 *
	 * @return an encoding WebSocket outbound
	 */
	BaseWeb2SocketExchange.Outbound createWebSocketEncodingOutbound(BaseWebSocketExchange.Outbound outbound, String subprotocol);
}
