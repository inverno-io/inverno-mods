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
package io.inverno.mod.base;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <p>
 * Utility methods and constants for charsets.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public final class Charsets {

	/**
	 * ISO-8859-1 charset constant
	 */
	public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
	
	/**
	 * UTF-8 charset constant
	 */
	public static final Charset UTF_8 = StandardCharsets.UTF_8;
	
	/**
	 * Default charset
	 */
	public static final Charset DEFAULT = Charsets.UTF_8;
	
	private Charsets() {}
	
	/**
	 * <p>
	 * Returns the specified charset if not null or the default charset.
	 * </p>
	 * 
	 * @param charset a charset
	 * 
	 * @return the specified charset or the default charset
	 */
	public static Charset orDefault(Charset charset) {
		return Charsets.or(charset, DEFAULT);
	}
	
	/**
	 * <p>
	 * Returns the specified charset if not null or the other charset which must not
	 * be null.
	 * </p>
	 * 
	 * @param charset a charset
	 * @param other   another charset
	 * 
	 * @return the specified charset or the other charset
	 * 
	 * @throws NullPointerException if the other charset is null
	 */
	public static Charset or(Charset charset, Charset other) {
		Objects.requireNonNull(other);
		return charset != null ? charset : other;
	}
}
