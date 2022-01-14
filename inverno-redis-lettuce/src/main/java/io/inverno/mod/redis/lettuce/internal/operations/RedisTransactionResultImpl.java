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
package io.inverno.mod.redis.lettuce.internal.operations;

import io.inverno.mod.redis.RedisTransactionResult;
import io.lettuce.core.TransactionResult;
import java.util.stream.Stream;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class RedisTransactionResultImpl implements RedisTransactionResult {

	private final TransactionResult transactionResult;
	
	/**
	 * 
	 * @param transactionResult 
	 */
	public RedisTransactionResultImpl(TransactionResult transactionResult) {
		this.transactionResult = transactionResult;
	}

	@Override
	public boolean wasDiscarded() {
		return this.transactionResult.wasDiscarded();
	}

	@Override
	public int size() {
		return this.transactionResult.size();
	}

	@Override
	public boolean isEmpty() {
		return this.transactionResult.isEmpty();
	}

	@Override
	public <T> T get(int index) {
		return this.transactionResult.get(index);
	}

	@Override
	public Stream<Object> stream() {
		return this.transactionResult.stream();
	}
}
