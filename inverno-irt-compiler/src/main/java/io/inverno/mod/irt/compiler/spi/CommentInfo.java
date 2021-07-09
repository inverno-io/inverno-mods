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
package io.inverno.mod.irt.compiler.spi;

/**
 * <p>
 * A comment info corresponds to a comment statement in a template declaration
 * in a template set source file.
 * </p>
 * 
 * <p>
 * A comment info typically contains statements that are ignored during the
 * generation of a template set class.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public interface CommentInfo extends StatementInfo {
	
	/**
	 * <p>
	 * Returns the list of commented statements.
	 * </p>
	 * 
	 * @return a list of statements
	 */
	StatementInfo[] getStatements();
}
