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

import io.inverno.mod.redis.operations.RedisHashReactiveOperations;
import io.lettuce.core.MapScanCursor;
import java.util.Map;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 * @param <B>
 */
public class HashScanResultImpl<A, B> extends AbstractScanResultImpl<MapScanCursor<A, B>> implements RedisHashReactiveOperations.HashScanResult<A, B> {

	/**
	 * 
	 * @param mapScanCursor 
	 */
	public HashScanResultImpl(MapScanCursor<A, B> mapScanCursor) {
		super(mapScanCursor);
	}
	
	@Override
	public Map<A, B> getEntries() {
		return this.scanCursor.getMap();
	}
}
