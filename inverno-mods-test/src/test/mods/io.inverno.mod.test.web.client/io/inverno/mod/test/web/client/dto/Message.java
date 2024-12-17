/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.test.web.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Message implements Comparable<Message> {

	private final String message;

	public Message(@JsonProperty("message") String message) {
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

	@Override
	public int compareTo(Message o) {
		return this.message.compareTo(o.message);
	}

	@Override
	public String toString() {
		return "Message{" +
			"message='" + message + '\'' +
			'}';
	}
}