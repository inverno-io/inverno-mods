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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.NoSuchElementException;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * A single row in a query result.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface Row {

	/**
	 * <p>
	 * Returns the value at the specified index in the specified type.
	 * </p>
	 * 
	 * @param <T>   the value type
	 * @param index the index of the value in the row
	 * @param type  the expected type of the value
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into the type
	 */
	<T> T get(int index, Class<T> type);

	/**
	 * <p>
	 * Returns the value of the column identified by the specified name in the
	 * specified type.
	 * </p>
	 * 
	 * @param <T>  the value type
	 * @param name the name of the column
	 * @param type the expected type of the value
	 * 
	 * @return the value or null
	 * @throws ClassCastException     if the value can't be cast into the type
	 * @throws NoSuchElementException if there is no column with the specified name
	 */
	<T> T get(String name, Class<T> type);
	
	/**
	 * <p>
	 * Returns the value at the specified index.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 */
	Object get(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 */
	Object get(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a String.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a String
	 */
	String getString(int index);

	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a String.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a String
	 */
	String getString(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a Boolean.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a Boolean
	 */
	Boolean getBoolean(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a
	 * Boolean.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a Boolean
	 */
	Boolean getBoolean(String name);

	/**
	 * <p>
	 * Returns the value at the specified index as a Byte.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a Byte
	 */
	Byte getByte(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a Byte.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a Byte
	 */
	Byte getByte(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a Short.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a Short
	 */
	Short getShort(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a Short.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a Short
	 */
	Short getShort(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as an Integer.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into an Integer
	 */
	Integer getInteger(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as an
	 * Integer.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into an Integer
	 */
	Integer getInteger(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a Long.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a Long
	 */
	Long getLong(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a Long.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a Long
	 */
	Long getLong(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a Float.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a Float
	 */
	Float getFloat(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a Float.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a Float
	 */
	Float getFloat(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a Double.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a Double
	 */
	Double getDouble(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a Double.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a Double
	 */
	Double getDouble(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a BigDecimal.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a BigDecimal
	 */
	BigDecimal getBigDecimal(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a
	 * BigDecimal.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a BigDecimal
	 */
	BigDecimal getBigDecimal(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a LocalDate.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a LocalDate
	 */
	LocalDate getLocalDate(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a
	 * LocalDate.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a LocalDate
	 */
	LocalDate getLocalDate(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a LocalTime.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a LocalTime
	 */
	LocalTime getLocalTime(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a
	 * LocalTime.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a LocalTime
	 */
	LocalTime getLocalTime(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a LocalDateTime.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a LocalDateTime
	 */
	LocalDateTime getLocalDateTime(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a
	 * LocalDateTime.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a
	 *                                LocalDateTime
	 */
	LocalDateTime getLocalDateTime(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as an OffsetTime.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into an OffsetTime
	 */
	OffsetTime getOffsetTime(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as an
	 * OffsetTime.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into an OffsetTime
	 */
	OffsetTime getOffsetTime(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as an OffsetDateTime.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into an OffsetDateTime
	 */
	OffsetDateTime getOffsetDateTime(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as an
	 * OffsetDateTime.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into an
	 *                                OffsetDateTime
	 */
	OffsetDateTime getOffsetDateTime(String name);
	
	/**
	 * <p>
	 * Returns the value at the specified index as a ByteBuf.
	 * </p>
	 * 
	 * @param index the index of the value in the row
	 * 
	 * @return the value or null
	 * @throws ClassCastException if the value can't be cast into a ByteBuf
	 */
	ByteBuf getByteBuf(int index);
	
	/**
	 * <p>
	 * Returns the value of the column identified by the specified name as a
	 * ByteBuf.
	 * </p>
	 * 
	 * @param name the name of the column
	 * 
	 * @return the value or null
	 * @throws NoSuchElementException if there is no column with the specified name
	 * @throws ClassCastException     if the value can't be cast into a ByteBuf
	 */
	ByteBuf getByteBuf(String name);
}
