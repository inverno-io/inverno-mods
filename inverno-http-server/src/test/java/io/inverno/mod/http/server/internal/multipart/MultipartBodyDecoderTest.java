package io.inverno.mod.http.server.internal.multipart;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.ContentDispositionCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.base.internal.header.GenericHeaderService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public class MultipartBodyDecoderTest {
	
	private static final HeaderService HEADER_SERVICE = new GenericHeaderService(List.of(new ContentTypeCodec(), new ContentDispositionCodec()));

	private static final MultipartFormDataBodyDecoder DECODER = new MultipartFormDataBodyDecoder(HEADER_SERVICE, new StringConverter());

	@Test
	public void testDecodeFile() throws IOException {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------f490929f7758651e");
		
		try(FileResource resource = new FileResource("src/test/resources/file_multipart.txt")) {
			List<ByteBuf> data = DECODER.decode(resource.read().map(Flux::from).get(), contentType)
				.flatMap(part -> {
					Assertions.assertEquals("text/plain", part.headers().getContentType());
					Assertions.assertTrue(part.getFilename().isPresent());
					Assertions.assertEquals("file.txt", part.getFilename().get());
					Assertions.assertEquals("file", part.getName());
					return part.raw().stream();
				})
				.collectList()
				.block();

			ByteArrayOutputStream bout = new ByteArrayOutputStream(); 
			for(ByteBuf d : data) {
				d.getBytes(d.readerIndex(), bout, d.readableBytes());
			}
			
			Files.write(Path.of("target/toto.txt"), bout.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			
			Assertions.assertArrayEquals(Files.readAllBytes(Path.of("src/test/resources/file.txt")), bout.toByteArray());
		}
	}
	
	@Test
	public void testSplitBoundary() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------5b4f48bb5ceeebdd");
		
		ByteBuf buf1 = Unpooled.copiedBuffer("--------------------------5b4f48bb5ceeebdd\n" +
			"content-length: 28\n" +
			"content-type: text/plain\n" +
			"content-disposition: form-data;name=\"file\";filename=\"file.txt\"\n" +
			"\n" +
			"This is a file upload test!\n" +
			"\n" +
			"--------------------------5", StandardCharsets.UTF_8);
		
		ByteBuf buf2 = Unpooled.copiedBuffer("b4f48bb5ceeebdd--", StandardCharsets.UTF_8);
		
		String body = DECODER.decode(Flux.just(buf1, buf2), contentType)
			.flatMap(part -> {
				Assertions.assertEquals("text/plain", part.headers().getContentType());
				Assertions.assertTrue(part.getFilename().isPresent());
				Assertions.assertEquals("file.txt", part.getFilename().get());
				Assertions.assertEquals("file", part.getName());
				return part.raw().stream();
			})
			.map(buf -> buf.toString(StandardCharsets.UTF_8))
			.collect(Collectors.joining())
			.block();

		Assertions.assertEquals("This is a file upload test!\n", body);
	}
	
	@Test
	public void testSplit() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------9a9f035763fdfd11");
		
		ByteBuf buf1 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"2\n", StandardCharsets.UTF_8);
		
		ByteBuf buf2 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		ByteBuf buf = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		List<String> bodies = DECODER.decode(Flux.just(buf1, buf2), contentType)
			.flatMap(part -> part.string().stream())
			.map(CharSequence::toString)
			.collectList()
			.block();
		
		Assertions.assertEquals(List.of("1", "2", "3"), bodies);
	}
	
	@Test
	public void testSplitBeforeCR() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------9a9f035763fdfd11");
		
		ByteBuf buf = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi\r\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\r\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		ByteBuf buf1 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi", StandardCharsets.UTF_8);
		
		ByteBuf buf2 = Unpooled.copiedBuffer("\r\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\r\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		List<String> bodies = DECODER.decode(Flux.just(buf1, buf2), contentType)
			.flatMap(part -> part.string().stream())
			.map(CharSequence::toString)
			.collectList()
			.block();
		
		Assertions.assertEquals(List.of("abcdefghi", "jklmnopqr"), bodies);
	}
	
	@Test
	public void testSplitAfterCR() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------9a9f035763fdfd11");
		
		ByteBuf buf = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi\r\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\r\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		ByteBuf buf1 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi\r", StandardCharsets.UTF_8);
		
		ByteBuf buf2 = Unpooled.copiedBuffer("\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\r\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		List<String> bodies = DECODER.decode(Flux.just(buf1, buf2), contentType)
			.flatMap(part -> part.string().stream())
			.map(CharSequence::toString)
			.collectList()
			.block();
		
		Assertions.assertEquals(List.of("abcdefghi", "jklmnopqr"), bodies);
	}
	
	@Test
	public void testSplitAfterCRLF() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------9a9f035763fdfd11");
		
		ByteBuf buf = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi\r\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\r\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		ByteBuf buf1 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi\r\n", StandardCharsets.UTF_8);
		
		ByteBuf buf2 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\r\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		List<String> bodies = DECODER.decode(Flux.just(buf1, buf2), contentType)
			.flatMap(part -> part.string().stream())
			.map(CharSequence::toString)
			.collectList()
			.block();
		
		Assertions.assertEquals(List.of("abcdefghi", "jklmnopqr"), bodies);
	}
	
	@Test
	public void testSplitBeforeLF() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------9a9f035763fdfd11");
		
		ByteBuf buf = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		ByteBuf buf1 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi", StandardCharsets.UTF_8);
		
		ByteBuf buf2 = Unpooled.copiedBuffer("\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		List<String> bodies = DECODER.decode(Flux.just(buf1, buf2), contentType)
			.flatMap(part -> part.string().stream())
			.map(CharSequence::toString)
			.collectList()
			.block();
		
		Assertions.assertEquals(List.of("abcdefghi", "jklmnopqr"), bodies);
	}
	
	@Test
	public void testSplitAfterLF() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------9a9f035763fdfd11");
		
		ByteBuf buf = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi\n" +
			"--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		ByteBuf buf1 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi\n", StandardCharsets.UTF_8);
		
		ByteBuf buf2 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"b\"\n" +
			"\n" +
			"jklmnopqr\r\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		List<String> bodies = DECODER.decode(Flux.just(buf1, buf2), contentType)
			.flatMap(part -> part.string().stream())
			.map(CharSequence::toString)
			.collectList()
			.block();
		
		Assertions.assertEquals(List.of("abcdefghi", "jklmnopqr"), bodies);
	}
	
	@Test
	public void testDataEndWithMultipleLF() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------9a9f035763fdfd11");
		
		ByteBuf buf1 = Unpooled.copiedBuffer("--------------------------9a9f035763fdfd11\n" +
			"content-disposition: form-data;name=\"a\"\n" +
			"\n" +
			"abcdefghi\n\n", StandardCharsets.UTF_8);
		
		ByteBuf buf2 = Unpooled.copiedBuffer("\n\n" +
			"--------------------------9a9f035763fdfd11--\n", StandardCharsets.UTF_8);
		
		List<String> bodies = DECODER.decode(Flux.just(buf1, buf2), contentType)
			.flatMap(part -> Flux.from(part.string().stream()).collect(Collectors.joining()))
			.map(CharSequence::toString)
			.collectList()
			.block();
		
		Assertions.assertEquals(List.of("abcdefghi\n\n\n"), bodies);
	}
	
	@Test
	public void testCancelPartStreamSubscription() {
		Headers.ContentType contentType = HEADER_SERVICE.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------76c6a8d26e6eecc3");
		
		ByteBuf buf1 = Unpooled.copiedBuffer("--------------------------76c6a8d26e6eecc3\r\n" +
			"content-disposition: form-data;name=\"a\"\r\n" +
			"\r\n", StandardCharsets.UTF_8);
		
		ByteBuf buf2 = Unpooled.copiedBuffer("1", StandardCharsets.UTF_8);
		
		ByteBuf buf3 = Unpooled.copiedBuffer("\r\n--------------------------76c6a8d26e6eecc3\r\n" +
			"content-disposition: form-data;name=\"b\"\r\n" +
			"\r\n", StandardCharsets.UTF_8);
		
		ByteBuf buf4 = Unpooled.copiedBuffer("2", StandardCharsets.UTF_8);
		
		ByteBuf buf5 = Unpooled.copiedBuffer("\r\n" +
			"--------------------------76c6a8d26e6eecc3--\r\n", StandardCharsets.UTF_8);
		
		Sinks.Many<ByteBuf> dataSink = Sinks.many().unicast().onBackpressureBuffer();
		
		dataSink.tryEmitNext(buf1);
		dataSink.tryEmitNext(buf2);
		dataSink.tryEmitNext(buf3);
		dataSink.tryEmitNext(buf4);
		dataSink.tryEmitNext(buf5);
		
		String response = Mono.from(DECODER.decode(dataSink.asFlux(), contentType)).flatMapMany(
				part -> Flux.concat(
					Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(part.getName() + " = ", Charsets.DEFAULT))), 
					Flux.from(part.raw().stream()),
					Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(", ", Charsets.DEFAULT)))
				)
			)
			.reduceWith(() -> Unpooled.unreleasableBuffer(Unpooled.buffer()), (acc, chunk) -> { try { return acc.writeBytes(chunk); } finally { chunk.release(); } })
			.map(buf -> "post_multipart_mono_raw: " + buf.toString(Charsets.DEFAULT))
			.block();
		
		Assertions.assertEquals("post_multipart_mono_raw: a = 1, ", response);
	}
}