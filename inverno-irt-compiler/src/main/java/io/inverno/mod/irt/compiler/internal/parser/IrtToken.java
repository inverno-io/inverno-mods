/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.irt.compiler.internal.parser;

/**
 * <p>
 * Custom IRT JavaCC token.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class IrtToken extends Token {

	private static final long serialVersionUID = 694865684308930840L;

	int realKind = IrtParserConstants.GT;

	/**
	 * Constructs a new token for the specified Image and Kind.
	 * 
	 * @param kind  the kind of token
	 * @param image the token image
	 */
	public IrtToken(int kind, String image) {
		this.kind = kind;
		this.image = image;
	}

	/**
	 * <p>
	 * Returns a new Token object.
	 * </p>
	 * 
	 * @param ofKind     the kind of token
	 * @param tokenImage the token image
	 */
	public static Token newToken(int ofKind, String tokenImage) {
		return new IrtToken(ofKind, tokenImage);
	}
}