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
package io.winterframework.mod.base.net;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import io.winterframework.mod.base.Charsets;

/**
 * @author jkuhn
 *
 */
public final class URIs {
	
	public static enum Option {
		NORMALIZED,
		PARAMETERIZED // {<name>[:<pattern>]}
	}

	public static URIBuilder uri(URIs.Option... options) {
		return new GenericURIBuilder(Charsets.DEFAULT, options);
	}
	
	public static URIBuilder uri(String path, URIs.Option... options) {
		return new GenericURIBuilder(path, true, Charsets.DEFAULT, options);
	}
	
	public static URIBuilder uri(String path, boolean ignoreTrailingSlash, URIs.Option... options) {
		return new GenericURIBuilder(path, ignoreTrailingSlash, Charsets.DEFAULT, options);
	}
	
	public static URIBuilder uri(String path, boolean ignoreTrailingSlash, Charset charset, URIs.Option... options) {
		return new GenericURIBuilder(path, ignoreTrailingSlash, charset, options);
	}
	
	public static URIBuilder uri(URI baseURI, URIs.Option... options) {
		return new GenericURIBuilder(baseURI, true, Charsets.DEFAULT, options);
	}
	
	public static URIBuilder uri(URI baseURI, boolean ignoreTrailingSlash, URIs.Option... options) {
		return new GenericURIBuilder(baseURI, ignoreTrailingSlash, Charsets.DEFAULT, options);
	}
	
	public static URIBuilder uri(URI baseURI, boolean ignoreTrailingSlash, Charset charset, URIs.Option... options) {
		return new GenericURIBuilder(baseURI, ignoreTrailingSlash, charset, options);
	}
	
	static String decodeURIComponent(String component, Charset charset) {
		Objects.requireNonNull(charset, "charset");
        boolean needToChange = false;
        int numChars = component.length();
        StringBuilder result = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        
        int i = 0;
        while (i < numChars) {
        	char nextByte = component.charAt(i);
        	if(nextByte == '%') {
        		try {
        			byte[] bytes = null;
                    // (numChars-i)/3 is an upper bound for the number
                    // of remaining bytes
                    if (bytes == null) {
                        bytes = new byte[(numChars-i)/3];
                    }
                    int pos = 0;

                    while ( i+2 < numChars && nextByte == '%') {
                        int v = Integer.parseInt(component, i + 1, i + 3, 16);
                        if (v < 0) {
                            throw new URIBuilderException("Illegal hex characters in escape (%) pattern - negative value");
                        }
                        bytes[pos++] = (byte) v;
                        i+= 3;
                        if (i < numChars) {
                            nextByte = component.charAt(i);
                        }
                    }

                    // A trailing, incomplete byte encoding such as
                    // "%x" will cause an exception to be thrown
                    if(i < numChars && nextByte == '%') {
                    	throw new URIBuilderException("Incomplete trailing escape (%) pattern");
                    }
                    result.append(new String(bytes, 0, pos, charset));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                    "URLDecoder: Illegal hex characters in escape (%) pattern - "
                    + e.getMessage());
                }
                needToChange = true;
        	}
        	else {
        		result.append(nextByte);
                 i++;
        	}
        }
        return (needToChange? result.toString() : component);
	}
	
	static String encodeURIComponent(String component, Predicate<Integer> escapedCharacters, Charset charset) {
		if(escapedCharacters == null) {
			return component;
		}
		Objects.requireNonNull(charset, "charset");
		if(StringUtils.isEmpty(component)) {
			return component;
		}
		byte[] bytes = component.getBytes(charset);
		ByteArrayOutputStream encodedOutput = null;
		
		for(int i=0;i<bytes.length;i++) {
			byte b = bytes[i];
			
			if(!escapedCharacters.test((int) b)) {
				if(encodedOutput != null) {
					encodedOutput.write(b);
				}
			}
			else {
				if(encodedOutput == null) {
					encodedOutput = new ByteArrayOutputStream(bytes.length);
					encodedOutput.write(bytes, 0, i);
				}
				encodedOutput.write('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				encodedOutput.write(hex1);
				encodedOutput.write(hex2);
			}
		}
		
		if(encodedOutput != null) {
			try {
				return encodedOutput.toString(charset.name());
			} 
			catch (UnsupportedEncodingException e) {
				throw new URIBuilderException(e);
			}
		}
		return component;
	}
	
	
	static String checkURIComponent(String component, Predicate<Integer> allowedCharacters, Charset charset) {
		if(allowedCharacters == null) {
			return component;
		}
		Objects.requireNonNull(charset, "charset");
		byte[] bytes = component.getBytes(charset);
		for(byte b : bytes) {
			if(!allowedCharacters.test((int)b)) {
				throw new URIBuilderException("Invalid character " + (char)b + " found in URI component");
			}
		}
		return component;
	}
}
