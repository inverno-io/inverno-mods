[inverno-javadoc]: https://inverno.io/docs/release/api/index.html

[vertx-sql-client]: https://github.com/eclipse-vertx/vertx-sql-client
[vertx-database-doc]: https://vertx.io/docs/#databases

# Vert.x SQL Client implementation

The Inverno Vert.x SQL client module is an implementation of the SQL client API on top of the [Vert.x Reactive SQL client][vertx-sql-client].

It provides multiple `SqlClient` implementations that wrap Vert.x SQL pooled client, pool or connection and exposes a `SqlCLient` bean created from the module's configuration and backed by a Vert.x pool. It can be used to execute SQL statements in an application.

In order to use the Inverno *Vert.x SQL client* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.sql.vertx;
    ...
}
```

And also declare this dependency as well as a dependency to the Vert.x implementation corresponding to the RDBMS we are targeting in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-sql-vertx</artifactId>
        </dependency>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-pg-client</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-sql-vertx:${VERSION_INVERNO_MODS}'
compile 'io.vertx:vertx-pg-client:4.1.2'
...
```

## Configuration

The `VertxSqlClientConfiguration` is used to create and configure the SQL client bean exposed by the module.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties.

## Sql Client bean

The module exposes a `SqlClient` bean which is backed by a Vert.x pool. It is created using the configuration and especially the `db_uri` property whose scheme indicates the RDBMS system and therefore the Vert.x pool implementation to use.

For instance, the following configuration can be used to connect to a PostgreSQL database:

```plaintext
db_uri="postgres://user:password@localhost:5432/sample_db"
```

> If you want to connect to a particular RDBMS, don't forget to add a dependency to the corresponding Vert.x SQL client implementation. Vert.x currently supports DB2, MSSQL, MySQL, PostgreSQL and Oracle.

The connection pool can be configured as well:

```plaintext
pool_maxSize=20
```

Please refer to the [Vert.x database documentation][vertx-database-doc] to get the options supported for each RDBMS implementations.

The Vert.x SQL client requires a `Vertx` instance which is provided in the Inverno application reactor when using a `VertxReactor`, otherwise a dedicated `Vertx` instance is created. In any case, this instance can be overridden by providing a custom one to the module.

## Vert.x wrappers

Depending on our needs, we can also choose to create a custom `SqlClient` using one the Vert.x SQL client wrappers provided by the module.

The `ConnectionSqlClient` wraps a Vert.x SQL connection, you can use to transform a single connection obtained via a Vert.x connection factory into a reactive `SqlClient`.

The `PooledClientSqlClient` wraps a Vert.x pooled SQL client that supports pipelining of queries on a single configuration for optimized performances. This implementation doesn't support transactions.

```java
SqlClient client = new PooledClientSqlClient(PgPool.client(...));
```

Finally, the `PoolSqlClient` wraps a Vert.x SQL pool. This is a common implementation supporting transactions and result streaming, it is used to create the module's SQL client bean.

```java
SqlClient client = new PoolSqlClient(PgPool.pool(...));
```
