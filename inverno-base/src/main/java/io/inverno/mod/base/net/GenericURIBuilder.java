/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.base.net;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>A generic URI builder implementation.</p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see URIBuilder
 */
class GenericURIBuilder implements URIBuilder {

	private final Charset charset;
	private final URIFlags flags;
	
	private SchemeComponent scheme;
	private UserInfoComponent userInfo;
	private HostComponent host;
	private PortComponent port;
	private final LinkedList<SegmentComponent> segments;
	private final LinkedList<QueryParameterComponent> queryParameters;
	private FragmentComponent fragment;

	/**
	 * <p>
	 * Creates a generic URI builder with the specified charset and options.
	 * </p>
	 * 
	 * @param charset a charset
	 * @param options an array of options
	 */
	public GenericURIBuilder(Charset charset, URIs.Option... options) {
		this.charset = charset;
		this.flags = new URIFlags(options);
		this.segments = new LinkedList<>();
		this.queryParameters = new LinkedList<>();
	}
	
	/**
	 * <p>
	 * Creates a generic URI builder with the specified charset and options from the
	 * specified path ignoring or not the trailing slash.
	 * </p>
	 * 
	 * @param path                a path
	 * @param ignoreTrailingSlash ignore or the trailing slash in the path
	 * @param charset             a charset
	 * @param options             an array of options
	 */
	public GenericURIBuilder(String path, boolean ignoreTrailingSlash, Charset charset, URIs.Option... options) {
		this.charset = charset;
		this.flags = new URIFlags(options);
		this.segments = new LinkedList<>();
		this.queryParameters = new LinkedList<>();
		if(path != null) {
			int queryIndex = path.indexOf('?');
			if(queryIndex < 0) {
				this.path(path, ignoreTrailingSlash);
			}
			else {
				String pathPart = path.substring(0, queryIndex);
				String queryPart = path.substring(queryIndex + 1);
				this.path(pathPart, ignoreTrailingSlash);
				for(String queryParameter : queryPart.split("&")) {
					String[] queryParameterNameValue = queryParameter.split("=");
					this.queryParameters.add(new QueryParameterComponent(this.flags, this.charset, URIs.decodeURIComponent(queryParameterNameValue[0], this.charset), URIs.decodeURIComponent(queryParameterNameValue[1], this.charset)));
				}
			}
		}
	}
	
	/**
	 * <p>
	 * Creates a generic URI builder with the specified charset and options from the
	 * specified URI ignoring or not the trailing slash.
	 * </p>
	 * 
	 * @param uri                 a URI
	 * @param ignoreTrailingSlash ignore or not trailing slash in the URI's path
	 * @param charset             a charset
	 * @param options             an array of options
	 */
	public GenericURIBuilder(URI uri, boolean ignoreTrailingSlash, Charset charset, URIs.Option... options) {
		this.charset = charset;
		this.flags = new URIFlags(options);
		this.segments = new LinkedList<>();
		this.queryParameters = new LinkedList<>();
		if(uri != null) {
			this.scheme(uri.getScheme());
			this.userInfo(uri.getUserInfo());
			this.host(uri.getHost());
			this.port(uri.getPort());
			
			if(uri.getRawPath() != null) {
				String[] segments = uri.getRawPath().split("/");
				for(int i=0;i<segments.length;i++) {
					if(ignoreTrailingSlash && i ==  segments.length - 1 && segments[i].equals("/")) {
						break;
					}
					this.segment(URIs.decodeURIComponent(segments[i], this.charset));
				}
			}
			
			if(uri.getRawQuery() != null) {
				for(String queryParameter : uri.getRawQuery().split("&")) {
					String[] queryParameterNameValue = queryParameter.split("=");
					this.queryParameters.add(new QueryParameterComponent(this.flags, this.charset, URIs.decodeURIComponent(queryParameterNameValue[0], this.charset), URIs.decodeURIComponent(queryParameterNameValue[1], this.charset)));
				}
			}
			this.fragment(URIs.decodeURIComponent(uri.getFragment(), this.charset));
		}
	}
	
	@Override
	public URIBuilder scheme(String scheme) {
		if(StringUtils.isNotBlank(scheme)) {
			this.scheme = new SchemeComponent(this.flags, this.charset, scheme);
		}
		else {
			this.scheme = null;
		}
		return this;
	}
	
	@Override
	public URIBuilder userInfo(String userInfo) {
		if(StringUtils.isNotBlank(userInfo)) {
			this.userInfo = new UserInfoComponent(this.flags, this.charset, userInfo);
		}
		else {
			this.userInfo = null;
		}
		return this;
	}
	
	@Override
	public URIBuilder host(String host) {
		if(StringUtils.isNotBlank(host)) {
			this.host = new HostComponent(this.flags, this.charset, host);
		}
		else {
			this.host = null;
		}
		return this;
	}
	
	@Override
	public URIBuilder port(Integer port) {
		if(port != null && port > -1) {
			this.port = new PortComponent(this.flags, this.charset, port.toString());
		}
		else {
			this.port = null;
		}
		return this;
	}
	
	@Override
	public URIBuilder port(String port) {
		if(StringUtils.isNotBlank(port)) {
			this.port = new PortComponent(this.flags, this.charset, port);
		}
		else {
			this.port = null;
		}
		return this;
	}
	
	@Override
	public URIBuilder path(String path, boolean ignoreTrailingSlash) {
		if(StringUtils.isNotBlank(path)) {
			this.segments.addAll(SegmentComponent.fromPath(this.flags, this.charset, path, !this.segments.isEmpty(), ignoreTrailingSlash));
		}
		return this;
	}
	
	@Override
	public URIBuilder clearPath() {
		this.segments.clear();
		return this;
	}
	
	@Override
	public URIBuilder segment(String segment) {
		if(segment != null) {
			SegmentComponent nextSegment = new SegmentComponent(this.flags, this.charset, segment);
			if(this.flags.isNormalized()) {
				// Note that, this doesn't apply to parameterized segment that might be set to
				// '.' or '..' as a result, normalization will also take place during the build of
				// a parameterized URI
				String nextSegmentValue = nextSegment.getRawValue();
				if(nextSegmentValue.equals(".")) {
					// Segment is ignored
				}
				else if(nextSegmentValue.equals("..")) {
					// We have to remove the previous segment if it is not '..' OR keep that segment if there's no previous segment
					if(!this.segments.isEmpty()) {
						SegmentComponent lastSegment = this.segments.peekLast();
						String lastSegmentValue = lastSegment.getRawValue();
						if(lastSegmentValue.equals("..") || (this.segments.size() == 1 && lastSegmentValue.equals(""))) {
							this.segments.add(nextSegment);
						}
						else {
							this.segments.removeLast();
						}
					}
					else {
						this.segments.add(nextSegment);
					}
				}
				else {
					this.segments.add(nextSegment);
				}
			}
			else {
				this.segments.add(nextSegment);
			}
		}
		return this;
	}
	
	@Override
	public URIBuilder queryParameter(String name, String value) {
		if(StringUtils.isNotBlank(name) && value != null) {
			this.queryParameters.add(new QueryParameterComponent(this.flags, this.charset, name, value));
		}
		return this;
	}
	
	@Override
	public URIBuilder clearQuery() {
		this.queryParameters.clear();
		return this;
	}
	
	@Override
	public URIBuilder fragment(String fragment) {
		if(StringUtils.isNotBlank(fragment)) {
			this.fragment = new FragmentComponent(this.flags, this.charset, fragment);
		}
		else {
			this.fragment = null;
		}
		return this;
	}
	
	@Override
	public List<String> getParameterNames() {
		List<URIParameter> parameters = new ArrayList<>();
		if(this.scheme != null) {
			parameters.addAll(this.scheme.getParameters());
		}
		if(this.userInfo != null) {
			parameters.addAll(this.userInfo.getParameters());
		}
		if(this.host != null) {
			parameters.addAll(this.host.getParameters());
		}
		if(this.port != null) {
			parameters.addAll(this.port.getParameters());
		}
		for(SegmentComponent segment : this.segments) {
			parameters.addAll(segment.getParameters());
		}
		for(QueryParameterComponent queryParameter : this.queryParameters) {
			parameters.addAll(queryParameter.getParameters());
		}
		if(this.fragment != null) {
			parameters.addAll(this.fragment.getParameters());
		}
		return parameters.stream().map(URIParameter::getName).collect(Collectors.toList());
	}
	
	@Override
	public List<String> getPathParameterNames() {
		List<URIParameter> parameters = new ArrayList<>();
		for(SegmentComponent segment : this.segments) {
			parameters.addAll(segment.getParameters());
		}
		return parameters.stream().map(URIParameter::getName).collect(Collectors.toList());
	}
	
	@Override
	public Map<String, List<String>> getQueryParameters(Object... values) throws URIBuilderException {
		AtomicInteger valuesIndex = new AtomicInteger();
		return this.queryParameters.stream().collect(Collectors.groupingBy(QueryParameterComponent::getParameterName, Collectors.mapping(queryParameter -> queryParameter.getParameterValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(queryParameter.getParameters().size()), values.length))), Collectors.toList())));
	}
	
	@Override
	public Map<String, List<String>> getQueryParameters(Map<String, ?> values) throws URIBuilderException {
		return this.queryParameters.stream().collect(Collectors.groupingBy(QueryParameterComponent::getParameterName, Collectors.mapping(queryParameter -> queryParameter.getParameterValue(values), Collectors.toList())));
	}
	
	@Override
	public Map<String, List<String>> getRawQueryParameters() throws URIBuilderException {
		return this.queryParameters.stream().collect(Collectors.groupingBy(QueryParameterComponent::getRawParameterName, Collectors.mapping(QueryParameterComponent::getRawParameterValue, Collectors.toList())));
	}
	
	@Override
	public URI build(Object[] values, boolean escapeSlash) throws URIBuilderException {
		return URI.create(this.buildString(values, escapeSlash));
	}
	
	@Override
	public URI build(Map<String, ?> values, boolean escapeSlash) throws URIBuilderException {
		return URI.create(this.buildString(values, escapeSlash));
	}
	
	@Override
	public String buildString(Object[] values, boolean escapeSlash) throws URIBuilderException {
		AtomicInteger valuesIndex = new AtomicInteger();
		StringBuilder uriBuilder = new StringBuilder();
		
		boolean absolute = false;
		if(this.scheme != null && this.scheme.isPresent()) {
			absolute = true;
			uriBuilder.append(this.scheme.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(this.scheme.getParameters().size()), values.length)))).append(":");
		}
		if(this.host != null && this.host.isPresent()) {
			absolute = true;
			uriBuilder.append("//");
			if(this.userInfo != null && this.userInfo.isPresent()) {
				uriBuilder.append(this.userInfo.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(this.userInfo.getParameters().size()), values.length)))).append("@");
			}
			uriBuilder.append(this.host.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(this.host.getParameters().size()), values.length))));
			if(this.port != null && this.port.isPresent()) {
				uriBuilder.append(":").append(this.port.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(this.port.getParameters().size()), values.length))));	
			}
		}
		
		int segmentSize = this.segments.size();
		if(segmentSize > 0) {
			boolean firstSegmentEmpty = this.segments.getFirst().getRawValue().isEmpty();
			if(firstSegmentEmpty && segmentSize == 1) {
				uriBuilder.append("/");
			}
			else {
				if(absolute && !firstSegmentEmpty) {
					uriBuilder.append("/");
				}
				
				if(this.flags.isNormalized() && this.flags.isParameterized()) {
					// Here we must implement https://tools.ietf.org/html/rfc3986#section-5.2.4
					LinkedList<String> segmentValues = new LinkedList<>();
					for(SegmentComponent segment : this.segments) {
						String nextSegmentValue = segment.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(segment.getParameters().size()), values.length)), escapeSlash);
						if(nextSegmentValue.equals(".")) {
							// Segment is ignored
						}
						else if(nextSegmentValue.equals("..")) {
							// We have to remove the previous segment if it is not '..' OR keep that segment if there's no previous segment
							if(!segmentValues.isEmpty()) {
								String lastSegmentValue = segmentValues.peekLast();
								if(lastSegmentValue.equals("..") || (segmentValues.size() == 1 && lastSegmentValue.equals(""))) {
									segmentValues.add(nextSegmentValue);
								}
								else {
									segmentValues.removeLast();
								}
							}
							else {
								segmentValues.add(nextSegmentValue);
							}
						}
						else {
							segmentValues.add(nextSegmentValue);
						}
					}
					uriBuilder.append(segmentValues.stream().collect(Collectors.joining("/")));
				}
				else {
					uriBuilder.append(this.segments.stream()
						.map(segment -> segment.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(segment.getParameters().size()), values.length)), escapeSlash))
						.collect(Collectors.joining("/"))
					);
				}
			}
		}
		
		if(!this.queryParameters.isEmpty()) {
			uriBuilder.append("?");
			uriBuilder.append(this.queryParameters.stream()
				.map(queryParameter -> queryParameter.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(queryParameter.getParameters().size()), values.length))))
				.collect(Collectors.joining("&"))
			);
		}
		
		if(this.fragment != null && this.fragment.isPresent()) {
			uriBuilder.append("#").append(this.fragment.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(this.fragment.getParameters().size()), values.length))));
		}
		return uriBuilder.toString();
	}
	
	@Override
	public String buildString(Map<String, ?> values, boolean escapeSlash) throws URIBuilderException {
		StringBuilder uriBuilder = new StringBuilder();
		
		boolean absolute = false;
		if(this.scheme != null && this.scheme.isPresent()) {
			absolute = true;
			uriBuilder.append(this.scheme.getValue(values)).append(":");
		}

		if(this.host != null && this.host.isPresent()) {
			absolute = true;
			uriBuilder.append("//");
			if(this.userInfo != null && this.userInfo.isPresent()) {
				uriBuilder.append(this.userInfo.getValue(values)).append("@");
			}
			uriBuilder.append(this.host.getValue(values));
			if(this.port != null && this.port.isPresent()) {
				uriBuilder.append(":").append(this.port.getValue(values));	
			}
		}
		
		int segmentSize = this.segments.size();
		if(segmentSize > 0) {
			boolean firstSegmentEmpty = this.segments.getFirst().getRawValue().isEmpty();
			if(firstSegmentEmpty && segmentSize == 1) {
				uriBuilder.append("/");
			}
			else {
				if(absolute && !firstSegmentEmpty) {
					uriBuilder.append("/");
				}
				
				if(this.flags.isNormalized() && this.flags.isParameterized()) {
					// Here we must implement https://tools.ietf.org/html/rfc3986#section-5.2.4
					LinkedList<String> segmentValues = new LinkedList<>();
					for(SegmentComponent segment : this.segments) {
						String nextSegmentValue = segment.getValue(values, escapeSlash);
						if(nextSegmentValue.equals(".")) {
							// Segment is ignored
						}
						else if(nextSegmentValue.equals("..")) {
							// We have to remove the previous segment if it is not '..' OR keep that segment if there's no previous segment
							if(!segmentValues.isEmpty()) {
								String lastSegmentValue = segmentValues.peekLast();
								if(lastSegmentValue.equals("..") || (segmentValues.size() == 1 && lastSegmentValue.equals(""))) {
									segmentValues.add(nextSegmentValue);
								}
								else {
									segmentValues.removeLast();
								}
							}
							else {
								segmentValues.add(nextSegmentValue);
							}
						}
						else {
							segmentValues.add(nextSegmentValue);
						}
					}
					uriBuilder.append(segmentValues.stream().collect(Collectors.joining("/")));
				}
				else {
					uriBuilder.append(this.segments.stream().map(segment -> segment.getValue(values, escapeSlash)).collect(Collectors.joining("/")));
				}
			}
		}

		if(!this.queryParameters.isEmpty()) {
			uriBuilder.append("?");
			uriBuilder.append(this.queryParameters.stream()
				.map(queryParameter -> queryParameter.getValue(values))
				.collect(Collectors.joining("&"))
			);
		}
		
		if(this.fragment != null && this.fragment.isPresent()) {
			uriBuilder.append("#").append(this.fragment.getValue(values));
		}
		return uriBuilder.toString();
	}
	
	@Override
	public String buildRawString() {
		StringBuilder uriBuilder = new StringBuilder();
		
		boolean absolute = false;
		if(this.scheme != null && this.scheme.isPresent()) {
			absolute = true;
			uriBuilder.append(this.scheme.getRawValue()).append(":");
		}

		if(this.host != null && this.host.isPresent()) {
			absolute = true;
			uriBuilder.append("//");
			if(this.userInfo != null && this.userInfo.isPresent()) {
				uriBuilder.append(this.userInfo.getRawValue()).append("@");
			}
			uriBuilder.append(this.host.getRawValue());
			if(this.port != null && this.port.isPresent()) {
				uriBuilder.append(":").append(this.port.getRawValue());	
			}
		}
		
		int segmentSize = this.segments.size();
		if(segmentSize > 0) {
			boolean firstSegmentEmpty = this.segments.getFirst().getRawValue().isEmpty();
			if(firstSegmentEmpty && segmentSize == 1) {
				uriBuilder.append("/");
			}
			else {
				if(absolute && !firstSegmentEmpty) {
					uriBuilder.append("/");
				}
				uriBuilder.append(this.segments.stream().map(segment -> segment.getRawValue()).collect(Collectors.joining("/")));
			}
		}
		
		if(!this.queryParameters.isEmpty()) {
			uriBuilder.append("?");
			uriBuilder.append(this.queryParameters.stream().map(queryParameter -> queryParameter.getRawValue()).collect(Collectors.joining("&")));
		}
		
		if(this.fragment != null && this.fragment.isPresent()) {
			uriBuilder.append("#").append(this.fragment.getRawValue());
		}
		return uriBuilder.toString();
	}
	
	@Override
	public String buildPath(Object[] values, boolean escapeSlash) {
		StringBuilder pathBuilder = new StringBuilder();
		boolean absolute = (this.scheme != null && this.scheme.isPresent()) || (this.host != null && this.host.isPresent());
		
		int segmentSize = this.segments.size();
		if(segmentSize > 0) {
			boolean firstSegmentEmpty = this.segments.getFirst().getRawValue().isEmpty();
			if(firstSegmentEmpty && segmentSize == 1) {
				pathBuilder.append("/");
			}
			else {
				if(absolute && !firstSegmentEmpty) {
					pathBuilder.append("/");
				}
				
				AtomicInteger valuesIndex = new AtomicInteger();
				if(this.flags.isNormalized() && this.flags.isParameterized()) {
					// Here we must implement https://tools.ietf.org/html/rfc3986#section-5.2.4
					LinkedList<String> segmentValues = new LinkedList<>();
					for(SegmentComponent segment : this.segments) {
						String nextSegmentValue = segment.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(segment.getParameters().size()), values.length)), escapeSlash);
						if(nextSegmentValue.equals(".")) {
							// Segment is ignored
						}
						else if(nextSegmentValue.equals("..")) {
							// We have to remove the previous segment if it is not '..' OR keep that segment if there's no previous segment
							if(!segmentValues.isEmpty()) {
								String lastSegmentValue = segmentValues.peekLast();
								if(lastSegmentValue.equals("..") || (segmentValues.size() == 1 && lastSegmentValue.equals(""))) {
									segmentValues.add(nextSegmentValue);
								}
								else {
									segmentValues.removeLast();
								}
							}
							else {
								segmentValues.add(nextSegmentValue);
							}
						}
						else {
							segmentValues.add(nextSegmentValue);
						}
					}
					pathBuilder.append(segmentValues.stream().collect(Collectors.joining("/")));
				}
				else {
					pathBuilder.append(this.segments.stream()
						.map(segment -> segment.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(segment.getParameters().size()), values.length)), escapeSlash))
						.collect(Collectors.joining("/"))
					);
				}
			}
		}
		return pathBuilder.toString();
	}
	
	@Override
	public String buildPath(Map<String, ?> values) {
		return this.buildPath(values, true);
	}
	
	@Override
	public String buildPath(Map<String, ?> values, boolean escapeSlash) {
		StringBuilder pathBuilder = new StringBuilder();
		boolean absolute = (this.scheme != null && this.scheme.isPresent()) || (this.host != null && this.host.isPresent());
		
		int segmentSize = this.segments.size();
		if(segmentSize > 0) {
			boolean firstSegmentEmpty = this.segments.getFirst().getRawValue().isEmpty();
			if(firstSegmentEmpty && segmentSize == 1) {
				pathBuilder.append("/");
			}
			else {
				if(absolute && !firstSegmentEmpty) {
					pathBuilder.append("/");
				}
				if(this.flags.isNormalized() && this.flags.isParameterized()) {
					// Here we must implement https://tools.ietf.org/html/rfc3986#section-5.2.4
					LinkedList<String> segmentValues = new LinkedList<>();
					for(SegmentComponent segment : this.segments) {
						String nextSegmentValue = segment.getValue(values, escapeSlash);
						if(nextSegmentValue.equals(".")) {
							// Segment is ignored
						}
						else if(nextSegmentValue.equals("..")) {
							// We have to remove the previous segment if it is not '..' OR keep that segment if there's no previous segment
							if(!segmentValues.isEmpty()) {
								String lastSegmentValue = segmentValues.peekLast();
								if(lastSegmentValue.equals("..") || (segmentValues.size() == 1 && lastSegmentValue.equals(""))) {
									segmentValues.add(nextSegmentValue);
								}
								else {
									segmentValues.removeLast();
								}
							}
							else {
								segmentValues.add(nextSegmentValue);
							}
						}
						else {
							segmentValues.add(nextSegmentValue);
						}
					}
					pathBuilder.append(segmentValues.stream().collect(Collectors.joining("/")));
				}
				else {
					pathBuilder.append(this.segments.stream().map(segment -> segment.getValue(values, escapeSlash)).collect(Collectors.joining("/")));
				}
			}
		}
		return pathBuilder.toString();
	}
	
	@Override
	public String buildRawPath() {
		if(!this.segments.isEmpty()) {
			return this.segments.stream().map(segment -> segment.getRawValue()).collect(Collectors.joining("/"));
		}
		return "";
	}

	@Override
	public String buildQuery(Object... values) {
		StringBuilder queryBuilder = new StringBuilder();
		AtomicInteger valuesIndex = new AtomicInteger();
		if(!this.queryParameters.isEmpty()) {
			queryBuilder.append(this.queryParameters.stream()
				.map(queryParameter -> queryParameter.getValue(Arrays.copyOfRange(values, valuesIndex.get(), Math.min(valuesIndex.addAndGet(queryParameter.getParameters().size()), values.length))))
				.collect(Collectors.joining("&"))
			);
		}
		return queryBuilder.toString();
	}
	
	@Override
	public String buildQuery(Map<String, ?> values) {
		StringBuilder queryBuilder = new StringBuilder();
		if(!this.queryParameters.isEmpty()) {
			queryBuilder.append("?");
			queryBuilder.append(this.queryParameters.stream()
				.map(queryParameter -> queryParameter.getValue(values))
				.collect(Collectors.joining("&"))
			);
		}
		return queryBuilder.toString();
	}
	
	@Override
	public String buildRawQuery() {
		StringBuilder queryBuilder = new StringBuilder();
		if(!this.queryParameters.isEmpty()) {
			queryBuilder.append(this.queryParameters.stream().map(queryParameter -> queryParameter.getRawValue()).collect(Collectors.joining("&")));
		}
		return queryBuilder.toString();
	}
	
	@Override
	public URIPattern buildPattern(boolean matchTrailingSlash) {
		StringBuilder patternBuilder = new StringBuilder("^");
		StringBuilder rawValueBuilder = new StringBuilder();
		
		boolean absolute = false;
		if(this.scheme != null && this.scheme.isPresent()) {
			absolute = true;
			patternBuilder.append(this.scheme.getPattern()).append(":");
			rawValueBuilder.append(this.scheme.getRawValue()).append(":");
		}
		if(this.host != null && this.host.isPresent()) {
			absolute = true;
			patternBuilder.append("//");
			rawValueBuilder.append("//");
			if(this.userInfo != null && this.userInfo.isPresent()) {
				patternBuilder.append(this.userInfo.getPattern()).append("@");
				rawValueBuilder.append(this.userInfo.getRawValue()).append("@");
			}
			patternBuilder.append(this.host.getPattern());
			rawValueBuilder.append(this.host.getRawValue());
			if(this.port != null && this.port.isPresent()) {
				patternBuilder.append(":").append(this.port.getPattern());	
				rawValueBuilder.append(":").append(this.port.getRawValue());
			}
		}
		int segmentSize = this.segments.size();
		if(segmentSize > 0) {
			boolean firstSegmentEmpty = this.segments.getFirst().getRawValue().isEmpty();
			if(firstSegmentEmpty && segmentSize == 1) {
				patternBuilder.append("/");
				rawValueBuilder.append("/");
			}
			else {
				if(absolute && !firstSegmentEmpty) {
					patternBuilder.append("/");
					rawValueBuilder.append("/");
				}
				patternBuilder.append(this.segments.stream().map(segment -> segment.getPattern()).collect(Collectors.joining("/")));
				rawValueBuilder.append(this.segments.stream().map(segment -> segment.getRawValue()).collect(Collectors.joining("/")));
				if(matchTrailingSlash) {
					patternBuilder.append("/?");
				}
			}
		}
		if(!this.queryParameters.isEmpty()) {
			patternBuilder.append("\\?");
			rawValueBuilder.append("?");
			patternBuilder.append(this.queryParameters.stream().map(queryParameter -> queryParameter.getPattern()).collect(Collectors.joining("&")));
			rawValueBuilder.append(this.queryParameters.stream().map(queryParameter -> queryParameter.getRawValue()).collect(Collectors.joining("&")));
		}
		if(this.fragment != null && this.fragment.isPresent()) {
			patternBuilder.append("#").append(this.fragment.getPattern());
			rawValueBuilder.append("#").append(this.fragment.getRawValue());
		}
		patternBuilder.append("$");
		
		List<String> groupNames = new LinkedList<>();
		if(this.scheme != null) {
			groupNames.addAll(this.scheme.getPatternGroupNames());
		}
		if(this.userInfo != null) {
			groupNames.addAll(this.userInfo.getPatternGroupNames());
		}
		if(this.host != null) {
			groupNames.addAll(this.host.getPatternGroupNames());
		}
		if(this.port != null) {
			groupNames.addAll(this.port.getPatternGroupNames());
		}
		for(SegmentComponent segment : this.segments) {
			groupNames.addAll(segment.getPatternGroupNames());
		}
		for(QueryParameterComponent queryParameter : this.queryParameters) {
			groupNames.addAll(queryParameter.getPatternGroupNames());
		}
		if(this.fragment != null) {
			groupNames.addAll(this.fragment.getPatternGroupNames());
		}
		
		return new GenericURIPattern(rawValueBuilder.toString(), patternBuilder.toString(), groupNames);
	}
	
	@Override
	public URIPattern buildPathPattern(boolean matchTrailingSlash) {
		StringBuilder pathPatternBuilder = new StringBuilder("^");
		String rawValue = "";
		boolean absolute = (this.scheme != null && this.scheme.isPresent()) || (this.host != null && this.host.isPresent());
		int segmentSize = this.segments.size();
		if(segmentSize > 0) {
			boolean firstSegmentEmpty = this.segments.getFirst().getRawValue().isEmpty();
			if(firstSegmentEmpty && segmentSize == 1) {
				pathPatternBuilder.append("/");
			}
			else {
				if(absolute && !firstSegmentEmpty) {
					pathPatternBuilder.append("/");
				}
				pathPatternBuilder.append(this.segments.stream().map(segment -> segment.getPattern()).collect(Collectors.joining("/")));
				if(matchTrailingSlash) {
					pathPatternBuilder.append("/?");
				}
				rawValue = this.segments.stream().map(segment -> segment.getRawValue()).collect(Collectors.joining("/"));
			}
		}
		pathPatternBuilder.append("$");
		
		List<String> groupNames = new LinkedList<>();
		for(SegmentComponent segment : this.segments) {
			groupNames.addAll(segment.getPatternGroupNames());
		}
		
		return new GenericURIPattern(rawValue, pathPatternBuilder.toString(), groupNames);
	}
	
	@Override
	public URIBuilder clone() {
		GenericURIBuilder copy = new GenericURIBuilder(this.charset, this.flags.getOptions());
		
		copy.fragment = this.fragment;
		copy.host = this.host;
		copy.port = this.port;
		copy.queryParameters.addAll(this.queryParameters);
		copy.scheme = this.scheme;
		copy.segments.addAll(this.segments);
		copy.userInfo = this.userInfo;
		
		return copy;
	}
	
	@Override
	public String toString() {
		return this.buildRawString();
	}
}
