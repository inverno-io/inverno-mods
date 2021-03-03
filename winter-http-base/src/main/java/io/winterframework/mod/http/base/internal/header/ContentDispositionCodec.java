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
package io.winterframework.mod.http.base.internal.header;

import java.util.Map;
import java.util.Set;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.http.base.header.HeaderService;
import io.winterframework.mod.http.base.header.Headers;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class ContentDispositionCodec extends ParameterizedHeaderCodec<ContentDispositionCodec.ContentDisposition, ContentDispositionCodec.ContentDisposition.Builder> {
	
	public ContentDispositionCodec() {
		super(ContentDispositionCodec.ContentDisposition.Builder::new, Set.of(Headers.NAME_CONTENT_DISPOSITION), DEFAULT_PARAMETER_DELIMITER, DEFAULT_VALUE_DELIMITER, false, false, false, false, true, false);
	}

	public static final class ContentDisposition extends ParameterizedHeader implements Headers.ContentDisposition {

		public static final String NAME = "name";
		public static final String FILENAME = "filename";
		public static final String CREATION_DATE = "creation-date";
		public static final String MODIFICATION_DATE = "modification-date";
		public static final String READ_DATE = "read-date";
		public static final String SIZE = "size";
		
		private String dispositionType;
		private String partName;
		private String filename;
		private String creationDateTime; // Mon, 12 Oct 2020 15:46:07 GMT
		private String modificationDatetime;
		private String readDateTime;
		private Integer size;
		
		private ContentDisposition(String headerName, 
				String headerValue, 
				String dispositionType, 
				Map<String, String> parameters,
				String partName, 
				String filename,
				String creationDateTime, 
				String modificationDatetime, 
				String readDateTime, 
				Integer size) {
			super(Headers.NAME_CONTENT_DISPOSITION, headerValue, dispositionType, parameters);
			
			this.dispositionType = dispositionType;
			this.partName = partName;
			this.filename = filename;
			this.creationDateTime = creationDateTime;
			this.modificationDatetime = modificationDatetime;
			this.readDateTime = readDateTime;
			this.size = size;
			
			if(!HeaderService.isToken(this.dispositionType)) { 
				throw new MalformedHeaderException("Invalid content disposition type");
			}
		}
		
		public String getDispositionType() {
			return dispositionType;
		}
		
		public String getPartName() {
			return partName;
		}
		
		public String getFilename() {
			return filename;
		}
		
		public String getCreationDateTime() {
			return creationDateTime;
		}
		
		public String getModificationDatetime() {
			return modificationDatetime;
		}
		
		public String getReadDateTime() {
			return readDateTime;
		}
		
		public Integer getSize() {
			return size;
		}
		
		public static final class Builder extends ParameterizedHeader.AbstractBuilder<ContentDisposition, Builder> {

			private String partName;
			private String filename;
			private String creationDateTime; // Mon, 12 Oct 2020 15:46:07 GMT
			private String modificationDatetime;
			private String readDateTime;
			private Integer size;
			
			@Override
			public Builder parameter(String name, String value) {
				if(name.equals(NAME)) {
					this.partName = value;
				}
				else if(name.equals(FILENAME)) {
					this.filename = value;
				}
				else if(name.equals(CREATION_DATE)) {
					this.creationDateTime = value;
				}
				else if(name.equals(MODIFICATION_DATE)) {
					this.modificationDatetime = value;
				}
				else if(name.equals(READ_DATE)) {
					this.readDateTime = value;
				}
				else if(name.equals(SIZE)) {
					this.size = Integer.parseInt(value);
				}
				return super.parameter(name, value);
			}
			
			@Override
			public ContentDisposition build() {
				return new ContentDisposition(this.headerName, this.headerValue, this.parameterizedValue, this.parameters, this.partName, this.filename, this.creationDateTime, this.modificationDatetime, this.readDateTime, this.size);
			}
		}
	}
}
