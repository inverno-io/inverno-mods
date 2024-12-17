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
package io.inverno.mod.discovery.http.meta.internal;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIMatcher;
import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.discovery.http.meta.HttpMetaServiceDescriptor;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.UnboundExchange;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An exchange transformer used to transform exchange request and/or response when resolving a service instance.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class HttpMetaServiceExchangeTransformer {

	private final HttpMetaServiceDescriptor.RequestTransformer requestTransformer;
	private final HttpMetaServiceDescriptor.ResponseTransformer responseTransformer;

	private final String destinationPathAbsolute;
	private final String destinationQuery;

	private final List<HttpMetaServiceExchangeTransformer.PathTranslator> pathTranslators;

	/**
	 * <p>
	 * Creates an HTTP meta service exchange transformer.
	 * </p>
	 *
	 * @param requestTransformer a request transformer descriptor
	 * @param responseTransformer a response transformer descriptor
	 * @param destinationPathAbsolute the absolute path of the destination URI when the transformer is defined for a destination or null
	 * @param destinationQuery the query component of the destination URI when the transformer is defined for a destination or null
	 */
	private HttpMetaServiceExchangeTransformer(HttpMetaServiceDescriptor.RequestTransformer requestTransformer, HttpMetaServiceDescriptor.ResponseTransformer responseTransformer, String destinationPathAbsolute, String destinationQuery) {
		this.requestTransformer = requestTransformer;
		this.responseTransformer = responseTransformer;

		this.destinationPathAbsolute = destinationPathAbsolute;
		this.destinationQuery = destinationQuery;

		if(requestTransformer != null && requestTransformer.getTranslatePath() != null) {
			this.pathTranslators = requestTransformer.getTranslatePath().entrySet().stream().map(e -> new HttpMetaServiceExchangeTransformer.PathTranslator(e.getKey(), e.getValue())).collect(Collectors.toList());
		}
		else {
			this.pathTranslators = null;
		}
	}

	/**
	 * <p>
	 * Creates an HTTP meta service exchange transformer from a route descriptor.
	 * </p>
	 *
	 * @param routeDescriptor a route descriptor
	 *
	 * @return an HTTP meta service exchange transformer
	 */
	public static HttpMetaServiceExchangeTransformer from(HttpMetaServiceDescriptor.RouteDescriptor routeDescriptor) {
		if(routeDescriptor.getTransformRequest() != null || routeDescriptor.getTransformResponse() != null) {
			return new HttpMetaServiceExchangeTransformer(routeDescriptor.getTransformRequest(), routeDescriptor.getTransformResponse(), null, null);
		}
		return null;
	}

	/**
	 * <p>
	 * Creates an HTTP meta service exchange transformer from a route descriptor.
	 * </p>
	 *
	 * @param destinationDescriptor a destination descriptor
	 *
	 * @return an HTTP meta service exchange transformer
	 */
	public static HttpMetaServiceExchangeTransformer from(HttpMetaServiceDescriptor.DestinationDescriptor destinationDescriptor) {
		String destinationPathAbsolute;
		String destinationQuery;
		if(destinationDescriptor.getURI() == null) {
			destinationPathAbsolute = null;
			destinationQuery = null;
		}
		else {
			URI requestTargetURI;
			if(destinationDescriptor.getURI().isOpaque()) {
				if(StringUtils.isBlank(destinationDescriptor.getURI().getFragment())) {
					requestTargetURI = URI.create(destinationDescriptor.getURI().getFragment());
				}
				else {
					requestTargetURI = URI.create("");
				}
			}
			else {
				requestTargetURI = destinationDescriptor.getURI();
			}

			if(StringUtils.isBlank(requestTargetURI.getPath()) || requestTargetURI.getPath().equals("/")) {
				destinationPathAbsolute = null;
			}
			else if(requestTargetURI.getPath().endsWith("/")) {
				destinationPathAbsolute = requestTargetURI.getPath().substring(0, requestTargetURI.getPath().length() - 1);
			}
			else {
				destinationPathAbsolute = requestTargetURI.getPath();
			}
			destinationQuery = StringUtils.isBlank(requestTargetURI.getQuery()) ? null : requestTargetURI.getQuery();
		}

		if(destinationDescriptor.getTransformRequest() != null || destinationDescriptor.getTransformResponse() != null || destinationPathAbsolute != null || destinationQuery != null) {
			return new HttpMetaServiceExchangeTransformer(destinationDescriptor.getTransformRequest(), destinationDescriptor.getTransformResponse(), destinationPathAbsolute, destinationQuery);
		}
		return null;
	}

	/**
	 * <p>
	 * Transforms the specified exchange.
	 * </p>
	 *
	 * @param exchange an exchange
	 */
	public void transform(Exchange<?> exchange) {
		if(this.requestTransformer != null) {
			if(this.pathTranslators != null) {
				for(HttpMetaServiceExchangeTransformer.PathTranslator pathTranslator : this.pathTranslators) {
					if(pathTranslator.translatePath(exchange)) {
						break;
					}
				}
			}

			if(this.requestTransformer.getSetAuthority() != null) {
				exchange.request().authority(this.requestTransformer.getSetAuthority());
			}

			if(this.requestTransformer.getAddHeaders() != null) {
				exchange.request().headers(headers -> this.requestTransformer.getAddHeaders().forEach(headers::add));
			}

			if(this.requestTransformer.getSetHeaders() != null) {
				exchange.request().headers(headers -> this.requestTransformer.getSetHeaders().forEach(headers::set));
			}

			if(this.requestTransformer.getRemoveHeaders() != null) {
				exchange.request().headers(headers -> this.requestTransformer.getRemoveHeaders().forEach(headers::remove));
			}
		}

		if(this.responseTransformer != null && exchange instanceof UnboundExchange) {
			((UnboundExchange<?>) exchange).intercept(interceptedExchange -> {
				if(this.responseTransformer.getAddHeaders() != null) {
					interceptedExchange.response().headers(headers -> this.responseTransformer.getAddHeaders().forEach(headers::add));
				}

				if(this.responseTransformer.getSetHeaders() != null) {
					interceptedExchange.response().headers(headers -> this.responseTransformer.getSetHeaders().forEach(headers::set));
				}

				if(this.responseTransformer.getRemoveHeaders() != null) {
					interceptedExchange.response().headers(headers -> this.responseTransformer.getRemoveHeaders().forEach(headers::remove));
				}
				return Mono.just(interceptedExchange);
			});
		}

		if(this.destinationPathAbsolute != null || this.destinationQuery != null) {
			String requestPathAbsolute = exchange.request().getPathAbsolute();
			String requestQuery = exchange.request().getQuery();

			String path;
			if(this.destinationPathAbsolute != null) {
				path = this.destinationPathAbsolute;
				if(!requestPathAbsolute.equals("/")) {
					path += requestPathAbsolute;
				}
			}
			else {
				path = requestPathAbsolute;
			}

			if (this.destinationQuery != null) {
				path += "?";
				if(StringUtils.isBlank(requestQuery)) {
					path += this.destinationQuery;
				}
				else {
					path += requestQuery + "&" + this.destinationQuery;
				}
			}
			else if(StringUtils.isNotBlank(requestQuery)) {
				path += "?" + requestQuery;
			}
			exchange.request().path(path);
		}
	}

	/**
	 * <p>
	 * A path translator.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private static class PathTranslator {

		private final String matcherPath;
		private final URIPattern matcherPathPattern;

		private final String translatedPath;
		private final URIBuilder translatedPathBuilder;

		/**
		 * <p>
		 * Creates a path translator.
		 * </p>
		 *
		 * @param matcherPath the matcher path
		 * @param path        the translated path
		 */
		public PathTranslator(String matcherPath, String path) {
			if(!matcherPath.startsWith("/")) {
				throw new IllegalArgumentException("Matcher path must be absolute");
			}
			URIBuilder matcherPathBuilder = URIs.uri(matcherPath, URIs.RequestTargetForm.PATH, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
			if(matcherPathBuilder.getParameterNames().isEmpty()) {
				this.matcherPath = matcherPath;
				this.matcherPathPattern = null;
			}
			else {
				this.matcherPath = null;
				this.matcherPathPattern = matcherPathBuilder.buildPathPattern(false);
			}

			if(!path.startsWith("/")) {
				throw new IllegalArgumentException("Translated path must be absolute");
			}
			URIBuilder pathBuilder = URIs.uri(path, URIs.RequestTargetForm.PATH, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
			if(pathBuilder.getParameterNames().isEmpty()) {
				this.translatedPath = path;
				this.translatedPathBuilder = null;
			}
			else {
				if(pathBuilder.getParameterNames().contains(null)) {
					throw new IllegalArgumentException("Translated path can't contain unnamed parameters");
				}
				this.translatedPath = null;
				this.translatedPathBuilder = pathBuilder;
			}

			if(this.translatedPathBuilder != null) {
				if(this.matcherPathPattern == null) {
					throw new IllegalArgumentException("Can't build a translated path from a static matcher path");
				}

				Set<String> namedMatcherParameterNames = matcherPathBuilder.getParameterNames().stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
				Set<String> namedTranslatedPathParameterNames = pathBuilder.getParameterNames().stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));

				namedTranslatedPathParameterNames.removeAll(namedMatcherParameterNames);
				if(!namedTranslatedPathParameterNames.isEmpty()) {
					throw new IllegalArgumentException("Translated path parameters are missing from matcher path pattern: " + String.join(", ", namedTranslatedPathParameterNames));
				}
			}
		}

		/**
		 * <p>
		 * Translates the path in the exchange.
		 * </p>
		 *
		 * @param exchange an exchange
		 *
		 * @return true if the path was translated, false otherwise
		 */
		public boolean translatePath(Exchange<?> exchange) {
			String newPath = null;
			String pathAbsolute = exchange.request().getPathAbsolute();
			if(this.matcherPath != null) {
				if(this.matcherPath.equals(pathAbsolute)) {
					newPath = this.translatedPath;
				}
			}
			else {
				URIMatcher pathMatcher = this.matcherPathPattern.matcher(pathAbsolute);
				if(pathMatcher.matches()) {
					newPath = this.translatedPathBuilder != null ? this.translatedPathBuilder.buildPath(pathMatcher.getParameters()) : this.translatedPath;
				}
			}

			if(newPath != null) {
				String rawQuery = exchange.request().getPathBuilder().buildRawQuery();
				if(StringUtils.isNotBlank(rawQuery)) {
					newPath += "?" + rawQuery;
				}
				String rawFragment = exchange.request().getPathBuilder().buildRawFragment();
				if(StringUtils.isNotBlank(rawFragment)) {
					newPath += "#" + rawFragment;
				}
				exchange.request().path(newPath);
			}
			return false;
		}
	}
}
