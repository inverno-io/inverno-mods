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

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.winterframework.mod.configuration.ConfigurationEntry;
import io.winterframework.mod.configuration.ConfigurationKey;
import io.winterframework.mod.configuration.converter.StringValueConverter;
import io.winterframework.mod.configuration.internal.AbstractHashConfigurationSource;
import io.winterframework.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.winterframework.mod.configuration.internal.parser.option.ParseException;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class CommandLineConfigurationSource extends AbstractHashConfigurationSource<String, CommandLineConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(CommandLineConfigurationSource.class);
	
	private List<String> args;
	
	public CommandLineConfigurationSource(String... args) {
		super(new StringValueConverter());
		this.args = Arrays.stream(args)
			.filter(arg -> arg.startsWith("--"))
			.map(arg -> arg.substring(2))
			.collect(Collectors.toList());
	}
	
	@Override
	protected Mono<List<ConfigurationEntry<ConfigurationKey, CommandLineConfigurationSource>>> load() {
		return Mono.defer(() -> Mono.just(this.args.stream()
				.map(option -> {
					ConfigurationOptionParser<CommandLineConfigurationSource> parser = new ConfigurationOptionParser<>(new StringReader(option));
					parser.setConfigurationSource(this);
					parser.setValueConverter(this.converter);
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
