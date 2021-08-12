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
package io.inverno.mod.sql;

import java.util.Collection;

/**
 * <p>
 * Exposes database row metadata.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface RowMetadata {

	/**
	 * <p>
	 * Returns the names of the columns in a row.
	 * </p>
	 * 
	 * @return a list of columns names
	 */
	Collection<String> getColumnNames();
	
	/**
	 * <p>Returns the list of column metadata.</p>
	 * 
	 * @return a list of column metadata
	 */
	Collection<ColumnMetadata> getColumnMetadatas();
	
	/**
	 * <p>
	 * Returns the column metadata of the column at the specified index in the row.
	 * </p>
	 * 
	 * @param index the index of the column in the row
	 * 
	 * @return a column metadata or null
	 */
	ColumnMetadata getColumnMetadata(int index);
	
	/**
	 * <p>
	 * Returns the column metadata of the column identified by the specified name in
	 * the row.
	 * </p>
	 * 
	 * @param name the name of the column in the row
	 * 
	 * @return a column metadata or null
	 */
	ColumnMetadata getColumnMetadata(String name);
}
