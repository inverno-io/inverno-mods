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

import io.inverno.mod.redis.operations.RedisSetReactiveOperations;
import io.lettuce.core.ValueScanCursor;
import java.util.List;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 */
public class SetScanResultImpl<A> extends AbstractScanResultImpl<ValueScanCursor<A>> implements RedisSetReactiveOperations.SetScanResult<A> {

	/**
	 * 
	 * @param valueScanCursor 
	 */
	public SetScanResultImpl(ValueScanCursor<A> valueScanCursor) {
		super(valueScanCursor);
	}

	@Override
	public List<A> getMembers() {
		return this.scanCursor.getValues();
	}
}
