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
package io.inverno.mod.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * Utilities to create weighted element collections used in weighted load balancers.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WeightedRandomTrafficLoadBalancer
 * @see WeightedRoundRobinTrafficLoadBalancer
 */
final class WeightedUtils {

	private WeightedUtils() {}

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
	 * @param <A>             the type of the weighted elements
	 * @param weightedElement a list of weighted elements
	 *
	 * @return a weighted collection of elements.
	 */
	public static <A extends Weighted> Collection<A> expandToLoadBalanced(Collection<A> weightedElement) {
		int[] weights = new int[weightedElement.size()];
		Iterator<A> weightedInstancesIterator = weightedElement.iterator();
		for(int i = 0;i < weights.length; i++) {
			weights[i] = weightedInstancesIterator.next().getWeight();
			if(weights[i] <= 0) {
				throw new IllegalArgumentException("weight must be a positive integer");
			}
			else if(weights[i] == 1) {
				List<A> instances = new ArrayList<>();
				for(A instance : weightedElement) {
					for(int j=0;j<instance.getWeight();j++) {
						instances.add(instance);
					}
				}
				Collections.shuffle(instances, ThreadLocalRandom.current());
				return instances;
			}
		}

		int[] sanitizedWeights = sanitizeWeights(weightedElement.stream().mapToInt(Weighted::getWeight).toArray());
		List<A> instances = new ArrayList<>();
		Iterator<A> instancesIterator = weightedElement.iterator();
		for(int sanitizedWeight : sanitizedWeights) {
			A instance = instancesIterator.next();
			for(int i = 0; i < sanitizedWeight; i++) {
				instances.add(instance);
			}
		}
		Collections.shuffle(instances, ThreadLocalRandom.current());
		return instances;
	}

	/**
	 * <p>
	 * Sanitizes the specified array of weights.
	 * </p>
	 *
	 * <p>
	 * This basically considers the prime factors of each weight value and returns the smallest list of integer that respect initial proportions.
	 * </p>
	 *
	 * @param weights an array of weights
	 *
	 * @return a sanitized array of weights
	 */
	private static int[] sanitizeWeights(int[] weights) {
		Map<Integer, PrimeFactorPowers> primeFactors = getPrimeFactors(weights);
		int[] sanitizedWeights = new int[weights.length];
		for(Map.Entry<Integer, PrimeFactorPowers> e : primeFactors.entrySet()) {
			for(int i=0;i<weights.length;i++) {
				Integer power = e.getValue().get(i);
				if(power != null) {
					if(sanitizedWeights[i] == 0) {
						sanitizedWeights[i] = 1;
					}
					if(power > 0) {
						sanitizedWeights[i] *= (int) Math.pow(e.getKey(), power);
					}
				}
			}
		}
		return sanitizedWeights;
	}

	/**
	 * <p>
	 * Returns the prime factors for the specified array of weights.
	 * </p>
	 *
	 * @param weights an array of weights
	 *
	 * @return a map with the prime factor as key and the corresponding array of powers for each weights
	 */
	private static Map<Integer, PrimeFactorPowers> getPrimeFactors(int[] weights) {
		Map<Integer, PrimeFactorPowers> primeFactorsMap = new HashMap<>();
		for(int i=0;i<weights.length;i++) {
			for(int factor = 2; factor <= weights[i]; factor++) {
				int power = 0;
				while(weights[i] % factor == 0) {
					power++;
					weights[i] /= factor;
				}
				if(power > 0) {
					primeFactorsMap.computeIfAbsent(factor, ign -> new PrimeFactorPowers(weights.length)).set(i, power);
				}
			}
		}
		return primeFactorsMap;
	}

	/**
	 * <p>
	 * Represents the prime factor powers for each weight identified by its index in the original array.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private static final class PrimeFactorPowers {

		private final int[] powers;

		private Integer min;
		private int count;

		/**
		 * <p>
		 * Creates a prime factor powers array of the specified length.
		 * </p>
		 *
		 * @param length the length of the array
		 */
		public PrimeFactorPowers(int length) {
			this.powers = new int[length];
		}

		/**
		 * <p>
		 * Sets the prime factor power of the weight at the specified index.
		 * </p>
		 *
		 * @param index an index
		 * @param value a prime factor power
		 */
		public void set(int index, int value) {
			this.count++;
			this.min = this.min == null ? value : Math.min(this.min, value);
			this.powers[index] = value;
		}

		/**
		 * <p>
		 * Returns the prime factor power of the weight at the specified index.
		 * </p>
		 *
		 * @param index an index
		 *
		 * @return a prime factor power
		 */
		public Integer get(int index) {
			if(this.powers[index] == 0) {
				return null;
			}
			if(this.count == this.powers.length) {
				return this.powers[index] - this.min;
			}
			return this.powers[index];
		}
	}
}
