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
package io.inverno.mod.http.base.internal.header;

import io.inverno.mod.http.base.header.ParameterizedHeader;
import io.inverno.mod.http.base.header.ParameterizedHeaderCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.BeanSocket;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.header.HeaderBuilder;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;

/**
 * <p>
 * Accept HTTP {@link HeaderCodec} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ParameterizedHeaderCodec
 */
@Bean(visibility = Visibility.PRIVATE)
public class AcceptCodec extends ParameterizedHeaderCodec<AcceptCodec.Accept, AcceptCodec.Accept.Builder> {

	/**
	 * <p>
	 * Creates an accept header codec that allows multiple media ranges to be
	 * specified in the header value.
	 * </p>
	 */
	@BeanSocket
	public AcceptCodec() {
		this(true);
	}
	
	/**
	 * <p>
	 * Creates an accept header codec that allows or not multiple media ranges to be
	 * specified in the header value.
	 * </p>
	 * 
	 * @param allowMultiple true to allow multiple media ranges, false otherwise
	 */
	public AcceptCodec(boolean allowMultiple) {
		super(AcceptCodec.Accept.Builder::new, Set.of(Headers.NAME_ACCEPT), DEFAULT_PARAMETER_DELIMITER, DEFAULT_PARAMETER_DELIMITER, DEFAULT_VALUE_DELIMITER, false, false, false, false, true, allowMultiple);
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
		}).collect(Collectors.joining(Character.toString(this.parameterValueDelimiter)));
	}

	/**
	 * <p>
	 * {@link Headers.Accept} header implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ParameterizedHeader
	 */
	public static final class Accept extends ParameterizedHeader implements Headers.Accept {

		private List<Headers.Accept.MediaRange> ranges;
		
		/**
		 * <p>
		 * Creates an accept header with the specified list of media range.
		 * </p>
		 * 
		 * @param ranges A list of media range.
		 */
		public Accept(List<Headers.Accept.MediaRange> ranges) {
			super(Headers.NAME_ACCEPT, null, null, null);
			this.ranges = ranges != null && !ranges.isEmpty() ? ranges.stream().sorted(Headers.Accept.MediaRange.COMPARATOR).collect(Collectors.toList()) : List.of(new MediaRange("*/*", 1, null));
		}
		
		private Accept(String headerName, String headerValue, List<Headers.Accept.MediaRange> ranges) {
			super(headerName, headerValue, null, null);
			this.ranges = ranges != null && !ranges.isEmpty() ? ranges.stream().sorted(Headers.Accept.MediaRange.COMPARATOR).collect(Collectors.toList()) : List.of(new MediaRange("*/*", 1, null));
		}

		@Override
		public List<Headers.Accept.MediaRange> getMediaRanges() {
			return this.ranges;
		}
		
		/**
		 * <p>
		 * {@link Headers.Accept.MediaRange} implementation.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.0
		 */
		public static class MediaRange implements Headers.Accept.MediaRange {

			private String mediaType;
			
			private String type;
			
			private String subType;
			
			private float weight;
			
			private Map<String, String> parameters;
			
			private int score;
			
			/**
			 * <p>
			 * Creates a media range with the specified media type, quality value and
			 * parameters.
			 * </p>
			 * 
			 * @param mediaType  a media type
			 * @param weight     a quality value
			 * @param parameters a map of parameters
			 */
			public MediaRange(String mediaType, float weight, Map<String, String> parameters) {
				this.setMediaType(mediaType);
				
				this.weight = (int)(weight * 1000) / 1000f;
				this.parameters = parameters != null ? Collections.unmodifiableMap(parameters) : Map.of();
				this.score = Headers.Accept.MediaRange.super.getScore();
			}
			
			/**
			 * <p>
			 * Creates a media range with the specified type, sub-type, quality value and
			 * parameters.
			 * </p>
			 * 
			 * @param type       a type
			 * @param subType    a sub-type
			 * @param weight     a quality value
			 * @param parameters a map of parameters
			 */
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
				switch (splitMediaType.length) {
					case 2:
						this.setType(splitMediaType[0]);
						this.setSubType(splitMediaType[1]);
						break;
					case 1:
						this.setType(splitMediaType[0]);
						this.setSubType("*");
						break;
					default:
					throw new NotAcceptableException("Empty media type");
				}
			}
			
			@Override
			public String getType() {
				return this.type;
			}
			
			private void setType(String type) {
				if(!type.equals("*") && !HeaderService.isToken(type)) {
					throw new NotAcceptableException("Invalid media Type: " + type);
				}
				this.type = type;
			}
			
			@Override
			public String getSubType() {
				return this.subType;
			}
			
			private void setSubType(String subType) {
				if(!subType.equals("*") && !HeaderService.isToken(subType)) {
					throw new NotAcceptableException("Invalid media Type: " + type);
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
		
		/**
		 * <p>
		 * Accept {@link HeaderBuilder} implementation
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.0
		 * 
		 * @see ParameterizedHeader.AbstractBuilder
		 */
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
						throw new NotAcceptableException("Invalid weight: " + weight);
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
