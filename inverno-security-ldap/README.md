[ldap]: https://en.wikipedia.org/wiki/Lightweight_Directory_Access_Protocol
[active_directory]: https://en.wikipedia.org/wiki/Active_Directory
[openldap]: https://www.openldap.org

[rfc2256]: https://datatracker.ietf.org/doc/html/rfc2256
[rfc2798]: https://datatracker.ietf.org/doc/html/rfc2798

# Security LDAP

The Inverno *security-ldap* module provides authenticators used to authenticate login credentials against [LDAP][ldap] or [Active Directory][active_directory] servers.

It also provides an identity resolver for resolving user identity from the LDAP attributes of a user entry.

The LDAP client provided in module *ldap* is therefore required, in order to use the the Inverno *securiy-ldap* module we need then to declare the following dependencies in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.ldap;
    requires io.inverno.mod.security.ldap;
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
            <artifactId>inverno-ldap</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-security-ldap</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-security-http:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-security-ldap:${VERSION_INVERNO_MODS}'
...
```

The following example shows how to configure a security manager to authenticate login credentials against an LDAP server, resolving the authenticated user's identity from the LDAP server and a role-based access controller from user's groups.

```java
// Provided by the ldap module
LDAPClient ldapClient = null;
		
SecurityManager<LoginCredentials, LDAPIdentity, RoleBasedAccessController> securityManager = SecurityManager.of(
    new LDAPAuthenticator(ldapClient, "dc=inverno,dc=io"),
    new LDAPIdentityResolver(ldapClient),
    new GroupsRoleBasedAccessControllerResolver()
);
```

## LDAP authenticator

The `LDAPAuthenticator` can authenticate `LoginCredentials` (username/password) against a standard LDAP server.

When the password specified in the credentials is a `RawPassword`, authentication is made by a binding operation to the LDAP server. If the password is an encoded password, authentication is made by comparing the encoded value to the password attribute (`userPassword` by default) of the LDAP user entry.

The user `DN` is obtained using username template (defaults to `cn={0},ou=users`) formatted with the username specified in the credentials. User groups are resolved by searching for groups using a search filter set to `(&(objectClass=groupOfNames)(member={0}))` by default.

An `LDAPAuthenticator` is created using an `LDAPClient` and a base `DN` which identifies the origanization where to look for entries. The following example shows how to create an `LDAPAuthenticator` to authenticate users in the `dc=inverno,dc=io` organization:

```java
// Provided by the ldap module
LDAPClient ldapClient = ...

LDAPAuthenticator ldapAuthenticator = new LDAPAuthenticator(ldapClient, "dc=inverno,dc=io");

LDAPAuthentication authentication = ldapAuthenticator.authenticate(LoginCredentials.of("jsmith", new RawPassword("password"))).block();
```

The `LDAPAuthentication` returned by the `LDAPAuthenticator` is a specific principal authentication that exposes the user's DN, it also extends `GroupAwareAuthentication` since LDAP users can be a organized in groups (i.e. `groupOfNames` class). These information are resolved when authenticating credentials in the LDAP authenticator. A `GroupsRoleBasedAccessControllerResolver` can then be used in a security manager or security interceptor to resolve a role-based access contoller using users groups as roles.

## Active Directory authenticator

The `ActiveDirectoryAuthenticator` is a similar implementation used to authenticate `LoginCredentials` against an [Active Directory][active_directory] server and returning `LDAPAuthentication`.

Although Active Directory can be accessed using LDAP, the internal semantic is quite different than standard LDAP server like [OpenLDAP][openldap] which is why we needed a specific implementation. 

Unlike the `LDAPAuthenticator`, authentication using password comparison is not supported and therefore it can only authenticate credentials specified with raw passwords using a bind operation. User groups are resolved from the `memberOf` attribute of the user entry which is resolved using a search user filter set to `(&(objectClass=user)(userPrincipalName={0}))` by default.

An `ActiveDirectoryAuthenticator` is created using an `LDAPClient` and a domain. The following example shows how to create an `ActiveDirectoryAuthenticator` to authenticate users in `inverno.io` domain:

```java
// Provided by the ldap module
LDAPClient ldapClient = ...

ActiveDirectoryAuthenticator adAuthenticator = new ActiveDirectoryAuthenticator(ldapClient, "inverno.io");

LDAPAuthentication authentication = adAuthenticator.authenticate(LoginCredentials.of("jsmith", new RawPassword("password"))).block();
```

## LDAP identity

An LDAP server is basically a directory service which can provide any kind of information about a user such as email addresses, postal addresses, phone numbers... The `LDAPIdentity` exposes standard LDAP attributes of `person`, `organizationalPerson` and `inetOrgPerson` classes as defined by [RFC 2256][rfc2256] and [RFC 2798][rfc2798].

The LDAP identity is resolved in a security manager or a security interceptor from an `LDAPAuthentication` using an `LDAPIdentityResolver` which basically look up the LDAP user entry with specific attributes in the LDAP server using the user DN and a search user filter set to `(&(objectClass=inetOrgPerson)(uid={0}))` by default.

An `LDAPIdentityResolver` is created using an `LDAPClient`. The following example shows how to create a simple `LDAPIdentityResolver` for resolving common identity attributes:

```java
// Provided by the ldap module
LDAPClient ldapClient = ...

LDAPIdentityResolver ldapIdentityResolver = new LDAPIdentityResolver();
```

It is possible to specify which attributes must be queried as follows:

```java
// Provided by the ldap module
LDAPClient ldapClient = ...

LDAPIdentityResolver ldapIdentityResolver = new LDAPIdentityResolver(ldapClient, "uid", "mail", "mobile");
```

