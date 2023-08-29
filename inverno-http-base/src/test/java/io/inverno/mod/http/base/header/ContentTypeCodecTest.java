/*
 * Copyright 2023 Jeremy KUHN
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
package io.inverno.mod.http.base.header;

import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

/**
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ContentTypeCodecTest {

	@Test
	public void testContentType() {
		ContentTypeCodec codec = new ContentTypeCodec();
		
		ContentTypeCodec.ContentType header = codec.decode(Headers.NAME_CONTENT_TYPE, " text/plain ");
		
		Assertions.assertEquals("text/plain", header.getHeaderValue());
	}
}
