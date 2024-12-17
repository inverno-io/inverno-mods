/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.base.net;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Net address resolver configuration.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface NetAddressResolverConfiguration {

	/**
	 * <p>
	 * Enables/Disables DNS name resolution.
	 * </p>
	 *
	 * <p>
	 * When set to false, the default address resolver is used.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code true}.
	 * </p>
	 *
	 * @return true to enable DNS resolution, false otherwise
	 */
	default boolean dns_enabled() {
		return true;
	}

	/**
	 * <p>
	 * The DNS servers to use when resolving an address.
	 * </p>
	 *
	 * @return the set of DNS servers
	 */
	Set<InetSocketAddress> name_servers();

	/**
	 * <p>
	 * Enables/Disables rotational sequential ordering of DNS servers.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 *
	 * @return true to activate rotational sequential ordering, false otherwise
	 */
	boolean rotate_servers();

	/**
	 * <p>
	 * The minimum TTL of the cached DNS resource records (in seconds).
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code 0}
	 * </p>
	 *
	 * @return the minimum TTL of the cached DNS resource records
	 */
	int cache_min_ttl();

	/**
	 * <p>
	 * The maximum TTL of the cached DNS resource records (in seconds).
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code 63072000} (2 years).
	 * </p>
	 *
	 * @return the maximum TTL of the cached DNS resource records
	 */
	default int cache_max_ttl() {
		return (int)TimeUnit.DAYS.toSeconds(365 * 2);
	}

	/**
	 * <p>
	 * The TTL of the cache for the failed DNS queries (in seconds).
	 * </p>
	 *
	 * <p>
	 * When set to {@code 0}, the cache for negative results is disabled.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code 0}.
	 * </p>
	 *
	 * @return the TTL of the cache for the failed DNS queries
	 */
	int cache_negative_ttl();

	/**
	 * <p>
	 * The capacity of the datagram packet buffer (in bytes).
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code 4096}.
	 * </p>
	 *
	 * @return the capacity of the datagram packet buffer
	 */
	default int max_payload_size() {
		return 4096;
	}

	/**
	 * <p>
	 * Enables/Disables the automatic inclusion of an optional records that tries to give the remote DNS server a hint about how much data the resolver can read per response.
	 * </p>
	 *
	 * <p>
	 * Some DNSServer may not support this and so fail to answer queries in which case this must be disabled.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 *
	 * @return true to enable the automatic inclusion of an optional records, false otherwise
	 */
	boolean include_optional_records();

	/**
	 * <p>
	 * The timeout of each DNS query performed by this resolver (in milliseconds).
	 * </p>
	 *
	 * <p>
	 * When set to {@code 0}, query timeout is disabled.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code 5000}.
	 * </p>
	 *
	 * @return the timeout of each DNS query
	 */
	default long query_timeout() {
		return 5000;
	}

	/**
	 * <p>
	 * The maximum allowed number of DNS queries to send when resolving an address.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code 16}.
	 * </p>
	 *
	 * @return the maximum allowed number of DNS queries to send
	 */
	default int max_queries_per_resolve() {
		return 16;
	}

	/**
	 * <p>
	 * Indicates whether DNS queries must be sent with the RD (recursion desired) flag set.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code true}.
	 * </p>
	 *
	 * @return true to send the RD (recursion desired) flag, false otherwise
	 */
	default boolean recursion_desired() {
		return true;
	}

	/**
	 * <p>
	 * The search domains to consider when resolving an address.
	 * </p>
	 *
	 * @return the set of search domains
	 */
	Set<String> search_domains();

	/**
	 * <p>
	 * The number of dots which must appear in a name before an initial absolute query is made.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code 1}.
	 * </p>
	 *
	 * @return the number of dots
	 */
	default int ndots() {
		return 1;
	}

	/**
	 * <p>
	 * Indicates whether domain or host names should be decoded to unicode when received as defined by <a href="https://datatracker.ietf.org/doc/html/rfc3492">RFC 3492</a>.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code true}.
	 * </p>
	 *
	 * @return true to decode domain or host names to unicode, false otherwise
	 */
	default boolean decode_idn() {
		return true;
	}

	/**
	 * <p>
	 * The maximum size of the cache that is used to consolidate lookups for different hostnames when in-flight.
	 * </p>
	 *
	 * <p>
	 * If multiple lookups are done for the same hostname and still in-flight only one actual query will be made and the result will be cascaded to the others.
	 * </p>
	 *
	 * <p>
	 * When set to {@code 0}, no consolidation is performed.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code 0}.
	 * </p>
	 *
	 * @return the maximum lookups to consolidate different hostnames
	 */
	int consolidate_cache_size();

	/**
	 * <p>
	 * Enables/Disables round-robin on resolved addresses when multiple ones are returned.
	 * </p>
	 *
	 * <p>
	 * When set to {@code true}, the resolver returns a random address from the list of resolved addresses.
	 * </p>
	 *
	 * @return true to enable round-robin on resolved addresses, false otherwise
	 */
	boolean round_robin_addresses();
}
