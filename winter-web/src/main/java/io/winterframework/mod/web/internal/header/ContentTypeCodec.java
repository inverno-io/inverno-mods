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
import io.winterframework.mod.web.Headers;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class ContentTypeCodec extends ParameterizedHeaderCodec<ContentTypeCodec.ContentType, ContentTypeCodec.ContentType.Builder> {
	
	public ContentTypeCodec() {
		super(ContentTypeCodec.ContentType.Builder::new, Set.of(Headers.CONTENT_TYPE), DEFAULT_DELIMITER, false, false, false, false, true);
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
				result.append(this.delimiter).append(e.getKey()).append("=").append(e.getValue());
			});
		}
		return result.toString();
	}

	public static final class ContentType extends ParameterizedHeader implements Headers.ContentType {
		
		private String mediaType;
		private String boundary;
		private Charset charset;
		
		public ContentType(String mediaType, Charset charset, String boundary) {
			super(Headers.CONTENT_TYPE, null, mediaType, new HashMap<>());
			this.setMediaType(mediaType);
			this.setCharset(charset);
			this.setBoundary(boundary);
		}
		
		private ContentType(String headerName, String headerValue, String mediaType, String boundary, Charset charset, Map<String, String> parameters) {
			super(headerName, headerValue, mediaType, parameters);
			
			this.mediaType = mediaType;
			this.boundary = boundary;
			this.charset = charset;
		}

		public String getMediaType() {
			return mediaType;
		}
		
		public void setMediaType(String mediaType) {
			this.mediaType = mediaType;
			this.parameterizedValue = mediaType;
		}
		
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
				return new ContentType(this.headerName, this.headerValue, this.parameterizedValue, this.boundary, this.charset, this.parameters);
			}
		}
	}
}
