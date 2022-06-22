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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.inverno.mod.base.converter.SplittablePrimitiveDecoder;
import io.inverno.mod.configuration.AbstractHashConfigurationSource;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.internal.JavaStringConverter;
import io.inverno.mod.configuration.internal.parser.option.ConfigurationOptionParser;
import io.inverno.mod.configuration.internal.parser.option.ParseException;
import io.inverno.mod.configuration.internal.parser.option.StringProvider;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A configuration source that looks up properties from command line arguments.
 * </p>
 *
 * <p>
 * Configuration properties are specified as application arguments using the following syntax (ABNF):
 * </p>
 *
 * <blockquote>
 *
 * <pre>
 * argument        = "--" {@literal property_name} [ "[" *(parameter ",") "]" ] "=" property_value
 *
 * property_name   = java_name
 *
 * property_value  = java_integer_literal
 *                 / java_floating_point_literal
 *                 / java_string_literal
 *                 / java_boolean_literal
 *                 / "unset"
 *                 / "null"
 *
 * parameter       = parameter_name "=" parameter_value
 *
 * parameter_name  = java_identifier
 *
 * parameter_value = java_integer_literal
 *                 / java_floating_point_literal
 *                 / java_string_literal
 *                 / java_boolean_literal
 *
 * </pre>
 *
 * </blockquote>
 *
 * <p>
 * The following are valid configuration properties passed as command line arguments:
 * </p>
 *
 * <ul>
 * <li>{@code --web.server_port=8080}</li>
 * <li>{@code --web.server_port[profile="ssl"]=8443}</li>
 * <li>{@code --db.url[env="dev"]="jdbc:oracle:thin:@dev.db.server:1521:sid"}</li>
 * <li>{@code --db.url[env="prod",zone="eu"]="jdbc:oracle:thin:@prod_eu.db.server:1521:sid"}</li>
 * <li>{@code --db.url[env="prod",zone="us"]="jdbc:oracle:thin:@prod_us.db.server:1521:sid"}</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see AbstractHashConfigurationSource
 */
public class CommandLineConfigurationSource extends AbstractHashConfigurationSource<String, CommandLineConfigurationSource> {

	private static final Logger LOGGER = LogManager.getLogger(CommandLineConfigurationSource.class);
	
	private List<String> args;
	
	/**
	 * <p>
	 * Creates a command line configuration source with the specified arguments using a Java String value decoder.
	 * </p>
	 *
	 * @param args the command line arguments
	 */
	public CommandLineConfigurationSource(String[] args) {
		this(args, new JavaStringConverter());
	}
	
	/**
	 * <p>
	 * Creates a command line configuration source with the specified arguments and the specified string value decoder.
	 * </p>
	 *
	 * @param args    the command line arguments
	 * @param decoder a string decoder
	 */
	public CommandLineConfigurationSource(String[] args, SplittablePrimitiveDecoder<String> decoder) {
		super(decoder);
		this.args = Arrays.stream(args)
			.filter(arg -> arg.startsWith("--"))
			.map(arg -> arg.substring(2))
			.collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Creates a command line configuration source from the specified initial source and using the specified defaulting strategy.
	 * </p>
	 *
	 * @param initial            the initial configuration source.
	 * @param defaultingStrategy a defaulting strategy
	 */
	private CommandLineConfigurationSource(CommandLineConfigurationSource initial, DefaultingStrategy defaultingStrategy) {
		super(initial, defaultingStrategy);
		this.args = initial.args;
	}
	
	@Override
	public CommandLineConfigurationSource withDefaultingStrategy(DefaultingStrategy defaultingStrategy) {
		return new CommandLineConfigurationSource(this.initial != null ? this.initial : this, defaultingStrategy);
	}
	
	@Override
	protected Mono<List<ConfigurationProperty>> load() {
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
