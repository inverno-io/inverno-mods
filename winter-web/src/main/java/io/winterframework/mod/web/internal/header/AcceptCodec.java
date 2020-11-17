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
package io.winterframework.mod.web.internal.header;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.Headers;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class AcceptCodec extends ParameterizedHeaderCodec<AcceptCodec.Accept, AcceptCodec.Accept.Builder> {

	public AcceptCodec() {
		super(AcceptCodec.Accept.Builder::new, Set.of(Headers.ACCEPT), DEFAULT_PARAMETER_DELIMITER, DEFAULT_VALUE_DELIMITER, false, false, false, false, true, true);
	}
	
	@Override
	public String encodeValue(Accept headerField) {
		return headerField.getMediaRanges().stream().map(range -> {
			StringBuilder result = new StringBuilder();
			
			result.append(range.getType()).append("/").append(range.getSubType());
			result.append(";q=").append(String.format("%.3f", range.getWeight()));
			
			Map<String, String> parameters = range.getParameters();
			if(!parameters.isEmpty()) {
				parameters.entrySet().stream().forEach(e -> {
					result.append(this.parameterDelimiter).append(e.getKey()).append("=").append(e.getValue());
				});
			}
			return result.toString();
		}).collect(Collectors.joining(Character.toString(this.valueDelimiter)));
	}

	public static final class Accept extends ParameterizedHeader implements Headers.Accept {

		private List<Headers.MediaRange> ranges;
		
		private Accept(String headerName, String headerValue, List<Headers.MediaRange> ranges) {
			super(headerName, headerValue, null, null);
			this.ranges = ranges != null ? ranges.stream().sorted(Headers.MediaRange.COMPARATOR).collect(Collectors.toList()) : List.of(new GenericMediaRange("*/*", 1, null));
		}

		@Override
		public List<Headers.MediaRange> getMediaRanges() {
			return this.ranges;
		}
		
		public static class Builder extends ParameterizedHeader.AbstractBuilder<Accept, Builder> {

			private List<Headers.MediaRange> ranges;
			
			private float weight = 1;
			
			private void addCurrentRange() {
				if(this.parameterizedValue != null) {
					if(this.ranges == null) {
						this.ranges = new ArrayList<>();
					}
					this.ranges.add(new GenericMediaRange(this.parameterizedValue, this.weight, this.parameters));
				}
				this.parameters = null;
				this.weight = 1;
			}
			
			@Override
			public Builder parameterizedValue(String parameterizedValue) {
				this.addCurrentRange();
				this.parameterizedValue = parameterizedValue;
				return this;
			}
			
			@Override
			public Builder parameter(String name, String value) {
				if(name.equals("q")) {
					this.weight = (int)(Float.parseFloat(value) * 1000) / 1000f;
					if(this.weight == 0) {
						// TODO 0 => Not Acceptable
						throw new RuntimeException("Not Acceptable: 0 weight");
					}
				}
				else {
					super.parameter(name, value);
				}
				return this;
			}
			
			@Override
			public Accept build() {
				this.addCurrentRange();
				return new Accept(this.headerName, this.headerValue, this.ranges);
			}
		}
	}
}
