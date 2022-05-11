/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.boot.internal.converter;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.converter.ReactiveConverter;
import io.inverno.mod.base.resource.MediaTypes;
import java.lang.reflect.Type;
import java.util.Iterator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
@Bean( name = "jsonStringMediaTypeConverter" )
public class JsonStringMediaTypeConverter implements @Provide MediaTypeConverter<String> {

	private static final String JSON_ARRAY_START = "[";
	private static final String JSON_ARRAY_SEPARATOR = ",";
	private static final String JSON_ARRAY_END = "]";

	private static final Mono<String> JSON_ARRAY_START_MONO = Mono.just(JSON_ARRAY_START);
	
	private final ReactiveConverter<String, Object> jsonStringConverter;
	
	public JsonStringMediaTypeConverter(ReactiveConverter<String, Object> jsonStringConverter) {
		this.jsonStringConverter = jsonStringConverter;
	}

	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.APPLICATION_JSON);
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Class<T> type) {
		return jsonStringConverter.decodeOne(value, type);
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Type type) {
		return jsonStringConverter.decodeOne(value, type);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Class<T> type) {
		return jsonStringConverter.decodeMany(value, type);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Type type) {
		return jsonStringConverter.decodeMany(value, type);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value) {
		return jsonStringConverter.encodeOne(value);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Class<T> type) {
		return jsonStringConverter.encodeOne(value, type);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Type type) {
		return jsonStringConverter.encodeOne(value, type);
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value) {
		return JSON_ARRAY_START_MONO.concatWith(((Flux<String>)this.jsonStringConverter.encodeMany(value)).zipWithIterable(new Separators(), (element, separator) -> element + separator)).concatWithValues(JSON_ARRAY_END);
	}
	
	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Class<T> type) {
		return JSON_ARRAY_START_MONO.concatWith(((Flux<String>)this.jsonStringConverter.encodeMany(value, type)).zipWithIterable(new Separators(), (element, separator) -> element + separator)).concatWithValues(JSON_ARRAY_END);
	}
	
	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Type type) {
		return JSON_ARRAY_START_MONO.concatWith(((Flux<String>)this.jsonStringConverter.encodeMany(value, type)).zipWithIterable(new Separators(), (element, separator) -> element + separator)).concatWithValues(JSON_ARRAY_END);
	}

	@Override
	public <T> T decode(String value, Class<T> type) throws ConverterException {
		return jsonStringConverter.decode(value, type);
	}

	@Override
	public <T> T decode(String value, Type type) throws ConverterException {
		return jsonStringConverter.decode(value, type);
	}

	@Override
	public <T> String encode(T value) throws ConverterException {
		return jsonStringConverter.encode(value);
	}

	@Override
	public <T> String encode(T value, Class<T> type) throws ConverterException {
		return jsonStringConverter.encode(value, type);
	}

	@Override
	public <T> String encode(T value, Type type) throws ConverterException {
		return jsonStringConverter.encode(value, type);
	}

	private static class Separators implements Iterable<String> {
		
		@Override
		public Iterator<String> iterator() {
			
			return new Iterator<String>() {
				
				private String current = "";

				@Override
				public boolean hasNext() {
					return true;
				}

				@Override
				public String next() {
					String next = this.current;
					this.current = JSON_ARRAY_SEPARATOR;
					return next;
				}
			};
		}
	}
	
}
