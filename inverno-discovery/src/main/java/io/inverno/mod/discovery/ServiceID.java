/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.discovery;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * A service ID identifies a service.
 * </p>
 *
 * <p>
 * It basically comes down to a service {@link URI} with particular constraints:
 * </p>
 *
 * <ul>
 * <li>It must be absolute (i.e. have a scheme).</li>
 * <li>It must have an authority component when not opaque.</li>
 * </ul>
 *
 * <p>
 * A service ID can be created with two kinds of service URI: when the URI is an opaque URI, the service ID is obtained from the scheme and the scheme specific part (e.g. scheme:service-name),
 * otherwise it is obtained from the scheme and the authority (e.g. scheme://service-name).
 * </p>
 *
 * <p>
 * A service URI conveys two information: the service ID and the request target. In case of a an opaque URI, the request target is specified in the fragment component
 * (e.g. {@code scheme:service-name#/path/to/resource?k1=v1&k2=v2}), otherwise, the resource target is specified by the path and query components
 * (e.g. {@code scheme://service-name/path/to/resource?k1=v1&k2=v2)}.
 * </p>
 *
 * <p>
 * When creating a service ID from a service URI, the request target is evicted because it doesn't identify the service.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public final class ServiceID implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The service URI.
	 */
	private final URI uri;

	/**
	 * <p>
	 * Creates a service ID.
	 * </p>
	 *
	 * <p>
	 * A valid service URI must be absolute, if it is an opaque URI with a fragment the fragment must represent an absolute path, if it is not an opaque URI, it must have an authority component.
	 * </p>
	 *
	 * @param uri a service URI
	 *
	 * @throws IllegalArgumentException if the specified URI is not a valid service URI
	 */
	private ServiceID(URI uri) throws IllegalArgumentException {
		if(!uri.isAbsolute()) {
			throw new IllegalArgumentException("URI must be absolute: " + uri);
		}
		try {
			if(uri.isOpaque()) {
				if(!StringUtils.isBlank(uri.getFragment()) && uri.getFragment().charAt(0) != '/') {
					throw new IllegalArgumentException("Opaque URI fragment path must be absolute: " + uri);
				}
				this.uri = new URI(uri.getScheme().toLowerCase(), uri.getSchemeSpecificPart(), null);
			}
			else {
				if(StringUtils.isBlank(uri.getAuthority())) {
					throw new IllegalArgumentException("URI must have an authority component: " + uri);
				}
				this.uri = new URI(uri.getScheme().toLowerCase(), uri.getAuthority(), null, null, null);
			}
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * <p>
	 * Returns the request target in the specified service URI.
	 * </p>
	 *
	 * @param uri a service URI
	 *
	 * @return a request target
	 *
	 * @throws IllegalArgumentException if the specified URI is not a valid service URI
	 */
	public static String getRequestTarget(URI uri) throws IllegalArgumentException {
		String requestTarget;
		if(!uri.isAbsolute()) {
			throw new IllegalArgumentException("URI must be absolute: " + uri);
		}
		else if(uri.isOpaque()) {
			requestTarget = uri.getFragment();
			if(StringUtils.isBlank(requestTarget)) {
				requestTarget = "/";
			}
			else if(!StringUtils.isBlank(requestTarget) && requestTarget.charAt(0) != '/') {
				throw new IllegalArgumentException("Opaque URI fragment path must be absolute: " + uri);
			}
		}
		else {
			if(StringUtils.isBlank(uri.getAuthority())) {
				throw new IllegalArgumentException("URI must have an authority component: " + uri);
			}
			requestTarget = StringUtils.isNotBlank(uri.getPath()) ? uri.getPath() : "/";
			if(StringUtils.isNotBlank(uri.getQuery())) {
				requestTarget += "?" + uri.getQuery();
			}
			if(StringUtils.isNotBlank(uri.getFragment())) {
				// Let's keep it for now although this seems a bit off-topic
				requestTarget += "#" + uri.getFragment();
			}
		}
		return requestTarget;
	}

	/**
	 * <p>
	 * Creates a service ID from the specified service URI.
	 * </p>
	 *
	 * @param uri a service URI
	 *
	 * @return a service ID
	 *
	 * @throws IllegalArgumentException if the specified URI is not a valid service URI
	 */
	public static ServiceID of(String uri) throws IllegalArgumentException {
		return new ServiceID(URI.create(uri));
	}

	/**
	 * <p>
	 * Creates a service ID from the specified service URI.
	 * </p>
	 *
	 * @param uri a service URI
	 *
	 * @return a service ID
	 *
	 * @throws IllegalArgumentException if the specified URI is not a valid service URI
	 */
	public static ServiceID of(URI uri) throws IllegalArgumentException {
		return new ServiceID(uri);
	}

	/**
	 * <p>
	 * Returns the service ID scheme.
	 * </p>
	 *
	 * @return the service ID scheme
	 */
	public String getScheme() {
		return this.uri.getScheme();
	}

	/**
	 * <p>
	 * Return the service ID URI.
	 * </p>
	 *
	 * <p>
	 * Note that the service ID URI doesn't contain the request target.
	 * </p>
	 *
	 * @return the service ID URI
	 */
	public URI getURI() {
		return this.uri;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServiceID that = (ServiceID) o;
		return Objects.equals(uri, that.uri);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(uri);
	}

	@Override
	public String toString() {
		return this.uri.toString();
	}
}
