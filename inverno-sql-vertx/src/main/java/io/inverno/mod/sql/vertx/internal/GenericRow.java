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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

import io.inverno.mod.sql.Row;
import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;

/**
 * <p>
 * Generic {@link Row} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericRow implements Row {

	private final io.vertx.sqlclient.Row row;

	/**
	 * <p>
	 * Creates a generic row.
	 * </p>
	 * 
	 * @param row the underlying Vert.x row
	 */
	public GenericRow(io.vertx.sqlclient.Row row) {
		this.row = row;
	}

	@Override
	public <T> T get(int index, Class<T> type) {
		return this.row.get(type, index);
	}

	@Override
	public <T> T get(String name, Class<T> type) {
		return this.row.get(type, name);
	}

	@Override
	public Object get(int index) {
		return this.row.get(Object.class, index);
	}

	@Override
	public Object get(String name) {
		return this.row.get(Object.class, name);
	}
	
	@Override
	public String getString(int index) {
		return this.row.getString(index);
	}
	
	@Override
	public String getString(String name) {
		return this.row.getString(name);
	}

	@Override
	public Boolean getBoolean(int index) {
		return this.row.getBoolean(index);
	}

	@Override
	public Boolean getBoolean(String name) {
		return this.row.getBoolean(name);
	}

	@Override
	public Byte getByte(int index) {
		Short s = this.row.getShort(index);
		return s != null ? s.byteValue() : null;
	}

	@Override
	public Byte getByte(String name) {
		Short s = this.row.getShort(name);
		return s != null ? s.byteValue() : null;
	}

	@Override
	public Short getShort(int index) {
		return this.row.getShort(index);
	}

	@Override
	public Short getShort(String name) {
		return this.row.getShort(name);
	}

	@Override
	public Integer getInteger(int index) {
		return this.row.getInteger(index);
	}

	@Override
	public Integer getInteger(String name) {
		return this.row.getInteger(name);
	}

	@Override
	public Long getLong(int index) {
		return this.row.getLong(index);
	}

	@Override
	public Long getLong(String name) {
		return this.row.getLong(name);
	}

	@Override
	public Float getFloat(int index) {
		return this.row.getFloat(index);
	}

	@Override
	public Float getFloat(String name) {
		return this.row.getFloat(name);
	}

	@Override
	public Double getDouble(int index) {
		return this.row.getDouble(index);
	}

	@Override
	public Double getDouble(String name) {
		return this.row.getDouble(name);
	}

	@Override
	public BigDecimal getBigDecimal(int index) {
		return this.row.getBigDecimal(index);
	}

	@Override
	public BigDecimal getBigDecimal(String name) {
		return this.row.getBigDecimal(name);
	}

	@Override
	public LocalDate getLocalDate(int index) {
		return this.row.getLocalDate(index);
	}

	@Override
	public LocalDate getLocalDate(String name) {
		return this.row.getLocalDate(name);
	}

	@Override
	public LocalTime getLocalTime(int index) {
		return this.row.getLocalTime(index);
	}

	@Override
	public LocalTime getLocalTime(String name) {
		return this.row.getLocalTime(name);
	}

	@Override
	public LocalDateTime getLocalDateTime(int index) {
		return this.row.getLocalDateTime(index);
	}

	@Override
	public LocalDateTime getLocalDateTime(String name) {
		return this.row.getLocalDateTime(name);
	}

	@Override
	public OffsetTime getOffsetTime(int index) {
		return this.row.getOffsetTime(index);
	}

	@Override
	public OffsetTime getOffsetTime(String name) {
		return this.row.getOffsetTime(name);
	}

	@Override
	public OffsetDateTime getOffsetDateTime(int index) {
		return this.row.getOffsetDateTime(index);
	}

	@Override
	public OffsetDateTime getOffsetDateTime(String name) {
		return this.row.getOffsetDateTime(name);
	}
	
	@Override
	public ByteBuf getByteBuf(int index) {
		Buffer buffer = this.row.getBuffer(index);
		return buffer != null ? buffer.getByteBuf() : null;
	}
	
	@Override
	public ByteBuf getByteBuf(String name) {
		Buffer buffer = this.row.getBuffer(name);
		return buffer != null ? buffer.getByteBuf() : null;
	}
}
