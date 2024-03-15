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

import io.inverno.mod.base.resource.FileResource;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Disabled;
import reactor.core.publisher.Flux;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
@Disabled
public class NettyTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public void testHttpPostRequestDecoder() throws IOException {
		
		/*String multipart = "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"toto\"\n"
				+ "Content-Length: 1\n"
				+ "\n"
				+ "abcd\n"
				+ "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"tata\"\n"
				+ "\n"
				+ "b\n"
				+ "-----------------------------41716138319688775731431041856\n"
				+ "Content-Disposition: form-data; name=\"somefile\"; filename=\"Toto.java\"\n"
				+ "Content-Type: text/x-java\n"
				+ "\n"
				+ "public class Toto {\n"
				+ "\n"
				+ "	public static void main(String[] args) {\n"
				+ "		String toto = \"toto\";\n"
				+ "		String tata = \"tata\";\n"
				+ "		System.out.println(\"Hello \" + toto + \" \" + tata);\n"
				+ "	}\n"
				+ "}\n"
				+ "\n"
				+ "-----------------------------41716138319688775731431041856--\n"
				+ "";
		
		ByteBuf data = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(multipart, CharsetUtil.UTF_8));*/

		HttpDataFactory httpDataFactory = new DefaultHttpDataFactory(true);
        HttpRequest httpRequest = new DefaultHttpRequest(
                io.netty.handler.codec.http.HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                "/");
        httpRequest.headers().add(HttpHeaderNames.CONTENT_TYPE, "multipart/form-data; boundary=------------------------f490929f7758651e");
        
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(httpDataFactory, httpRequest);
        decoder.setDiscardThreshold(0);
        
//        decoder.offer(new DefaultHttpContent(data));
        
        try(FileResource resource = new FileResource("src/test/resources/file_multipart.txt")) {
        	for(ByteBuf data : Flux.from(resource.read()).collectList().block()) {
        		decoder.offer(new DefaultHttpContent(data));
        	}
        }
        
        decoder.offer(LastHttpContent.EMPTY_LAST_CONTENT);
        
        try {
	        while(decoder.hasNext()) {
	        	System.out.println("next");
	        	InterfaceHttpData ihdata = decoder.next();
	        	try {
		        	switch(ihdata.getHttpDataType()) {
			        	case FileUpload:
			        		FileUpload fileUpload = (FileUpload) ihdata;
			        		try(FileChannel output = FileChannel.open(Path.of(fileUpload.getFilename()), StandardOpenOption.CREATE, StandardOpenOption.WRITE);FileChannel input = FileChannel.open(fileUpload.getFile().toPath(), StandardOpenOption.READ)) {
			        			System.out.println("Received file: " + fileUpload.getFilename() + " - " + fileUpload.getFile().getAbsolutePath());
			        			output.transferFrom(input, 0, input.size());
			        			System.out.println("Created file: " + fileUpload.getFilename());
			        		} 
			        		catch (IOException e) {
								e.printStackTrace();
							}
			        		break;
			        	case Attribute:
			        		Attribute attribute = (Attribute) ihdata;
			        		try {
			        			System.out.println("Received attribute: " + attribute.getName() + " = " + attribute.getString());
			        		}
			        		catch(IOException e) {
			        			e.printStackTrace();
			        		}
			        	case InternalAttribute:
			        	default:
		        	}
	        	}
	        	finally {
	        		// This is done when the decoder is destroyed
//	        		ihdata.release();
	        	}
	        }
        }
        catch(HttpPostRequestDecoder.EndOfDataDecoderException e) {
        	// ignore
        }
        finally {
        	try {
				decoder.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
	}

}
