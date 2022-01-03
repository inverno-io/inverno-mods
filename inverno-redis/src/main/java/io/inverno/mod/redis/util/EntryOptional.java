/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.redis.util;

import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author jkuhn
 * @param <A>
 * @param <B>
 */
public class EntryOptional<A, B> {
	
	private final A key;
		
	private final Optional<B> value;

	private EntryOptional(A key, B value) {
		Objects.requireNonNull(key);
		this.key = key;
		this.value = Optional.ofNullable(value);
	}

	public A getKey() {
		return this.key;
	}

	public Optional<B> getValue() {
		return this.value;
	}

	public static <A, B> EntryOptional<A, B> empty(A key) {
		return new EntryOptional<>(key, null);
	}

	public static <A, B> EntryOptional<A, B> of(A key, B value) {
		return new EntryOptional<>(key, Objects.requireNonNull(value));
	}

	public static <A, B> EntryOptional<A, B> ofNullable(A key, B value) {
		return new EntryOptional<>(key, value);
	}
}
