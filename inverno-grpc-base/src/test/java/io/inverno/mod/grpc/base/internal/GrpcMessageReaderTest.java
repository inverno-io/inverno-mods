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

import com.google.protobuf.ExtensionRegistry;
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
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GrpcMessageReaderTest {
	
	static {
		System.setProperty("log4j2.simplelogLevel", "INFO");
		System.setProperty("log4j2.simplelogLogFile", "system.out");
//		System.setProperty("io.netty.leakDetection.level", "PARANOID");
//		System.setProperty("io.netty.leakDetection.targetRecords", "20");
	}

	private static final NetService DUMMY_NET_SERVICE = new DummyNetService();
	
	@Test
	public void test_read_single_buffer() {
		GrpcMessageReader<TestMessage> reader = new GrpcMessageReader<>(TestMessage.getDefaultInstance(), ExtensionRegistry.getEmptyRegistry(), DUMMY_NET_SERVICE);
		
		TestMessage testMessage = TestMessage.newBuilder().setValue("This is a simple test message").build();
		byte[] testMessageBytes = testMessage.toByteArray();
		
		ByteBuf buffer = Unpooled.buffer(testMessageBytes.length + 5);
		buffer.writeByte(0);
		buffer.writeInt(testMessageBytes.length);
		buffer.writeBytes(testMessageBytes);

		List<TestMessage> result = Flux.from(reader.apply(Mono.just(buffer))).collectList().block();

		Assertions.assertEquals(1, result.size());

		Assertions.assertEquals(testMessage, result.get(0));
		
		Assertions.assertEquals(0, buffer.refCnt());
	}
	
	@Test
	public void test_read_split_buffers() {
		GrpcMessageReader<TestMessage> reader = new GrpcMessageReader<>(TestMessage.getDefaultInstance(), ExtensionRegistry.getEmptyRegistry(), DUMMY_NET_SERVICE);
		
		TestMessage testMessage = TestMessage.newBuilder().setValue("This is a simple test message").build();
		byte[] testMessageBytes = testMessage.toByteArray();
		
		int buf1_size = testMessageBytes.length/3 + 5;
		int buf2_size = testMessageBytes.length/3;
		int buf3_size = testMessageBytes.length - 2 * (testMessageBytes.length/3);
		
		ByteBuf buffer1 = Unpooled.buffer(buf1_size);
		buffer1.writeByte(0);
		buffer1.writeInt(testMessageBytes.length);
		buffer1.writeBytes(testMessageBytes, 0, buf1_size - 5);

		ByteBuf buffer2 = Unpooled.buffer(buf2_size);
		buffer2.writeBytes(testMessageBytes, buf1_size - 5, buf2_size);

		ByteBuf buffer3 = Unpooled.buffer(buf3_size);
		buffer3.writeBytes(testMessageBytes, buf1_size - 5 + buf2_size, buf3_size);

		List<TestMessage> result = Flux.from(reader.apply(Flux.just(buffer1, buffer2, buffer3))).collectList().block();

		Assertions.assertEquals(1, result.size());

		Assertions.assertEquals(testMessage, result.get(0));
		
		Assertions.assertEquals(0, buffer1.refCnt());
		Assertions.assertEquals(0, buffer2.refCnt());
		Assertions.assertEquals(0, buffer3.refCnt());
	}
	
	@Test
	public void test_read_two_messages() {
		GrpcMessageReader<TestMessage> reader = new GrpcMessageReader<>(TestMessage.getDefaultInstance(), ExtensionRegistry.getEmptyRegistry(), DUMMY_NET_SERVICE);
		
		TestMessage testMessage1 = TestMessage.newBuilder().setValue("This is a simple test message").build();
		byte[] testMessage1Bytes = testMessage1.toByteArray();
		
		TestMessage testMessage2 = TestMessage.newBuilder().setValue("This is another message").build();
		byte[] testMessage2Bytes = testMessage2.toByteArray();
		
		ByteBuf buffer = Unpooled.buffer(testMessage1Bytes.length + 5 + testMessage2Bytes.length + 5);
		buffer.writeByte(0);
		buffer.writeInt(testMessage1Bytes.length);
		buffer.writeBytes(testMessage1Bytes, 0, testMessage1Bytes.length);
		buffer.writeByte(0);
		buffer.writeInt(testMessage2Bytes.length);
		buffer.writeBytes(testMessage2Bytes, 0, testMessage2Bytes.length);
		
		List<TestMessage> result = Flux.from(reader.apply(Flux.just(buffer))).collectList().block();
		
		Assertions.assertEquals(2, result.size());

		Assertions.assertEquals(testMessage1, result.get(0));
		Assertions.assertEquals(testMessage2, result.get(1));
		
		Assertions.assertEquals(0, buffer.refCnt());
	}
	
	@Test
	public void test_read_with_retained_buffer() {
		GrpcMessageReader<TestMessage> reader = new GrpcMessageReader<>(TestMessage.getDefaultInstance(), ExtensionRegistry.getEmptyRegistry(), DUMMY_NET_SERVICE);
		
		TestMessage testMessage1 = TestMessage.newBuilder().setValue("This is a simple test message").build();
		byte[] testMessage1Bytes = testMessage1.toByteArray();
		
		TestMessage testMessage2 = TestMessage.newBuilder().setValue("This is another message").build();
		byte[] testMessage2Bytes = testMessage2.toByteArray();
		
		int buf1_size = testMessage1Bytes.length/2 + 5;
		int buf2_size = testMessage1Bytes.length - testMessage1Bytes.length/2 + 5 + testMessage2Bytes.length/2;
		int buf3_size = testMessage2Bytes.length - testMessage2Bytes.length/2;
		
		ByteBuf buffer1 = Unpooled.buffer(buf1_size);
		buffer1.writeByte(0);
		buffer1.writeInt(testMessage1Bytes.length);
		buffer1.writeBytes(testMessage1Bytes, 0, testMessage1Bytes.length/2);

		ByteBuf buffer2 = Unpooled.buffer(buf2_size);
		buffer2.writeBytes(testMessage1Bytes, testMessage1Bytes.length/2, testMessage1Bytes.length - testMessage1Bytes.length/2);
		buffer2.writeByte(0);
		buffer2.writeInt(testMessage2Bytes.length);
		buffer2.writeBytes(testMessage2Bytes, 0, testMessage2Bytes.length/2);

		ByteBuf buffer3 = Unpooled.buffer(buf3_size);
		buffer3.writeBytes(testMessage2Bytes, testMessage2Bytes.length/2,  testMessage2Bytes.length - testMessage2Bytes.length/2);

		List<TestMessage> result = Flux.from(reader.apply(Flux.just(buffer1, buffer2, buffer3))).collectList().block();

		Assertions.assertEquals(2, result.size());

		Assertions.assertEquals(testMessage1, result.get(0));
		Assertions.assertEquals(testMessage2, result.get(1));
		
		Assertions.assertEquals(0, buffer1.refCnt());
		Assertions.assertEquals(0, buffer2.refCnt());
		Assertions.assertEquals(0, buffer3.refCnt());
	}
	
	@Test
	public void test_read_with_split_prefix() {
		GrpcMessageReader<TestMessage> reader = new GrpcMessageReader<>(TestMessage.getDefaultInstance(), ExtensionRegistry.getEmptyRegistry(), DUMMY_NET_SERVICE);
		
		TestMessage testMessage = TestMessage.newBuilder().setValue("This is a simple test message").build();
		byte[] testMessageBytes = testMessage.toByteArray();
		
		int buf1_size = testMessageBytes.length/3 + 5;
		int buf2_size = testMessageBytes.length/3;

		ByteBuf prefixBuffer = Unpooled.buffer(5);
		prefixBuffer.writeByte(0);
		prefixBuffer.writeInt(testMessageBytes.length);		
		
		ByteBuf buffer1 = Unpooled.buffer(3);
		buffer1.writeBytes(prefixBuffer, 3);

		ByteBuf buffer2 = Unpooled.buffer(testMessageBytes.length + 2);
		buffer2.writeBytes(prefixBuffer, 2);
		buffer2.writeBytes(testMessageBytes);

		List<TestMessage> result = Flux.from(reader.apply(Flux.just(buffer1, buffer2))).collectList().block();

		Assertions.assertEquals(1, result.size());

		Assertions.assertEquals(testMessage, result.get(0));
		
		Assertions.assertEquals(0, buffer1.refCnt());
		Assertions.assertEquals(0, buffer2.refCnt());
	}
	
	@Test
	public void test_read_compressed() {
		GzipGrpcMessageCompressor compressor = new GzipGrpcMessageCompressor(GrpcBaseConfigurationLoader.load(ign -> {}), DUMMY_NET_SERVICE);
		GrpcMessageReader<TestMessage> reader = new GrpcMessageReader<>(TestMessage.getDefaultInstance(), ExtensionRegistry.getEmptyRegistry(), DUMMY_NET_SERVICE, compressor);
		
		TestMessage testMessage = TestMessage.newBuilder().setValue("This is a simple test message").build();
		ByteBuf testMessageBuffer = Unpooled.copiedBuffer(testMessage.toByteArray());
		ByteBuf compressedTestMessageBuffer = compressor.compress(testMessageBuffer);
		
		ByteBuf buffer = Unpooled.buffer(compressedTestMessageBuffer.readableBytes() + 5);
		buffer.writeByte(1);
		buffer.writeInt(compressedTestMessageBuffer.readableBytes());
		buffer.writeBytes(compressedTestMessageBuffer);

		List<TestMessage> result = Flux.from(reader.apply(Mono.just(buffer))).collectList().block();

		Assertions.assertEquals(1, result.size());

		Assertions.assertEquals(testMessage, result.get(0));
		
		Assertions.assertEquals(0, testMessageBuffer.refCnt());
		Assertions.assertEquals(0, buffer.refCnt());
	}
	
	@Test
	public void test_read_uncompressed_with_compressor() {
		GzipGrpcMessageCompressor compressor = new GzipGrpcMessageCompressor(GrpcBaseConfigurationLoader.load(ign -> {}), DUMMY_NET_SERVICE);
		GrpcMessageReader<TestMessage> reader = new GrpcMessageReader<>(TestMessage.getDefaultInstance(), ExtensionRegistry.getEmptyRegistry(), DUMMY_NET_SERVICE, compressor);
		
		TestMessage testMessage = TestMessage.newBuilder().setValue("This is a simple test message").build();
		byte[] testMessageBytes = testMessage.toByteArray();
		
		ByteBuf buffer = Unpooled.buffer(testMessageBytes.length + 5);
		buffer.writeByte(0);
		buffer.writeInt(testMessageBytes.length);
		buffer.writeBytes(testMessageBytes);

		List<TestMessage> result = Flux.from(reader.apply(Mono.just(buffer))).collectList().block();

		Assertions.assertEquals(1, result.size());

		Assertions.assertEquals(testMessage, result.get(0));
		
		Assertions.assertEquals(0, buffer.refCnt());
	}
}
