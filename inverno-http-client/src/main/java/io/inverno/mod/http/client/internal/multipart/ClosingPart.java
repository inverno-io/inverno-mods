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

package io.inverno.mod.http.client.internal.multipart;

import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.PartHeaders;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ClosingPart implements Part<Object> {

	public static final ClosingPart INSTANCE = new ClosingPart();
	
	@Override
	public Part<Object> name(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Part<Object> filename(String filename) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PartHeaders headers() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Part<Object> headers(Consumer<PartHeaders> headersConfigurer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> void stream(Publisher<T> value) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

}
