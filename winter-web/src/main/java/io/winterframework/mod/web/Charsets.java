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
package io.winterframework.mod.web;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author jkuhn
 *
 */
public final class Charsets {

	public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
	
	public static final Charset UTF_8 = StandardCharsets.UTF_8;
	
	public static final Charset DEFAULT = Charsets.UTF_8;
	
	private Charsets() {}
	
	public static Charset orDefault(Charset charset) {
		return Charsets.or(charset, DEFAULT);
	}
	
	public static Charset or(Charset charset, Charset other) {
		Objects.requireNonNull(other, () -> "other");
		return charset != null ? charset : other;
	}
}
