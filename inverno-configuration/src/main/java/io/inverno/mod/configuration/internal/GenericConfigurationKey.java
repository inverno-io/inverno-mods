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
package io.inverno.mod.configuration.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.inverno.mod.configuration.ConfigurationKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * <p>
 * Generic {@link ConfigurationKey} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationKey
 */
public class GenericConfigurationKey implements ConfigurationKey {

	protected final String name;
	
	protected final Collection<Parameter> parameters;
	
	protected Map<String, Parameter> parametersByKey;
	
	/**
	 * <p>
	 * Creates a key with the specified property name.
	 * </p>
	 * 
	 * @param name a property name
	 * 
	 * @throws IllegalArgumentException if the specified name is empty
	 */
	public GenericConfigurationKey(String name) throws IllegalArgumentException {
		this(name, null);
	}
	
	/**
	 * <p>
	 * Creates a key with the specified property name and parameters.
	 * </p>
	 * 
	 * @param name       a property name
	 * @param parameters a collection of parameters
	 * 
	 * @throws IllegalArgumentException if the specified name is empty
	 */
	public GenericConfigurationKey(String name, Collection<Parameter> parameters) throws IllegalArgumentException {
		if(name == null || name.equals("")) {
			throw new IllegalArgumentException("Name can't be null or empty");
		}
		this.name = name;
		this.parameters = parameters != null ? Collections.unmodifiableCollection(parameters) : List.of();
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public Collection<Parameter> getParameters() {
		return this.parameters;
	}

	@Override
	public Optional<Parameter> getParameter(String key) {
		if(this.parametersByKey == null) {
			this.parametersByKey = this.parameters.stream().collect(Collectors.toMap(Parameter::getKey, Function.identity()));
		}
		return Optional.ofNullable(this.parametersByKey.get(key));
	}

	@Override
	public boolean matches(ConfigurationKey other, boolean exact) {
		Objects.requireNonNull(other);
		if(!this.name.equals(other.getName())) {
			return false;
		}
		
		Map<String, Parameter> thisParameters = this.parameters.stream().collect(Collectors.toMap(Parameter::getKey, p -> p));
		Map<String, Parameter> otherParameters = new HashMap<>();
		for(Parameter p : other.getParameters()) {
			if(p.isUndefined()) {
				Parameter thisParameter = thisParameters.get(p.getKey());
				if(thisParameter != null && !thisParameter.isUndefined()) {
					return false;
				}
				thisParameters.put(p.getKey(), p);
			}
			otherParameters.put(p.getKey(), p);
		}
		
		if(exact && thisParameters.size() != otherParameters.size()) {
			return false;
		}
		
		for(String key : otherParameters.keySet()) {
			Parameter otherParameter = otherParameters.get(key);
			Parameter thisParameter = thisParameters.get(key);
			
			if(thisParameter == null) {
				return false;
			}
			
			if(otherParameter.isUndefined()) {
				return thisParameter.isUndefined();
			}
			
			// Keep this for clarity as it is already handled above
			if(thisParameter.isUndefined()) {
				return false;
			}
			
			if(otherParameter.isWildcard() || thisParameter.isWildcard()) {
				continue;
			}

			if(!otherParameter.getValue().equals(thisParameter.getValue())) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parameters == null) ? 0 : this.parameters.stream().collect(Collectors.toMap(Parameter::getKey, Parameter::getValue)).hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericConfigurationKey other = (GenericConfigurationKey) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (parameters.size() != other.parameters.size() || !parameters.containsAll(other.parameters))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.name + (this.parameters.isEmpty() ? "" : "[" + this.parameters.stream().map(Parameter::toString).collect(Collectors.joining(",")) + "]");
	}
}
