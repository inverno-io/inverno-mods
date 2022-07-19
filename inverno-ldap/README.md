[inverno-javadoc]: https://inverno.io/docs/release/api/index.html
[ldap]: https://en.wikipedia.org/wiki/Lightweight_Directory_Access_Protocol

# LDAP

The Inverno LDAP client module specifies a basic reactive API for interacting with an LDAP or Active Directory server.

It also provides a default JDK based implementation of the `LDAPClient` exposed in the module.

This module requires an `ExecutorService` used to execute JDK blocking operations in separate thread. The *boot* module provides a global worker pool which is ideal in such situations, so in order to use the Inverno *ldap* module, we should declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app {
    ...
    requires io.inverno.mod.boot;
    requires io.inverno.mod.ldap;
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
            <artifactId>inverno-boot</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-ldap</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-ldap:${VERSION_INVERNO_MODS}'
...
```

## Configuration

The `LDAPClientConfiguration` is used to create and configure the JDK based LDAP client bean exposed by the module.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties.

## LDAP Client API

The LDAP client API defines the `LDAPClient` interface which provides reactive methods to bind, search and get LDAP entries in an LDAP server.

### LDAP Operations

The API exposes `LDAPOperations` interface which is extended by the `LDAPCLient` and which allows to fluently send commands to the LDAP server.

#### Bind

The `bind()` method exposed on the `LDAPClient` allows to authenticate a user and obtain an `LDAPOperations` instance bound to that user.

The following is a complete example where user `jsmith` is authenticated and multiple operations are executed on the bound `LDAPOperations` instance.

```java
String uid = "jsmith";
String userDN = "cn=jsmith,ou=users,dc=inverno,dc=io";
User user = Mono.from(client.bind(
        "cn={0},ou=users,dc=inverno,dc=io",
        new Object[] {uid},
        "password", 
        ops -> ops.search(userDN, new String[] {"uid"}, "(&(objectClass=inetOrgPerson)(uid={0}))", uid)
            .flatMap(userEntry -> ops.search("dc=inverno,dc=io", new String[]{ "cn" }, "(&(objectClass=groupOfNames)(member={0}))", userEntry.getDN())
                .map(groupEntry -> groupEntry.getAttribute("cn").map(LDAPAttribute::asString).get())
                .collectList()
                .map(groups -> new User(userEntry.getDN(), userEntry.getAttribute("uid").map(LDAPAttribute::asString).get(), groups)))
            )
    )
    .block();
```

As stated before, the `LDAPClient` extends `LDAPOperations` and any operations can then be directly invoked on the client instance. Whether an LDAP client instance is authenticated or not on the LDAP server is implementation specific.

#### Get a single entry

A single entry identified by a specific `DN` can be retrieved as follows:

```java
LDAPOperations operations = ...

LDAPEntry jsmithEntry = operations.get("cn=jsmith,ou=users,dc=inverno,dc=io").block();
```

The `DN` can also be specified as a templatized expression using `{i}` notation and a list or arguments:

```java
LDAPOperations operations = ...

LDAPEntry jsmithEntry = operations.get("cn={0},ou=users,dc=inverno,dc=io", "jsmith").block();
```

It is also possible to specify which attributes must be retrieved:

```java
LDAPOperations operations = ...

LDAPEntry jsmithEntry = operations.get("cn={0},ou=users,dc=inverno,dc=io", new String[] {"cn", "uid", "mail", "userPassword"}, "jsmith").block();
```

LDAP Attributes are exposed on the resulting `LDAPEntry`, raw attribute values can be obtained as follows:

```java
// Gets the value of attribute 'mail' or null
// if multiple 'mail' attributes are defined, one of them is returned in a non-deterministic way
Object mail = jsmithEntry.get("mail").orElse(null);

// Gets all values for attribute 'mail' or an empty list
List<Object> allMail = jsmithEntry.getAll("mail");

// Get all attributes 
List<Map.Entry<String, Object>> all = jsmithEntry.getAll();
```

It is also possible to get attributes as convertible `LDAPAttribute` as follows:

```java
// Gets the value of attribute 'birthDate' as a local date or null
// if multiple 'birthDate' attributes are defined, one of them is returned in a non-deterministic way
LocalDate birthDate = jsmithEntry.getAttribute("birthDate").map(LDAPAttribute::asLocalDate).orElse(null);

// Gets all values for attribute 'address' as strings or an empty list
List<String> addresses = jsmithEntry.getAllAttribute("address").stream().map(LDAPAttribute::asString).collect(Collectors.toList());

// Get all attributes
List<LDAPAttribute> allAttribute = jsmithEntry.getAllAttribute();
```

#### Search

We can search for entries using a base context and a filter expression. In the following example we search for `inetOrgPerson` class entries with `CN` and `UID` attributes in the `users` organizational unit:

```java
List<LDAPEntry> result = client.search("ou=users,dc=inverno,dc=io", new String[] {"cn", "uid"}, "(objectClass=inetOrgPerson)")
    .collectList()
    .block();
```

The filter can be templatized using the `{i}` notation. In the following we search for the groups user `jsmith` belongs to:

```java
List<LDAPEntry> result = client.search("dc=inverno,dc=io", new String[]{ "cn" }, "(&(objectClass=groupOfNames)(member={0}))", "cn=jsmith,ou=users,dc=inverno,dc=io")
    .collectList()
    .block();
```

Complex queries can be created using a `SearchBuilder` which allows specifying a search scope among other things:

```java
List<LDAPEntry> result = client.search()
    .scope(LDAPOperations.SearchScope.WHOLE_SUBTREE)
    .build("ou=users,dc=inverno,dc=io", new String[] {"cn", "uid"}, "(objectClass=inetOrgPerson)")
    .collectList()
    .block();
```

## LDAP Client bean

The module exposes an `LDAPClient` bean implemented using JDK `DirContext` to access the LDAP server. The client is created using the module's configuration which specifies:

- the LDAP server URI (e.g. `ldap://remoteLDAP:1389`)
- the authentication choice (`simple` by default)
- the referral policy (follow referrals by default)
- the admin user `DN` which shall be used by default to connect to the server
- the admin user credentials, typically a password

If no admin user `DN` and credentials are specified the client connects to the server anonymously unless operations are executed inside a `bind()` invocation.

For instance, the following configuration can be used to connect to a remote LDAP server using an admin `DN`:

```plaintext
uri="ldap://remoteLDAP:1389"
admin_dn="cn=admin,ou=users,dc=inverno,dc=io"
admin_credentials="admin_password"
```

> Since the JDK directory service interface uses blocking operations, the client also requires an `ExecutorService` to make it reactive by executing blocking operations in separate threads and make sure no blocking operation is ever run in a reactor I/O thread. The *boot* module typically provides a global worker pool that must be used in such situations but it is also possible to use a specific `ExecutorService` as well when this makes sense.