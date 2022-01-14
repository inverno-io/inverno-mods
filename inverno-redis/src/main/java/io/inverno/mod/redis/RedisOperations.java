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
package io.inverno.mod.redis;

import io.inverno.mod.redis.operations.RedisGeoReactiveOperations;
import io.inverno.mod.redis.operations.RedisHLLReactiveOperations;
import io.inverno.mod.redis.operations.RedisHashReactiveOperations;
import io.inverno.mod.redis.operations.RedisKeyReactiveOperations;
import io.inverno.mod.redis.operations.RedisListReactiveOperations;
import io.inverno.mod.redis.operations.RedisScriptingReactiveOperations;
import io.inverno.mod.redis.operations.RedisSetReactiveOperations;
import io.inverno.mod.redis.operations.RedisSortedSetReactiveOperations;
import io.inverno.mod.redis.operations.RedisStreamReactiveOperations;
import io.inverno.mod.redis.operations.RedisStringReactiveOperations;

/**
 * <p>
 * Redis reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisOperations<A, B> extends
        RedisGeoReactiveOperations<A, B>, RedisHashReactiveOperations<A, B>, RedisHLLReactiveOperations<A, B>,
        RedisKeyReactiveOperations<A, B>, RedisListReactiveOperations<A, B>, RedisScriptingReactiveOperations<A, B>,
        RedisSetReactiveOperations<A, B>, RedisSortedSetReactiveOperations<A, B>,
        RedisStreamReactiveOperations<A, B>, RedisStringReactiveOperations<A, B> {

}
