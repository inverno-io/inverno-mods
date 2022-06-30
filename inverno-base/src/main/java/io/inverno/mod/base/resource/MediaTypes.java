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
package io.inverno.mod.base.resource;

/**
 * <p>
 * A collection of well known media types.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public final class MediaTypes {

	/**
	 * The {@code application} type used in {@link #normalizeApplicationMediaType(java.lang.String) }
	 */
	private static final String TYPE_APPLICATION = "application";
	
	/**
	 * {@code application/jose}
	 */
	public static final String APPLICATION_JOSE = "application/jose";
	
	/**
	 * {@code application/jose+json}
	 */
	public static final String APPLICATION_JOSE_JSON = "application/jose+json";
	
	/**
	 * {@code application/json}
	 */
	public static final String APPLICATION_JSON = "application/json";
	
	/**
	 * {@code application/jwk+json}
	 */
	public static final String APPLICATION_JWK_JSON = "application/jwk+json";
	
	/**
	 * {@code application/jwk-set+json}
	 */
	public static final String APPLICATION_JWK_SET_JSON = "application/jwk-set+json";
	
	/**
	 * {@code application/jwt}
	 */
	public static final String APPLICATION_JWT = "application/jwt";
	
	/**
	 * {@code application/octet-stream}
	 */
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	/**
	 * {@code application/x-ndjson}
	 */
	public static final String APPLICATION_X_NDJSON = "application/x-ndjson";
	
	/**
	 * {@code application/x-www-form-urlencoded}
	 */
	public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
	
	/**
	 * {@code application/xml}
	 */
	public static final String APPLICATION_XML = "application/xml";
	
	/**
	 * {@code multipart/form-data}
	 */
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";
	
	/**
	 * {@code multipart/mixed}
	 */
	public static final String MULTIPART_MIXED = "multipart/mixed";
	
	/**
	 * {@code text/event-stream}
	 */
	public static final String TEXT_EVENT_STREAM = "text/event-stream";
	
	/**
	 * {@code text/html}
	 */
	public static final String TEXT_HTML = "text/html";
	
	/**
	 * {@code text/plain}
	 */
	public static final String TEXT_PLAIN = "text/plain";
	
	/**
	 * {@code text/xml}
	 */
	public static final String TEXT_XML = "text/xml";
	
	/**
	 * <p>
	 * Normalizes the specified media type.
	 * </p>
	 * 
	 * <p>
	 * This method basically restores the missing {@code application/} prefix omitted for compactness. This reduced syntax can be used for instance in a JOSE header content type parameter or in a
	 * Websocket subprotocol.
	 * </p>
	 * 
	 * @param mediaType a media type
	 * 
	 * @return a normalized application media type
	 */
	public static String normalizeApplicationMediaType(String mediaType) {
		if(mediaType == null) {
			return null;
		}
		mediaType = mediaType.toLowerCase();
		String normalizedMediaType;
		if(mediaType.indexOf('/') < 0) {
			normalizedMediaType = TYPE_APPLICATION + "/" + mediaType;
		}
		else {
			normalizedMediaType = mediaType;
		}
		return normalizedMediaType;
	}
}
