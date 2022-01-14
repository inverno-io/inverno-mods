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

import io.inverno.mod.redis.operations.RedisScriptingReactiveOperations;
import io.lettuce.core.FlushMode;
import io.lettuce.core.ScriptOutputType;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public final class ScriptUtils {
	
	/**
	 * 
	 */
	private ScriptUtils() {}
	
	/**
	 * <p>
	 * Converts script output type to Lettuce script output type
	 * </p>
	 * 
	 * @param output
	 * @return 
	 */
	public static ScriptOutputType convertOutput(RedisScriptingReactiveOperations.ScriptOutput output) {
		if(output == null) {
			return null;
		}
		switch(output) {
			case BOOLEAN: return ScriptOutputType.BOOLEAN;
			case INTEGER: return ScriptOutputType.INTEGER;
			case MULTI: return ScriptOutputType.MULTI;
			case STATUS: return ScriptOutputType.STATUS;
			case VALUE: return ScriptOutputType.VALUE;
			default: throw new IllegalStateException("Unsupported output: " + output);
		}
	}
	
	/**
	 * <p>
	 * Converts script flush mode to Lettuce script flush mode
	 * </p>
	 * 
	 * @param flushMode
	 * @return 
	 */
	public static FlushMode convertFlushMode(RedisScriptingReactiveOperations.ScriptFlushMode flushMode) {
		if(flushMode == null) {
			return null;
		}
		switch(flushMode) {
			case SYNC: return FlushMode.SYNC;
			case ASYNC: return FlushMode.ASYNC;
			default: throw new IllegalStateException("Unsupported flushMode: " + flushMode);
		}
	}
}
