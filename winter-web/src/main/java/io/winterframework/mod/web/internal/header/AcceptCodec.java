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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.BeanSocket;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class AcceptCodec extends ParameterizedHeaderCodec<AcceptCodec.Accept, AcceptCodec.Accept.Builder> {

	@BeanSocket
	public AcceptCodec() {
		this(true);
	}
	
	public AcceptCodec(boolean allowMultiple) {
		super(AcceptCodec.Accept.Builder::new, Set.of(Headers.ACCEPT), DEFAULT_PARAMETER_DELIMITER, DEFAULT_VALUE_DELIMITER, false, false, false, false, true, allowMultiple);
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

		private List<Headers.Accept.MediaRange> ranges;
		
		private Accept(String headerName, String headerValue, List<Headers.Accept.MediaRange> ranges) {
			super(headerName, headerValue, null, null);
			this.ranges = ranges != null ? ranges.stream().sorted(Headers.Accept.MediaRange.COMPARATOR).collect(Collectors.toList()) : List.of(new MediaRange("*/*", 1, null));
		}

		@Override
		public List<Headers.Accept.MediaRange> getMediaRanges() {
			return this.ranges;
		}
		
		public static class MediaRange implements Headers.Accept.MediaRange {

			private String mediaType;
			
			private String type;
			
			private String subType;
			
			private float weight;
			
			private Map<String, String> parameters;
			
			private int score;
			
			public MediaRange(String mediaType, float weight, Map<String, String> parameters) {
				this.setMediaType(mediaType);
				
				this.weight = (int)(weight * 1000) / 1000f;
				this.parameters = parameters != null ? Collections.unmodifiableMap(parameters) : Map.of();
				this.score = Headers.Accept.MediaRange.super.getScore();
			}
			
			public MediaRange(String type, String subType, float weight, Map<String, String> parameters) {
				this.setType(type.toLowerCase());
				this.setSubType(subType.toLowerCase());
				
				this.weight = (int)(weight * 1000) / 1000f;
				this.parameters = parameters != null ? Collections.unmodifiableMap(parameters) : Map.of();
				this.score = Headers.Accept.MediaRange.super.getScore();
			}
			
			@Override
			public String getMediaType() {
				if(this.mediaType == null) {
					this.mediaType = this.type + "/" + this.subType;
				}
				return this.mediaType;
			}
			
			private void setMediaType(String mediaType) {
				this.mediaType = mediaType.toLowerCase();
				String[] splitMediaType = this.mediaType.split("/");
				if(splitMediaType.length == 2) {
					this.setType(splitMediaType[0]);
					this.setSubType(splitMediaType[1]);
				}
				else if(splitMediaType.length == 1) {
					this.setType(splitMediaType[0]);
					this.setSubType("*");
				}
				else {
					// TODO => Not Acceptable
					throw new RuntimeException("Not Acceptable: empty media type");
				}
			}
			
			@Override
			public String getType() {
				return this.type;
			}
			
			private void setType(String type) {
				if(!type.equals("*") && !HeaderService.isToken(type)) {
					// TODO Not Acceptable
					throw new RuntimeException("Not Acceptable: invalid media type");
				}
				this.type = type;
			}
			
			@Override
			public String getSubType() {
				return this.subType;
			}
			
			private void setSubType(String subType) {
				if(!subType.equals("*") && !HeaderService.isToken(subType)) {
					// TODO Not Acceptable
					throw new RuntimeException("Not Acceptable: invalid media type");
				}
				this.subType = subType;
			}

			@Override
			public float getWeight() {
				return this.weight;
			}
			
			@Override
			public int getScore() {
				return this.score;
			}
			
			@Override
			public Map<String, String> getParameters() {
				return this.parameters;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((mediaType == null) ? 0 : mediaType.hashCode());
				result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
				result = prime * result + Float.floatToIntBits(weight);
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
				MediaRange other = (MediaRange) obj;
				if (mediaType == null) {
					if (other.mediaType != null)
						return false;
				} else if (!mediaType.equals(other.mediaType))
					return false;
				if (parameters == null) {
					if (other.parameters != null)
						return false;
				} else if (!parameters.equals(other.parameters))
					return false;
				if (Float.floatToIntBits(weight) != Float.floatToIntBits(other.weight))
					return false;
				return true;
			}
		}
		
		public static final class Builder extends ParameterizedHeader.AbstractBuilder<Accept, Builder> {

			private List<Headers.Accept.MediaRange> ranges;
			
			private float weight = 1;
			
			private void addCurrentRange() {
				if(this.parameterizedValue != null) {
					if(this.ranges == null) {
						this.ranges = new ArrayList<>();
					}
					this.ranges.add(new MediaRange(this.parameterizedValue, this.weight, this.parameters));
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
					this.weight = Float.parseFloat(value);
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