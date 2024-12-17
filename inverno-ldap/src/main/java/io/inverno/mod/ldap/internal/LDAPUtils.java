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
package io.inverno.mod.ldap.internal;

import io.inverno.mod.ldap.LDAPException;
import javax.naming.NamingException;
import javax.naming.directory.InvalidSearchFilterException;

/**
 * <p>
 * JDK LDAP implementation utilities.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public final class LDAPUtils {

	private LDAPUtils() {}
	
	/* Borrowed from com.sun.jndi.toolkit.dir.SearchFilter */
	/**
	 * <p>
	 * Formats the expression {@code expr} using arguments from the array {@code exprArgs}.
	 * </p>
	 * 
	 * <p>
	 * <code>{i}</code> specifies the {@code i}'th element from the array {@code exprArgs} is to be substituted for the string "<code>{i}</code>".
	 * </p>
	 * 
	 * <p>
	 * To escape '{' or '}' (or any other character), use '\'.
	 * </p>
	 *
	 * @param expr     the expression
	 * @param exprArgs the expression arguments
	 * 
	 * @return a formatted expression string
	 * 
	 * @throws LDAPException if there was an error formatting the expression
	 */
	public static String format(String expr, Object... exprArgs) throws LDAPException {
		try {
			int param;
			int where = 0, start = 0;
			StringBuilder answer = new StringBuilder(expr.length());
	
			while ((where = findUnescaped('{', expr, start)) >= 0) {
				int pstart = where + 1; // skip '{'
				int pend = expr.indexOf('}', pstart);
	
				if (pend < 0) {
					throw new InvalidSearchFilterException("unbalanced {: " + expr);
				}
	
				// at this point, pend should be pointing at '}'
				try {
					param = Integer.parseInt(expr.substring(pstart, pend));
				} catch (NumberFormatException e) {
					throw new InvalidSearchFilterException("integer expected inside {}: " + expr);
				}
	
				if (param >= exprArgs.length) {
					throw new InvalidSearchFilterException("number exceeds argument list: " + param);
				}
	
				answer.append(expr.substring(start, where)).append(getEncodedStringRep(exprArgs[param]));
				start = pend + 1; // skip '}'
			}
	
			if (start < expr.length()) {
				answer.append(expr.substring(start));
			}
			return answer.toString();
		}
		catch(NamingException e) {
			throw new JdkLDAPException(e);
		}
	}

	/**
	 * <p>
	 * Finds the first occurrence of {@code ch} in {@code val} starting from position {@code start}. It doesn't count if {@code ch} has been escaped by a backslash (\)
	 * </p>
	 *
	 * @param ch    the character to find
	 * @param val   the value
	 * @param start the start position
	 *
	 * @return the index of {@code ch} in val
	 */
	private static int findUnescaped(char ch, String val, int start) {
		int len = val.length();

		while (start < len) {
			int where = val.indexOf(ch, start);
			// if at start of string, or not there at all, or if not escaped
			if (where == start || where == -1 || val.charAt(where - 1) != '\\')
				return where;

			// start search after escaped star
			start = where + 1;
		}
		return -1;
	}

	/**
	 * <p>
	 * Returns the string representation of an object (such as an attr value). If obj is a byte array, encode each item as \xx, where xx is hex encoding of the byte value. Else, if obj is not a
	 * String, use its string representation (toString()). Special characters in obj (or its string representation) are then encoded appropriately according to RFC 2254.
	 * </p>
	 * 
	 * <pre>{@code
	 *     *       \2a
	 *     (       \28
	 *     )       \29
	 *     \       \5c
	 *     NUL     \00
	 * }</pre>
	 * 
	 * @param obj an object
	 * 
	 * @return the object {@code String} representation
	 */
	private static String getEncodedStringRep(Object obj) throws NamingException {
		String str;
		if (obj == null)
			return null;

		if (obj instanceof byte[]) {
			// binary data must be encoded as \hh where hh is a hex char
			byte[] bytes = (byte[]) obj;
			StringBuilder b1 = new StringBuilder(bytes.length * 3);
			for(byte aByte : bytes) {
				b1.append('\\');
				hexDigit(b1, aByte);
			}
			return b1.toString();
		}
		if (!(obj instanceof String)) {
			str = obj.toString();
		}
		else {
			str = (String) obj;
		}
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		char ch;
		for (int i = 0; i < len; i++) {
			switch (ch = str.charAt(i)) {
			case '*':
				sb.append("\\2a");
				break;
			case '(':
				sb.append("\\28");
				break;
			case ')':
				sb.append("\\29");
				break;
			case '\\':
				sb.append("\\5c");
				break;
			case 0:
				sb.append("\\00");
				break;
			default:
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * <p>
	 * Writes the hex representation of a byte to a StringBuilder.
	 * </p>
	 * 
	 * @param buf the string builder
	 * @param x   the byte to write
	 */
	private static void hexDigit(StringBuilder buf, byte x) {
		char c;

		c = (char) ((x >> 4) & 0xf);
		if (c > 9) {
			c = (char) ((c - 10) + 'A');
		}
		else {
			c = (char) (c + '0');
		}

		buf.append(c);
		c = (char) (x & 0xf);
		if (c > 9) {
			c = (char) ((c - 10) + 'A');
		}
		else {
			c = (char) (c + '0');
		}
		buf.append(c);
	}
}
