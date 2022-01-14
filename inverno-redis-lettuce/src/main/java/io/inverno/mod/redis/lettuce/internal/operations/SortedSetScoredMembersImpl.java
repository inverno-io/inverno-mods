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
package io.inverno.mod.redis.lettuce.internal.operations;

import io.inverno.mod.redis.operations.RedisSortedSetReactiveOperations;
import io.lettuce.core.ScoredValue;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 */
public class SortedSetScoredMembersImpl<A> implements RedisSortedSetReactiveOperations.SortedSetScoredMembers<A> {

	private final List<ScoredValue<A>> scoredValues;
	
	/**
	 * 
	 */
	public SortedSetScoredMembersImpl() {
		this.scoredValues = new LinkedList<>();
	}
	
	/**
	 * 
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	public ScoredValue<A>[] getScoredValues() {
		return this.scoredValues.toArray(ScoredValue[]::new);
	}
	
	@Override
	public RedisSortedSetReactiveOperations.SortedSetScoredMembers<A> entry(double score, A value) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}
}
