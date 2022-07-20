# SQL Client

The Inverno SQL client module specifies a reactive API for executing SQL statement on a RDBMS.

This module only exposes the API and a proper implementation module must be considered to obtain `SqlClient` instances.

In order to use the Inverno *SQL client* module, we need to declare a dependency to the API and at least one implementation in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.sql; // this is actually optional since implementations should already define a transitive dependency
    requires io.inverno.mod.sql.vertx; // Vert.x implementation
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
            <artifactId>inverno-sql</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-sql-vertx</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-sql:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-sql-vertx:${VERSION_INVERNO_MODS}'
...
```

## SQL client API

The Sql client API defines the `SqlClient` interface which provides reactive methods to execute SQL statements on a RDBMS.

### Query and update

The `SqlClient` extends the `SqlOperations` interface which defines methods for common RDBMS operations such as query or update in addition to the more general statements and prepared statements.

We can query a database as follows:

```java
SqlClient client = ...

Flux<Person> persons = Flux.from(
    client.query("SELECT * FROM person")
)
.map(row -> new Person(row.getString("firstname"), row.getString("name"), row.getLocalDate("birthdate"))); // Map the resulting rows

persons.subscribe(...); // The query is executed on subscribe following reactive principles
```

Prepared queries are also supported:

```java
Publisher<Row> results = client.query("SELECT * FROM person WHERE name = $1", "John");
```

A row mapping function can be specified directly in the query as well

```java
Publisher<Person> results = client.query(
    "SELECT * FROM person WHERE name = $1", 
    row -> new Person(row.getString("firstname"), row.getString("name"), row.getLocalDate("birthdate")), 
    "Smith"
);
```

A single result can also be queried as follows:

```java
Mono<Person> person = client.queryForObject( // only consider the first row in the results
    "SELECT * FROM person WHERE name = $1", 
    row -> new Person(row.getString("firstname"), row.getString("name"), row.getLocalDate("birthdate")),
    "Smith"
);
```

> The two previous examples are actually optimizations of the first one which enable implementations to optimize the query, resulting in faster execution.

The database can be updated as follows:

```java
client.update(
    "UPDATE person SET birthdate = $1 WHERE id = $2", 
    LocalDate.of(1970, 1, 1), 123
);
```

It can also be updated in a batch as follows:

```java
client.batchUpdate(
    "UPDATE person SET birthdate = $1 WHERE id = $2", 
    List.of(
        new Object[]{ LocalDate.of(1970, 1, 1), 123 },
        new Object[]{ LocalDate.of(1980, 1, 1), 456 },
        new Object[]{ LocalDate.of(1990, 1, 1), 789 }
    )
);
```

> Note that all these operations use prepared statements which protect against SQL injection attacks.

### Statements

The `SqlClient` also defines methods to create more general statements and prepared statements.

A static statement can be created and executed as follows:

```java
SqlClient client = ...

Publisher<SqlResult> results = client.statement("SELECT * FROM person").execute();

// The statement is executed on subscribe following reactive principles
results.subscribe(...);
```

The execution of a statement returns `SqlResult` for each SQL operations in the statement in a publisher.

The `SqlResult` exposes row metadata and depending on the operation type either the number of rows affected by the operation (`UPDATE` or `DELETE`) or the resulting rows (`SELECT`).

Following preceding example:

```java
Flux<Person> persons = Flux.from(client.statement("SELECT * FROM person").execute())
    .single() // Make sure we have only one result
    .flatMapMany(SqlResult::rows)
    .map(row -> new Person(row.getString("firstname"), row.getString("name"), row.getLocalDate("birthdate")))

persons.subscribe(...);
```

Queries can also be fluently appended to a statement as follows:

```java
Publisher<SqlResult> results = client
    .statement("SELECT * FROM person")
    .and("SELECT * FROM city")
    .and("SELECT * FROM country")
    .execute();
```

Unlike prepared statements, static statements are not pre-compiled and do not protect against SQL injection attacks which is why prepared statements should be preferred when there is a need for performance, dynamic or user provided queries.

A prepared statement can be created and executed as follows:

```java
SqlClient client = ...

Publisher<SqlResult> results = client.preparedStatement("SELECT * FROM person WHERE name = $1")
    .bind("Smith") // bind the query argument
    .execute();

// The statement is executed on subscribe following reactive principles
results.subscribe(...);
```

As for a static statement, a prepared statement returns `SqlResult` for each SQL operations in the statement, however it is not possible to specify multiple operation in a prepared statement. But it is possible to transform it into a batch which will result in multiple operations and therefore multiple `SqlResult`.

In order to create a batch statement, we must bind multiple query arguments as follows:

```java
Publisher<SqlResult> results = client.preparedStatement("SELECT * FROM person WHERE name = $1")
    .bind("Smith")         // first query
    .and().bind("Cooper")  // second query
    .and().bind("Johnson") // third query
    .execute();

// Returns 3 since we have created a batch statement with three queries
long resultCount = Flux.from(results).count().block();
```

### Transactions

The API provides two ways to execute statement in a transaction which can be managed explicitly or implicitly.

We can choose to manage transaction explicitly by obtaining a `TransactionalSqlOperations` which exposes `commit()` and `rollback()` methods that we must invoke explicitly to close the transaction:

In the following example we perform a common `SELECT/UPDATE` operation within a transaction:

```java
SqlClient client = ...

final float debit = 42.00f;
final int accountId = 1;

Mono<Integer> affectedRows = Mono.usingWhen(
    client.transaction(), 
    tops -> tops
        .queryForObject("SELECT balance FROM account WHERE id = $1", row -> row.getFloat(0), accountId)
        .flatMap(balance -> ops
            .update("UPDATE account SET balance = $1 WHERE id = $2", balance - debit, accountId)
            .doOnNext(rowCount -> {
                if(balance - debit < 0) {
                    throw new IllegalStateException();
                }
            })
        )
    ,
    tops -> {                                // Complete
        // extra processing before commit
        // ...
        
        return tops.commit();
    },
    (tops, ex) -> {                          // Error
        // extra processing before roll back
        // ...
        
        return tops.rollback();
    }, 
    tops -> {                                // Cancel
        // extra processing before commit
        // ...
        
        return tops.rollback();
    }
);

// On subscribe, a transaction is created, the closure method is invoked and the transaction is explicitly committed or rolled back when the publisher terminates.
affectedRows.subscribe(...);
```

The following example does the same but with implicit transaction management:

```java
SqlClient client = ...

final float debit = 42.00f;
final int accountId = 1;

Publisher<Integer> affectedRows = client.transaction(ops -> ops
    .queryForObject("SELECT balance FROM account WHERE id = $1", row -> row.getFloat(0), accountId)
    .flatMap(balance -> ops
        .update("UPDATE account SET balance = $1 WHERE id = $2", balance - debit, accountId)
        .doOnNext(rowCount -> {
            if(balance - debit < 0) {
                throw new IllegalStateException();
            }
        })
    )
);

// Same as before but the transaction is implicitly committed or rolled back
affectedRows.subscribe(...);
```

> Note that transactions might not be supported by all implementations, for instance the Vert.x pooled client implementation does not support transactions and an `UnsupportedOperationException` will be thrown if you try to create a transaction.

### Connections

Some `SqlClient` implementations backed by a connection pool for instance can be used to execute multiple SQL statements on a single connection released once the resulting publisher terminates (either closed or returned to the pool).

For instance we can execute multiple statements on a single connection as follows:

```java
SqlClient client = ...

final int postId = 1;

client.connection(ops -> ops
    .queryForObject("SELECT likes FROM posts WHERE id = $1", row -> row.getInteger(0), postId)
    .flatMap(likes -> ops.update("UPDATE posts SET likes = $1 WHERE id = $2", likes + 1, postId))
);
```
