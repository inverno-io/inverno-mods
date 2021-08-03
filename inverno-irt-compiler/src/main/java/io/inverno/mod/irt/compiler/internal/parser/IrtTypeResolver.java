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
package io.inverno.mod.irt.compiler.internal.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import io.inverno.mod.irt.compiler.internal.IrtCompilationException;
import io.inverno.mod.irt.compiler.internal.Range;

/**
 * <p>
 * The IRT type resolver is used in the IRT parser to resolve types during
 * parsing.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public class IrtTypeResolver {

	/**
	 * Types utility.
	 */
	private final Types typeUtils;
	/**
	 * Elements utility.
	 */
	private final Elements elementUtils;

	/**
	 * A map of imported type elements
	 */
	private final Map<String, TypeElement> importedTypes;
	/**
	 * A set of wilddard imports
	 */
	private final Set<String> wildcardImports;

	/**
	 * The declared package of the template set being parsed.
	 */
	private List<String> packageParts;

	/**
	 * <p>
	 * Creates a new IRT Type resolver.
	 * </p>
	 * 
	 * @param typeUtils    the types utility
	 * @param elementUtils the element utility
	 */
	public IrtTypeResolver(Types typeUtils, Elements elementUtils) {
		this.typeUtils = typeUtils;
		this.elementUtils = elementUtils;

		this.importedTypes = new HashMap<>();
		this.wildcardImports = new HashSet<>();

		// java.lang is implicityl imported
		this.wildcardImports.add("java.lang.");
	}

	/**
	 * <p>
	 * Sets the package declared in the template set being parsed.
	 * </p>
	 * 
	 * @param packageParts the parts composing the package name
	 */
	public void setPackage(List<String> packageParts) {
		this.packageParts = packageParts;
	}

	/**
	 * <p>
	 * Registers an import which can be basic, static and ends with a wildcard.
	 * </p>
	 * 
	 * @param importParts the parts composing the name to import
	 */
	public void addImport(List<String> importParts) {
		if (importParts == null || importParts.isEmpty()) {
			throw new IllegalArgumentException("Import identifier is null or empty");
		}

		if (importParts.get(importParts.size() - 1).equals("*")) {
			if (importParts.size() == 1) {
				throw new IllegalArgumentException("Invalid import identifier: *");
			}
			this.wildcardImports.add(importParts.subList(0, importParts.size() - 1).stream().collect(Collectors.joining(".")) + ".");
		} 
		else {
			TypeElement typeElement = this.elementUtils.getTypeElement(importParts.stream().collect(Collectors.joining(".")));
			if (typeElement != null) {
				this.importedTypes.put(importParts.get(importParts.size() - 1), typeElement);
			}
		}
	}

	/**
	 * <p>
	 * Resolves a type.
	 * </p>
	 * 
	 * @param range     the range in the source file where the type to resolved is
	 *                  defined
	 * @param nameParts the parts composing the name of the type to resolve
	 * @return A type element
	 * 
	 * @throws IrtCompilationException if the specified type name doesn't resolve to
	 *                                 a type
	 */
	public TypeElement resolveType(Range range, List<String> nameParts) throws IrtCompilationException {
		if (nameParts == null || nameParts.isEmpty()) {
			throw new IllegalArgumentException("Type identifier is null or empty");
		}

		// 1. Is type imported?
		TypeElement resolvedType = this.importedTypes.get(nameParts.get(0));
		if (resolvedType != null) {
			if (nameParts.size() > 1) {
				resolvedType = this.elementUtils.getTypeElement(resolvedType.getQualifiedName().toString() + "." + nameParts.subList(1, nameParts.size()).stream().collect(Collectors.joining(".")));
				if (resolvedType == null) {
					throw new IrtCompilationException(nameParts.stream().collect(Collectors.joining(".")) + " cannot be resolved to a type", range);
				}
			}
		}
		else {
			// 2. Try wildcard imports package
			for (String wildcardImport : this.wildcardImports) {
				resolvedType = this.elementUtils
						.getTypeElement(wildcardImport + nameParts.stream().collect(Collectors.joining(".")));
				if (resolvedType != null) {
					break;
				}
			}
		}

		// 3. Try within template package
		if (resolvedType == null && this.packageParts != null && !this.packageParts.isEmpty()) {
			resolvedType = this.elementUtils.getTypeElement(Stream.concat(this.packageParts.stream(), nameParts.stream()).collect(Collectors.joining(".")));
		}

		// 4. Try fully qualified name
		if (resolvedType == null) {
			resolvedType = this.elementUtils.getTypeElement(nameParts.stream().collect(Collectors.joining(".")));
		}

		if (resolvedType == null) {
			throw new IrtCompilationException(nameParts.stream().collect(Collectors.joining(".")) + " cannot be resolved to a type", range);
		}

		return resolvedType;
	}

	/**
	 * <p>
	 * Returns the types utility.
	 * </p>
	 * 
	 * @return the types utility
	 */
	public Types getTypeUtils() {
		return this.typeUtils;
	}

	/**
	 * <p>
	 * Returns the elements utility.
	 * </p>
	 * 
	 * @return the elements utility
	 */
	public Elements getElementUtils() {
		return this.elementUtils;
	}
}
