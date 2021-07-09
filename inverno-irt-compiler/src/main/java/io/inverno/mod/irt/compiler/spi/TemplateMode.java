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
package io.inverno.mod.irt.compiler.spi;

import java.io.OutputStream;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * The template mode specifies how a the compiler should generate a template set
 * class.
 * </p>
 * 
 * <p>
 * The mode basically specifies the type of output generated when rendering a data
 * model. The compiler shall also optimize how static contents are stored and output
 * based on the mode, providing zero copy whenever possible.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public enum TemplateMode {

	/**
	 * The publisher ByteBuf mode is used to generate a template set class that
	 * outputs rendered data in a publisher of {@link ByteBuf}.
	 */
	PUBLISHER_BYTEBUF,
	/**
	 * The ByteBuf mode is used to generate a template set class that outputs
	 * rendered data in a {@link ByteBuf}.
	 */
	BYTEBUF,
	/**
	 * The publisher String mode is used to generate a template set class that
	 * outputs rendered data in a publisher of String.
	 */
	PUBLISHER_STRING,
	/**
	 * The String mode is used to generate a template set class that outputs
	 * rendered data in a String.
	 */
	STRING,
	/**
	 * The Stream mode is used to generate a template set class that outputs
	 * rendered data in an {@link OutputStream}.
	 */
	STREAM;
}
