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
package io.inverno.mod.base;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * A container object which may or may not contain a value which can be null. If a value has been set, {@link #isSet()} returns {@code true}. If not value has been set, the object is considered
 * <i>undefined</i> and {@code isUndefined()} returns {@code false}.
 * </p>
 *
 * <p>
 * Unlike {@link Optional} which is used to represent "no result": a value or {@code null}, a {@code Settable} is used to represent <i>undefined</i> values: a value, {@code null} or <i>undefined</i>.
 * </p>
 *
 * <p>
 * This basically allows to implement overriding logic where a base value can be left untouched or overridden with a new value or {@code null} depending on whether a corresponding {@code Settable} was
 * set or not.
 * </p>
 *
 * <p>
 * It should be used as return method type where there is a need to represent <i>undefined</i> values for which it is important to determine whether a value, including {@code null}, was set or not. A
 * variable whose type is {@code Settable} should never itself be null; it should always point to a {@code Settable} instance.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <T> the type of value
 */
public final class Settable<T> {

	private static final Settable<?> UNDEFINED = new Settable<>();

	private final boolean set;
	private final T value;

	/**
	 * <p>
	 * Creates an <i>undefined</i> settable.
	 * </p>
	 */
	private Settable() {
		this.set = false;
		this.value = null;
	}

	/**
	 * <p>
	 * Creates a "set" settable with the specified value.
	 * </p>
	 *
	 * @param value a value or {@code null}
	 */
	private Settable(T value) {
		this.set = true;
		this.value = value;
	}

	/**
	 * <p>
	 * If a value is set, returns the result of applying the given {@code Settable}-bearing mapping function to the value, otherwise returns an undefined {@code Settable}.
	 * </p>
	 *
	 * <p>
	 * This method is similar to {@link #map(Function)}, but the mapping function is one whose result is already an {@code Settable}, and if invoked, {@code flatMap} does not wrap it within an
	 * additional {@code Settable}.
	 * </p>
	 *
	 * @param <U>    the type of value of the {@code Settable} returned by the mapping function
	 * @param mapper the mapping function to apply to a value, if set
	 *
	 * @return the result of applying an {@code Settable}-bearing mapping function to the value of this {@code Settable}, if a value is set, otherwise an undefined {@code Settable}
	 *
	 * @throws NullPointerException if the mapping function is {@code null} or returns a {@code null} result
	 */
	@SuppressWarnings("unchecked")
	public <U> Settable<U> flatMap(Function<? super T, ? extends Settable<? extends U>> mapper) throws NullPointerException {
		Objects.requireNonNull(mapper);
		if(this.set) {
			return Objects.requireNonNull((Settable<U>)mapper.apply(this.value));
		}
		else {
			return Settable.undefined();
		}
	}

	/**
	 * <p>
	 * If a value is set, returns the value, otherwise throws {@code NoSuchElementException}.
	 * </p>
	 *
	 * <p>
	 * The preferred alternative to this method is {@link #orElseThrow()}.
	 * </p>
	 *
	 * @return the value described by this {@code Settable} which can be {@code null}
	 *
	 * @throws NoSuchElementException if no value is set
	 */
	public T get() throws NoSuchElementException {
		if(!this.set) {
			throw new NoSuchElementException("No value was set");
		}
		return this.value;
	}

	/**
	 * <p>
	 * If a value is set, performs the given action with the value, otherwise does nothing.
	 * </p>
	 *
	 * @param action the action to be performed, if a value is set
	 *
	 * @throws NullPointerException if value is set and the given action is {@code null}
	 */
	public void ifSet(Consumer<? super T> action) throws NullPointerException {
		if(this.set) {
			action.accept(this.value);
		}
	}

	/**
	 * <p>
	 * If a value is set, performs the given action with the value, otherwise performs the given empty-based action.
	 * </p>
	 *
	 * @param action          the action to be performed, if a value is set
	 * @param undefinedAction the empty-based action to be performed, if no value is set
	 *
	 * @throws NullPointerException if a value is set and the given action is {@code null}, or no value is set and the given empty-based action is {@code null}.
	 */
	public void ifSetOrElse(Consumer<? super T> action, Runnable undefinedAction) throws NullPointerException {
		if(this.set) {
			action.accept(this.value);
		}
		else {
			undefinedAction.run();
		}
	}

	/**
	 * <p>
	 * If a value is set, returns {@code true}, otherwise {@code false}.
	 * </p>
	 *
	 * @return {@code true} if a value is set, otherwise {@code false}
	 */
	public boolean isSet() {
		return this.set;
	}

	/**
	 * <p>
	 * If no value is set, returns {@code true}, otherwise {@code false}.
	 * </p>
	 *
	 * @return {@code true} if no value is set, otherwise {@code false}
	 */
	public boolean isUndefined() {
		return !this.set;
	}

	/**
	 * <p>
	 * If a value is set, returns an {@code Settable} describing the result of applying the given mapping function to the value, otherwise returns an undefined {@code Settable}.
	 * </p>
	 *
	 * <p>
	 * By the definition, the mapping function can return a {@code null} resulting in a {@code Settable} with a {@code null} value being returned.
	 * </p>
	 *
	 * @param <U>    the type of the value returned from the mapping function
	 * @param mapper the mapping function to apply to a value, if set
	 *
	 * @return a {@code Settable} describing the result of applying a mapping function to the value of this {@code Settable}, if a value is set, otherwise an empty {@code Settable}
	 *
	 * @throws NullPointerException if the mapping function is {@code null}
	 */
	public <U> Settable<U> map(Function<? super T, ? extends U> mapper) throws NullPointerException {
		Objects.requireNonNull(mapper);
		if(this.set) {
			return Settable.of(mapper.apply(this.value));
		}
		else {
			return Settable.undefined();
		}
	}

	/**
	 * <p>
	 * If a value is set, returns the value, otherwise returns {@code other}.
	 * </p>
	 *
	 * @param other the value to be returned, if no value is set. May be {@code null}.
	 *
	 * @return the value, if set, otherwise {@code other}
	 */
	public T orElse(T other) {
		return this.set ? this.value : other;
	}

	/**
	 * <p>
	 * If a value is set, returns the value, otherwise returns the result produced by the supplying function.
	 * </p>
	 *
	 * @param supplier the supplying function that produces a value to be returned
	 *
	 * @return the value, if set, otherwise the result produced by the supplying function
	 *
	 * @throws NullPointerException if no value is set and the supplying function is {@code null}
	 */
	public T orElseGet(Supplier<? extends T> supplier) throws NullPointerException {
		return this.set ? this.value : supplier.get();
	}

	/**
	 * <p>
	 * If a value is set, returns the value, otherwise throws {@code NoSuchElementException}.
	 * </p>
	 *
	 * @return the value described by this {@code Settable} which can be {@code null}
	 *
	 * @throws NoSuchElementException if no value is set
	 */
	public T orElseThrow() throws NoSuchElementException {
		if(!this.set) {
			throw new NoSuchElementException("No value was set");
		}
		return this.value;
	}

	/**
	 * <p>
	 * If a value is set, returns the value, otherwise throws an exception produced by the exception supplying function.
	 * </p>
	 *
	 * @param <X>               the type of the exception to be thrown
	 * @param exceptionSupplier the supplying function that produces an exception to be thrown
	 *
	 * @return the value described by this {@code Settable} which can be {@code null}
	 *
	 * @throws X                    if no value is set
	 * @throws NullPointerException if no value is set and the exception supplying function is {@code null}
	 */
	public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X, NullPointerException {
		if(!this.set) {
			throw exceptionSupplier.get();
		}
		return this.value;
	}

	/**
	 * <p>
	 * Converts the {@code Settable} to an {@code Optional}.
	 * </p>
	 *
	 * <p>
	 * The returned {@code Optional} is empty when the {@code Settable} is <i>undefined</i> or if its value is {@code null}.
	 * </p>
	 *
	 * @return an {@code Optional} with a present value if the {@code Settable} is set with a non-{@code null} value or an empty {@code Optional} if it is not set or set with a {@code null} value
	 */
	public Optional<T> toOptional() {
		return Optional.ofNullable(this.value);
	}

	/**
	 * <p>
	 * Returns an <i>undefined</i> {@code Settable} instance. No value is set for this {@code Settable}.
	 * </p>
	 *
	 * <p>
	 * Though it may be tempting to do so, avoid testing if an object is undefined by comparing with {@code ==} or {@code !=} against instances returned by {@code Settable.undefined()}. There is no
	 * guarantee that it is a singleton. Instead, use {@link #isUndefined()} or {@link #isSet()}.
	 * </p>
	 *
	 * @param <T> The type of the non-existent value
	 *
	 * @return an undefined {@code Settable}
	 */
	@SuppressWarnings("unchecked")
	public static <T> Settable<T> undefined() {
		return (Settable<T>)UNDEFINED;
	}

	/**
	 * <p>
	 * Returns an {@code Settable} describing the given value which can be {@code null}.
	 * </p>
	 *
	 * @param <T> the type of the value
	 * @param value the value to describe, which can be {@code null}
	 *
	 * @return a {@code Settable} with the value set
	 */
	public static <T> Settable<T> of(T value) {
		return new Settable<>(value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Settable<?> settable = (Settable<?>) o;
		return set == settable.set && Objects.equals(value, settable.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(set, value);
	}
}
