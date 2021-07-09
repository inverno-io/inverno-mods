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

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

/**
 * <p>
 * Template set definition which specifies the methods used in a generated
 * template set implementation to render objects in a procedural or reactive
 * way.
 * </p>
 * 
 * <p>
 * This interface is intended to be implemented by a template compiler
 * generating an implementation based on a template set file following an
 * appropriate grammar.
 * </p>
 * 
 * <p>
 * Such implementation must define template methods accepting zero or more
 * parameters and returning a {@link CompletableFuture} which completes once
 * input arguments are fully rendered to the output which is done using the
 * generic methods specified in this interface.
 * </p>
 * 
 * <p>
 * As a result, depending on the implementation, data can be rendered in
 * different ways as a string, an output stream or a stream of data which emits
 * events each time new rendered data are generated.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 * 
 * @see AbstractTemplateSet
 */
public interface TemplateSet {

	/**
	 * An empty completed future which returns immediately.
	 */
	static final CompletableFuture<Void> COMPLETED_FUTURE = CompletableFuture.completedFuture(null);
	
	/**
	 * <p>
	 * Renders an object to the output.
	 * </p>
	 * 
	 * <p>
	 * This method might invoke specialized methods based on the actual type of the
	 * object. If no specific method is found the string representation of the
	 * object must be rendered.
	 * </p>
	 * 
	 * @param value the object to render
	 * 
	 * @return a future which completes once the value is rendered
	 */
	CompletableFuture<Void> render(Object value);
	
	/**
	 * <p>
	 * Renders a string to the output.
	 * </p>
	 * 
	 * @param value the string to render
	 * 
	 * @return a future which completes once the value is rendered
	 */
	CompletableFuture<Void> render(String value);
	
	/**
	 * <p>
	 * Renders a byte array to the output.
	 * </p>
	 * 
	 * @param value The byte array to render
	 * 
	 * @return a future which completes once the value is rendered
	 */
	CompletableFuture<Void> render(byte[] value);
	
	/**
	 * <p>
	 * Invokes a template defined in the template set implementation on the
	 * specified object.
	 * </p>
	 * 
	 * @param <T>   the type of the object
	 * @param value the value onto which template should be applied
	 * 
	 * @return a renderable object which can invoke the template method matching the
	 *         object type
	 */
	<T> Renderable<T> applyTemplate(T value);

	/**
	 * <p>
	 * Invokes a template defined in the template set implementation on each element
	 * of the specified array.
	 * </p>
	 * 
	 * <p>
	 * Elements are guaranteed to be rendered in sequence.
	 * </p>
	 * 
	 * @param <T>   the type of the object
	 * @param array the array of elements onto which template should be applied
	 * 
	 * @return an indexable renderable object which can invoke the template method
	 *         matching the object type
	 */
	<T> IndexableRenderable<T> applyTemplate(T[] array);
	
	/**
	 * <p>
	 * Invokes a template defined in the template set implementation on each element
	 * of the specified iterable.
	 * </p>
	 * 
	 * <p>
	 * Elements are guaranteed to be rendered in sequence.
	 * </p>
	 * 
	 * @param <T>      the type of the object
	 * @param iterable the iterable providing the elements onto which template
	 *                 should be applied
	 * 
	 * @return an indexable renderable object which can invoke the template method
	 *         matching the object type
	 */
	<T> IndexableRenderable<T> applyTemplate(Iterable<T> iterable);
	
	/**
	 * <p>
	 * Invokes a template defined in the template set implementation on each element
	 * of the specified stream.
	 * </p>
	 * 
	 * <p>
	 * Elements are guaranteed to be rendered in sequence.
	 * </p>
	 * 
	 * @param <T>    the type of the object
	 * @param stream the stream of elements onto which template should be applied
	 * 
	 * @return an indexable renderable object which can invoke the template method
	 *         matching the object type
	 */
	<T> IndexableRenderable<T> applyTemplate(Stream<T> stream);
	
	/**
	 * <p>
	 * Invokes a template defined in the template set implementation on each element
	 * emitted in the specified publisher until the publisher completes.
	 * </p>
	 * 
	 * <p>
	 * Elements are guaranteed to be rendered in sequence.
	 * </p>
	 * 
	 * @param <T>       the type of the object
	 * @param publisher the stream of elements onto which template should be applied
	 * 
	 * @return an indexable renderable object which can invoke the template method
	 *         matching the object type
	 */
	<T> IndexableRenderable<T> applyTemplate(Publisher<T> publisher);
	
	/**
	 * <p>
	 * The root template method which basically invokes the {@link #render(Object)}
	 * method.
	 * </p>
	 * 
	 * @param value the value onto which the template is applied
	 * 
	 * @return a future which completes once the value is rendered
	 */
	default CompletableFuture<Void> template(Object value) {
		return this.render(value);
	}
	
	/**
	 * <p>
	 * A renderable is used to render a value with a particular type of object
	 * typically by invoking the corresponding template in a TemplateSet
	 * implementation.
	 * </p>
	 * 
	 * <p>
	 * This interface enables the compiler to select the template method based on
	 * the actual type of object to render. It especially allows to transform an
	 * object to render using {@link Pipe} before it is submitted to a template
	 * method.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 *
	 * @param <T> The type of the value to render
	 */
	static interface Renderable<T> {
		
		/**
		 * <p>
		 * Renders a value using the specified renderer.
		 * </p>
		 * 
		 * @param renderer the renderer to invoke
		 * 
		 * @return a future which completes once the renderer is done rendering data to the output
		 */
		CompletableFuture<Void> render(Function<T, CompletableFuture<Void>> renderer);
	}
	
	/**
	 * <p>
	 * A particular {@link Renderable} with the ability to expose the index of the
	 * value in a group of elements to the renderer (eg. an array, an iterable, a
	 * stream, a publisher...).
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 * 
	 * @param <T> the type of the value to render
	 */
	static interface IndexableRenderable<T> extends Renderable<T> {
		
		/**
		 * <p>
		 * Renders a value using the specified renderer accepting the value and its index.
		 * </p>
		 * 
		 * @param renderer the renderer to invoke
		 * 
		 * @return a future which completes once the renderer is done rendering data to the output
		 */
		CompletableFuture<Void> render(BiFunction<Long, T, CompletableFuture<Void>> renderer);
	}
}
