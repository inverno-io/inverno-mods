/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis;

import java.util.stream.Stream;

/**
 *
 * @author jkuhn
 */
public interface RedisTransactionResult {

	/**
     * @return {@code true} if the transaction batch was discarded.
     * @since 5.1
     */
    boolean wasDiscarded();
	
	/**
     * Returns the number of elements in this collection. If this {@link TransactionResult} contains more than
     * {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
     *
     * @return the number of elements in this collection
     */
    int size();
	
	/**
     * Returns {@code true} if this {@link TransactionResult} contains no elements.
     *
     * @return {@code true} if this {@link TransactionResult} contains no elements
     */
    boolean isEmpty();
	
	/**
     * Returns the element at the specified position in this {@link TransactionResult}.
     *
     * @param index index of the element to return
     * @param <T> inferred type
     * @return the element at the specified position in this {@link TransactionResult}
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    <T> T get(int index);

    /**
     * Returns a sequential {@code Stream} with this {@link TransactionResult} as its source.
     *
     * @return a sequential {@code Stream} over the elements in this {@link TransactionResult}
     */
    Stream<Object> stream();
}
