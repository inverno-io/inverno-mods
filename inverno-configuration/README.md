[inverno-javadoc]: https://inverno.io/docs/release/api/index.html
[cprops-grammar]: https://github.com/inverno-io/inverno-mods/tree/master/inverno-configuration/src/main/javacc/configuration_properties.jj

[javacc]: https://javacc.github.io/javacc/
[redis]: https://redis.io/

# Configuration

The Inverno *configuration* module defines a unified configuration API for building agile highly configurable applications.

Configuration is one of the most important aspect of an application and sadly one of the most neglected. There are very few decent configuration frameworks and most of the time they relate to one part of the issue. It is important to approach configuration by considering it as a whole and not as something that can be solved by a property file here and a database there. Besides, it must be the first issue to tackle during the design phase as it will impact all aspects of the application. For instance, we can imagine an application where configuration is defined in simple property files, a complete configuration would probably be needed for each environment where the application is deployed, maintenance would be probably problematic even more when we know that configuration properties can be added, modified or removed over time.

In its most basic form, a configuration is not more than a set of properties associating a value to a key. It would be naive to think that this is enough to build an agile and customizable application, but in the end, a property should still be considered as the basic building block for configurations.

Now, the first thing to notice is that any part of an application can potentially be configurable, from a server IP address to a color of a button in a user interface, there are multiple forms of configuration with different expectations that must coexist in an application. For instance, some parts of the configuration are purely static and do not change during the operation of an application, this is the case of a bootstrap configuration which mostly relates to the operating environment (eg. a server port). Some other parts, on the other hand, are more dynamic and can change during the operation of an application, this is the case of tenant specific configuration or even user preferences. 

Following this, we can see that a configuration greatly depends on the context in which it is loaded. The definition of a configuration, which is basically a list of property names, is dictated by the application, so when the application is running, this definition should be fixed but the context is not. For instance, the bootstrap configuration is different from one operating environment to another, user preferences are not the same from one user to another...

We can summarize this as follows:

- a configuration is a set of configuration properties.
- the configuration of an application is actually composed of multiple configurations with their own specificities.
- the definition of a configuration is bound to the application as a result the only way to change it is to change the application.
- a configuration depends on a particular context which must be considered when setting or getting configuration properties. 

The configuration API has been created to address previous points, giving a maximum flexibility to precisely design how an application should be configured.

In order to use the Inverno *configuration* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.configuration;
    ...
}
```

And also declare that dependency in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-configuration</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-configuration:${VERSION_INVERNO_MODS}'
...
```

## Configuration source

A configuration source can be any data store that holds configuration data, the API abstracts configuration data sources to provide a unified access to configuration data through the `ConfigurationSource` interface. Specific implementations should be considered depending on the type of configuration: a bootstrap configuration is most likely to be static and stored in configuration files or environment variables whereas a tenant specific configuration is most likely to be stored in a distributed data store. However this is not a universal rule, depending on the needs we can very well consider any kind of configuration source for any kind of configuration. The configuration source abstracts these concerns from the the rest of the application.

The `ConfigurationSource` is the main entry point for accessing configuration properties, it shall be used every time there's a need to retrieve configuration properties. It defines only one method for creating a `ConfigurationQuery` instance eventually executed in order to retrieve one or more configuration properties.

For instance, property `server.uri` can be retrieved as follows:

```java
ConfigurationSource<?, ?, ?> source = ...

source.get("server.url")                        // 1
    .execute()                                  // 2
    .single()                                   // 3
    .map(queryResult -> queryResult
        .getResult()                            // 4
        .flatMap(property -> property.asURI())  // 5
        .orElse(URI.create("http://localhost")) // 6
    )
    .subscribe(serverURI -> ...);               // 7
```

In the preceding example:

1. creates a configuration query to retrieve the `server.url` property
2. executes the query, the API is reactive so nothing will happen until a subscription is actually made on the resulting `Flux` of `ConfigurationQueryResult`
3. transforms the `Flux` to a `Mono` since we expect a single result
4. gets the resulting configuration property, a query result is always returned even if the property does not exist in the source therefore `getResult()` returns an `Optional` that lets you decide what to do if the property is missing
5. converts the property value to URI if present, a property can be defined in a source with a null value which explains why the property value is also an `Optional` and why we need to use `flatMap()`
6. returns the actual value if it exists or the specified default value
7. we subscribe to the `Mono` which actually runs the query in the source and returns the property value or the default value if the property value is null or not defined in the source

This seems to be a lot of steps to simply retrieve one property value, but if you look closely you'll understand that each of them is actually necessary:

- we want to be able to retrieve multiple properties and/or create more complex queries in a batch so `.execute()` is required to mark the end of a batch of queries
- we want to be reactive so `.single().map()` and `subscribe()` are required
- we want to have access to the configuration query key at the origin of a property for troubleshooting as a result the a query result must expose `getQueryKey()` and `getResult()` methods
- we want to be able to convert a property value and provide different behaviors when a property does not exist in a source or when it does exist but with a null value, as a result `.flatMap(property -> property.asURI()).orElse(URI.create("http://localhost"))` is required

As we said earlier, a configuration depends on the context: a given property might have different values when considering different contexts. The configuration API defines a configuration property with a name, a value and a set of parameters specifying the context for which the property is defined. Such configuration property is referred to as a **parameterized configuration property**.

> Some configuration source implementations do not support parameterized configuration property, they simply ignore parameters specified in queries and return the value associated to the property name. This is especially the case of environment variables which don't allow to specify property parameters.

In order to retrieve a property in a particular context we can then parameterized the configuration query as follows:

```java
source.get("server.url")
    .withParameters("environment", "production", "zone", "us")
    .execute()
    ...
```

In the preceding example, we query the source for property `server.url` defined for the production environment in zone US. To state the obvious, both the list of parameters and their values can be determined at runtime using actual contextual values. This is what makes parameterized properties so powerful as it is suitable for a wide range of use cases. This is all the more true when using a configuration source which supports some kind of defaulting such as the [Composite Configuration source](#composite-configuration-source).

> Whether the exact or nearest value matching the query is returned by a configuration source is implementation dependent but since the *configuration* module provides the [Composite Configuration source](#composite-configuration-source) which can wrap any configuration source to add support for defaulting, it is a good practice to implement configuration sources that only support exact matching of a configuration query key (ie. including name and parameters).

As said before the API let's you fluently query multiple properties in a batch and map the results in a configuration object.

```java
source
    .get("server.port", "db.url", "db.user", "db.password").withParameters("environment", "production", "zone", "us")
    .and()
    .get("db.schema").withParameters("environment", "production", "zone", "us", "tenant", "someCompany")
    .execute()
    .collectMap(queryResult -> queryResult.getQueryKey().getName(), queryResult -> queryResult.getResult())
    .map(properties -> {
        ApplicationConfiguration config = new ApplicationConfiguration();
        
        properties.get("server.port").flatMap(property -> property.asInteger()).ifPresent(config::setServerPort);
        properties.get("db.url").flatMap(property -> property.asURL()).ifPresent(config::setDbURL);
        properties.get("db.user").flatMap(property -> property.asString()).ifPresent(config::setDbUser);
        String dbPassword = properties.get("db.password").flatMap(property -> property.asString()).ifPresent(config::setDbPassword);
        String dbSchema = properties.get("db.schema").flatMap(property -> property.asString()).ifPresent(config::setDbSchema);
        
        return config;
    })
    .subscribe(config -> {
        ...
    });
```

The beauty of being reactive is that it comes with a lot of cool features such as the ability to re-execute a query or caching the result. A `Flux` or a `Mono` executes on subscriptions, which means we can create a complex query to retrieve a whole configuration, keep the resulting Reactive Streams `Publisher` and subscribe to it when needed. A Reactive Stream publisher can also cache configuration results.

```java
Mono<ApplicationConfiguration> configurationLoader = ... // see previous example

// Query the source on each subscriptions
configurationLoader.subscribe(config -> {
    ...
});

// Cache the configuration for five minutes
Mono<ApplicationConfiguration> cachedConfigurationLoader = configurationLoader.cache(Duration.ofMinutes(5));

// Query the source on first subscriptions, further subscriptions within a window of 5 minutes will get the cached configuration
cachedConfigurationLoader.subscribe(config -> {
    ...
});
```

> Although publisher caching is a cool feature, it might not be ideal for complex caching use cases and more solid solution should be considered.

A configuration source relies on a `SplittablePrimitiveDecoder` to decode property values. Configuration source implementations usually provide a default decoder but it is possible to inject custom decoders to decode particular configuration values. The expected decoder implementation depends on the configuration source implementation but most of the time a string to object decoder is expected.

```java
SplittablePrimitiveDecoder<String> customDecoder = ...

PropertyFileConfigurationSource source = new PropertyFileConfigurationSource(new ClasspathResource(URI.create("classpath:/path/to/configuration")), customDecoder)
```

The regular and most efficient way to query a configuration source is to target specific configuration properties identified by a name and a set of parameters, however there are some cases that actually require to list all values defined for a particular property name and matching a particular set of parameters. 

for instance, this is typically the case when configuring log levels, since we can hardly know the name of each and every loggers used in an application, it is easier, safer and more efficient in that case to list all the configuration properties defined for a `logging.level` property and apply the configuration to the loggers based on the parameters of the returned properties.

For instance, the following properties can be defined in the configuration:

```
logging.level[]=info
logging.level[logger="logger1"]=debug
logging.level[logger="logger2"]=trace
logging.level[logger="logger3"]=error
```

These configuration properties can then be listed in the application as follows:

```java
// Returns all logging.level properties defined in the configuration source
List<ConfigurationProperty> result = source.list("logging.level")
	.executeAll()
	.collectList()
	.block();

// Apply logging configuration
for(ConfigurationProperty p : result) {
    Optional<String> loggerName = p.getKey().getParameter("logger");
    Level level = p.as(Level.class).get();
	// Configure logger...
}
```

The `executeAll()` method returns all the properties defined in the configuration source for a particular property name and matching the set of parameters defined in the query whether they are defined with extra parameters or not. For instance, if we extend our example by adding an `environment` parameter:

```
logging.level[]=info
logging.level[environment="dev",logger="logger1"]=debug
logging.level[environment="prod",logger="logger2"]=trace
logging.level[logger="logger3"]=error
```

The following list query will return all values that are defined with a `logger` parameter whether they are defined with an `environment` parameter or not. Please note how the `logger` parameter is specified in the query as a wildcard:

```java
// Returns logging.level[environment="dev", logger="logger1"], logging.level[environment="prod", logger="logger2"] and logging.level[logger="logger3"]=error which are all defined with parameter logger
List<ConfigurationProperty> result = source.list("logging.level")
	.withParameters(Parameter.wildcard("logger"))
	.executeAll()
	.collectList()
	.block();
```

On the other hand, the `execute()` method is exact and returns all the properties defined in the configuration source for a particular property name and which parameters exactly match the set of parameters defined in the query, excluding those that are defined with extra parameters:

```java
// Returns logging.level[logger="logger3"]=error which exactly defines parameter logger
List<ConfigurationProperty> result = source.list("logging.level")
	.withParameters(Parameter.wildcard("logger"))
	.execute()
	.collectList()
	.block();
```

### Map configuration source

The map configuration is the most basic configuration source implementation. It exposes configuration properties stored in a map in memory. It doesn't support parameterized properties, regardless of the parameters specified in a query, only the property name is considered when resolving a value.

```java
MapConfigurationSource source = new MapConfigurationSource(Map.of("server.url", new URL("http://localhost")));
...
```

This implementation can be used for testing purpose in order to provide a mock configuration source.

### System environment configuration source

The system environment configuration source exposes system environment variables as configuration properties. As for the map configuration source, this implementation doesn't support parameterized properties.

```plaintext
$ export SERVER_URL=http://localhost
```

```java
SystemEnvironmentConfigurationSource source = new SystemEnvironmentConfigurationSource();
...
```

This implementation can be used to bootstrap an application using system environment variables.

### System properties configuration source

The system properties configuration source exposes system properties as configuration properties. As for the two previous implementations, it doesn't support parameterized properties.

```plaintext
$ java -Dserver.url=http://localhost ...
```

```java
SystemPropertiesConfigurationSource source = new SystemPropertiesConfigurationSource();
...
```

This implementation can be used to bootstrap an application using system properties.

### Command line configuration source

The command line configuration source exposes configuration properties specified as command line arguments of the application. This implementation supports parameterized properties.

Configuration properties must be specified as application arguments using the following syntax: `--property[parameter_1=value_1...parameter_n=value_n]=value` where property and parameter names are valid Java identifiers and property and parameter values are Java primitives such as integer, boolean, string... A complete description of the syntax can be found in the [API documentation][inverno-javadoc].

For instance the following are valid configuration properties specified as command line arguments:

```plaintext
$ java ... Main \
--web.server_port=8080 \
--web.server_port[profile="ssl"]=8443 \
--db.url[env="dev"]="jdbc:oracle:thin:@dev.db.server:1521:sid" \
--db.url[env="prod",zone="eu"]="jdbc:oracle:thin:@prod_eu.db.server:1521:sid" \
--db.url[env="prod",zone="us"]="jdbc:oracle:thin:@prod_us.db.server:1521:sid"
```

```java
public static void main(String[] args) {
    CommandLineConfigurationSource source = new CommandLineConfigurationSource(args);
    ...
}
...
```

### `.properties` file configuration source

The `.properties` file configuration source exposes configuration properties specified in a `.properties` file. This implementation supports parameterized properties.

Configuration properties can be specified in a property file using a syntax similar to the command line configuration source for the property key. Some characters must be escaped with respect to the `.properties` file format. Property values don't need to follow Java's notation for strings since they are considered as strings by design.

```properties
web.server_port=8080 
web.server_port[profile\="ssl"]=8443
db.url[env\="dev"]=jdbc:oracle:thin:@dev.db.server:1521:sid
db.url[env\="prod",zone\="eu"]=jdbc:oracle:thin:@prod_eu.db.server:1521:sid
db.url[env\="prod",zone\="us"]=jdbc:oracle:thin:@prod_us.db.server:1521:sid
```

```java
PropertyFileConfigurationSource source = new PropertyFileConfigurationSource(new ClasspathResource(URI.create("classpath:/path/to/file")));
...
```

### `.cprops` file configuration source

The `.cprops` file configuration source exposes configuratio properties specified in a `.cprops` file. This implementation supports parameterized properties.

The `.cprops` file format has been introduced to facilitate the definition and reading of parameterized properties. In particular it allows to regroup the definition of properties with common parameters into sections and many more.

For instance:

```properties
server.port=8080
db.url=jdbc:oracle:thin:@localhost:1521:sid
db.user=user
db.password=password
log.level=ERROR
application.greeting.message="""
 === Welcome! === 

     This is      
    a formated    
     message.     

 ================
"""

[ environment="test" ] {
    db.url=jdbc:oracle:thin:@test:1521:sid
    db.user=user_test
    db.password=password_test
}

[ environment="production" ] {
    db.url=jdbc:oracle:thin:@production:1521:sid
    db.user=user_production
    db.password=password_production
    
    [ zone="US" ] {
        db.url=jdbc:oracle:thin:@production.us:1521:sid
    }
    
    [ zone="EU" ] {
        db.url=jdbc:oracle:thin:@production.eu:1521:sid
    }
    
    [ zone="EU", node="node1" ] {
        log.level=DEBUG
    }
}
```

A complete [JavaCC][javacc] [grammar][cprops-grammar] is available in the source of the configuration module.

```java
CPropsFileConfigurationSource source = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/path/to/file")));
...
```

### Bootstrap configuration source

The bootstrap configuration source is a [composite configuration source](#composite-configuration-source) preset with configuration sources typically used when bootstrapping an application.

This implementation resolves configuration properties from the following sources in that order, from the highest priority to the lowest:

- command line
- system properties
- system environment variables
- the `configuration.cprops` file in `./conf/` or `${inverno.conf.path}/` directories if one exists (if the first one exists the second one is ignored)
- the `configuration.cprops` file in `${java.home}/conf/` directory if it exists
- the `configuration.cprops` file in the application module if it exists

This source is typically created in a `main` method to load the bootstrap configuration on startup.

```java
public class Application {

    public static void main(String[] args) {
        BootstrapConfigurationSource source = new BootstrapConfigurationSource(Application.class.getModule(), args);
        
        // Load configuration
        ApplicationConfiguration configuration = ConfigurationLoader
            .withConfiguration(ApplicationConfiguration.class)
            .withSource(source)
            .load()
            .block();

        // Start the application with the configuration
        ...
    }
}
```

### Redis configuration source

The [Redis][redis] configuration source exposes configuration properties stored in a Redis data store. This implementation supports parameterized properties and it is also configurable which means it can be used to set configuration properties in the data store at runtime.

It also provides a simple but effective versioning system which allows to set multiple properties and activate or revert them atomically. A global revision keeps track of the whole data store but it is also possible to version a particular branch in the tree of properties.

The following example shows how to set configuration properties for the `dev` and `prod` environment and activates them globally or independently.

```java
RedisClient redisClient = ...
RedisConfigurationSource source = new RedisConfigurationSource(redisClient);

source
    .set("db.url", "jdbc:oracle:thin:@dev.db.server:1521:sid").withParameters("environment", "dev").and()
    .set("db.url", "jdbc:oracle:thin:@prod_eu.db.server:1521:sid").withParameters("environment", "prod", "zone", "eu").and()
    .set("db.url", "jdbc:oracle:thin:@prod_us.db.server:1521:sid").withParameters("environment", "prod", "zone", "us")
    .execute()
    .blockLast();
    
// Activate working revision globally
source.activate().block();

// Activate working revision for dev environment and prod environment independently
source.activate("environment", "dev").block();
source.activate("environment", "prod").block();
```

It is also possible to fallback to a particular revision by specifying it in the `activate()` method:

```java
// Activate revision 2 globally
source.activate(2).block();
```

This implementation is particularly suitable to load tenant specific configuration in a multi-tenant application, or user preferences... basically any kind of configuration that can and will be dynamically changed at runtime.

> Parameterized properties and versioning per branch are two simple yet powerful features but it is important to be picky here otherwise there is a real risk of messing things up. You should thoughtfully decide when a configuration branch can be versioned, for instance the versioned sets of properties must be disjointed (if this is not obvious, think again), this is actually checked in the Redis configuration source and an exception will be thrown if you try to do things like this, basically trying to version the same property twice.

### Composite Configuration source

The composite configuration source is a configuration source implementation with two particular features: first it allows to compose multiple configuration sources into one configuration source and then it supports defaulting strategies to determine the best matching value for a given configuration query key.

The property returned for a configuration query key then depends on two factors: the order in which configuration sources were defined in the composite configuration source, from the highest priority to the lowest, and then how close is a property from the configuration query key. 

The `CompositeConfigurationSource` resolves a configuration property by querying its sources in sequence from the highest priority to the lowest. It relies on a `CompositeConfigurationStrategy` to determine at each round which queries to execute and retain the best matching property from the results. The best matching property is the property whose key is the closest to the original configuration query key according to a metric implemented in the strategy. The algorithm stops when an exact match is found or when there's no more configuration source to query.

The `DefaultCompositeConfigurationStrategy` defines the default strategy implementation. It determines the best matching property for a given original query by prioritizing query parameters from left to right: the best matching property is then the one matching the most continuous parameters from right to left. In practice, if we consider query key `property[p1=v1,...pn=vn]`, it supersedes key `property[p2=v2,...pn=vn]` which supersedes key `property[p3=v3,...pn=vn]`... which supersedes key `property[]`. As a result, an original query with `n` parameters results in `n+1` queries being actually executed if no property was retained in previous rounds and `n-p` queries if a property with p parameters (p<n) was retained in previous rounds. The order into which parameters are specified in the original query is then significant: `property[p1=v1,p2=v2] != property[p2=v2,p1=v1]`.

When defining configuration parameters, we should then order them from the most specific to the most general when querying a composite source. For example, the `node` parameter which is more specific than the `zone` parameter should come first then the `zone` parameter which is more specific than the `environment` parameter should come next and finally the `environment` parameter which is the most general should come last.

For instance, we can consider two parameterized configuration sources: `source1` and `source2`.

`source1` holds the following properties:

- `server.url[]=null`
- `server.url[zone="US", environment="production"]="https://prod.us"`
- `server.url[zone="EU"]="https://default.eu"`

`source2` holds the following properties:

- `server.url[]="https://default"`
- `server.url[environment="test"]="https://test"`
- `server.url[environment="production"]="https://prod"`

We can compose them in a composite configuration source as follows:

```java
ConfigurationSource<?, ?, ?> source1 = ...
ConfigurationSource<?, ?, ?> source2 = ...

CompositeConfigurationSource source = new CompositeConfigurationSource(List.of(source1, source2));

source                                                         // 1
    .get("server.url")
    .withParameters("zone", "US", "environment", "production")
    .execute()
    ...

source                                                         // 2
    .get("server.url")
    .withParameters("environment", "test")
    .execute()
    ...

source                                                         // 3
    .get("server.url")
    .execute()
    ...


source                                                         // 4
    .get("server.url")
    .withParameters("zone", "EU", "environment", "production")
    .execute()...

source                                                         // 5
    .get("server.url")
    .withParameters("environment", "production", "zone", "EU")
    .execute()
    ...
    
```

In the example above:

1. `server.url[environment="production",zone="US"]` is exactly defined in `source1` => `https://prod.us` defined in `source1` is returned
2. `server.url[environment="test"]` is not defined in `source1` but exactly defined in `source2` => `https://test`  defined in `source2` is returned
3. Although `server.url[]` is defined in both `source1` and `source2`, `source1` has the highest priority and therefore => `null` is returned
4. There is no exact match for `server.url[zone="EU", environment="production"]` in both `source1` and `source2`, the priority is given to the parameters from left to right, the property matching `server.url[environment="production"]` shall be returned => `https://prod` defined in `source2` is returned
5. Here we've simply changed the order of the parameters in the previous query, again the priority is given to parameters from left to right, since there is no match for `server.url[environment="production", zone="EU"]`, `server.url[zone="EU"]` is considered => `https://default.eu` defined in `source1` is returned

As you can see, the order into which parameters are specified in a query is significant and different results might be returned.

When considering multiple configuration sources, properties can be defined with the exact same key in two different sources, the source with the highest priority wins. In the last example we've been able to set the value of `server.url[]` to `null` in `source1`, however `null` is itself a value with a different meaning than a missing property, the `unset` value can be used in such situation to *unset* a property defined in a source with a lower priority.

For instance, considering previous example, we could have defined `server.url[]=unset` instead of `server.url[]=null` in `source1`, the query would then have returned an empty query result indicating an undefined property.

Prioritization and defaulting also apply when listing configuration properties on a composite configuration source. In case of conflict between two configuration sources, the default strategy retains the one defined by the source with the highest priority. Results also contains all properties defined with less parameters than the list query from right to left.

For instance, if we consider the following sources: `source1` and `source2`.

`source1` holds the following properties:

- `logging.level[environment="dev"]=info`
- `logging.level[environment="dev",name="test1"]=info`
- `logging.level[environment="prod",name="test1"]=info`
- `logging.level[environment="prod",name="test4"]=error`
- `logging.level[environment="prod",name="test5"]=info`
- `logging.level[environment="prod",name="test1",node="node-1"]=trace`

`source2` holds the following properties:

- `logging.level[environment="dev",node="node-1"]=info`
- `logging.level[environment="dev",name="test1"]=debug`
- `logging.level[environment="dev",name="test2"]=debug`
- `logging.level[environment="dev",name="test2",node="node-1"]=debug`
- `logging.level[environment="prod",name="test1"]=warn`
- `logging.level[environment="prod",name="test2"]=error`
- `logging.level[environment="prod",name="test3"]=info`

If we can compose them in a composite configuration source, we can list configuration properties as follows:

```java
ConfigurationSource<?, ?, ?> source1 = ...
ConfigurationSource<?, ?, ?> source2 = ...

CompositeConfigurationSource source = new CompositeConfigurationSource(List.of(source1, source2));

source                                       // 1
    .list("logging.level")
    .withParameters(
        Parameter.of("environment", "prod"), 
        Parameter.wildcard("name")
    )
    .execute()
    ...

source                                       // 2
    .list("logging.level")
    .withParameters(
        Parameter.of("environment", "dev"), 
        Parameter.wildcard("name")
    )
    .executeAll()
    ...

```

In the example above:

1. `execute()` is exact and returns properties defined with parameters `environment` and `name`, with parameter `environment` only and with no parameter following defaulting rules implemented in the default strategy. As a result the following properties are returned: 
    - `logging.level[environment="prod",name="test1"]=info` defined in `source1` and overriding the property defined in `source2`
    - `logging.level[environment="prod",name="test2"]=error` defined in `source2`
    - `logging.level[environment="prod",name="test3"]=info` defined in `source2`
    - `logging.level[environment="prod",name="test4"]=error` defined in `source1`
    - `logging.level[environment="prod",name="test5"]=info` defined in `source1`
2. `executeAll()` returns all properties defined with parameters `environment`, `name` and any other parameter, with parameter `environment` only and with no parameter following defaulting rules implemented in the default strategy. As a result the following properties are returned: 
    - `logging.level[environment="dev"]=info` defined in `source1` which is the property that would be returned when querying the source with an unspecified name (eg. `logging.level[environment="dev",name="unspecifiedLogger"]`)
    - `logging.level[environment="dev",name="test1"]=info` defined in `source1` and overriding the property defined in `source2`
    - `logging.level[environment="dev",name="test2"]=debug` defined in `source2`
    - `logging.level[environment="dev",name="test2",node="node-1"]=debug` defined in `source2`

> it is important to note that list operations, especially on a very large set of data can become quite expensive and impact performances, as a result they must be used wisely.

### Configurable configuration source

A configurable configuration source is a particular configuration source which supports configuration properties updates. The [Redis configuration source](#redis-configuration-source) is an example of configurable configuration source.

The `ConfigurableConfigurationSource` interface is the main entry point for updating configuration properties, it shall be used every time there's a need to retrieve or set configuration properties.

It extends the `ConfigurationSource` with one method for creating a `ConfigurationUpdate` instance eventually executed in order to set one or more configuration properties in the configuration source.

For instance, a parameterized property `server.port` can be set in a configuration source as follows:

```java
ConfigurableConfigurationSource<?, ?, ?, ?, ?> source = null;

source.set("server.port", 8080)
    .withParameters("environment", "production", "zone", "us")
    .execute()
    .single()
    .subscribe(
        updateResult -> {
            try {
                updateResult.check();
                // Update succeeded
                ...
            }
            catch(ConfigurationSourceException e) {
                // Update failed
                ...
            }
        }
    );
```

A configurable configuration source relies on a `JoinablePrimitiveEncoder` to encode property values. Implementations usually provide a default encoder but it is possible to inject custom encoders to encode particular configuration values. The expected encoder implementation depends on the configuration source implementation but most of the time an object to string encoder is expected.

```java
RedisClient redisClient = ...
JoinablePrimitiveEncoder<String> customEncoder = ...
SplittablePrimitiveDecoder<String> customDecoder = ...

RedisConfigurationSource source = new RedisConfigurationSource(redisClient, customEncoder, customDecoder)
```

## Configuration loader

The API offers a great flexibility but as we've seen it might require some efforts to load a configuration in a usable explicit Java bean. Hopefully, this has been anticipated and the configuration module provides a configuration loader to smoothly load configuration objects in the application.

The `ConfigurationLoader` interface is the main entry point for loading configuration objects from a configuration source. It can be used in two different ways, either dynamically using Java reflection or statically using the Inverno compiler.

### Dynamic loader

A dynamic loader can be created by invoking static method `ConfigurationLoader#withConfiguration()` which accepts a single `Class` argument specifying the type of the configuration that must be loaded. 

A valid configuration type must be an interface defining configuration properties as non-void no-argument methods whose names correspond to the configuration properties to retrieve and to map to the resulting configuration object, default values can be specified in default methods.

For instance the following interface represents a valid configuration type which can be loaded by a configuration loader:

```java
public interface AppConfiguration {

    // query property 'server_host'
    String server_host();

    // query property 'server_port'
    default int server_port() {
        return 8080;
    }
}
```

It can be loaded at runtime as follows:

```java
ConfigurationSource<?, ?, ?> source = ...

ConfigurationLoader
    .withConfiguration(AppConfiguration.class)
    .withSource(source)
    .withParameters("environment", "production")
    .load()
    .map(configuration -> startServer(configuration.server_host(), configuration.server_port()))
    .subscribe();
```

In the above example, the configuration source is queried for properties `server_host[environment="production"]` and `server_port[environment="production"]`.

The dynamic loader also supports nested configurations when the return type of a method is an interface representing a valid configuration type.

```java
public interface ServerConfiguration {

    // query property 'server_host'
    String server_host();
    
    // query property 'server_port'
    default int server_port() {
        return 8080;
    }
}
```

```java
public interface AppConfiguration {
    
    // Prefix child property names with 'server_configuration'
    ServerConfiguration server_configuration();
}
```

In the above example, the configuration source is queried for properties `server_configuration.server_host[environment="production"]` and `server_configuration.server_port[environment="production"]`.

It is also possible to load a configuration by invoking static method `ConfigurationLoader#withConfigurator()` which allows to load any type of configuration (not only interface) by relying on a configurator and a mapping function. 

A configurator defines configuration properties as void single argument methods whose names correspond to the configuration properties to retrieve and inject into a configurator instance using a dynamic configurer `Consumer<Configurator>`. The mapping function is finally applied to that configurer to actually create the resulting configuration object.

For instance, previous example could have been implemented as follows:

```java
public class AppConfiguration {
    
    private String server_host;
    private String server_port = 8080;
    
    // query property 'server_host'
    public void server_host(String server_host) {
        this.server_host = server_host;
    }
    
    // query property 'server_port'
    public void server_port(int server_port) {
        this.server_port = server_port;
    }
    
    public String server_host() {
        return server_host;
    }
    
    public int server_port() {
        return server_port;
    }
}
```

```java
ConfigurationSource<?, ?, ?> source = ...

ConfigurationLoader
    .withConfigurator(AppConfiguration.class, configurer -> {
        AppConfiguration configuration = new AppConfiguration();
        configurer.apply(configuration);
        return configuration;
    })
    .withSource(source)
    .withParameters("environment", "production")
    .load()
    .map(configuration -> startServer(configuration.server_host(), configuration.server_port()))
    .subscribe();
```

### Static loader

Dynamic loading is fine but it relies on Java reflection which induces extra processing at runtime and might cause unexpected runtime errors due to the lack of static checking. This is all the more true as most of the time configuration definitions are known at compile time. For these reasons, it is better to create adhoc configuration loader implementations. Fortunately, the configuration Inverno compiler plugin can generate these for us.

In order to create a configuration bean in an Inverno module, we simply need to create an interface for our configuration as specified above and annotates it with `@Configuration`, this will tell the configuration Inverno compiler plugin to generate a corresponding configuration loader implementation as well as a module bean making our configuration directly available inside our module.

```java
@Configuration
public interface AppConfiguration {

    // query property 'server_host'
    String server_host();

    // query property 'server_port'
    int server_port();
}
```

The preceding code will result in the generation of class `AppConfigurationLoader` which can then be used to load configuration at runtime without resorting to reflection.

```java
ConfigurationSource<?, ?, ?> source = ...

new AppConfigurationLoader()
    .withSource(source)
    .withParameters("environment", "production")
    .load()
    .map(configuration -> startServer(configuration.server_host(), configuration.server_port()))
    .subscribe();
```

A configuration can also be obtained *manually* as follows:

```java
AppConfiguration defaultConfiguration = AppConfigurationLoader.load(configurator -> configurator.server_host("0.0.0.0"));

AppConfiguration customConfiguration = AppConfigurationLoader.load(configurator -> configurator.server_host("0.0.0.0"));
```

By default, the generated loader also defines an overridable module bean which loads the configuration in the module. This bean defines three optional sockets:

- **configurationSource** indicates the configuration source to query when initializing the configuration bean
- **parameters** indicates the parameters to consider when querying the source
- **configurer** provides a way to overrides default values

If no configuration source is present, a default configuration is created, otherwise the configuration source is queried with the parameters, the resulting configuration is then *patched* with the configurer if present. The bean is overridable by default which means we can inject our own implementation if we feel like it.

It is possible to disable the activation of the configuration bean or make it non overridable in the `@Configuration` interface:

```java
@Configuration(generateBean = false, overridable = false)
public interface AppConfiguration {
    ...
}
```

Finally, nested beans can be specified in a configuration which is convenient when a module is composing multiple modules and we wish to aggregate all configurations into one single representation in the composite module. 

For instance, we can have the following configuration defined in a component module:

```java
@Configuration
public interface ComponentModuleConfiguration {
    ...
}
```

and the following configuration defined in the composite module:

```java
@Configuration
public interface CompositeModuleConfiguration {

    @NestedBean
    ComponentModuleConfiguration component_module_configuration();
}
```

In the preceding example, we basically indicate to the Inverno framework that the `ComponentModuleConfiguration` defined in the `CompositeModuleConfiguration` must be injected into the component module instance.

