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
package io.inverno.mod.irt.compiler.internal;

/**
 * <p>
 * Represents a position, line and column, in an IRT source file.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class Position {

	private final int line;

	private final int column;

	/**
	 * <p>
	 * Creates a new position in an IRT source file.
	 * </p>
	 * 
	 * @param line   a line in an IRT source file
	 * @param column a column in the line
	 */
	public Position(int line, int column) {
		this.line = line;
		this.column = column;
	}

	/**
	 * <p>
	 * Returns the line in the IRT source file.
	 * </p>
	 * 
	 * @return a line number starting at 1
	 */
	public int getLine() {
		return line;
	}

	/**
	 * <p>
	 * Returns the column in the line.
	 * </p>
	 * 
	 * @return a column number starting at 1
	 */
	public int getColumn() {
		return column;
	}

	@Override
	public String toString() {
		return "(l:" + this.line + ",c:" + this.column + ")";
	}
}
