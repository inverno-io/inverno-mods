/**
 * 
 */
package io.winterframework.mod.web;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * @author jkuhn
 *
 */
public final class Headers {

	public static final String CONTENT_DISPOSITION = "content-disposition";
	public static final String CONTENT_TYPE = "content-type";
	public static final String CONTENT_LENGTH = "content-length";
	public static final String COOKIE = "cookie";
	public static final String HOST = "host";
	public static final String SET_COOKIE = "set-cookie";
	public static final String TRANSFER_ENCODING = "transfer-encoding";
	
	// HTTP/2 pseudo headers
	public static final String PSEUDO_AUTHORITY = ":authority";
	public static final String PSEUDO_METHOD = ":method";
	public static final String PSEUDO_PATH = ":path";
	public static final String PSEUDO_SCHEME = ":scheme";
	public static final String PSEUDO_STATUS = ":status";
	
	private Headers() {}
	
	/**
	 * https://tools.ietf.org/html/rfc7231#section-3.1.1.5
	 * 
	 * @author jkuhn
	 *
	 */
	public static interface ContentType extends Header {
		
		public static final String BOUNDARY = "boundary";
		
		public static final String CHARSET = "charset";
		
		String getMediaType();
		
		String getBoundary();

		Charset getCharset();
	}
	
	/**
	 * https://tools.ietf.org/html/rfc6266#section-4.1
	 * 
	 * @author jkuhn
	 *
	 */
	public static interface ContentDisposition extends Header {

		public static final String PART_NAME = "name";
		public static final String FILENAME = "filename";
		public static final String CREATION_DATE = "creation-date";
		public static final String MODIFICATION_DATE = "modification-date";
		public static final String READ_DATE = "read-date";
		public static final String SIZE = "size";
		
		String getDispositionType();
		
		String getPartName();
		
		String getFilename();
		
		String getCreationDateTime();
		
		String getModificationDatetime();
		
		String getReadDateTime();
		
		Integer getSize();
	}
	
	/**
	 * https://tools.ietf.org/html/rfc6265#section-4.1
	 * 
	 * @author jkuhn
	 * 
	 */
	public static interface SetCookie extends Header {
		
		public static final String EXPIRES = "Expires";
		public static final String MAX_AGE = "Max-Age";
		public static final String DOMAIN = "Domain";
		public static final String PATH = "Path";
		public static final String SECURE = "Secure";
		public static final String HTTPONLY = "HttpOnly";
		
		String getName();
		
		String getValue();
		
		String getExpires();
		
		Integer getMaxAge();
		
		String getDomain();
		
		String getPath();
		
		Boolean isSecure();
		
		Boolean isHttpOnly();
	}
	
	/**
	 * https://tools.ietf.org/html/rfc6265#section-4.2
	 * 
	 * @author jkuhn
	 *
	 */
	public static interface Cookie extends Header {
		
		public Map<String, List<io.winterframework.mod.web.Cookie>> getPairs();
	}
}
