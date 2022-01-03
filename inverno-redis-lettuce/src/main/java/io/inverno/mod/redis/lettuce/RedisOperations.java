/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.lettuce;

import io.lettuce.core.api.reactive.RedisGeoReactiveCommands;
import io.lettuce.core.api.reactive.RedisHLLReactiveCommands;
import io.lettuce.core.api.reactive.RedisHashReactiveCommands;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import io.lettuce.core.api.reactive.RedisListReactiveCommands;
import io.lettuce.core.api.reactive.RedisScriptingReactiveCommands;
import io.lettuce.core.api.reactive.RedisSetReactiveCommands;
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands;
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;

/**
 *
 * @author jkuhn
 */
public interface RedisOperations<A, B> extends
        RedisGeoReactiveCommands<A, B>, RedisHashReactiveCommands<A, B>, RedisHLLReactiveCommands<A, B>,
        RedisKeyReactiveCommands<A, B>, RedisListReactiveCommands<A, B>, RedisScriptingReactiveCommands<A, B>,
        RedisSetReactiveCommands<A, B>, RedisSortedSetReactiveCommands<A, B>,
        RedisStreamReactiveCommands<A, B>, RedisStringReactiveCommands<A, B> {

}
