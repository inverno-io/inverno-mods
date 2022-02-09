/*
 * Copyright 2022 Jeremy KUHN
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import io.inverno.mod.sql.PreparedStatement;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.data.NullValue;
import io.vertx.sqlclient.impl.ListTuple;

/**
 * <p>
 * Base {@link PreparedStatement} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public abstract class AbstractPreparedStatement implements PreparedStatement {

	protected static final NullValue OBJECT_NULL_VALUE = NullValue.of(Object.class);
	
	protected final LinkedList<Tuple> batch;
	
	protected ListTuple currentParameters;
	
	/**
	 * <p>
	 * Creates a prepared statement.
	 * </p>
	 */
	public AbstractPreparedStatement() {
		this.batch = new LinkedList<>();
		this.currentParameters = new ListTuple(new ArrayList<>());
		this.batch.add(this.currentParameters);
	}
	
	@Override
	public PreparedStatement and() {
		this.currentParameters = new ListTuple(new ArrayList<>());
		this.batch.add(this.currentParameters);
		return this;
	}

	@Override
	public PreparedStatement bindAt(int index, Object value) {
		if(this.currentParameters.size() < index) {
			while(this.currentParameters.size() < index) {
				this.currentParameters.addValue(OBJECT_NULL_VALUE);
			}
			this.currentParameters.addValue(value);
		}
		else {
			this.currentParameters.setValue(index, value);
		}
		return this;
	}

	@Override
	public PreparedStatement bindNullAt(int index, Class<?> type) {
		if(this.currentParameters.size() < index) {
			while(this.currentParameters.size() < index) {
				this.currentParameters.addValue(OBJECT_NULL_VALUE);
			}
			this.currentParameters.addValue(NullValue.of(type));
		}
		else {
			this.currentParameters.setValue(index, NullValue.of(type));
		}
		return this;
	}

	@Override
	public PreparedStatement bind(Object... values) {
		this.batch.pollLast();
		this.currentParameters = new ListTuple(Arrays.asList(values));
		this.batch.add(this.currentParameters);
		return this;
	}
	
	@Override
	public PreparedStatement bind(List<Object[]> values) {
		if(values == null || values.isEmpty()) {
			throw new IllegalArgumentException("Bindings list is null or empty");
		}
		this.batch.pollLast();
		values.forEach(value -> this.batch.add(new ListTuple(Arrays.asList(value))));
		this.currentParameters = (ListTuple)this.batch.getLast();
		return this;
	}
	
	@Override
	public PreparedStatement bind(Stream<Object[]> values) {
		if(values == null) {
			throw new IllegalArgumentException("Bindings stream is null");
		}
		this.batch.pollLast();
		values.forEach(value -> this.batch.add(new ListTuple(Arrays.asList(value))));
		this.currentParameters = (ListTuple)this.batch.getLast();;
		return this;
	}
	
	@Override
	public PreparedStatement fetchSize(int rows) {
		// Noop since we are not streaming result
		return this;
	}

	@Override
	public synchronized PreparedStatement reset() {
		this.batch.clear();
		this.currentParameters = new ListTuple(new ArrayList<>());
		this.batch.add(this.currentParameters);
		
		return this;
	}
}
