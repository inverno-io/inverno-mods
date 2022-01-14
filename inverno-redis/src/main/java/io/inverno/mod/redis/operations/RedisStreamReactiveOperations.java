/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.redis.operations;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis Streams reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisStreamReactiveOperations<A, B> {

	/**
	 * <a href="https://redis.io/commands/xack">XACK</a> key group id
	 * 
	 * @param key
	 * @param group
	 * @param messageId
	 * @return 
	 */
	Mono<Long> xack(A key, A group, String messageId);
	
	/**
	 * <a href="https://redis.io/commands/xack">XACK</a> key group id [id ...]
	 * 
	 * @param key
	 * @param group
	 * @param messageIds
	 * @return 
	 */
	Mono<Long> xack(A key, A group, Consumer<StreamMessageIds> messageIds);
	
	/**
	 * <a href="https://redis.io/commands/xadd">XADD</a> key * field value
	 * 
	 * @param key
	 * @param field
	 * @param value
	 * @return 
	 */
	Mono<String> xadd(A key, A field, B value);
	
	/**
	 * <a href="https://redis.io/commands/xadd">XADD</a> key * field value [field value ...] 
	 * 
	 * @param key
	 * @param entries
	 * @return 
	 */
	Mono<String> xadd(A key, Consumer<StreamEntries<A, B>> entries);
	
	/**
	 * <a href="https://redis.io/commands/xadd">XADD</a> key [NOMKSTREAM] [MAXLEN|MINID [=|~] threshold [LIMIT count]] *|id field value [field value ...]
	 * 
	 * @return 
	 */
	StreamXaddBuilder<A, B> xadd();
	
	/**
	 * <a href="https://redis.io/commands/xautoclaim">XAUTOCLAIM</a> key group consumer min-idle-time start
	 * 
	 * @param key
	 * @param group
	 * @param consumer
	 * @param minIdleTime
	 * @param start
	 * @return 
	 */
	Mono<StreamClaimedMessages<A, B>> xautoclaim(A key, A group, A consumer, long minIdleTime, String start);
	
	/**
	 * <a href="https://redis.io/commands/xautoclaim">XAUTOCLAIM</a> key group consumer min-idle-time start [COUNT count] [JUSTID] 
	 * 
	 * @return 
	 */
	StreamXautoclaimBuilder<A, B> xautoclaim();
	
	/**
	 * <a href="https://redis.io/commands/xclaim">XCLAIM</a> key group consumer min-idle-time id
	 * 
	 * @param key
	 * @param group
	 * @param consumer
	 * @param messageId
	 * @param minIdleTime
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xclaim(A key, A group, A consumer, long minIdleTime, String messageId);
	
	/**
	 * <a href="https://redis.io/commands/xclaim">XCLAIM</a> key group consumer min-idle-time id [id ...]
	 * 
	 * @param key
	 * @param group
	 * @param consumer
	 * @param minIdleTime
	 * @param messageIds
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xclaim(A key, A group, A consumer, long minIdleTime, Consumer<StreamMessageIds> messageIds);
	
	/**
	 * <a href="https://redis.io/commands/xclaim">XCLAIM</a> key group consumer min-idle-time id [id ...] [IDLE ms] [TIME ms-unix-time] [RETRYCOUNT count] [FORCE] [JUSTID] 
	 * 
	 * @return
	 */
	StreamXclaimBuilder<A, B> xclaim();
	
	/**
	 * <a href="https://redis.io/commands/xdel">XDEL</a> key id
	 * 
	 * @param key
	 * @param messageId
	 * @return
	 */
	Mono<Long> xdel(A key, String messageId);
	
	/**
	 * <a href="https://redis.io/commands/xdel">XDEL</a> key id [id ...]
	 * 
	 * @param key
	 * @param messageIds
	 * @return
	 */
	Mono<Long> xdel(A key, Consumer<StreamMessageIds> messageIds);
	
	
	/**
	 * <a href="https://redis.io/commands/xgroup-create">XGROUP CREATE</a> key groupname id|$
	 * 
	 * @param key
	 * @param group
	 * @param id
	 * @return 
	 */
	Mono<String> xgroupCreate(A key, A group, String id);
	
	/**
	 * <a href="https://redis.io/commands/xgroup-create">XGROUP CREATE</a> key groupname id|$ [MKSTREAM] 
	 * 
	 * @return 
	 */
	StreamXgroupCreateBuilder<A> xgroupCreate();

	/**
	 * <a href="https://redis.io/commands/xgroup-createconsumer">XGROUP CREATECONSUMER</a> key groupname consumername 
	 * 
	 * @param key
	 * @param group
	 * @param consumer
	 * @return 
	 */
	Mono<Boolean> xgroupCreateconsumer(A key, A group, A consumer);
	
	/**
	 * <a href="https://redis.io/commands/xgroup-delconsumer">XGROUP DELCONSUMER</a> key groupname consumername 
	 * 
	 * @param key
	 * @param group
	 * @param consumer
	 * @return 
	 */
	Mono<Long> xgroupDelconsumer(A key, A group, A consumer);
	
	/**
	 * <a href="https://redis.io/commands/xgroup-destroy">XGROUP DESTROY</a> key groupname
	 * 
	 * @param key
	 * @param group
	 * @return 
	 */
	Mono<Boolean> xgroupDestroy(A key, A group);
	
	/**
	 * <a href="https://redis.io/commands/xgroup-setid">XGROUP SETID</a> key groupname id|$ 
	 * 
	 * @param key
	 * @param group
	 * @param id
	 * @return 
	 */
	Mono<String> xgroupSetid(A key, A group, String id);
	
	/**
	 * <a href="https://redis.io/commands/xinfo-consumers">XINFO CONSUMERS</a> key groupname
	 * 
	 * @param key
	 * @param group
	 * @return 
	 */
	Flux<Object> xinfoConsumers(A key, A group);

	/**
	 * <a href="https://redis.io/commands/xinfo-groups">XINFO GROUPS</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Flux<Object> xinfoGroups(A key);

	/**
	 * <a href="https://redis.io/commands/xinfo-stream">XINFO STREAM</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Flux<Object> xinfoStream(A key);
	
	/**
	 * <a href="https://redis.io/commands/xinfo-stream">XINFO STREAM</a> key FULL
	 * 
	 * @param key
	 * @return 
	 */
	Flux<Object> xinfoStreamFull(A key);
	
	/**
	 * <a href="https://redis.io/commands/xinfo-stream">XINFO STREAM</a> key FULL COUNT count
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<Object> xinfoStreamFull(A key, long count);

	/**
	 * <a href="https://redis.io/commands/xlen">XLEN</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> xlen(A key);
	
	/**
	 * <a href="https://redis.io/commands/xpending">XPENDING</a> key group [[IDLE min-idle-time] start end count [consumer]]
	 * 
	 * @param key
	 * @param group
	 * @return 
	 */
	Mono<StreamPendingMessages> xpending(A key, A group);
	
	/**
	 * <a href="https://redis.io/commands/xpending">XPENDING</a> key group [[IDLE min-idle-time] start end count [consumer]]
	 * 
	 * @param key
	 * @param group
	 * @param start
	 * @param end
	 * @param count
	 * @return 
	 */
	Flux<StreamPendingMessage> xpendingExtended(A key, A group, String start, String end, long count);
	
	/**
	 * <a href="https://redis.io/commands/xpending">XPENDING</a> key group [[IDLE min-idle-time] start end count [consumer]]
	 * 
	 * @return 
	 */
	StreamXpendingExtendedBuilder<A> xpendingExtended();
	
	
	/**
	 * <a href="https://redis.io/commands/xrange">XRANGE</a> key start end
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xrange(A key, String start, String end);

	/**
	 * <a href="https://redis.io/commands/xrange">XRANGE</a> key start end [COUNT count]
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @param count
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xrange(A key, String start, String end, long count);

	/**
	 * <a href="https://redis.io/commands/xread">XREAD</a> STREAMS key id
	 * 
	 * @param key
	 * @param messageId
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xread(A key, String messageId);

	/**
	 * <a href="https://redis.io/commands/xread">XREAD</a> STREAMS key [key ...] id [id ...] 
	 * 
	 * @param streams
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xread(Consumer<StreamStreams<A>> streams);
	
	/**
	 * <a href="https://redis.io/commands/xread">XREAD</a> [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] id [id ...] 
	 * 
	 * @return 
	 */
	StreamXreadBuilder<A, B> xread();
	
	/**
	 * <a href="https://redis.io/commands/xreadgroup">XREADGROUP</a> GROUP group consumer STREAMS key id
	 * 
	 * @param group
	 * @param consumer
	 * @param key
	 * @param messageId
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xreadgroup(A group, A consumer, A key, String messageId);
	
	/**
	 * <a href="https://redis.io/commands/xreadgroup">XREADGROUP</a> GROUP group consumer STREAMS key [key ...] id [id ...] 
	 * 
	 * @param group
	 * @param consumer
	 * @param streams
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xreadgroup(A group, A consumer, Consumer<StreamStreams<A>> streams);
	
	/**
	 * <a href="https://redis.io/commands/xreadgroup">XREADGROUP</a> GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] id [id ...] 
	 * 
	 * @return 
	 */
	StreamXreadgroupBuilder<A, B> xreadgroup();
	
	/**
	 * <a href="https://redis.io/commands/xrevrange">XREVRANGE</a> key end start
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xrevrange(A key, String start, String end);
	
	/**
	 * <a href="https://redis.io/commands/xrevrange">XREVRANGE</a> key end start COUNT count
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @param count
	 * @return 
	 */
	Flux<StreamMessage<A, B>> xrevrange(A key, String start, String end, long count);
	
	/**
	 * <a href="https://redis.io/commands/xtrim">XTRIM</a> key MAXLEN threshold 
	 * 
	 * @param key
	 * @param threshold
	 * @return 
	 */
	Mono<Long> xtrimMaxLen(A key, long threshold);
	
	/**
	 * <a href="https://redis.io/commands/xtrim">XTRIM</a> key MAXLEN threshold LIMIT count
	 * 
	 * @param key
	 * @param threshold
	 * @param count
	 * @return 
	 */
	Mono<Long> xtrimMaxLen(A key, long threshold, long count);
	
	/**
	 * <a href="https://redis.io/commands/xtrim">XTRIM</a> key MINID streamId
	 * 
	 * @param key
	 * @param streamId
	 * @return 
	 */
	Mono<Long> xtrimMinId(A key, String streamId);
	
	/**
	 * <a href="https://redis.io/commands/xtrim">XTRIM</a> key MINID threshold LIMIT count
	 * 
	 * @param key
	 * @param streamId
	 * @param count
	 * @return 
	 */
	Mono<Long> xtrimMinId(A key, String streamId, long count);
	
	/**
	 * <a href="https://redis.io/commands/xtrim">XTRIM</a> key MAXLEN|MINID [=|~] threshold [LIMIT count]
	 * 
	 * @return 
	 */
	StreamXtrimBuilder<A> xtrim();
	
	/**
	 * <a href="https://redis.io/commands/xadd">XADD</a> key [NOMKSTREAM] [MAXLEN|MINID [=|~] threshold [LIMIT count]] *|id field value [field value ...]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StreamXaddBuilder<A, B> {
		
		/**
		 * 
		 * @return 
		 */
		StreamXaddBuilder<A, B> nomkstream();
		
		/**
		 * 
		 * @param threshold
		 * @return 
		 */
		StreamXaddBuilder<A, B> maxlen(long threshold);
		
		/**
		 * 
		 * @param minid
		 * @return 
		 */
		StreamXaddBuilder<A, B> minid(String minid);
		
		/**
		 * 
		 * @return 
		 */
		StreamXaddBuilder<A, B> exact();
		
		/**
		 * 
		 * @return 
		 */
		StreamXaddBuilder<A, B> approximate();
		
		/**
		 * 
		 * @param limit
		 * @return 
		 */
		StreamXaddBuilder<A, B> limit(long limit);
		
		/**
		 * 
		 * @param id
		 * @return 
		 */
		StreamXaddBuilder<A, B> id(String id);
		
		/**
		 * 
		 * @param key
		 * @param field
		 * @param value
		 * @return 
		 */
		Mono<String> build(A key, A field, B value);
		
		/**
		 * 
		 * @param key
		 * @param entries
		 * @return 
		 */
		Mono<String> build(A key, Consumer<StreamEntries<A, B>> entries);
	}
	
	/**
	 * <a href="https://redis.io/commands/xautoclaim">XAUTOCLAIM</a> key group consumer min-idle-time start [COUNT count] [JUSTID] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StreamXautoclaimBuilder<A, B> {	
		
		/**
		 * 
		 * @return 
		 */
		StreamXautoclaimBuilder<A, B> justid();
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		StreamXautoclaimBuilder<A, B> count(long count);
		
		/**
		 * 
		 * @param key
		 * @param group
		 * @param consumer
		 * @param minIdleTime
		 * @param start
		 * @return 
		 */
		Mono<StreamClaimedMessages<A, B>> build(A key, A group, A consumer, long minIdleTime, String start);
	}
	
	/**
	 * <a href="https://redis.io/commands/xclaim">XCLAIM</a> key group consumer min-idle-time id [id ...] [IDLE ms] [TIME ms-unix-time] [RETRYCOUNT count] [FORCE] [JUSTID] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StreamXclaimBuilder<A, B> {
		
		/**
		 * 
		 * @param ms
		 * @return 
		 */
		StreamXclaimBuilder<A, B> idle(long ms);
		
		/**
		 * 
		 * @param msUnixTime
		 * @return 
		 */
		StreamXclaimBuilder<A, B> time(long msUnixTime);
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		StreamXclaimBuilder<A, B> retrycount(long count);
		
		/**
		 * 
		 * @return 
		 */
		StreamXclaimBuilder<A, B> force();
		
		/**
		 * 
		 * @return 
		 */
		StreamXclaimBuilder<A, B> justid();
		
		/**
		 * 
		 * @param key
		 * @param group
		 * @param consumer
		 * @param minIdleTime
		 * @param messageId
		 * @return 
		 */
		Flux<StreamMessage<A, B>> build(A key, A group, A consumer, long minIdleTime, String messageId);
		
		/**
		 * 
		 * @param key
		 * @param group
		 * @param consumer
		 * @param minIdleTime
		 * @param messageIds
		 * @return 
		 */
		Flux<StreamMessage<A, B>> build(A key, A group, A consumer, long minIdleTime, Consumer<StreamMessageIds> messageIds);
	}
	
	/**
	 * <a href="https://redis.io/commands/xgroup-create">XGROUP CREATE</a> key groupname id|$ [MKSTREAM]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface StreamXgroupCreateBuilder<A> {	
		
		/**
		 * 
		 * @return 
		 */
		StreamXgroupCreateBuilder<A> mkstream();
		
		/**
		 * 
		 * @param key
		 * @param groupname
		 * @param id
		 * @return 
		 */
		Mono<String> build(A key, A groupname, String id);
	}
	
	/**
	 * <a href="https://redis.io/commands/xpending">XPENDING</a> key group [IDLE min-idle-time] start end count [consumer]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface StreamXpendingExtendedBuilder<A> {	
		
		/**
		 * 
		 * @param minIdleTime
		 * @return 
		 */
		StreamXpendingExtendedBuilder<A> idle(long minIdleTime);
		
		/**
		 * 
		 * @param consumer
		 * @return 
		 */
		StreamXpendingExtendedBuilder<A> consumer(A consumer);
		
		/**
		 * 
		 * @param key
		 * @param group
		 * @param start
		 * @param end
		 * @param count
		 * @return 
		 */
		Flux<StreamPendingMessage> build(A key, A group, String start, String end, long count);
	}
	
	/**
	 * <a href="https://redis.io/commands/xread">XREAD</a> [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] id [id ...] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StreamXreadBuilder<A, B> {	
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		StreamXreadBuilder<A, B> count(long count);
		
		/**
		 * 
		 * @param milliseconds
		 * @return 
		 */
		StreamXreadBuilder<A, B> block(long milliseconds);
		
		/**
		 * 
		 * @param key
		 * @param messageId
		 * @return 
		 */
		Flux<StreamMessage<A, B>> build(A key, String messageId);
		
		/**
		 * 
		 * @param streams
		 * @return 
		 */
		Flux<StreamMessage<A, B>> build(Consumer<StreamStreams<A>> streams);
	}
	
	/**
	 * <a href="https://redis.io/commands/xreadgroup">XREADGROUP</a> GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] id [id ...] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StreamXreadgroupBuilder<A, B> {
		
		/**
		 * 
		 * @param count
		 * @return 
		 */
		StreamXreadgroupBuilder<A, B> count(long count);
		
		/**
		 * 
		 * @param milliseconds
		 * @return 
		 */
		StreamXreadgroupBuilder<A, B> block(long milliseconds);
		
		/**
		 * 
		 * @return 
		 */
		StreamXreadgroupBuilder<A, B> noack();
		
		/**
		 * 
		 * @param group
		 * @param consumer
		 * @param key
		 * @param messageId
		 * @return 
		 */
		Flux<StreamMessage<A, B>> build(A group, A consumer, A key, String messageId);
		
		/**
		 * 
		 * @param group
		 * @param consumer
		 * @param streams
		 * @return 
		 */
		Flux<StreamMessage<A, B>> build(A group, A consumer, Consumer<StreamStreams<A>> streams);
	}
	
	/**
	 * <a href="https://redis.io/commands/xtrim">XTRIM</a> key MAXLEN|MINID [=|~] threshold [LIMIT count]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface StreamXtrimBuilder<A> {
		
		/**
		 * 
		 * @param threshold
		 * @return 
		 */
		StreamXtrimBuilder<A> maxlen(long threshold);
		
		/**
		 * 
		 * @param streamId
		 * @return 
		 */
		StreamXtrimBuilder<A> minid(String streamId);
		
		/**
		 * 
		 * @return 
		 */
		StreamXtrimBuilder<A> exact();
		
		/**
		 * 
		 * @return 
		 */
		StreamXtrimBuilder<A> approximate();
		
		/**
		 * 
		 * @param limit
		 * @return 
		 */
		StreamXtrimBuilder<A> limit(long limit);

		/**
		 * 
		 * @param key
		 * @return 
		 */
		Mono<Long> build(A key);
	}

	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StreamClaimedMessages<A, B> {
		
		/**
		 * 
		 * @return 
		 */
		String getStreamId();
		
		/**
		 * 
		 * @return 
		 */
		List<StreamMessage<A, B>> getMessages();
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StreamMessage<A, B> {
		
		/**
		 * 
		 * @return 
		 */
		String getId();
		
		/**
		 * 
		 * @return 
		 */
		Map<A, B> getEntries();
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface StreamPendingMessages {
		
		/**
		 * 
		 * @return 
		 */
		long getCount();
		
		/**
		 * 
		 * @return 
		 */
		String getLowerMessageId();
		
		/**
		 * 
		 * @return 
		 */
		String getUpperMessageId();

		/**
		 * 
		 * @return 
		 */
		Map<String, Long> getConsumerCount();
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface StreamPendingMessage {
		
		/**
		 * 
		 * @return 
		 */
		String getId();

		/**
		 * 
		 * @return 
		 */
		String getConsumer();

		/**
		 * 
		 * @return 
		 */
		long getMsSinceLastDelivery();

		/**
		 * 
		 * @return 
		 */
		long getRedeliveryCount();
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	interface StreamMessageIds {
		
		/**
		 * 
		 * @param id
		 * @return 
		 */
		StreamMessageIds id(String id);
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StreamEntries<A, B> {
		
		/**
		 * 
		 * @param field
		 * @param value
		 * @return 
		 */
		StreamEntries<A, B> entry(A field, B value);
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface StreamStreams<A> {
		
		/**
		 * 
		 * @param key
		 * @param messageId
		 * @return 
		 */
		StreamStreams<A> stream(A key, String messageId);
	}
}
