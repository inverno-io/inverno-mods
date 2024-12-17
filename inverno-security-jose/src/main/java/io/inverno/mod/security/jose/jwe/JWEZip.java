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
package io.inverno.mod.security.jose.jwe;

/**
 * <p>
 * A JWE zip implements a JWE compression algorithm used to compress or decompress a JWE object payload based on the compression algorithm JWE JOSE header parameter.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWEZip {
	
	/**
	 * DEFLATE ZIP algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-4.1.3">RFC7516 Section 4.1.3</a>.
	 */
	String ZIP_DEFLATE = "DEF";
	
	/**
	 * <p>
	 * Determines whether the JWE zip supports the specified compression algorithm.
	 * </p>
	 * 
	 * @param zip a compression algorithm
	 * 
	 * @return true if the JWE zip supports the compression algorithm, false otherwise
	 */
	boolean supports(String zip);
	
	/**
	 * <p>
	 * Compresses the specified data.
	 * </p>
	 * 
	 * @param data the data to compress
	 * 
	 * @return compressed data
	 * 
	 * @throws JWEZipException if these was an error compressing the data
	 */
	byte[] compress(byte[] data) throws JWEZipException;
	
	/**
	 * <p>
	 * Decompresses the specified data.
	 * </p>
	 * 
	 * @param data the data to decompress
	 * 
	 * @return decompressed data
	 * 
	 * @throws JWEZipException if these was an error decompressing the data 
	 */
	byte[] decompress(byte[] data) throws JWEZipException;
}