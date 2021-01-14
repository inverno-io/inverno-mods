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
package io.winterframework.mod.configuration.source;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.winterframework.mod.base.converter.PrimitiveDecoder;
import io.winterframework.mod.base.converter.StringConverter;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.ConfigurationProperty;
import io.winterframework.mod.configuration.internal.AbstractHashConfigurationSource;
import io.winterframework.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.winterframework.mod.configuration.internal.parser.option.ParseException;
import io.winterframework.mod.configuration.internal.parser.option.StringProvider;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class CommandLineConfigurationSource extends AbstractHashConfigurationSource<String, CommandLineConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(CommandLineConfigurationSource.class);
	
	private List<String> args;
	
	public CommandLineConfigurationSource(String[] args) {
		this(args, new StringConverter());
	}
	
	public CommandLineConfigurationSource(String[] args, PrimitiveDecoder<String> decoder) {
		super(decoder);
		this.args = Arrays.stream(args)
			.filter(arg -> arg.startsWith("--"))
			.map(arg -> arg.substring(2))
			.collect(Collectors.toList());
	}
	
	@Override
	protected Mono<List<ConfigurationProperty<ConfigurationKey, CommandLineConfigurationSource>>> load() {
		return Mono.defer(() -> Mono.just(this.args.stream()
				.map(option -> {
					ConfigurationOptionParser<CommandLineConfigurationSource> parser = new ConfigurationOptionParser<>(new StringProvider(option));
					parser.setConfigurationSource(this);
					try {
						return parser.StartOption();
					} 
					catch (ParseException e) {
						LOGGER.warn(() -> "Ignoring option: " + option + " after parsing error: " + e.getMessage());
					}
					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList())
			)
		);
	}
}
