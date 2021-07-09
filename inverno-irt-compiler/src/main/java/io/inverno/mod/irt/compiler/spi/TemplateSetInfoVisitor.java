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
 * A template set info visitor is used to process a template set info.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 * @param <R> the visitor result type
 * @param <P> the visitor parameter type
 */
public interface TemplateSetInfoVisitor<R, P> {
	
	/**
	 * <p>
	 * Visits template set info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(TemplateSetInfo info, P p);
	
	/**
	 * <p>
	 * Visits package info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(PackageInfo info, P p);

	/**
	 * <p>
	 * Visits import info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(ImportInfo info, P p);

	/**
	 * <p>
	 * Visits include info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(IncludeInfo info, P p);

	/**
	 * <p>
	 * Visits option info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(OptionInfo info, P p);

	/**
	 * <p>
	 * Visits template info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(TemplateInfo info, P p);

	/**
	 * <p>
	 * Visits template select info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(TemplateInfo.SelectInfo info, P p);

	/**
	 * <p>
	 * Visits statement info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(StatementInfo info, P p);

	/**
	 * <p>
	 * Visits comment statement info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(CommentInfo info, P p);

	/**
	 * <p>
	 * Visits static content statement info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(StaticContentInfo info, P p);

	/**
	 * <p>
	 * Visits value statement info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(ValueInfo info, P p);

	/**
	 * <p>
	 * Visits if statement info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(IfInfo info, P p);

	/**
	 * <p>
	 * Visits if case info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(IfInfo.CaseInfo info, P p);

	/**
	 * <p>
	 * Visits apply statement info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(ApplyInfo info, P p);

	/**
	 * <p>
	 * Visits apply target parameter info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(ApplyInfo.TargetParameterInfo info, P p);

	/**
	 * <p>
	 * Visits apply target info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(ApplyInfo.TargetInfo info, P p);

	/**
	 * <p>
	 * Visits apply argument info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(ApplyInfo.ArgumentInfo info, P p);

	/**
	 * <p>
	 * Visits name info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(NameInfo info, P p);

	/**
	 * <p>
	 * Visits parameter info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(ParameterInfo info, P p);
	
	/**
	 * <p>
	 * Visits pipe info.
	 * </p>
	 * 
	 * @param info the info to visit
	 * @param p    a visitor parameter
	 * 
	 * @return a visitor result
	 */
	R visit(PipeInfo info, P p);
}
