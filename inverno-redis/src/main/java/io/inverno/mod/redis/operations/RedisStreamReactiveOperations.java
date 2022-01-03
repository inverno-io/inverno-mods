/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.operations;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * @author jkuhn
 * @param <A>
 * @param <B>
 */
public interface RedisStreamReactiveOperations<A, B> /*extends RedisStreamReactiveCommands<A, B>*/ {

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
	StreamGroupCreateBuilder<A> xgroupCreate();

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
	 * @param <A>
	 * @param <B> 
	 */
	interface StreamXaddBuilder<A, B> {	
		StreamXaddBuilder<A, B> nomkstream();
		StreamXaddBuilder<A, B> maxlen(long threshold);
		StreamXaddBuilder<A, B> minid(String streamId);
		StreamXaddBuilder<A, B> exact();
		StreamXaddBuilder<A, B> approximate();
		StreamXaddBuilder<A, B> limit(long limit);
		
		Mono<String> build(A key, A field, B value);
		Mono<String> build(A key, Consumer<StreamEntries<A, B>> entries);
	}
	
	/**
	 * <a href="https://redis.io/commands/xautoclaim">XAUTOCLAIM</a> key group consumer min-idle-time start [COUNT count] [JUSTID] 
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface StreamXautoclaimBuilder<A, B> {	
		
		StreamXautoclaimBuilder<A, B> justid();
		StreamXautoclaimBuilder<A, B> count(long count);
		
		Mono<StreamClaimedMessages<A, B>> build(A key, A group, A consumer, long minIdleTime, String start);
	}
	
	/**
	 * <a href="https://redis.io/commands/xclaim">XCLAIM</a> key group consumer min-idle-time id [id ...] [IDLE ms] [TIME ms-unix-time] [RETRYCOUNT count] [FORCE] [JUSTID] 
	 * 
	 * @param <A>
	 * @param <B>
	 */
	interface StreamXclaimBuilder<A, B> {
		
		StreamXclaimBuilder<A, B> idle(long ms);
		StreamXclaimBuilder<A, B> time(long msUnixTime);
		StreamXclaimBuilder<A, B> retrycount(long count);
		StreamXclaimBuilder<A, B> force();
		StreamXclaimBuilder<A, B> justid();
		
		Flux<StreamMessage<A, B>> build(A key, A group, A consumer, long minIdleTime, String messageId);
		
		Flux<StreamMessage<A, B>> build(A key, A group, A consumer, long minIdleTime, Consumer<StreamMessageIds> messageIds);
	}
	
	/**
	 * <a href="https://redis.io/commands/xgroup-create">XGROUP CREATE</a> key groupname id|$ [MKSTREAM]
	 * 
	 * @param <A>
	 */
	interface StreamGroupCreateBuilder<A> {	
		
		StreamGroupCreateBuilder<A> mkstream();
		
		Mono<String> build(A key, String groupname, String id);
	}
	
	/**
	 * <a href="https://redis.io/commands/xpending">XPENDING</a> key group [IDLE min-idle-time] start end count [consumer]
	 * 
	 * @param <A>
	 */
	interface StreamXpendingExtendedBuilder<A> {	
		
		StreamXpendingExtendedBuilder<A> idle(long minIdleTime);
		
		StreamXpendingExtendedBuilder<A> consumer(A consumer);
		
		Flux<StreamPendingMessage> build(A key, A group, String start, String end, long count);
	}
	
	/**
	 * <a href="https://redis.io/commands/xread">XREAD</a> [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] id [id ...] 
	 * 
	 * @param <A>
	 * @param <B>
	 */
	interface StreamXreadBuilder<A, B> {	
		
		StreamXreadBuilder<A, B> count(long count);
		
		StreamXreadBuilder<A, B> block(long milliseconds);
		
		Flux<StreamMessage<A, B>> build(A key, String messageId);
		
		Flux<StreamMessage<A, B>> build(Consumer<StreamStreams<A>> streams);
	}
	
	/**
	 * <a href="https://redis.io/commands/xreadgroup">XREADGROUP</a> GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] id [id ...] 
	 * 
	 * @param <A>
	 * @param <B>
	 */
	interface StreamXreadgroupBuilder<A, B> {
		
		StreamXreadgroupBuilder<A, B> count(long count);
		
		StreamXreadgroupBuilder<A, B> block(long milliseconds);
		
		StreamXreadgroupBuilder<A, B> noack();
		
		Flux<StreamMessage<A, B>> build(A group, A consumer, A key, String messageId);
		
		Flux<StreamMessage<A, B>> build(A group, A consumer, Consumer<StreamStreams<A>> streams);
	}
	
	/**
	 * <a href="https://redis.io/commands/xtrim">XTRIM</a> key MAXLEN|MINID [=|~] threshold [LIMIT count]
	 * 
	 * @param <A>
	 */
	interface StreamXtrimBuilder<A> {
		
		StreamXtrimBuilder<A> maxlen(long threshold);
		StreamXtrimBuilder<A> minid(String streamId);
		StreamXtrimBuilder<A> exact();
		StreamXtrimBuilder<A> approximate();
		StreamXtrimBuilder<A> limit(long limit);

		Mono<Long> build(A key);
	}

	interface StreamClaimedMessages<A, B> {
		
		String getStreamId();
		
		List<StreamMessage<A, B>> getMessages();
	}
	
	interface StreamMessage<A, B> {
		
		String getId();
		
		Map<A, B> getEntries();
	}
	
	interface StreamPendingMessages {
		
		long getCount();
		
		String getLowerMessageId();
		
		String getUpperMessageId();

		Map<String, Long> getConsumerCount();
	}
	
	interface StreamPendingMessage {
		
		String getId();

		String getConsumer();

		long getMsSinceLastDelivery();

		long getRedeliveryCount();
	}
	
	interface StreamMessageIds {
		StreamMessageIds id(String id);
	}
	
	interface StreamEntries<A, B> {
		StreamEntries<A, B> entry(A field, B value);
	}
	
	interface StreamStreams<A> {
		
		StreamStreams<A> stream(A key, String messageId);
	}
}
