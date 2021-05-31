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
package io.inverno.mod.web.compiler.internal;

import io.inverno.core.compiler.spi.ReporterInfo;

/**
 * <p>
 * A silent reporter info which ignores error and warning reports.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ReporterInfo
 */
class NoOpReporterInfo implements ReporterInfo {

	private final ReporterInfo reporter;
	
	/**
	 * <p>
	 * Creates a no-op reporter info.
	 * </p>
	 */
	public NoOpReporterInfo() {
		this(null);
	}
	/**
	 * <p>
	 * Creates a no-op reporter info wrapping the specified reporter.
	 * </p>
	 * 
	 * @param reporter the underlying reporter
	 */
	public NoOpReporterInfo(ReporterInfo reporter) {
		this.reporter = reporter;
	}
	
	public ReporterInfo getReporter() {
		return reporter;
	}

	@Override
	public boolean hasError() {
		return this.reporter != null ? this.reporter.hasError() : false;
	}

	@Override
	public boolean hasWarning() {
		return this.reporter != null ? this.reporter.hasWarning() : false;
	}

	@Override
	public void error(String message) {

	}

	@Override
	public void warning(String message) {

	}

}
