/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.configuration.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceException;
import io.inverno.mod.configuration.AbstractHashConfigurationSource;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.internal.JavaStringConverter;
import io.inverno.mod.configuration.internal.parser.properties.ConfigurationPropertiesParser;
import io.inverno.mod.configuration.internal.parser.properties.ParseException;
import io.inverno.mod.configuration.internal.parser.properties.StreamProvider;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A configuration source that looks up properties in a configuration properties file following the {@code .cprops} file format.
 * </p>
 *
 * <p>
 * This source supports parameterized configuration properties defined in a configuration file as follows:
 * </p>
 *
 * <blockquote>
 *
 * <pre>
 * web {
 *     server_port=8080
 *     [ profile = "ssl" ] {
 *         server_port=8443
 *     }
 * }
 *
 * [ env="dev" ] {
 *     db.url="jdbc:oracle:thin:@dev.db.server:1521:sid"
 * }
 *
 * [ env="prod" ] {
 *     db.url="jdbc:oracle:thin:@dev.db.server:1521:sid"
 *     [ zone="eu" ] {
 *         db.url="jdbc:oracle:thin:@prod_eu.db.server:1521:sid"
 *     }
 *     [ zone="us" ] {
 *         db.url="jdbc:oracle:thin:@prod_us.db.server:1521:sid"
 *     }
 * }
 * </pre>
 *
 * </blockquote>
 *
 * <p>
 * Please refer to the {@code .cprops} format definition for more information on how to create parameterized configuration in the {@code .cprops} format.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see AbstractHashConfigurationSource
 */
public class CPropsFileConfigurationSource extends AbstractHashConfigurationSource<String, CPropsFileConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(CPropsFileConfigurationSource.class);
	
	private Path propertyFile;
	
	private Resource propertyResource;
	
	private InputStream propertyInput;
	
	private Duration propertiesTTL;
	
	private Mono<List<ConfigurationProperty>> properties;
	
	/**
	 * <p>
	 * Creates a {@code .cprops} file configuration source with the file at the specified path.
	 * </p>
	 *
	 * @param propertyFile the path to the {@code .cprops} file
	 */
	public CPropsFileConfigurationSource(Path propertyFile) {
		this(propertyFile, new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a {@code .cprops} file configuration source with the file at the specified path and the specified string value decoder.
	 * </p>
	 *
	 * @param propertyFile the path to the {@code .cprops} file
	 * @param decoder      a string decoder
	 */
	public CPropsFileConfigurationSource(Path propertyFile, SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
		this.propertyFile = propertyFile;
	}

	/**
	 * <p>
	 * Creates a {@code .cprops} file configuration source with the specified input stream.
	 * </p>
	 *
	 * @param propertyInput the {@code .cprops} input
	 */
	public CPropsFileConfigurationSource(InputStream propertyInput) {
		this(propertyInput, new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a {@code .cprops} file configuration source with the specified input stream and string value decoder.
	 * </p>
	 *
	 * @param propertyInput the {@code .cprops} input
	 * @param decoder       a string decoder
	 */
	public CPropsFileConfigurationSource(InputStream propertyInput, SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
		this.propertyInput = propertyInput;
	}

	/**
	 * <p>
	 * Creates a {@code .cprops} file configuration source with the specified resource
	 * </p>
	 * 
	 * @param propertyResource the {@code .properties} resource
	 */
	public CPropsFileConfigurationSource(Resource propertyResource) {
		this(propertyResource, new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a {@code .cprops} file configuration source with the specified resource and string value decoder.
	 * </p>
	 *
	 * @param propertyResource the {@code .properties} resource
	 * @param decoder          a string decoder
	 */
	public CPropsFileConfigurationSource(Resource propertyResource, SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
		this.propertyResource = propertyResource;
	}
	
	/**
	 * <p>
	 * Sets the time-to-live duration of the properties loaded with {@link #load()}.
	 * </p>
	 *
	 * <p>
	 * If set to null, which is the default, properties are cached indefinitely.
	 * </p>
	 *
	 * <p>
	 * Note that this ttl doesn't apply to a source created with an {@link InputStream} which is cached indefinitely since the steam can't be read twice.
	 * </p>
	 *
	 * @param ttl the properties time-to-live or null to cache properties indefinitely
	 */
	public void setPropertiesTTL(Duration ttl) {
		this.propertiesTTL = ttl;
		this.properties = null;
	}
	
	/**
	 * <p>
	 * Returns the time-to-live duration of the properties loaded with {@link #load()}.
	 * </p>
	 *
	 * @return the properties time-to-live or null if properties are cached indefinitely
	 */
	public Duration getPropertiesTTL() {
		return this.propertiesTTL;
	}

	/**
	 * <p>
	 * Opens an input stream to read the {@code .cprops} file.
	 * </p>
	 *
	 * @return An input stream
	 *
	 * @throws IOException       if there was an I/O error opening the file
	 * @throws ResourceException if there was an error opening the resource
	 */
	private InputStream open() throws IOException, ResourceException {
		if(this.propertyFile != null) {
			return Files.newInputStream(this.propertyFile);
		}
		else if(this.propertyResource != null) {
			return this.propertyResource.openReadableByteChannel().map(Channels::newInputStream).orElseThrow(() -> new ResourceException("Property file " + this.propertyResource.getURI() + " is not readable"));
		}
		else {
			return this.propertyInput;
		}
	}
	
	@Override
	protected Mono<List<ConfigurationProperty>> load() {
		if(this.properties == null) {
			this.properties = Mono.defer(() -> {
				try(InputStream input = this.open()) {
					ConfigurationPropertiesParser<CPropsFileConfigurationSource> parser = new ConfigurationPropertiesParser<>(new StreamProvider(input));
					parser.setConfigurationSource(this);
					return Mono.just(parser.StartConfigurationProperties());
				} 
				catch (IOException | ParseException | ResourceException e) {
					LOGGER.warn(() -> "Invalid property file: " + e.getMessage());
					return Mono.error(e);
				}
			});
			
			if(this.propertyInput != null) {
				this.properties = this.properties.cache();
			}
			else if(this.propertiesTTL != null) {
				this.properties = this.properties.cache(this.propertiesTTL);
			}
		}
		return this.properties;
	}
}
