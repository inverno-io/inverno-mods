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
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.BeanSocket;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.NotAcceptableException;
import io.winterframework.mod.web.header.Headers;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class AcceptLanguageCodec extends ParameterizedHeaderCodec<AcceptLanguageCodec.AcceptLanguage, AcceptLanguageCodec.AcceptLanguage.Builder> {

	@BeanSocket
	public AcceptLanguageCodec() {
		this(true);
	}
	
	public AcceptLanguageCodec(boolean allowMultiple) {
		super(AcceptLanguageCodec.AcceptLanguage.Builder::new, Set.of(Headers.NAME_ACCEPT_LANGUAGE), DEFAULT_PARAMETER_DELIMITER, DEFAULT_VALUE_DELIMITER, false, false, false, false, true, allowMultiple);
	}
	
	@Override
	public String encodeValue(AcceptLanguage headerField) {
		return headerField.getLanguageRanges().stream().map(range -> {
			StringBuilder result = new StringBuilder();
			
			result.append(range.getLanguageTag());
			result.append(";q=").append(String.format("%.3f", range.getWeight()));
			return result.toString();
		}).collect(Collectors.joining(Character.toString(this.valueDelimiter)));
	}

	public static final class AcceptLanguage extends ParameterizedHeader implements Headers.AcceptLanguage {

		private List<Headers.AcceptLanguage.LanguageRange> ranges;
		
		private AcceptLanguage(String headerName, String headerValue, List<Headers.AcceptLanguage.LanguageRange> ranges) {
			super(headerName, headerValue, null, null);
			this.ranges = ranges != null ? ranges.stream().sorted(Headers.AcceptLanguage.LanguageRange.COMPARATOR).collect(Collectors.toList()) : List.of(new AcceptLanguageCodec.AcceptLanguage.LanguageRange("*", 1));
		}

		@Override
		public List<Headers.AcceptLanguage.LanguageRange> getLanguageRanges() {
			return this.ranges;
		}
		
		public static final class LanguageRange implements Headers.AcceptLanguage.LanguageRange {

			private String languageTag;
			
			private String primarySubTag;
			
			private String secondarySubTag;
			
			private float weight;
			
			private int score;
			
			public LanguageRange(String languageTag, float weight) {
				this.languageTag = languageTag;
				this.weight = weight;
				
				String[] splitLanguageTag = this.languageTag.split("-");
				if(splitLanguageTag.length == 2) {
					this.setPrimarySubTag(splitLanguageTag[0]);
					this.setSecondarySubTag(splitLanguageTag[1]);
				}
				else if(splitLanguageTag.length == 1) {
					this.setPrimarySubTag(splitLanguageTag[0]);
				}
				else {
					throw new NotAcceptableException("Empty language tag");
				}
				
				this.score = Headers.AcceptLanguage.LanguageRange.super.getScore();
			}
			
			@Override
			public String getLanguageTag() {
				return this.languageTag;
			}
			
			@Override
			public String getPrimarySubTag() {
				return this.primarySubTag;
			}
			
			private void setPrimarySubTag(String primarySubTag) {
				if(!primarySubTag.equals("*")) {
					byte size = 0;
					for(byte b : primarySubTag.getBytes()) {
						size++;
						if( !((b >= 0x41 && b <= 0x5A) || (b >= 0x61 && b <= 0x7A)) || size > 8) {
							throw new NotAcceptableException("Invalid language tag");
						}
					}
					if(size == 0) {
						throw new NotAcceptableException("Invalid language tag");
					}
				}
				this.primarySubTag = primarySubTag;
			}
			
			@Override
			public String getSecondarySubTag() {
				return this.secondarySubTag;
			}
			
			private void setSecondarySubTag(String secondarySubTag) {
				if(this.primarySubTag.equals("*")) {
					throw new NotAcceptableException("Invalid language tag");
				}
				byte size = 0;
				for(byte b : secondarySubTag.getBytes()) {
					size++;
					if( !((b >= 0x41 && b <= 0x5A) || (b >= 0x61 && b <= 0x7A) || Character.isDigit(b)) || size > 8) {
						throw new NotAcceptableException("Invalid language tag: " + this.languageTag);
					}
				}
				if(size == 0) {
					throw new NotAcceptableException("Invalid language tag");
				}
				this.secondarySubTag = secondarySubTag;
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
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((languageTag == null) ? 0 : languageTag.hashCode());
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
				LanguageRange other = (LanguageRange) obj;
				if (languageTag == null) {
					if (other.languageTag != null)
						return false;
				} else if (!languageTag.equals(other.languageTag))
					return false;
				if (Float.floatToIntBits(weight) != Float.floatToIntBits(other.weight))
					return false;
				return true;
			}
		}
		
		public static final class Builder extends ParameterizedHeader.AbstractBuilder<AcceptLanguage, Builder> {

			private List<Headers.AcceptLanguage.LanguageRange> ranges;
			
			private float weight = 1;
			
			private void addCurrentRange() {
				if(this.parameterizedValue != null) {
					if(this.ranges == null) {
						this.ranges = new ArrayList<>();
					}
					this.ranges.add(new AcceptLanguageCodec.AcceptLanguage.LanguageRange(this.parameterizedValue, this.weight));
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
						throw new NotAcceptableException("Invalid weight: " + weight);
					}
				}
				// TODO ignore or report a malformed header?
				//else {}
				return this;
			}
			
			@Override
			public AcceptLanguage build() {
				this.addCurrentRange();
				return new AcceptLanguage(this.headerName, this.headerValue, this.ranges);
			}
		}
	}
}
