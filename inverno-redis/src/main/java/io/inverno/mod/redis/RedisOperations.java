/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
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
 *
 * @author jkuhn
 * @param <A>
 * @param <B>
 */
public interface RedisOperations<A, B> extends
        RedisGeoReactiveOperations<A, B>, RedisHashReactiveOperations<A, B>, RedisHLLReactiveOperations<A, B>,
        RedisKeyReactiveOperations<A, B>, RedisListReactiveOperations<A, B>, RedisScriptingReactiveOperations<A, B>,
        RedisSetReactiveOperations<A, B>, RedisSortedSetReactiveOperations<A, B>,
        RedisStreamReactiveOperations<A, B>, RedisStringReactiveOperations<A, B> {

}
