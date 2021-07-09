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
package io.inverno.mod.irt.compiler.internal;

import java.util.List;

import io.inverno.mod.irt.compiler.spi.CommentInfo;
import io.inverno.mod.irt.compiler.spi.StatementInfo;

/**
 * <p>
 * Generic {@link CommentInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public class GenericCommentInfo extends BaseInfo implements CommentInfo {

	private final StatementInfo[] statements;
	
	/**
	 * <p>
	 * Creates a generic comment info.
	 * </p>
	 * 
	 * @param range      the range in the IRT source file where the info is defined
	 * @param statements a list of statements
	 */
	public GenericCommentInfo(Range range, List<StatementInfo> statements) {
		super(range);
		this.statements = statements.stream().toArray(StatementInfo[]::new);
	}

	@Override
	public StatementInfo[] getStatements() {
		return this.statements;
	}
}
