/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.ldap;

import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An LDAP Client exposes reactive methods to query an LDAP server.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface LDAPClient extends LDAPOperations {
	
	/**
	 * <p>
	 * Indicates the successful completion of an operation.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
	static final int CODE_SUCCESS = 0;
	/**
	 * <p>
	 * Indicates that the operation is not properly sequenced with relation to other operations (of same or different type).
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_OPERATIONS_ERROR = 1;
    /**
	 * <p>
	 * Indicates the server received data that is not well-formed.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_PROTOCOL_ERROR = 2;
    /**
	 * <p>
	 * Indicates that the time limit specified by the client was exceeded before the operation could be completed.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_TIME_LIMIT_EXCEEDED = 3;
    /**
	 * <p>
	 * Indicates that the size limit specified by the client was exceeded before the operation could be completed.

	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_SIZE_LIMIT_EXCEEDED = 4;
    /**
	 * <p>
	 * Indicates that the Compare operation has successfully completed and the assertion has evaluated to FALSE or Undefined.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_COMPARE_FALSE = 5;
    /**
	 * <p>
	 * Indicates that the Compare operation has successfully completed and the assertion has evaluated to TRUE.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_COMPARE_TRUE = 6;
    /**
	 * <p>
	 * Indicates that the authentication method or mechanism is not supported.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_AUTH_METHOD_NOT_SUPPORTED = 7;
    /**
	 * <p>
	 * Indicates the server requires strong(er) authentication in order to complete the operation.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_STRONG_AUTH_REQUIRED = 8;
    /**
	 * <p>
	 * Indicates that a referral needs to be chased to complete the operation
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_REFERRAL = 10;
    /**
	 * <p>
	 * Indicates that an administrative limit has been exceeded.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_ADMIN_LIMIT_EXCEEDED = 11;
    /**
	 * <p>
	 * Indicates a critical control is unrecognized.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_UNAVAILABLE_CRITICAL_EXTENSION = 12;
    /**
	 * <p>
	 * Indicates that data confidentiality protections are required.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_CONFIDENTIALITY_REQUIRED = 13;
    /**
	 * <p>
	 * Indicates the server requires the client to send a new bind request, with the same SASL mechanism, to continue the authentication process.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_SASL_BIND_IN_PROGRESS = 14;
    /**
	 * <p>
	 * Indicates that the named entry does not contain the specified attribute or attribute value.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_NO_SUCH_ATTRIBUTE = 16;
    /**
	 * <p>
	 * Indicates that a request field contains an unrecognized attribute description.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_UNDEFINED_ATTRIBUTE_TYPE = 17;
    /**
	 * <p>
	 * Indicates that an attempt was made (e.g., in an assertion) to use a matching rule not defined for the attribute type concerned.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_INAPPROPRIATE_MATCHING = 18;
    /**
	 * <p>
	 * Indicates that the client supplied an attribute value that does not conform to the constraints placed upon it by the data model.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_CONSTRAINT_VIOLATION = 19;
    /**
	 * <p>
	 * Indicates that the client supplied an attribute or value to be added to an entry, but the attribute or value already exists.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_ATTRIBUTE_OR_VALUE_EXISTS = 20;
    /**
	 * <p>
	 * Indicates that a purported attribute value does not conform to the syntax of the attribute.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_INVALID_ATTRIBUTE_SYNTAX = 21;
    /**
	 * <p>
	 * Indicates that the object does not exist in the DIT.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_NO_SUCH_OBJECT = 32;
    /**
	 * <p>
	 * Indicates that an alias problem has occurred.  For example, the code may used to indicate an alias has been dereferenced that names no object.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_ALIAS_PROBLEM = 33;
    /**
	 * <p>
	 * Indicates that an LDAPDN or RelativeLDAPDN field (e.g., search base, target entry, ModifyDN newrdn, etc.) of a request does not conform to the required syntax or contains attribute values that
	 * do not conform to the syntax of the attribute's type.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_INVALID_DN_SYNTAX = 34;
    /**
	 * <p>
	 * Indicates that the specified operation cannot be performed on a leaf entry.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_IS_LEAF = 35;
    /**
	 * <p>
	 * Indicates that a problem occurred while dereferencing an alias.  Typically, an alias was encountered in a situation  where it was not allowed or where access was denied.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_ALIAS_DEREFERENCING_PROBLEM = 36;
    /**
	 * <p>
	 * Indicates the server requires the client that had attempted to bind anonymously or without supplying credentials to provide some form of credentials.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_INAPPROPRIATE_AUTHENTICATION = 48;
    /**
	 * <p>
	 * Indicates that the provided credentials (e.g., the user's name and password) are invalid.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_INVALID_CREDENTIALS = 49;
    /**
	 * <p>
	 * Indicates that the client does not have sufficient access rights to perform the operation.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_INSUFFICIENT_ACCESS_RIGHTS = 50;
    /**
	 * <p>
	 * Indicates that the server is too busy to service the operation.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_BUSY = 51;
    /**
	 * <p>
	 * Indicates that the server is shutting down or a subsystem necessary to complete the operation is offline.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_UNAVAILABLE = 52;
    /**
	 * <p>
	 * Indicates that the server is unwilling to perform the operation.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_UNWILLING_TO_PERFORM = 53;
    /**
	 * <p>
	 * Indicates that the server has detected an internal loop (e.g., while dereferencing aliases or chaining an operation).
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_LOOP_DETECT = 54;
    /**
	 * <p>
	 * Indicates that the entry's name violates naming restrictions.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_NAMING_VIOLATION = 64;
    /**
	 * <p>
	 * Indicates that the entry violates object class restrictions.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_OBJECT_CLASS_VIOLATION = 65;
    /**
	 * <p>
	 * Indicates that the operation is inappropriately acting upon a non-leaf entry.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_NOT_ALLOWED_ON_NON_LEAF = 66;
    /**
	 * <p>
	 * Indicates that the operation is inappropriately attempting to remove a value that forms the entry's relative distinguished name.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_NOT_ALLOWED_ON_RDN = 67;
    /**
	 * <p>
	 * Indicates that the request cannot be fulfilled (added, moved, or renamed) as the target entry already exists.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_ENTRY_ALREADY_EXISTS = 68;
    /**
	 * <p>
	 * Indicates that an attempt to modify the object class(es) of an entry's 'objectClass' attribute is prohibited.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_OBJECT_CLASS_MODS_PROHIBITED = 69;
    /**
	 * <p>
	 * Indicates that the operation cannot be performed as it would affect multiple servers (DSAs).
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_AFFECTS_MULTIPLE_DSAS = 71;
    /**
	 * <p>
	 * Indicates the server has encountered an internal error.
	 * </p>
	 * 
	 * <a href="https://datatracker.ietf.org/doc/html/rfc4511#appendix-A.2">RFC4511 Appendix A.2</a>
	 */
    static final int CODE_OTHER = 80;

	/**
	 * <p>
	 * Authenticates to the server and executes a set of operations.
	 * </p>
	 * 
	 * <p>
	 * This method shall obtain a single authenticated connection used to execute the operations invoked in the specified function. That connection is closed once the returned publisher terminates.
	 * </p>
	 * 
	 * @param <T>         The type of results
	 * @param dn          the DN of the user to authenticate
	 * @param credentials the user credentials
	 * @param function    the function to be run using the authenticated connection
	 * 
	 * @return a publisher of results
	 * 
	 * @throws LDAPException if there was an error during binding operation or subsequent operations
	 */
	<T> Publisher<T> bind(String dn, String credentials, Function<LDAPOperations, Publisher<T>> function) throws LDAPException;
	
	/**
	 * <p>
	 * Authenticates to the server and executes a set of operations.
	 * </p>
	 * 
	 * <p>
	 * This method shall obtain a single authenticated connection used to execute the operations invoked in the specified function. That connection is closed once the returned publisher terminates.
	 * </p>
	 * 
	 * <p>
	 * The specified user DN is an expression formatted with the specified DN arguments. 
	 * </p>
	 * 
	 * @param <T>         The type of results
	 * @param dn          A expression to use to get the DN of the suer to authenticate
	 * @param dnArgs      the arguments to use the format the DN expression
	 * @param credentials the user credentials
	 * @param function    the function to be run using the authenticated connection
	 * 
	 * @return a publisher of results
	 * 
	 * @throws LDAPException if there was an error during binding operation or subsequent operations
	 */
	<T> Publisher<T> bind(String dn, Object[] dnArgs, String credentials, Function<LDAPOperations, Publisher<T>> function) throws LDAPException;
	
	/**
	 * <p>
	 * Closes the LDAP client and free resources.
	 * </p>
	 *
	 * @return a Mono that completes when the client is closed
	 * 
	 * @throws LDAPException if there was an error closing the client
	 */
	Mono<Void> close() throws LDAPException;
}
