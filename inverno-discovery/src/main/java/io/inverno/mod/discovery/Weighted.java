package io.inverno.mod.discovery;

import java.util.Collection;

/**
 * <p>
 * A weighted element defining a weight
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface Weighted {

	/**
	 * <p>
	 * Returns the weight of the weighted element in a collection of weighted elements.
	 * </p>
	 *
	 * @return a strictly positive integer
	 */
	int getWeight();

	/**
	 * <p>
	 * Returns a weighted collection of weighted elements for load balancing.
	 * </p>
	 *
	 * <p>
	 * The returned collection reflects the weight of the specified weighted elements. For instance, if we have 3 elements A, B, C as input with weights 2, 4, 6 respectively the returned
	 * collection contains exactly 1 A instance, 2 B instances and 3 C instances.
	 * </p>
	 *
	 * @param <A>              the type of the weighted elements
	 * @param weightedElements a list of weighted elements
	 *
	 * @return a weighted collection of elements.
	 */
	static <A extends Weighted> Collection<A> expandToLoadBalanced(Collection<A> weightedElements) {
		return WeightedUtils.expandToLoadBalanced(weightedElements);
	}
}
