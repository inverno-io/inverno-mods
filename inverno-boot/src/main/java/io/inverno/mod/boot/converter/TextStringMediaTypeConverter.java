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
package io.inverno.mod.boot.converter;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.MediaTypes;
import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * String {@code text/plain} media type converter.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see MediaTypeConverter
 */
@Bean( name = "textStringMediaTypeConverter" )
public class TextStringMediaTypeConverter implements @Provide MediaTypeConverter<String> {

	private final ObjectConverter<String> stringConverter;

	/**
	 * <p>
	 * Creates a {@code text/plain} media type converter.
	 * </p>
	 * 
	 * @param stringConverter the underlying string converter
	 */
	public TextStringMediaTypeConverter(ObjectConverter<String> stringConverter) {
		this.stringConverter = stringConverter;
	}

	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.TEXT_PLAIN);
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Class<T> type) {
		return this.decodeOne(value, (Type)type);
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Type type) {
		// TODO make this more reactive: we basically don't know if there's a delimiter and what is it so we have no choice but to buffer and delegate to the stringConverter
		return Flux.from(value)
			.reduceWith(() -> new StringBuilder(), (sb, chunk) -> sb. append(chunk))
			.map(buf -> this.stringConverter.decode(buf.toString(), type));
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Class<T> type) {
		return this.decodeMany(value, (Type)type);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Type type) {
		// TODO make this more reactive: we basically don't know if there's a delimiter and what is it so we have no choice but to buffer and delegate to the stringConverter
		return Flux.from(value)
			.reduceWith(() -> new StringBuilder(), (sb, chunk) -> sb. append(chunk))
			.flatMapIterable(buf -> this.stringConverter.decodeToList(buf.toString(), type));
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value) {
		return value.map(t -> this.encode(t));
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Class<T> type) {
		return this.encodeOne(value, (Type)type);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Type type) {
		return value.map(t -> this.encode(t, type));
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value) {
		return value.map(t -> this.encode(t));
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Class<T> type) {
		return this.encodeMany(value, (Type)type);
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Type type) {
		return value.map(t -> this.encode(t, type));
	}
	
	public <T> List<T> decodeToList(String value, Class<T> type) {
		return stringConverter.decodeToList(value, type);
	}

	public <T> List<T> decodeToList(String value, Type type) {
		return stringConverter.decodeToList(value, type);
	}

	public <T> Set<T> decodeToSet(String value, Class<T> type) {
		return stringConverter.decodeToSet(value, type);
	}

	public <T> Set<T> decodeToSet(String value, Type type) {
		return stringConverter.decodeToSet(value, type);
	}

	public <T> T[] decodeToArray(String value, Class<T> type) {
		return stringConverter.decodeToArray(value, type);
	}

	public <T> T[] decodeToArray(String value, Type type) {
		return stringConverter.decodeToArray(value, type);
	}

	public <T> String encodeList(List<T> value) throws ConverterException {
		return stringConverter.encodeList(value);
	}

	public <T> String encodeList(List<T> value, Class<T> type) throws ConverterException {
		return stringConverter.encodeList(value, type);
	}

	public <T> String encodeList(List<T> value, Type type) throws ConverterException {
		return stringConverter.encodeList(value, type);
	}

	public <T> String encodeSet(Set<T> value) throws ConverterException {
		return stringConverter.encodeSet(value);
	}

	public <T> String encodeSet(Set<T> value, Class<T> type) throws ConverterException {
		return stringConverter.encodeSet(value, type);
	}

	public <T> String encodeSet(Set<T> value, Type type) throws ConverterException {
		return stringConverter.encodeSet(value, type);
	}

	public <T> String encodeArray(T[] value) throws ConverterException {
		return stringConverter.encodeArray(value);
	}

	public <T> String encodeArray(T[] value, Class<T> type) throws ConverterException {
		return stringConverter.encodeArray(value, type);
	}

	public <T> String encodeArray(T[] value, Type type) throws ConverterException {
		return stringConverter.encodeArray(value, type);
	}

	public String encode(Byte value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Short value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Integer value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Long value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Float value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Double value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Character value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Boolean value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(String value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(BigInteger value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(BigDecimal value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(LocalDate value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(LocalDateTime value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(ZonedDateTime value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Currency value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Locale value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(File value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Path value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(URI value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(URL value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Pattern value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(InetAddress value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public String encode(Class<?> value) throws ConverterException {
		return stringConverter.encode(value);
	}

	public Byte decodeByte(String value) throws ConverterException {
		return stringConverter.decodeByte(value);
	}

	public Short decodeShort(String value) throws ConverterException {
		return stringConverter.decodeShort(value);
	}

	public Integer decodeInteger(String value) throws ConverterException {
		return stringConverter.decodeInteger(value);
	}

	public Long decodeLong(String value) throws ConverterException {
		return stringConverter.decodeLong(value);
	}

	public Float decodeFloat(String value) throws ConverterException {
		return stringConverter.decodeFloat(value);
	}

	public Double decodeDouble(String value) throws ConverterException {
		return stringConverter.decodeDouble(value);
	}

	public Character decodeCharacter(String value) throws ConverterException {
		return stringConverter.decodeCharacter(value);
	}

	public Boolean decodeBoolean(String value) throws ConverterException {
		return stringConverter.decodeBoolean(value);
	}

	public String decodeString(String value) throws ConverterException {
		return stringConverter.decodeString(value);
	}

	public BigInteger decodeBigInteger(String value) throws ConverterException {
		return stringConverter.decodeBigInteger(value);
	}

	public BigDecimal decodeBigDecimal(String value) throws ConverterException {
		return stringConverter.decodeBigDecimal(value);
	}

	public LocalDate decodeLocalDate(String value) throws ConverterException {
		return stringConverter.decodeLocalDate(value);
	}

	public LocalDateTime decodeLocalDateTime(String value) throws ConverterException {
		return stringConverter.decodeLocalDateTime(value);
	}

	public ZonedDateTime decodeZonedDateTime(String value) throws ConverterException {
		return stringConverter.decodeZonedDateTime(value);
	}

	public Currency decodeCurrency(String value) throws ConverterException {
		return stringConverter.decodeCurrency(value);
	}

	public Locale decodeLocale(String value) throws ConverterException {
		return stringConverter.decodeLocale(value);
	}

	public File decodeFile(String value) throws ConverterException {
		return stringConverter.decodeFile(value);
	}

	public Path decodePath(String value) throws ConverterException {
		return stringConverter.decodePath(value);
	}

	public URI decodeURI(String value) throws ConverterException {
		return stringConverter.decodeURI(value);
	}

	public URL decodeURL(String value) throws ConverterException {
		return stringConverter.decodeURL(value);
	}

	public Pattern decodePattern(String value) throws ConverterException {
		return stringConverter.decodePattern(value);
	}

	public InetAddress decodeInetAddress(String value) throws ConverterException {
		return stringConverter.decodeInetAddress(value);
	}

	public Class<?> decodeClass(String value) throws ConverterException {
		return stringConverter.decodeClass(value);
	}

	@Override
	public <T> T decode(String value, Class<T> type) throws ConverterException {
		return stringConverter.decode(value, type);
	}

	@Override
	public <T> T decode(String value, Type type) throws ConverterException {
		return stringConverter.decode(value, type);
	}

	@Override
	public <T> String encode(T value) throws ConverterException {
		return stringConverter.encode(value);
	}

	@Override
	public <T> String encode(T value, Class<T> type) throws ConverterException {
		return stringConverter.encode(value, type);
	}

	@Override
	public <T> String encode(T value, Type type) throws ConverterException {
		return stringConverter.encode(value, type);
	}
}
