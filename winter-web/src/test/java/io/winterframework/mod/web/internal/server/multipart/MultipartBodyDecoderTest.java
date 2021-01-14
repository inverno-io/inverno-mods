package io.winterframework.mod.web.internal.server.multipart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.base.resource.FileResource;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.internal.header.ContentDispositionCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.internal.header.GenericHeaderService;

public class MultipartBodyDecoderTest {

	@Test
	public void testDecodeFile() throws IOException {
		HeaderService headerService = new GenericHeaderService(List.of(new ContentTypeCodec(), new ContentDispositionCodec()));
		Headers.ContentType contentType = headerService.<Headers.ContentType>decode("content-type: multipart/form-data; boundary=------------------------f490929f7758651e");
		
		MultipartBodyDecoder decoder = new MultipartBodyDecoder(headerService);
		
		try(FileResource resource = new FileResource("src/test/resources/file_multipart.txt")) {
			List<ByteBuf> data = decoder.decode(resource.read().get(), contentType)
				.flatMap(part -> {
					Assertions.assertEquals("text/plain", part.headers().getContentType());
					Assertions.assertTrue(part.getFilename().isPresent());
					Assertions.assertEquals("file.txt", part.getFilename().get());
					Assertions.assertEquals("file", part.getName());
					return part.data();
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
