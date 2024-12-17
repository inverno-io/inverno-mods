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
package io.inverno.mod.http.base.router.link;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link Pattern} wrapper for representing providing {@link #equals(Object)} and {@link #hashCode()} using {@link Pattern#pattern()}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
class PatternWrapper {

	private final Pattern pattern;

	/**
	 * <p>
	 * Creates a pattern wrapper
	 * </p>
	 *
	 * @param pattern the wrapped pattern
	 */
	public PatternWrapper(Pattern pattern) {
		this.pattern = pattern;
	}

	/**
	 * <p>
	 * Returns the wrapped pattern.
	 * </p>
	 *
	 * @return the wrapped pattern
	 */
	public Pattern unwrap() {
		return pattern;
	}

	/**
	 * <p>
	 * Matches the specified input with the wrapped pattern.
	 * </p>
	 *
	 * @param input the input to match
	 *
	 * @return true if the input matches the pattern, false otherwise
	 */
	public boolean matches(String input) {
		return this.pattern.matcher(input).matches();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PatternWrapper that = (PatternWrapper) o;
		return Objects.equals(pattern != null ? pattern.pattern() : null, that.pattern != null ? that.pattern.pattern() : null);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(pattern != null ? pattern.pattern() : null);
	}
}
