/**
 * 
 */
package io.winterframework.mod.web.internal.header;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Headers.MediaRange;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class ContentTypeCodec extends ParameterizedHeaderCodec<ContentTypeCodec.ContentType, ContentTypeCodec.ContentType.Builder> {
	
	public ContentTypeCodec() {
		super(ContentTypeCodec.ContentType.Builder::new, Set.of(Headers.CONTENT_TYPE), DEFAULT_PARAMETER_DELIMITER, DEFAULT_VALUE_DELIMITER, false, false, false, false, true, false);
	}
	
	@Override
	public String encodeValue(ContentType headerField) {
		StringBuilder result = new StringBuilder();
		
		result.append(headerField.getParameterizedValue());
		
		Map<String, String> parameters = new HashMap<>(headerField.getParameters());
		if(headerField.boundary != null) {
			parameters.put(ContentType.BOUNDARY, headerField.boundary);
		}
		if(headerField.charset != null) {
			parameters.put(ContentType.CHARSET, headerField.charset.toString());
		}
		if(!parameters.isEmpty()) {
			parameters.entrySet().stream().forEach(e -> {
				result.append(this.parameterDelimiter).append(e.getKey()).append("=").append(e.getValue());
			});
		}
		return result.toString();
	}

	public static final class ContentType extends ParameterizedHeader implements Headers.ContentType {
		
		private String mediaType;
		private String type;
		private String subType;
		private String boundary;
		private Charset charset;
		
		public ContentType(String mediaType, Charset charset, String boundary, Map<String, String> parameters) {
			super(Headers.CONTENT_TYPE, null, mediaType, parameters);
			this.setMediaType(mediaType.toLowerCase());
			this.setCharset(charset);
			this.setBoundary(boundary);
		}
		
		private ContentType(String headerName, String headerValue, String parameterizedValue, Map<String, String> parameters, String boundary, Charset charset) {
			super(headerName, headerValue, parameterizedValue, parameters);
			
			this.setMediaType(parameterizedValue.toLowerCase());
			this.boundary = boundary;
			this.charset = charset;
		}

		public String getMediaType() {
			return mediaType;
		}
		
		public void setMediaType(String mediaType) {
			this.mediaType = mediaType;
			this.parameterizedValue = mediaType;
			
			String[] splitMediaType = mediaType.split("/");
			if(splitMediaType.length != 2) {
				// TODO Not Acceptable
				throw new RuntimeException("Not Acceptable: invalid content type");
			}
			
			this.type = splitMediaType[0];
			this.subType = splitMediaType[1];
			
			if(!HeaderService.isToken(this.type) || !HeaderService.isToken(this.subType)) {
				// TODO Not Acceptable
				throw new RuntimeException("Not Acceptable: invalid content type");
			}
		}
		
		@Override
		public String getType() {
			return this.type;
		}
		
		@Override
		public String getSubType() {
			return this.subType;
		}
		
		@Override
		public String getBoundary() {
			return boundary;
		}
		
		public void setBoundary(String boundary) {
			this.boundary = boundary;
			if(boundary != null) {
				this.parameters.put(BOUNDARY, charset.toString());
			}
			else {
				this.parameters.remove(BOUNDARY);
			}
		}

		@Override
		public Charset getCharset() {
			return charset;
		}
		
		public void setCharset(Charset charset) {
			this.charset = charset;
			if(charset != null) {
				this.parameters.put(CHARSET, charset.toString());
			}
			else {
				this.parameters.remove(CHARSET);
			}
		}
		
		@Override
		public MediaRange toMediaRange() {
			return new GenericMediaRange(this.type, this.subType, 1, this.parameters);
		}
		
		public static final class Builder extends ParameterizedHeader.AbstractBuilder<ContentType, Builder> {

			private String boundary;
			
			private Charset charset;
			
			@Override
			public Builder parameter(String name, String value) {
				if(name.equalsIgnoreCase(BOUNDARY)) {
					this.boundary = value;
				}
				else if(name.equalsIgnoreCase(CHARSET)) {
					this.charset = Charset.forName(value);
				}
				return super.parameter(name, value);
			}
			
			@Override
			public ContentType build() {
				return new ContentType(this.headerName, this.headerValue, this.parameterizedValue, this.parameters, this.boundary, this.charset);
			}
		}
	}
}
