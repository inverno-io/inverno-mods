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
package io.winterframework.mod.web.internal.server;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.ResponseHeaders;

/**
 * @author jkuhn
 *
 */
public interface AbstractResponseHeaders extends ResponseHeaders {
	
	boolean isWritten();
	
	void setWritten(boolean written);

	Optional<Headers.ContentType> getContentType();
	
	String getContentTypeString();
	
	CharSequence getContentTypeCharSequence();
	
	<T extends Header> Optional<T> get(String name);

	String getString(String name);
	
	CharSequence getCharSequence(String name);
	
	<T extends Header> List<T> getAll(String name);
	
	List<String> getAllString(String name);
	
	List<CharSequence> getAllCharSequence(String name);

	List<Header> getAll();
	
	List<Map.Entry<String, String>> getAllString();
	
	List<Map.Entry<CharSequence, CharSequence>> getAllCharSequence();
	
	Set<String> getNames();
	
	Long getSize();
	
	int getStatus();
}
