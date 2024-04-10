/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.grpc.base.internal;

import examples.TestMessage;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.grpc.base.GrpcBaseConfigurationLoader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GrpcMessageWriterTest {

	private static final NetService DUMMY_NET_SERVICE = new DummyNetService();
	
	@Test
	public void test_write() {
		GrpcMessageWriter<TestMessage> writer = new GrpcMessageWriter<>(DUMMY_NET_SERVICE);
		
		TestMessage testMessage = TestMessage.newBuilder().setValue("This is a simple test message").build();
		byte[] testMessageBytes = testMessage.toByteArray();
		
		List<ByteBuf> result = Flux.from(writer.apply(Mono.just(testMessage))).collectList().block();
		
		Assertions.assertEquals(1, result.size());
		
		ByteBuf resultMessageBuffer = result.get(0);
		Assertions.assertEquals(0, resultMessageBuffer.readByte());
		Assertions.assertEquals(testMessageBytes.length, resultMessageBuffer.readInt());

		byte[] resultMessageBytes = new byte[resultMessageBuffer.readableBytes()];
		resultMessageBuffer.readBytes(resultMessageBytes);
		Assertions.assertArrayEquals(testMessageBytes, resultMessageBytes);
	}
	
	@Test
	public void test_write_compressed() {
		GzipGrpcMessageCompressor compressor = new GzipGrpcMessageCompressor(GrpcBaseConfigurationLoader.load(ign -> {}), DUMMY_NET_SERVICE);
		GrpcMessageWriter<TestMessage> writer = new GrpcMessageWriter<>(DUMMY_NET_SERVICE, compressor);
		
		TestMessage testMessage = TestMessage.newBuilder().setValue("This is a simple test message").build();
		ByteBuf testMessageBuffer = Unpooled.copiedBuffer(testMessage.toByteArray());
		ByteBuf compressedTestMessageBuffer = compressor.compress(testMessageBuffer);
		byte[] compressedTestMessageBytes = new byte[compressedTestMessageBuffer.readableBytes()];
		compressedTestMessageBuffer.readBytes(compressedTestMessageBytes);
				
		List<ByteBuf> result = Flux.from(writer.apply(Mono.just(testMessage))).collectList().block();
		
		Assertions.assertEquals(1, result.size());
		
		ByteBuf resultMessageBuffer = result.get(0);
		Assertions.assertEquals(1, resultMessageBuffer.readByte());
		Assertions.assertEquals(compressedTestMessageBytes.length, resultMessageBuffer.readInt());

		byte[] resultMessageBytes = new byte[resultMessageBuffer.readableBytes()];
		resultMessageBuffer.readBytes(resultMessageBytes);
		Assertions.assertArrayEquals(compressedTestMessageBytes, resultMessageBytes);
	}
}
