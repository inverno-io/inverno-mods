/*
 * Copyright 2025 Jeremy KUHN
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
package io.inverno.mod.session.jwt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.session.Session;
import java.util.function.Supplier;

/**
 * <p>
 * A JWT session allows to store stateless data in a JWT session identifier stored on the frontend in addition to the stateful session data stored on the backend.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the stateless session data type
 */
public interface JWTSession<A, B> extends Session<A> {

	/**
	 * <p>
	 * Returns the stateless session data.
	 * </p>
	 *
	 * @return the stateless session data or null if exist in the session
	 */
	@JsonProperty("statelessData")
	B getStatelessData();

	/**
	 * <p>
	 * Returns the stateless session data or creates them using the specified supplier if none exist in the session.
	 * </p>
	 *
	 * @param supplier a stateless session data supplier
	 *
	 * @return the stateless session data
	 */
	@JsonIgnore
	default B getStatelessData(Supplier<B> supplier) {
		B statelessData = this.getStatelessData();
		if(statelessData == null) {
			statelessData = supplier.get();
			this.setStatelessData(statelessData);
		}
		return statelessData;
	}

	/**
	 * <p>
	 * Sets the stateless session data.
	 * </p>
	 *
	 * @param statelessSessionData the stateless session data
	 */
	void setStatelessData(B statelessSessionData);
}
