/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.base.internal.netty;

import io.inverno.mod.http.base.header.Headers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>
 * Optimized {@link HttpHeaders} implementation.
 * </p>
 *
 * <p>
 * This implementation is based on a linked list keeping a pointer to the head and the tail of the list for fast access.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class LinkedHttpHeaders extends HttpHeaders {

	private HeaderNode head;
	
	private HeaderNode tail;
	
	private HeaderNode[] buckets = new HeaderNode[16];
	
	public LinkedHttpHeaders() {
		this.head = this.tail = new HeaderNode();
	}
	
	private CharSequence convertToCharSequence(Object value) {
		if(value instanceof CharSequence) {
			return (CharSequence)value;
		}
		else if(value instanceof TemporalAccessor){
			return Headers.FORMATTER_RFC_5322_DATE_TIME.format((TemporalAccessor)value);
		}
		else if(value instanceof Date) {
			return Headers.FORMATTER_RFC_5322_DATE_TIME.format(((Date)value).toInstant());
		}
		else if(value instanceof Calendar) {
			return Headers.FORMATTER_RFC_5322_DATE_TIME.format(((Calendar)value).toInstant());
		}
		else {
			return value.toString();
		}
	}
	
	private CharSequence get0(CharSequence name) {
		int hashCode = AsciiString.hashCode(name);
		int bucketIndex = hashCode & 0x0000000F;
		HeaderNode current = buckets[bucketIndex];
		CharSequence value = null;
		while (current != null) {
			CharSequence key = current.key;
			if (current.hashCode == hashCode && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
				value = current.getValue();
			}
			current = current.bucketNext;
		}
		return value;
	}
	
	@Override
	public String get(String name) {
		return this.get((CharSequence)name);
	}
	
	@Override
	public String get(CharSequence name) {
	    CharSequence ret = this.get0(name);
	    return ret != null ? ret.toString() : null;
	}

	@Override
	public List<String> getAll(String name) {
	    return this.getAll((CharSequence)name);
	}
	
	@Override
	public List<String> getAll(CharSequence name) {
		LinkedList<String> values = new LinkedList<>();
	    int h = AsciiString.hashCode(name);
	    int i = h & 0x0000000F;
	    HeaderNode e = buckets[i];
	    while (e != null) {
	      CharSequence key = e.key;
	      if (e.hashCode == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
	        values.addFirst(e.getValue().toString());
	      }
	      e = e.bucketNext;
	    }
	    return values;
	}
	
	public List<CharSequence> getAllCharSequence(CharSequence name) {
		LinkedList<CharSequence> values = new LinkedList<>();
	    int h = AsciiString.hashCode(name);
	    int i = h & 0x0000000F;
	    HeaderNode e = buckets[i];
	    while (e != null) {
	      CharSequence key = e.key;
	      if (e.hashCode == h && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
	        values.addFirst(e.getValue());
	      }
	      e = e.bucketNext;
	    }
	    return values;
	}
	
	public CharSequence getCharSequence(CharSequence name) {
		return this.get0(name);
	}
	
	@Override
	public Integer getInt(CharSequence name) {
		String value = this.get(name);
		return value != null ? Integer.parseInt(value) : null;
	}

	@Override
	public int getInt(CharSequence name, int defaultValue) {
		Objects.requireNonNull(name);
		String value = this.get(name);
		return value != null ? Integer.parseInt(value) : defaultValue;
	}
	
	public Long getLong(CharSequence name) {
		String value = this.get(name);
		return value != null ? Long.parseLong(value) : null;
	}

	public long getLong(CharSequence name, long defaultValue) {
		Objects.requireNonNull(name);
		String value = this.get(name);
		return value != null ? Long.parseLong(value) : defaultValue;
	}

	@Override
	public Short getShort(CharSequence name) {
		String value = this.get(name);
		return value != null ? Short.parseShort(value) : null;
	}

	@Override
	public short getShort(CharSequence name, short defaultValue) {
		Objects.requireNonNull(name);
		String value = this.get(name);
		return value != null ? Short.parseShort(value) : defaultValue;
	}

	@Override
	public Long getTimeMillis(CharSequence name) {
		String value = this.get(name);
		return value != null ? Headers.FORMATTER_RFC_5322_DATE_TIME.parse(value).getLong(ChronoField.MILLI_OF_SECOND) : null;
	}

	@Override
	public long getTimeMillis(CharSequence name, long defaultValue) {
		String value = this.get(name);
		return value != null ? Headers.FORMATTER_RFC_5322_DATE_TIME.parse(value).getLong(ChronoField.MILLI_OF_SECOND) : defaultValue;
	}

	private void add0(CharSequence name, CharSequence value) {
		int hashCode = AsciiString.hashCode(name);
		int bucketIndex = hashCode & 0x0000000F;
		this.add0(name, value, hashCode, bucketIndex);
	}
	
	private void add0(CharSequence name, CharSequence value, int hashCode, int bucketIndex) {
		HeaderNode bucketHead = buckets[bucketIndex];
		HeaderNode newNode;
		buckets[bucketIndex] = newNode = new HeaderNode(hashCode, name, value);
		newNode.bucketNext = bucketHead;
		
		HeaderNode headKeep = this.head;
		this.head = newNode;
		headKeep.previous = newNode;
		newNode.next = headKeep;
	}

	@Override
	public HttpHeaders add(String name, Object value) {
	    return this.add((CharSequence)name, value);
	}
	
	@Override
	public HttpHeaders add(CharSequence name, Object value) {
		if(value instanceof Iterable) {
			this.add(name, (Iterable<?>)value);
		}
		else {
			this.add0(name, this.convertToCharSequence(value));
		}
	    return this;
	}
	
	@Override
	public HttpHeaders add(String name, Iterable<?> values) {
	    return this.add((CharSequence)name, values);
	}
	
	@Override
	public HttpHeaders add(CharSequence name, Iterable<?> values) {
		int hashCode = AsciiString.hashCode(name);
	    int bucketIndex = hashCode & 0x0000000F;
	    for (Object value : values) {
	    	this.add0(name, this.convertToCharSequence(value), hashCode, bucketIndex);
	    }
	    return this;
	}
	
	public HttpHeaders addCharSequence(CharSequence name, CharSequence value) {
		this.add0(name, value);
	    return this;
	}
	
	public HttpHeaders addCharSequence(CharSequence name, Iterable<CharSequence> values) {
		int hashCode = AsciiString.hashCode(name);
	    int bucketIndex = hashCode & 0x0000000F;
	    for (CharSequence value : values) {
	    	this.add0(name, value, hashCode, bucketIndex);
	    }
	    return this;
	}

	@Override
	public HttpHeaders addInt(CharSequence name, int value) {
		this.add0(name, Integer.toString(value));
		return this;
	}
	
	public HttpHeaders addLong(CharSequence name, long value) {
		this.add0(name, Long.toString(value));
		return this;
	}

	@Override
	public HttpHeaders addShort(CharSequence name, short value) {
		this.add0(name, Short.toString(value));
		return this;
	}

	private LinkedHttpHeaders set0(CharSequence name, CharSequence value) {
		int hashCode = AsciiString.hashCode(name);
		int bucketIndex = hashCode & 0x0000000F;
		return this.set0(name, value, hashCode, bucketIndex);
	}
	
	private LinkedHttpHeaders set0(CharSequence name, CharSequence value, int hashCode, int bucketIndex) {
		remove0(name, hashCode, bucketIndex);
		if (value != null) {
			add0(name, value, hashCode, bucketIndex);
		}
		return this;
	}

	@Override
	public HttpHeaders set(String name, Object value) {
		return this.set((CharSequence)name, value);
	}
	
	@Override
	public HttpHeaders set(CharSequence name, Object value) {
		Objects.requireNonNull(value);
		if(value instanceof Iterable) {
			this.set(name, (Iterable<?>)value);
		}
		else {
			this.set0(name, this.convertToCharSequence(value));
		}
	    return this;
	}

	@Override
	public HttpHeaders set(String name, Iterable<?> values) {
	    return this.set((CharSequence)name, values);
	}
	
	@Override
	public HttpHeaders set(CharSequence name, Iterable<?> values) {
		int hashCode = AsciiString.hashCode(name);
	    int bucketIndex = hashCode & 0x0000000F;
	    for (Object value : values) {
	    	this.set0(name, this.convertToCharSequence(value), hashCode, bucketIndex);
	    }
	    return this;
	}

	public LinkedHttpHeaders setCharSequence(CharSequence name, CharSequence value) {
		this.set0(name, value);
		return this;
	}
	
	public HttpHeaders setCharSequence(CharSequence name, Iterable<CharSequence> values) {
		int hashCode = AsciiString.hashCode(name);
	    int bucketIndex = hashCode & 0x0000000F;
	    for (CharSequence value : values) {
	    	this.set0(name, value, hashCode, bucketIndex);
	    }
	    return this;
	}
	
	@Override
	public HttpHeaders setInt(CharSequence name, int value) {
		this.set0(name, Integer.toString(value));
		return this;
	}
	
	public HttpHeaders setLong(CharSequence name, long value) {
		this.set0(name, Long.toString(value));
		return this;
	}

	@Override
	public HttpHeaders setShort(CharSequence name, short value) {
		this.set0(name, Short.toString(value));
		return this;
	}
	
	private void remove0(CharSequence name) {
		int hashCode = AsciiString.hashCode(name);
		int bucketIndex = hashCode & 0x0000000F;
		this.remove0(name, hashCode, bucketIndex);
	}
	
	private void remove0(CharSequence name, int hashCode, int bucketIndex) {
		HeaderNode current = buckets[bucketIndex];
		HeaderNode previous = null;
		while (current != null) {
			CharSequence key = current.key;
			if (current.hashCode == hashCode && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
				if(previous == null) {
					this.buckets[bucketIndex] = current.bucketNext;
				}
				else {
					previous.bucketNext = current.bucketNext;
				}
				if(current.previous == null) {
					current.next.previous = null;
					this.head = current.next;
				}
				else {
					current.previous.next = current.next;
					current.next.previous = current.previous;
				}
			}
			else {
				previous = current;
			}
			current = current.bucketNext;
		}
	}

	@Override
	public HttpHeaders remove(String name) {
		return this.remove((CharSequence)name);
	}
	
	@Override
	public HttpHeaders remove(CharSequence name) {
		this.remove0(name);
		return this;
	}
	
	@Override
	public HttpHeaders clear() {
		Arrays.fill(this.buckets, null);
		this.tail.previous = null;
		this.head = this.tail;
		return this;
	}

	@Override
	public List<Entry<String, String>> entries() {
		List<Map.Entry<String, String>> entries = new ArrayList<>();
		this.forEach(entries::add);
		return entries;
	}
	
	public List<Entry<CharSequence, CharSequence>> entriesCharSequence() {
		List<Map.Entry<CharSequence, CharSequence>> entries = new ArrayList<>();
		for(Iterator<Map.Entry<CharSequence, CharSequence>> entriesIterator = this.iteratorCharSequence();entriesIterator.hasNext();) {
			entries.add(entriesIterator.next());
		}
		return entries;
	}
	
	@Override
	public Set<String> names() {
		Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		HeaderNode current = this.head;
		while (current.next != null) {
			names.add(current.getKey().toString());
			current = current.next;
		}
		return names;
	}

	private boolean contains0(CharSequence name, CharSequence value, boolean ignoreCase) {
		int hashCode = AsciiString.hashCode(name);
		int bucketIndex = hashCode & 0x0000000F;
		HeaderNode current = buckets[bucketIndex];
		while (current != null) {
			CharSequence key = current.key;
			if (current.hashCode == hashCode && (name == key || AsciiString.contentEqualsIgnoreCase(name, key))) {
				CharSequence currentValue = current.getValue();
				return currentValue == value || (ignoreCase && AsciiString.contentEqualsIgnoreCase(currentValue, value)) || AsciiString.contentEquals(currentValue, value);
			}
			current = current.bucketNext;
		}
		return false;
	}
	
	@Override
	public boolean contains(String name) {
		return this.get0(name) != null;
	}
	
	@Override
	public boolean contains(CharSequence name) {
		return this.get0(name) != null;
	}
	
	@Override
	public boolean contains(CharSequence name, CharSequence value, boolean ignoreCase) {
		return this.contains0(name, value, ignoreCase);
	}
	
	@Override
	public boolean contains(String name, String value, boolean ignoreCase) {
		return this.contains0(name, value, ignoreCase);
	}
	
	@Override
	public boolean isEmpty() {
		return this.head.next == null;
	}

	@Override
	public int size() {
		int size = 0;
		HeaderNode current = this.head;
		while(current != null) {
			size++;
		}
		return size;
	}

	@Override
	@Deprecated
	public Iterator<Entry<String, String>> iterator() {
		return new Iterator<Map.Entry<String, String>>() {
			HeaderNode current = tail;

			@Override
			public boolean hasNext() {
				return this.current.previous != null;
			}

			@Override
			public Map.Entry<String, String> next() {
				HeaderNode next = this.current.previous;
				if(next == null) {
					throw new NoSuchElementException();
				}
				this.current = next;
				return Map.entry(next.key.toString(), next.value.toString());
			}
		};
	}

	@Override
	public Iterator<Entry<CharSequence, CharSequence>> iteratorCharSequence() {
		return new Iterator<Map.Entry<CharSequence, CharSequence>>() {
			HeaderNode current = tail;

			@Override
			public boolean hasNext() {
				return this.current.previous != null;
			}

			@Override
			public Map.Entry<CharSequence, CharSequence> next() {
				HeaderNode next = this.current.previous;
				if(next == null) {
					throw new NoSuchElementException();
				}
				this.current = next;
				return next;
			}
		};
	}
	
	public void encode(ByteBuf buf) {
		HeaderNode current = this.tail.previous;
		while (current != null) {
			encoderHeader(current.key, current.value, buf);
			current = current.previous;
		}
	}

	private static final int COLON_AND_SPACE_SHORT = (HttpConstants.COLON << 8) | HttpConstants.SP;
	static final int CRLF_SHORT = (HttpConstants.CR << 8) | HttpConstants.LF;

	static void encoderHeader(CharSequence name, CharSequence value, ByteBuf buf) {
		final int nameLen = name.length();
		final int valueLen = value.length();
		final int headerLen = nameLen + valueLen + 4;
		buf.ensureWritable(headerLen);
		int offset = buf.writerIndex();
		writeAscii(buf, offset, name);
		offset += nameLen;
		ByteBufUtil.setShortBE(buf, offset, COLON_AND_SPACE_SHORT);
		offset += 2;
		writeAscii(buf, offset, value);
		offset += valueLen;
		ByteBufUtil.setShortBE(buf, offset, CRLF_SHORT);
		offset += 2;
		buf.writerIndex(offset);
	}

	private static void writeAscii(ByteBuf buf, int offset, CharSequence value) {
		if (value instanceof AsciiString) {
			ByteBufUtil.copy((AsciiString) value, 0, buf, offset, value.length());
		} 
		else {
			buf.setCharSequence(offset, value, StandardCharsets.US_ASCII);
		}
	}

	private final class HeaderNode implements Map.Entry<CharSequence, CharSequence> {

		final CharSequence key;
		final int hashCode;
		CharSequence value;
		
		HeaderNode next, previous;
		HeaderNode bucketNext;
		
		private HeaderNode() {
			this.hashCode = -1;
			this.key = null;
			this.value = null;
		}
		
		private HeaderNode(int hashCode, CharSequence key, CharSequence value) {
			this.hashCode = hashCode;
			this.key = key;
			this.value = value;
		}
		
		@Override
		public CharSequence getKey() {
			return this.key;
		}

		@Override
		public CharSequence getValue() {
			return this.value;
		}

		@Override
		public CharSequence setValue(CharSequence value) {
			CharSequence previousValue = this.value;
			this.value = value;
			return previousValue;
		}
	}
}
