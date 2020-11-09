/**
 * 
 */
package io.winterframework.mod.web.internal.header;

import java.util.Map;
import java.util.Set;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.Headers;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class ContentDispositionCodec extends ParameterizedHeaderCodec<ContentDispositionCodec.ContentDisposition, ContentDispositionCodec.ContentDisposition.Builder> {
	
	
	public ContentDispositionCodec() {
		super(ContentDispositionCodec.ContentDisposition.Builder::new, Set.of(Headers.CONTENT_DISPOSITION), DEFAULT_DELIMITER, false, false, false, false, true);
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
				String partName, 
				String filename,
				String creationDateTime, 
				String modificationDatetime, 
				String readDateTime, 
				Integer size,
				Map<String, String> parameters) {
			super(Headers.CONTENT_DISPOSITION, headerValue, dispositionType, parameters);
			
			this.dispositionType = dispositionType;
			this.partName = partName;
			this.filename = filename;
			this.creationDateTime = creationDateTime;
			this.modificationDatetime = modificationDatetime;
			this.readDateTime = readDateTime;
			this.size = size;
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
				return new ContentDisposition(this.headerName, this.headerValue, this.parameterizedValue, this.partName, this.filename, this.creationDateTime, this.modificationDatetime, this.readDateTime, this.size, this.parameters);
			}
		}
	}
}
