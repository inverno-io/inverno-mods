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
package io.inverno.mod.sql.vertx.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.inverno.mod.sql.ColumnMetadata;
import io.inverno.mod.sql.RowMetadata;

/**
 * <p>
 * Generic {@link RowMetadata} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericRowMetadata implements RowMetadata {

	private final List<String> columnNames;
	
	private Map<String, ColumnMetadata> columnMetadata;

	/**
	 * <p>
	 * Creates generic row metadata.
	 * </p>
	 * 
	 * @param columnNames the name of the columns in the row
	 */
	public GenericRowMetadata(List<String> columnNames) {
		this.columnNames = columnNames;
	}

	@Override
	public Collection<String> getColumnNames() {
		return this.columnNames;
	}

	@Override
	public Collection<ColumnMetadata> getColumnMetadata() {
		if(this.columnMetadata == null) {
			this.columnMetadata = this.columnNames.stream().collect(Collectors.toMap(Function.identity(), GenericColumnMetadata::new, (e1, e2) -> {throw new RuntimeException();}, LinkedHashMap::new));
		}
		return this.columnMetadata.values();
	}

	@Override
	public ColumnMetadata getColumnMetadata(int index) {
		return this.columnMetadata.get(this.columnNames.get(index));
	}

	@Override
	public ColumnMetadata getColumnMetadata(String name) {
		return this.columnMetadata.get(name);
	}
}
