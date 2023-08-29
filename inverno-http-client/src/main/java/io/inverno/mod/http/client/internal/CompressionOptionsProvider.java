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
package io.inverno.mod.http.client.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.compression.Zstd;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * <p>
 * A compression options provider.
 * </p>
 * 
 * <p>
 * It provides configured {@link CompressionOptions} when initializing channels.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see EndpointChannelConfigurer
 * @see Http2ConnectionFactory
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class CompressionOptionsProvider implements Supplier<CompressionOptions[]> {

	private final HttpClientConfiguration configuration;

	private CompressionOptions[] compressionOptions;
	
	/**
	 * <p>
	 * Creates a compression options provider
	 * </p>
	 * 
	 * @param configuration 
	 */
	public CompressionOptionsProvider(HttpClientConfiguration configuration) {
		this.configuration = configuration;
	}
	
	/**
	 * <p>
	 * Returns the list of compression options configured in the module.
	 * </p>
	 */
	@Override
	public CompressionOptions[] get() {
		if(this.compressionOptions == null) {
			synchronized(this) {
				compressionOptions = this.createOptions(this.configuration);
			}
		}
		return compressionOptions;
	}
	
	/**
	 * <p>
	 * Returns the list of compression options configured in the specified configuration.
	 * </p>
	 * 
	 * @param configuration an HTTP client configuration
	 * 
	 * @return a list of compression options
	 */
	public CompressionOptions[] get(HttpClientConfiguration configuration) {
		if(configuration == this.configuration) {
			return this.get();
		}
		return this.createOptions(configuration);
	}
	
	/**
	 * <p>
	 * Creates compression options from the specified configuration.
	 * </p>
	 * 
	 * @param configuration an HTTP client configuration
	 * 
	 * @return a list of compression options
	 */
	private CompressionOptions[] createOptions(HttpClientConfiguration configuration) {
		List<CompressionOptions> compressionOptionsList = new ArrayList<>();

		compressionOptionsList.add(StandardCompressionOptions.deflate(configuration.compression_deflate_compressionLevel(), configuration.compression_deflate_windowBits(), configuration.compression_deflate_memLevel()));
		compressionOptionsList.add(StandardCompressionOptions.gzip(configuration.compression_gzip_compressionLevel(), configuration.compression_gzip_windowBits(), configuration.compression_gzip_memLevel()));
		if(Zstd.isAvailable()) {
			compressionOptionsList.add(StandardCompressionOptions.zstd(configuration.compression_zstd_compressionLevel(), configuration.compression_zstd_blockSize(), configuration.compression_zstd_maxEncodeSize()));
		}

		// Brotli lib is currently an unnamed module so we can't configure it...
		/*if(Brotli.isAvailable()) {
			compressionOptionsList.add(StandardCompressionOptions.brotli(new Encoder.Parameters().setQuality(configuration.compression_brotli_quality()).setMode(configuration.compression_brotli_mode()).setWindow(configuration.compression_brotli_window())));
		}*/

		return compressionOptionsList.stream().toArray(CompressionOptions[]::new);
	}
}
