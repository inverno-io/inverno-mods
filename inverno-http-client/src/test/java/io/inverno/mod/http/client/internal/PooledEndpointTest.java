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

package io.inverno.mod.http.client.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class PooledEndpointTest {

	@Test
	public void testConnectionRequestBuffer() {
		
		ConnectionRequestBuffer buffer = new ConnectionRequestBuffer();
		
		ConnectionRequest r1 = new ConnectionRequest();
		ConnectionRequest r2 = new ConnectionRequest();
		ConnectionRequest r3 = new ConnectionRequest();
		
		Assertions.assertEquals(0, buffer.size());
		buffer.addFirst(r1);
		Assertions.assertEquals(1, buffer.size());
		buffer.addFirst(r2);
		Assertions.assertEquals(2, buffer.size());
		buffer.addFirst(r3);
		Assertions.assertEquals(3, buffer.size());
		
		Iterator<ConnectionRequest> iterator = buffer.iterator();
		Assertions.assertEquals(r3, iterator.next());
		Assertions.assertEquals(r2, iterator.next());
		Assertions.assertEquals(r1, iterator.next());
		Assertions.assertFalse(iterator.hasNext());
		
		buffer.remove(r3);
		Assertions.assertEquals(2, buffer.size());
		buffer.remove(r2);
		Assertions.assertEquals(1, buffer.size());
		buffer.remove(r1);
		Assertions.assertEquals(0, buffer.size());
		
		buffer.addFirst(r1);
		buffer.addFirst(r2);
		buffer.addFirst(r3);

		buffer.remove(r2);
		Assertions.assertEquals(2, buffer.size());
		
		iterator = buffer.iterator();
		Assertions.assertEquals(r3, iterator.next());
		Assertions.assertEquals(r1, iterator.next());
		Assertions.assertFalse(iterator.hasNext());
	}


	private class ConnectionRequest {
		
		ConnectionRequest next;
		ConnectionRequest previous;
		
		boolean queued;	
	}
	
	private class ConnectionRequestBuffer implements Iterable<ConnectionRequest> {

		private final ConnectionRequest head;
		private int size;
		
		private ConnectionRequestBuffer() {
			this.head = new ConnectionRequest();
			this.head.previous = this.head.next = this.head;
		}
		
		public ConnectionRequest poll() {
			if(this.head == this.head.next) {
				return null;
			}
			ConnectionRequest request = this.head.next;
			this.remove(request);
			return request;
		}
		
		public void addFirst(ConnectionRequest request) {
			if(request == null || request.queued) {
				throw new IllegalStateException();
			}
			request.queued = true;
			request.previous = this.head;
			request.next = this.head.next;
			this.head.next = this.head.next.previous = request;
			this.size++;
		}
		
		public void addLast(ConnectionRequest request) {
			if(request == null || request.queued) {
				throw new IllegalStateException();
			}
			request.queued = true;
			request.next = this.head;
			request.previous = this.head.previous;
			this.head.previous = this.head.previous.next = request;
			this.size++;
		}
		
		public boolean remove(ConnectionRequest request) {
			if(request == null || !request.queued) {
				return false;
			}
			
			request.next.previous = request.previous;
			request.previous.next = request.next;
			request.next = request.previous = null;
			request.queued = false;
			this.size--;
			return true;
		}
		
		public int size() {
			return this.size;
		}
		
		public boolean isEmpty() {
			return this.size > 0;
		}
		
		@Override
		public Iterator<ConnectionRequest> iterator() {
			return new Iterator<ConnectionRequest>() {
				
				ConnectionRequest cursor = ConnectionRequestBuffer.this.head;
				
				@Override
				public boolean hasNext() {
					return this.cursor.next != ConnectionRequestBuffer.this.head;
				}

				@Override
				public ConnectionRequest next() {
					this.cursor = this.cursor.next;
					if(this.cursor == ConnectionRequestBuffer.this.head) {
						throw new NoSuchElementException();
					}
					return this.cursor;
				}
			};
		}
	}
}
