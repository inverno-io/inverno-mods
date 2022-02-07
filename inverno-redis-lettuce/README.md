[lettuce]: https://lettuce.io

# Lettuce Redis Client implementation

The Inverno Lettuce Redis client module is an implementation of the Redis client API on top of the [Lettuce client][lettuce].

It provides `PoolRedisClient` and `PoolRedisClusterClient` implementations that wrap a Lettuce `AsyncPool` used to acquire `StatefulRedisConnection` and `StatefulRedisClusterConnection` respectively. The `PoolRedisClusterClient` doesn't implement `RedisTransactionalClient` since transactions are not supported by Redis in a clustered environment.

The module also exposes a `RedisClient<String, String>` bean created from the module's configuration and backed by a Lettuce `BoundedAsyncPool<StatefulRedisConnection<String, String>>` instance.

SQL pooled client, pool or connection and exposes a `RedisCLient` bean created from the module's configuration and backed by a Vert.x pool. It can be used to execute SQL statements in an application.

In order to use the Inverno *Lettuce Redis client* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.redis.lettuce;
    ...
}
```

And also declare this dependencies in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
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
compile 'io.inverno.mod:inverno-redis-lettuce:${VERSION_INVERNO_MODS}'
...
```

## Configuration

The `LettuceRedisClientConfiguration` is used to create and configure the Redis client bean exposed by the module.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties.

## Redis Client bean

The module exposes a `RedisClient<String, String>` bean which is backed by a Lettuce `BoundedAsyncPool<StatefulRedisConnection<String, String>>` instance. It is created using the configuration and especially the `uri` property which specified the Redis server to connect to which is `redis://localhost:6379` by default

For instance, the following configuration can be used to connect to a remote Redis server:

```plaintext
uri="redis://remoteRedis"
```

The connection pool can be configured as well: 

```plaintext
pool_max_active=8
pool_min_idle=0
pool_max_idle=8
```

Secured connection using TLS and/or authentication can also be configured as follows:

```plaintext
tls=true
username=user
password=password
```

By default, this Redis client relies on a dedicated event loop group but it can also rely on Inverno's reactor when a `Reactor` instance is available. This is transparent when assembling an application with the *boot* module which exposes Inverno's reactor.

## Lettuce wrappers

Depending on our needs, we can also choose to create a custom `RedisClient` using one the Lettuce Redis client wrappers provided by the module.

The `PoolRedisClient` implementation wraps a Lettuce `AsyncPool<StatefulRedisConnection<K, V>>`, it is then possible to create a `RedisClient` client instance using specific key/value codecs:

```java
BoundedAsyncPool<StatefulRedisConnection<byte[], byte[]>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
        () -> this.client.connectAsync(ByteArrayCodec.INSTANCE, RedisURI.create("redis://localhost"), 
        BoundedPoolConfig.create()
    );
	RedisClient<byte[], byte[]> byteArrayClient = new PoolRedisClient<>(pool, byte[].class, byte[].class);
```

The `PoolRedisClusterClient` implementation should be used to connect to a Redis cluster, it wraps a Lettuce `AsyncPool<StatefulRedisClusterConnection<K, V>>`