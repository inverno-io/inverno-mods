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
package io.inverno.mod.security.accesscontrol;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.commons.text.StringEscapeUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An access controller that uses permissions to control the access to services or resources based on the permissions that were granted to an authenticated entity.
 * </p>
 * 
 * <p>
 * This access controller is able to determine whether an authenticated entity has a particular set of permissions in a particular application context.
 * </p>
 * 
 * <p>
 * The following code shows how to determine whether permission {@code print} has been granted:
 * </p>
 * 
 * <pre>{@code
 *     PermissionBasedAccessController accessController = ...
 *     accessController.hasPermission("print").doOnNext(granted -> {...})...    }</pre>
 * 
 * <p>
 * Permissions can be parameterized in order to control the access to a service or a resouce based on a particular context. For instance, if a printing application wants to control access to printers based on the their names or a user's location, permissions can be checked by defining these as parameters as follows:
 * </p>
 * 
 * <pre>{@code
 *     PermissionBasedAccessController accessController = ...
 *     accessController.hasPermission("print", "name", "lp1200", "location", "desk_1234").doOnNext(granted -> {...})...    }</pre>
 * 
 * <p>
 * The priority given to parameters and defaulting support when evaluating permissions is implementation specific.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface PermissionBasedAccessController extends AccessController {
	
	/**
	 * <p>
	 * A parameter is used to specify the context in which a permission is evaluated.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	class Parameter {
		
		/**
		 * The parameter key.
		 */
		protected final String key;
		
		/**
		 * The parameter value.
		 */
		protected final Object value;
		
		/**
		 * <p>
		 * Creates a basic permission parameter.
		 * </p>
		 * 
		 * @param key   the parameter key
		 * @param value the parameter value
		 * 
		 * @throws IllegalArgumentException if the key is null or empty or if the value is null or not a string, nor a primitive
		 */
		private Parameter(String key, Object value) throws IllegalArgumentException {
			if(key == null || key.equals("")) {
				throw new IllegalArgumentException("Parameter key can't be null or empty");
			}
			this.key = key;
			if(value == null) {
				throw new IllegalArgumentException("Parameter value can't be null");
			}
			if(!(value instanceof Number || value instanceof Boolean || value instanceof Character || value instanceof CharSequence)) {
				throw new IllegalArgumentException("Parameter value can only be a number, a boolean, a character or a string");
			}
			this.value = value;
		}
		
		/**
		 * <p>
		 * Returns the parameter name.
		 * </p>
		 * 
		 * @return the name of the parameter
		 */
		public String getKey() {
			return key;
		}

		/**
		 * <p>
		 * Returns the parameter value.
		 * </p>
		 * 
		 * @return the value of the parameter
		 */
		public Object getValue() {
			return value;
		}
		
		/**
		 * <p>
		 * Creates a permission parameter with the specified key and value.
		 * </p>
		 *
		 * @param key   the parameter key
		 * @param value the parameter value
		 *
		 * @return a parameter
		 *
		 * @throws IllegalArgumentException if the key is null or empty or if the value is null or not a string, nor a primitive
		 */
		public static Parameter of(String key, Object value) throws IllegalArgumentException {
			return new Parameter(key, value);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Parameter other = (Parameter) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append(this.key)
				.append("=");
			if(this.value instanceof Number || this.value instanceof Boolean) {
				str.append(this.value);
			}
			else {
				str.append("\"").append(StringEscapeUtils.escapeJava(this.value.toString())).append("\"");
			}
			return str.toString();
		}
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameter.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the parameter key
	 * @param v1 the parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1, String k2, String v2) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1, String k2, String v2, String k3, String v3) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * @param k8 the eigth parameter key
	 * @param v8 the eigth parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7), PermissionBasedAccessController.Parameter.of(k8, v8)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * @param k8 the eigth parameter key
	 * @param v8 the eigth parameter value
	 * @param k9 the ninth parameter key
	 * @param v9 the ninth parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8, String k9, String v9) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7), PermissionBasedAccessController.Parameter.of(k8, v8), PermissionBasedAccessController.Parameter.of(k9, v9)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * @param k8 the eigth parameter key
	 * @param v8 the eigth parameter value
	 * @param k9 the ninth parameter key
	 * @param v9 the ninth parameter value
	 * @param k10 the tenth parameter key
	 * @param v10 the tenth parameter value
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8, String k9, String v9, String k10, String v10) {
		return this.hasPermission(permission, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7), PermissionBasedAccessController.Parameter.of(k8, v8), PermissionBasedAccessController.Parameter.of(k9, v9), PermissionBasedAccessController.Parameter.of(k10, v10)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param parameters an array of parameters
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	default Mono<Boolean> hasPermission(String permission, PermissionBasedAccessController.Parameter... parameters) {
		return this.hasPermission(permission, parameters != null ? Arrays.asList(parameters) : List.of());
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permission the permission to evaluate
	 * @param parameters a list of parameters
	 * 
	 * @return a mono emitting true if the permission is granted, false otherwise
	 */
	Mono<Boolean> hasPermission(String permission, List<PermissionBasedAccessController.Parameter> parameters);
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameter.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the parameter key
	 * @param v1 the parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1, String k2, String v2) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * @param k8 the eigth parameter key
	 * @param v8 the eigth parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7), PermissionBasedAccessController.Parameter.of(k8, v8)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * @param k8 the eigth parameter key
	 * @param v8 the eigth parameter value
	 * @param k9 the ninth parameter key
	 * @param v9 the ninth parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8, String k9, String v9) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7), PermissionBasedAccessController.Parameter.of(k8, v8), PermissionBasedAccessController.Parameter.of(k9, v9)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * @param k8 the eigth parameter key
	 * @param v8 the eigth parameter value
	 * @param k9 the ninth parameter key
	 * @param v9 the ninth parameter value
	 * @param k10 the tenth parameter key
	 * @param v10 the tenth parameter value
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8, String k9, String v9, String k10, String v10) {
		return this.hasAnyPermission(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7), PermissionBasedAccessController.Parameter.of(k8, v8), PermissionBasedAccessController.Parameter.of(k9, v9), PermissionBasedAccessController.Parameter.of(k10, v10)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param parameters an array of parameters
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyPermission(Set<String> permissions, PermissionBasedAccessController.Parameter... parameters) {
		return this.hasAnyPermission(permissions, parameters != null ? Arrays.asList(parameters) : List.of());
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param parameters a list of parameters
	 * 
	 * @return a mono emitting true if a permission is granted, false otherwise
	 */
	Mono<Boolean> hasAnyPermission(Set<String> permissions, List<PermissionBasedAccessController.Parameter> parameters);
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameter.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the parameter key
	 * @param v1 the parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1, String k2, String v2) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * @param k8 the eigth parameter key
	 * @param v8 the eigth parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7), PermissionBasedAccessController.Parameter.of(k8, v8)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * @param k8 the eigth parameter key
	 * @param v8 the eigth parameter value
	 * @param k9 the ninth parameter key
	 * @param v9 the ninth parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8, String k9, String v9) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7), PermissionBasedAccessController.Parameter.of(k8, v8), PermissionBasedAccessController.Parameter.of(k9, v9)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param k1 the first parameter key
	 * @param v1 the first parameter value
	 * @param k2 the second parameter key
	 * @param v2 the second parameter value
	 * @param k3 the third parameter key
	 * @param v3 the third parameter value
	 * @param k4 the fourth parameter key
	 * @param v4 the fourth parameter value
	 * @param k5 the fifth parameter key
	 * @param v5 the fifth parameter value
	 * @param k6 the sixth parameter key
	 * @param v6 the sixth parameter value
	 * @param k7 the seventh parameter key
	 * @param v7 the seventh parameter value
	 * @param k8 the eigth parameter key
	 * @param v8 the eigth parameter value
	 * @param k9 the ninth parameter key
	 * @param v9 the ninth parameter value
	 * @param k10 the tenth parameter key
	 * @param v10 the tenth parameter value
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8, String k9, String v9, String k10, String v10) {
		return this.hasAllPermissions(permissions, List.of(PermissionBasedAccessController.Parameter.of(k1, v1), PermissionBasedAccessController.Parameter.of(k2, v2), PermissionBasedAccessController.Parameter.of(k3, v3), PermissionBasedAccessController.Parameter.of(k4, v4), PermissionBasedAccessController.Parameter.of(k5, v5), PermissionBasedAccessController.Parameter.of(k6, v6), PermissionBasedAccessController.Parameter.of(k7, v7), PermissionBasedAccessController.Parameter.of(k8, v8), PermissionBasedAccessController.Parameter.of(k9, v9), PermissionBasedAccessController.Parameter.of(k10, v10)));
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param parameters an array of parameters
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	default Mono<Boolean> hasAllPermissions(Set<String> permissions, PermissionBasedAccessController.Parameter... parameters) {
		return this.hasAllPermissions(permissions, parameters != null ? Arrays.asList(parameters) : List.of());
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all the specified permissions in the context defined by the specified parameters.
	 * </p>
	 * 
	 * @param permissions the set of permissions to evaluate
	 * @param parameters a list of parameters
	 * 
	 * @return a mono emitting true if all permissions are granted, false otherwise
	 */
	Mono<Boolean> hasAllPermissions(Set<String> permissions, List<PermissionBasedAccessController.Parameter> parameters);
}
