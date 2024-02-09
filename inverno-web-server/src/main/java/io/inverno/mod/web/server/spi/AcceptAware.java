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
package io.inverno.mod.web.server.spi;

/**
 * <p>
 * Specifies criteria required to match requests or resources based on accepted media ranges or language ranges.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
public interface AcceptAware {

	/**
	 * <p>
	 * Returns a media type or a media range as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-3.1.1.5">RFC 7231 Section 3.1.1.5</a> and
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>
	 * </p>
	 * 
	 * @return a media type, a media range or null
	 */
	String getProduce();
	
	/**
	 * <p>
	 * Returns a language tag or a language range as defined <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a>.
	 * </p>
	 * 
	 * @return a language tag, a language range or null
	 */
	String getLanguage();
}
