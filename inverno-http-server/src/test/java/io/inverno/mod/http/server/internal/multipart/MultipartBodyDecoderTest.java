package io.inverno.mod.http.server.internal.multipart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.ContentDispositionCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.base.internal.header.GenericHeaderService;
import reactor.core.publisher.Flux;

public class MultipartBodyDecoderTest {

	@Test
	public void testDecodeFile() throws IOException {
		GenericHeaderService headerService = new GenericHeaderService(List.of(new ContentTypeCodec(), new ContentDispositionCodec()));
		Headers.ContentType contentType = headerService.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------f490929f7758651e");
		
		ObjectConverter<String> parameterConverter = new StringConverter();
		
		MultipartFormDataBodyDecoder decoder = new MultipartFormDataBodyDecoder(headerService, parameterConverter);
		
		try(FileResource resource = new FileResource("src/test/resources/file_multipart.txt")) {
			List<ByteBuf> data = decoder.decode(resource.read().map(Flux::from).get(), contentType)
				.flatMap(part -> {
					Assertions.assertEquals(" text/plain", part.headers().getContentType());
					Assertions.assertTrue(part.getFilename().isPresent());
					Assertions.assertEquals("file.txt", part.getFilename().get());
					Assertions.assertEquals("file", part.getName());
					return part.raw().stream();
				})
				.collectList()
				.block();

			ByteArrayOutputStream bout = new ByteArrayOutputStream(); 
			
			for(ByteBuf d : data) {
				bout.write(d.array(), d.arrayOffset(), d.readableBytes());
			}
			Assertions.assertArrayEquals(Files.readAllBytes(Paths.get("src/test/resources/file.txt")), bout.toByteArray());
		}
	}
}
