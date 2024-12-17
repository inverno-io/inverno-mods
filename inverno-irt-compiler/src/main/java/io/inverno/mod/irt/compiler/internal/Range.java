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

import io.inverno.mod.irt.compiler.internal.parser.Token;

/**
 * <p>
 * Represents a range between two positions in an IRT source file.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class Range {

	/**
	 * Begin position
	 */
	private final Position begin;
	
	/**
	 * End position
	 */
	private final Position end;
	
	/**
	 * <p>
	 * Creates a range between two positions in an IRT source file.
	 * </p>
	 * 
	 * @param begin the beginning position
	 * @param end   the end position
	 */
	public Range(Position begin, Position end) {
		this.begin = begin;
		this.end = end;
	}
	
	/**
	 * <p>
	 * Creates a range between two positions in an IRT source file.
	 * </p>
	 * 
	 * @param beginLine   the beginning line number
	 * @param beginColumn the beginning column number
	 * @param endLine     the end line number
	 * @param endColumn   the end column number
	 */
	public Range(int beginLine, int beginColumn, int endLine, int endColumn) {
		this.begin = new Position(beginLine, beginColumn);
		this.end = new Position(endLine, endColumn);
	}
	
	/**
	 * <p>
	 * Creates a range between two tokens in an IRT source file.
	 * </p>
	 * 
	 * @param beginToken the beginning token
	 * @param endToken the end token
	 */
	public Range(Token beginToken, Token endToken) {
		this.begin = new Position(beginToken.beginLine, beginToken.beginColumn);
		this.end = new Position(endToken.endLine, endToken.endColumn);
	}
	
	/**
	 * <p>
	 * Returns a new range with the specified begin position.
	 * </p>
	 * 
	 * @param begin the new beginning position
	 * 
	 * @return a new range
	 */
	public Range withBegin(Position begin) {
		return new Range(begin, this.end);
	}
	
	/**
	 * <p>
	 * Returns a new range with the specified begin position.
	 * </p>
	 * 
	 * @param beginLine   the new beginning line number
	 * @param beginColumn the new beginning column number
	 * 
	 * @return a new range
	 */
	public Range withBegin(int beginLine, int beginColumn) {
		return new Range(new Position(beginLine, beginColumn), this.end);
	}
	
	/**
	 * <p>
	 * Returns a new range with the specified begin token.
	 * </p>
	 * 
	 * @param beginToken the new beginning token
	 * 
	 * @return a new range
	 */
	public Range withBegin(Token beginToken) {
		return new Range(new Position(beginToken.beginLine, beginToken.beginColumn), this.end);
	}
	
	/**
	 * <p>
	 * Returns a new range with the specified end position.
	 * </p>
	 * 
	 * @param end the new end position
	 * 
	 * @return a new range
	 */
	public Range withEnd(Position end) {
		return new Range(this.begin, end);
	}
	
	/**
	 * <p>
	 * Returns a new range with the specified end position.
	 * </p>
	 * 
	 * @param endLine   the new end line number
	 * @param endColumn the new end column number
	 * 
	 * @return a new range
	 */
	public Range withEnd(int endLine, int endColumn) {
		return new Range(this.begin, new Position(endLine, endColumn));
	}
	
	/**
	 * <p>
	 * Returns a new range with the specified end token.
	 * </p>
	 * 
	 * @param endToken the new end token
	 * 
	 * @return a new range
	 */
	public Range withEnd(Token endToken) {
		return new Range(this.begin, new Position(endToken.endLine, endToken.endColumn));
	}
	
	/**
	 * <p>
	 * Returns the begin position.
	 * </p>
	 * 
	 * @return a position
	 */
	public Position getBegin() {
		return begin;
	}
	
	/**
	 * <p>
	 * Returns the end position.
	 * </p>
	 * 
	 * @return the end position
	 */
	public Position getEnd() {
		return end;
	}
	
	@Override
	public String toString() {
		return this.begin.toString() + "->" + this.end.toString();
	}
}
