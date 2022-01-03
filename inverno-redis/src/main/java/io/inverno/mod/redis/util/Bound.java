/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.util;

import java.util.Objects;

/**
 *
 * @author jkuhn
 * @param <A>
 */
public class Bound<A> {

	private static final Bound<?> UNBOUNDED = new Bound<>(null, true);
	
	private final A value;
	private final boolean inclusive;
	
	private Bound(A value, boolean inclusive) {
		this.value = value;
		this.inclusive = inclusive;
	}
	
	public boolean isInclusive() {
		return this.inclusive;
	}
	
	public boolean isUnbounded() {
		return this.value == null;
	}
	
	public A getValue() {
		return this.value;
	}
	
	@SuppressWarnings("unchecked")
	public static <A> Bound<A> unbounded() {
		return (Bound<A>) UNBOUNDED;
	}
	
	public static <A> Bound<A> inclusive(A value) {
		Objects.requireNonNull(value);
		return new Bound<>(value, true);
	}
	
	public static <A> Bound<A> exclusive(A value) {
		Objects.requireNonNull(value);
		return new Bound<>(value, false);
	}
}
