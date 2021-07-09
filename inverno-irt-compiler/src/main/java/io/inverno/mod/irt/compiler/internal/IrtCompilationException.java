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

import java.util.stream.Collectors;

import io.inverno.mod.irt.compiler.internal.parser.ParseException;
import io.inverno.mod.irt.compiler.internal.parser.Token;

/**
 * <p>
 * thrown to indicate an IRT source file compilation error.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public class IrtCompilationException extends RuntimeException {

	private static final long serialVersionUID = 4026172320823928324L;

	private Range range;
	
	/**
	 * <p>
	 * Creates an IRT compilation exception.
	 * </p>
	 */
	public IrtCompilationException() {
	}

	/**
	 * <p>
	 * Creates an IRT compilation exception wrapping the specified parse exception.
	 * </p>
	 * 
	 * @param parseException the parse exception to wrap
	 */
	public IrtCompilationException(ParseException parseException) {
		super(extractMessage(parseException), parseException);
		if(parseException.currentToken != null) {
			this.range = new Range(parseException.currentToken.next, parseException.currentToken.next);
		}
	}
	
	/**
	 * <p>
	 * Creates an IRT compilation exception.
	 * </p>
	 * 
	 * @param message a message
	 */
	public IrtCompilationException(String message) {
		super(message);
	}
	
	/**
	 * <p>
	 * Creates an IRT compilation exception.
	 * </p>
	 * 
	 * @param message a message
	 * @param range   the range in the IRT source file where the error was detected
	 */
	public IrtCompilationException(String message, Range range) {
		super(message);
		this.range = range;
	}

	/**
	 * <p>
	 * Creates an IRT compilation exception.
	 * </p>
	 * 
	 * @param cause the cause
	 */
	public IrtCompilationException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * <p>
	 * Creates an IRT compilation exception.
	 * </p>
	 * 
	 * @param cause the cause
	 * @param range the range in the IRT source file where the error was detected
	 */
	public IrtCompilationException(Throwable cause, Range range) {
		super(cause);
		this.range = range;
	}

	/**
	 * <p>
	 * Creates an IRT compilation exception.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause the cause
	 */
	public IrtCompilationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * <p>
	 * Creates an IRT compilation exception.
	 * </p>
	 * 
	 * @param message a message
	 * @param cause   the cause
	 * @param range   the range in the IRT source file where the error was detected
	 */
	public IrtCompilationException(String message, Throwable cause, Range range) {
		super(message, cause);
		this.range = range;
	}

	@Override
	public String getMessage() {
		if(this.range != null) {
			return this.range.toString() + ": " + super.getMessage();
		}
		return super.getMessage();
	}
	
	/**
	 * <p>
	 * Extracts the error message from the specified parse exception.
	 * </p>
	 * 
	 * @param parseException a parse exception
	 * 
	 * @return an error message
	 */
	private static String extractMessage(ParseException parseException) {
		if(parseException.currentToken == null) {
			return parseException.getMessage();
		}
		
		StringBuilder message = new StringBuilder();
	    StringBuilder expected = new StringBuilder();
	    
	    int maxSize = 0;
	    java.util.TreeSet<String> sortedOptions = new java.util.TreeSet<String>();
	    for (int i = 0; i < parseException.expectedTokenSequences.length; i++) {
	      if (maxSize < parseException.expectedTokenSequences[i].length) {
	        maxSize = parseException.expectedTokenSequences[i].length;
	      }
	      for (int j = 0; j < parseException.expectedTokenSequences[i].length; j++) {
	    	  sortedOptions.add(parseException.tokenImage[parseException.expectedTokenSequences[i][j]]);
	      }
	    }
	    
	    expected.append(sortedOptions.stream().collect(Collectors.joining(", ")));
	    
	    message.append("Encountered unexpected token:");
	    
	    Token tok = parseException.currentToken.next;
	    for (int i = 0; i < maxSize; i++) {
	      String tokenText = tok.image;
	  	  String escapedTokenText = add_escapes(tokenText);
	      if (i != 0) {
	      	message.append(" ");
	      }
	      if (tok.kind == 0) {
	      	message.append(parseException.tokenImage[0]);
	        break;
	      }
	      message.append(" \"");
		  message.append(escapedTokenText);
	      message.append("\"");
	      message.append(" " + parseException.tokenImage[tok.kind]);
	      tok = tok.next;
	    }
	    message.append(".");
	    
	    if (parseException.expectedTokenSequences.length == 0) {
	        // Nothing to add here
	    } else {
	    	int numExpectedTokens = parseException.expectedTokenSequences.length;
	    	message.append(" Was expecting"+ (numExpectedTokens == 1 ? ":" : " one of: "));
	    	message.append(expected.toString());
	    }
	    
	    return message.toString();
	}
	
	/**
	 * <p>
	 * Used to convert raw characters to their escaped version when these raw
	 * version cannot be used as part of an ASCII string literal.
	 * </p>
	 * 
	 * @param str the string to escape
	 */
	private static String add_escapes(String str) {
		StringBuilder retval = new StringBuilder();
		char ch;
		for (int i = 0; i < str.length(); i++) {
			switch (str.charAt(i)) {
			case '\b':
				retval.append("\\b");
				continue;
			case '\t':
				retval.append("\\t");
				continue;
			case '\n':
				retval.append("\\n");
				continue;
			case '\f':
				retval.append("\\f");
				continue;
			case '\r':
				retval.append("\\r");
				continue;
			case '\"':
				retval.append("\\\"");
				continue;
			case '\'':
				retval.append("\\\'");
				continue;
			case '\\':
				retval.append("\\\\");
				continue;
			default:
				if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
					String s = "0000" + Integer.toString(ch, 16);
					retval.append("\\u" + s.substring(s.length() - 4, s.length()));
				} else {
					retval.append(ch);
				}
				continue;
			}
		}
		return retval.toString();
	}
}
