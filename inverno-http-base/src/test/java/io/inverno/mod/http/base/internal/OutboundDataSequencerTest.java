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
package io.inverno.mod.http.base.internal;

import io.inverno.mod.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class OutboundDataSequencerTest {

	private static final String[] CHUNKS = new String[] {
		"ab", "abcd", "abc", "abcde", "abcdefgh"
	};
	
	private static final String DATA;
	
	static {
		DATA = Arrays.stream(CHUNKS).collect(Collectors.joining());
	}
	
	private Flux<ByteBuf> data;
	
	@BeforeEach
	public void init() {
		this.data = Flux.fromArray(CHUNKS).map(s -> Unpooled.copiedBuffer(s, Charsets.DEFAULT));
	}
	
	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, Integer.MAX_VALUE})
	public void test(int bufferCapacity) {
		
		List<ByteBuf> result = this.data.transform(new OutboundDataSequencer(bufferCapacity)).collectList().block();
		
		int size = (int)Math.ceil((double)DATA.length() / (double)bufferCapacity);
		Assertions.assertEquals(size, result.size());
		
		for(int i=0;i<size;i++) {
			String value = DATA.substring(i * bufferCapacity, Math.min(DATA.length(), (i+1) * bufferCapacity));
			
			Assertions.assertEquals(value, result.get(i).toString(Charsets.DEFAULT));
		}
		
		result.forEach(ByteBuf::release);
	}
}
