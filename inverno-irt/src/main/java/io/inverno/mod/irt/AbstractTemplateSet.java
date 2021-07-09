/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.irt;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * Base {@link TemplateSet} implementation.
 * </p>
 * 
 * <p>
 * This class especially provides general implementations for
 * <code>applyTemplate</code> methods which allow to apply templates on the
 * elements of an array, an iterable, a stream and a publisher while making sure
 * the rendering process is non-blocking following reactive programming
 * principles.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 *
 */
public abstract class AbstractTemplateSet implements TemplateSet {

	/**
	 * The charset to use to encode rendered data.
	 */
	protected final Charset charset;
	
	/**
	 * <p>Creates a template set.</p>
	 * 
	 * @param charset the charset to use to encode data
	 */
	public AbstractTemplateSet(Charset charset) {
		this.charset = charset;
	}

	@Override
	public CompletableFuture<Void> render(Object value) {
		if(value instanceof String) {
			return this.render((String)value);
		}
		else if(value instanceof ByteBuf) {
			return this.render((ByteBuf)value);
		}
		else {
			return this.render(value.toString());
		}
	}
	
	@Override
	public <T> Renderable<T> applyTemplate(T value) {
		return new GenericRenderable<>(value);
	}
	
	@Override
	public <T> IndexableRenderable<T> applyTemplate(T[] array) {
		return new ArrayRenderable<>(array);
	}
	
	@Override
	public <T> IndexableRenderable<T> applyTemplate(Publisher<T> publisher) {
		return new PublisherRenderable<>(publisher);
	}
	
	@Override
	public <T> IndexableRenderable<T> applyTemplate(Iterable<T> iterable) {
		return new IterableRenderable<>(iterable);
	}
	
	@Override
	public <T> IndexableRenderable<T> applyTemplate(Stream<T> stream) {
		return new StreamRenderable<>(stream);
	}
	
	/**
	 * <p>
	 * Generic {@link Renderable} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 * @param <T> The type of the value to render
	 */
	private static class GenericRenderable<T> implements Renderable<T> {

		/**
		 * The value to render.
		 */
		private final T value;
		
		/**
		 * <p>
		 * Creates a generic renderable.
		 * </p>
		 * 
		 * @param value the value to render
		 */
		public GenericRenderable(T value) {
			this.value = value;
		}
		
		@Override
		public CompletableFuture<Void> render(Function<T, CompletableFuture<Void>> renderer) {
			return renderer.apply(this.value);
		}
	}
	
	/**
	 * <p>
	 * An array {@link Renderable} implementation which iterates over an array and
	 * render elements in sequence.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 * @param <T> The type of the value to render
	 */
	private static class ArrayRenderable<T> implements IndexableRenderable<T> {

		/**
		 * The array of elements to render.
		 */
		private final T[] array;
		
		/**
		 * <p>
		 * Creates an array renderable.
		 * </p>
		 * 
		 * @param array the array of elements to render
		 */
		public ArrayRenderable(T[] array) {
			this.array = array;
		}
		
		@Override
		public CompletableFuture<Void> render(Function<T, CompletableFuture<Void>> renderer) {
			CompletableFuture<Void> future = null;
			for(T arg : this.array) {
				if(future == null) {
					future = renderer.apply(arg);
				}
				else {
					future.thenCompose(ign -> renderer.apply(arg));
				}
			}
			return future;
		}

		@Override
		public CompletableFuture<Void> render(BiFunction<Long, T, CompletableFuture<Void>> renderer) {
			CompletableFuture<Void> future = null;
			long i = 0;
			for(T arg : this.array) {
				final long index = i++;
				if(future == null) {
					future = renderer.apply(index, arg);
				}
				else {
					future.thenCompose(ign -> renderer.apply(index, arg));
				}
			}
			return future;
		}
	}
	
	/**
	 * <p>
	 * An iterable {@link Renderable} implementation which renders the elements in
	 * an iterable in sequence.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 * @param <T> The type of the value to render
	 */
	private static class IterableRenderable<T> implements IndexableRenderable<T> {

		/**
		 * The iterable of elements to render
		 */
		private final Iterable<T> iterable;
		
		/**
		 * <p>
		 * Creates an iterable renderable.
		 * </p>
		 * 
		 * @param iterable the iterable of elements to render
		 */
		public IterableRenderable(Iterable<T> iterable) {
			this.iterable = iterable;
		}
		
		@Override
		public CompletableFuture<Void> render(Function<T, CompletableFuture<Void>> renderer) {
			CompletableFuture<Void> future = null;
			for(T arg : this.iterable) {
				if(future == null) {
					future = renderer.apply(arg);
				}
				else {
					future.thenCompose(ign -> renderer.apply(arg));
				}
			}
			return future;
		}

		@Override
		public CompletableFuture<Void> render(BiFunction<Long, T, CompletableFuture<Void>> renderer) {
			CompletableFuture<Void> future = null;
			long i = 0;
			for(T arg : this.iterable) {
				final long index = i++;
				if(future == null) {
					future = renderer.apply(index, arg);
				}
				else {
					future.thenCompose(ign -> renderer.apply(index, arg));
				}
			}
			return future;
		}
	}
	
	/**
	 * <p>
	 * A stream {@link Renderable} implementation which renders the elements in a
	 * stream in sequence.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 * @param <T> The type of the value to render
	 */
	private static class StreamRenderable<T> implements IndexableRenderable<T> {

		/**
		 * The stream of elements to render.
		 */
		private final Stream<T> stream;
		
		/**
		 * <p>
		 * Creates an stream renderable.
		 * </p>
		 * 
		 * @param stream the stream of elements to render
		 */
		public StreamRenderable(Stream<T> stream) {
			this.stream = stream;
		}

		@Override
		public CompletableFuture<Void> render(Function<T, CompletableFuture<Void>> renderer) {
			CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
			this.stream.forEach(arg -> future.thenCompose(ign -> renderer.apply(arg)));
			return future;
		}

		@Override
		public CompletableFuture<Void> render(BiFunction<Long, T, CompletableFuture<Void>> renderer) {
			CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
			final AtomicLong index = new AtomicLong(0);
			this.stream.forEach(arg -> future.thenCompose(ign -> renderer.apply(index.getAndIncrement(), arg)));
			return future;
		}
	}

	/**
	 * <p>
	 * A publisher {@link Renderable} implementation which renders the elements
	 * emitted by a publisher in sequence.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 * @param <T> The type of the value to render
	 */
	private static class PublisherRenderable<T> extends CompletableFuture<Void> implements IndexableRenderable<T>, Subscriber<T> {

		/**
		 * The publisher of elements to render
		 */
		private final Publisher<T> publisher;
		
		/**
		 * The publisher's subscription
		 */
		private Subscription subscription;
		
		/**
		 * The renderer used to render elements
		 */
		private Function<T, CompletableFuture<Void>> renderer;

		/**
		 * The current element index
		 */
		private long index;
		/**
		 * The indexable renderer used to render indexed elements 
		 */
		private BiFunction<Long, T, CompletableFuture<Void>> indexableRenderer;
		
		/**
		 * <p>
		 * Creates a publisher renderable.
		 * </p>
		 * 
		 * @param publisher The publisher of elements to render
		 */
		public PublisherRenderable(Publisher<T> publisher) {
			this.publisher = publisher;
		}
		
		@Override
		public CompletableFuture<Void> render(Function<T, CompletableFuture<Void>> renderer) {
			this.renderer = renderer;
			this.publisher.subscribe(this);
			return this;
		}
		
		@Override
		public CompletableFuture<Void> render(BiFunction<Long, T, CompletableFuture<Void>> renderer) {
			this.index = 0;
			this.indexableRenderer = renderer;
			this.publisher.subscribe(this);
			return this;
		}
		
		@Override
		public void onSubscribe(Subscription s) {
			this.subscription = s;
			s.request(1);
		}

		@Override
		public void onNext(T t) {
			if(this.renderer != null) {
				this.renderer.apply(t).thenRun(() -> this.subscription.request(1));
			}
			else {
				this.indexableRenderer.apply(this.index++, t).thenRun(() -> this.subscription.request(1));
			}
		}

		@Override
		public void onError(Throwable t) {
		}

		@Override
		public void onComplete() {
			this.complete(null);
		}
	}
}
