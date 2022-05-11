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
package io.inverno.mod.security.jose.internal.jwe;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwe.JWEZipException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * <p>
 * Deflate JWE compression algorithm ({@code DEF}) implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class DeflateJWEZip implements JWEZip {

	/**
	 * <p>
	 * Creates a JWE deflate compression.
	 * </p>
	 */
	public DeflateJWEZip() {
	}

	@Override
	public boolean supports(String zip) {
		return zip.equals(ZIP_DEFLATE);
	}
	
	@Override
	public byte[] compress(byte[] data) throws JWEZipException {
		Deflater deflater = new Deflater(Deflater.DEFLATED, true);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try(DeflaterOutputStream deflaterOutput = new DeflaterOutputStream(output, deflater)) {
			deflaterOutput.write(data);
		}
		catch(IOException e) {
			throw new JWEZipException(e);
		}
		finally {
			deflater.end();
		}
		return output.toByteArray();
	}

	@Override
	public byte[] decompress(byte[] data) throws JWEZipException {
		Inflater inflater = new Inflater(true);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try(InflaterInputStream inflaterInput = new InflaterInputStream(new ByteArrayInputStream(data), inflater)) {
			byte[] b = new byte[1024];
			int len;
			while( (len = inflaterInput.read(b)) > 0) {
				output.write(b, 0, len);
			}
		}
		catch(IOException e) {
			throw new JWEZipException(e);
		}
		finally {
			inflater.end();
		}
		return output.toByteArray();
	}
}
