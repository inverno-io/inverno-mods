[redis]: https://redis.io/
[redis_commands]: https://redis.io/commands
[redis_transactions]: https://redis.io/topics/transactions

# Redis Client

The Inverno Redis client module specifies a reactive API for executing commands on a [Redis][redis] data store.

This module only exposes the API and a proper implementation module must be considered to obtain `RedisClient` instances.

In order to use the Inverno *Redis client* module, we need to declare a dependency to the API and at least one implementation in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.redis; // this is actually optional since implementations should already define a transitive dependency
    requires io.inverno.mod.redis.lettuce; // Lettuce implementation
    ...
}
```

And also declare these dependencies in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-redis-lettuce</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-redis:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-redis-vertx:${VERSION_INVERNO_MODS}'
...
```

## Redis Client API

The Redis client API defines the `RedisClient` and `RedisTransactionalClient` interfaces which provide reactive methods to create and execute [Redis commands][redis_commands]. 

The `RedisTransactionalClient` interface extends the `RedisClient` interface with Redis transactional support (ie. `MULTI`, `DISCARD`, `EXEC`...).

### Redis Operations

The API exposes mutiple `*Operations` interfaces which are all extended by the `RedisCLient` and which allows to fluently send commands to a Redis data store.

There are currently ten such interfaces that exposes the >200 commands supported in Redis:

- `RedisHashReactiveOperations`
- `RedisKeyReactiveOperations`
- `RedisScriptingReactiveOperations`
- `RedisSortedSetReactiveOperations`
- `RedisStringReactiveOperations`
- `RedisGeoReactiveOperations`
- `RedisHLLReactiveOperations`
- `RedisListReactiveOperations`
- `RedisSetReactiveOperations`
- `RedisStreamReactiveOperations`

The API is pretty straighfoward and provides guidance on how to create and send commands to the Redis data store. For instance a simple string value can be queried as follows:

```java
RedisClient<String, String> client = ...

Mono<String> getSomeKey = client.get("someKey");

getSomeKey.subscribe(...); // The command is sent on subscribe following reactive principles

```

Complex commands are created using builders, for instance command `ZRANGE mySortedSet 0 +inf BYSCORE REV LIMIT 0 1 WITHSCORES` can be created and executed as follows:

```java
RedisClient<String, String> client = ...

Flux<SortedSetScoredMember<String>> zrangeWithScores = client.zrangeWithScores()
    .reverse()
    .byScore()
    .limit(0, 1)
    .build("mySortedSet", Bound.inclusive(0), Bound.unbounded());

zrangeWithScores.subscribe(...); // The command is sent on subscribe following reactive principles

```

### Keys and Values codecs

The `RedisClient` supports encoding and decoding of Redis keys and values, as a result the `RedisClient` client is a generic type which allows to specified the types of key and values.

The actual encoding/decoding logic is implementation specific.

### Connections

Commands can be executed directly on the client instance in which case a connection is obtained each time an operation method is invoked on the client and released once the resulting publisher terminates. This might not be an issue when a single command is issued or when using an implementation based on a single connection, However if there's a need to exectute multiuple commands or when using an implementation backed by a connection pool, it is often better to execute multiple SQL statements on a single connection released once the resulting publisher terminates (the connection can be either closed or returned to the pool).

Multiple commands can be executed on a single connection as follows:

```java
RedisClient<String, String> client = ...

Flux<String> results = Flux.from(client.connection(operations -> 
    Flux.concat(
        operations.get("key1"),
        operations.get("key2"),
        operations.get("key3")
    )
));

results.subscribe(...); // Commands are sent sent on subscribe following reactive principles

```

### Batch

Commands can also be executed in batch, delaying the network flush so that multiple commands are sent to the server in one shot. This can have a significant positive impact on performances as the client doesn't have to wait for a response to send the next command.

A batch of commands can be executed as follows:

```java
RedisClient<String, String> client = ...

Flux<String> results = Flux.from(client.batch(operations -> 
    Flux.just(
        operations.get("key1"),
        operations.get("key2"),
        operations.get("key3")
    )
));

results.subscribe(...); // Commands are sent on subscribe following reactive principles

```

### Transactions

Redis supports transactions through `MULTI`, `EXEC` and `DISCARD` commands which is a bit different than traditional begin/commit/rollback we can find in RDBMS. Please have a look at [Redis trasnactions documentation][redis_transactions] to have a proper understanding on how transactions work in Redis.

Commands can be executed within a transaction using a `RedisTransactionalClient`, a transaction can be managed implicitly or explicitly by obtaining a `RedisTransactionalOperations` and explicitly invoke `exec()` or `rollback()`.

In the following example, two `SET` commands are executed within a transaction, when subscribing to the returned `Mono<RedisTransactionResult>`, the two set publishers are subscribed on and the transaction is executed implicitly and a `RedisTransactionResult` is eventually emitted and holds transaction results:

```java
RedisClient<String, String> client = ...

Mono<RedisTransactionResult> transaction = client
    .multi(operations -> 
        Flux.just(
            operations.set("key_1", "value_1"), 
            operations.set("key_2", "value_2")
        )
    );

RedisTransactionResult result = transaction.block(); // Commands are sent on subscribe following reactive principles

if(!result.wasDiscarded()) {
    Assertions.assertEquals("OK", result.get(0));
    Assertions.assertEquals("OK", result.get(1));
}
else {
    // Report error
}

```

If any error is raised during the processing, typically when the client subscribes to the returned command publishers, the transaction is discarded.

The same transaction can be explicitly managed as follows:

```java
RedisClient<String, String> client = ...

Mono<RedisTransactionResult> transaction = client
    .multi()
    .flatMap(operations -> {
        operations.set("key_1", "value_1").subscribe();
        operations.set("key_2", "value_2").subscribe();

        return operations.exec();
    });

RedisTransactionResult result = transaction.block(); // Commands are sent on subscribe following reactive principles

```

In above example, it is important to subscribe to command publishers explicitly otherwise they won't be part of the transaction.

Redis uses optimistic locking using check-and-set through the `WATCH` command which is used to indicate which keys should be monitored for changes during a transaction. When creating a transaction, it is possible to specified watches that would discard the transaction if any change is detected.

For instance, the following transaction will be discarded if the value of key `key_3` is changed after the transaction begin:

```java
RedisClient<String, String> client = ...

Mono<RedisTransactionResult> transaction = client
    .multi("key_3") // watch 'key_3'
    // let's change the value of 'key_3' using another connection to get the transaction discarded
    .doOnNext(ign -> client.set("key_3", "value_3").block()) 
    .flatMap(operations -> {
        operations.set("key_3", "value_3").subscribe();
        
        return operations.exec();
    });

RedisTransactionResult result = transaction.block();

// Transaction was discarded since 'key_3' changed before the transaction after the start of the transaction and before it ended
Assertions.assertTrue(result.wasDiscarded());
```