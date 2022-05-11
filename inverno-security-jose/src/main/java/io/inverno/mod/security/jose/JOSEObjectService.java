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
package io.inverno.mod.security.jose;

import io.inverno.mod.security.jose.jwe.JWEService;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jws.JWSService;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;

/**
 * <p>
 * A JOSE object service is a main entry point for creating and reading particular JOSE objects.
 * </p>
 * 
 * <p>
 * It allows to fluently create JOSE objects using {@link JOSEObjectBuilder} for compact serialization or {@link JsonJOSEObjectBuilder} for JSON serialization. It also exposes {@link JOSEObjectReader}
 * and {@link JsonJOSEObjectReader} to read compact representation or JSON representation respectively.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see JWSService
 * @see JWEService
 */
public interface JOSEObjectService  {
	
	/**
	 * <p>
	 * Returns a new JOSE object builder.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will try to resolve the JWK to use to secure the JOSE object based on the JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T> the payload type
	 * 
	 * @return a new JOSE object builder
	 */
	<T> JOSEObjectBuilder<T, ?, ?, ?, ?> builder();
	
	/**
	 * <p>
	 * Returns a new JOSE object builder with the specified payload type.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will try to resolve the JWK to use to secure the JOSE object based on the JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * 
	 * @return a new JOSE object builder
	 */
	<T> JOSEObjectBuilder<T, ?, ?, ?, ?> builder(Class<T> type);
	
	/**
	 * <p>
	 * Returns a new JOSE object builder with the specified payload type.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will try to resolve the JWK to use to secure the JOSE object based on the JOSE header. It will fail if it wasn't able to find a suitable key.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * 
	 * @return a new JOSE object builder
	 */
	<T> JOSEObjectBuilder<T, ?, ?, ?, ?> builder(Type type);
	
	/**
	 * <p>
	 * Returns a new JOSE object builder using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will use the specified keys to secure the JOSE object, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JOSE header.
	 * </p>
	 * 
	 * @param <T> the payload type
	 * @param keys the keys to consider to secure the JOSE object
	 * 
	 * @return a new JOSE object builder
	 */
	<T> JOSEObjectBuilder<T, ?, ?, ?, ?> builder(Publisher<? extends JWK> keys);
	
	/**
	 * <p>
	 * Returns a new JOSE object builder with the specified payload type and using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will use the specified keys to secure the JOSE object, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JOSE header.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * @param keys the keys to consider to secure the JOSE object
	 * 
	 * @return a new JOSE object builder
	 */
	<T> JOSEObjectBuilder<T, ?, ?, ?, ?> builder(Class<T> type, Publisher<? extends JWK> keys);
	
	/**
	 * <p>
	 * Returns a new JOSE object builder with the specified payload type and using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned builder will use the specified keys to secure the JOSE object, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable key have
	 * been specified or if they are not consistent with the JOSE header.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * @param keys the keys to consider to secure the JOSE object
	 * 
	 * @return a new JOSE object builder
	 */
	<T> JOSEObjectBuilder<T, ?, ?, ?, ?> builder(Type type, Publisher<? extends JWK> keys);
	
	/**
	 * <p>
	 * Returns a new JOSE object reader with the specified payload type.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the public JWK that corresponds to the key used to secure the JOSE object based on the JOSE header. It will fail if it wasn't able to find a suitable
	 * key.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * 
	 * @return a new JOSE object reader
	 */
	<T> JOSEObjectReader<T, ?, ?, ?> reader(Class<T> type);
	
	/**
	 * <p>
	 * Returns a new JOSE object reader with the specified payload type.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will try to resolve the public JWK that corresponds to the key used to secure the JOSE object based on the JOSE header. It will fail if it wasn't able to find a suitable
	 * key.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * 
	 * @return a new JOSE object reader
	 */
	<T> JOSEObjectReader<T, ?, ?, ?> reader(Type type);
	
	/**
	 * <p>
	 * Returns a new JOSE object reader with the specified payload type and using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to verify or decrypt the JOSE object, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable
	 * key have been specified or if they are not consistent with the JOSE header.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * @param keys the keys to consider to verify or decrypt the JOSE object
	 * 
	 * @return a new JOSE object reader
	 */
	<T> JOSEObjectReader<T, ?, ?, ?> reader(Class<T> type, Publisher<? extends JWK> keys);
	
	/**
	 * <p>
	 * Returns a new JOSE object reader with the specified payload type and using the specified keys.
	 * </p>
	 * 
	 * <p>
	 * The returned reader will use the specified keys to verify or decrypt the JOSE object, the first succeeding key will be retained and remaining keys will be ignored. It will fail if no suitable
	 * key have been specified or if they are not consistent with the JOSE header.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * @param keys the keys to consider to verify or decrypt the JOSE object
	 * 
	 * @return a new JOSE object reader
	 */
	<T> JOSEObjectReader<T, ?, ?, ?> reader(Type type, Publisher<? extends JWK> keys);
	
	/**
	 * <p>
	 * Returns a new JSON JOSE object builder.
	 * </p>
	 * 
	 * @param <T> the payload type
	 * 
	 * @return a new JSON JOSE object builder
	 */
	<T> JsonJOSEObjectBuilder<T, ?, ?, ?> jsonBuilder();
	
	/**
	 * <p>
	 * Returns a new JSON JOSE object builder with the specified payload type.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * 
	 * @return a new JSON JOSE object builder
	 */
	<T> JsonJOSEObjectBuilder<T, ?, ?, ?> jsonBuilder(Class<T> type);
	
	/**
	 * <p>
	 * Returns a new JSON JOSE object builder with the specified payload type.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * 
	 * @return a new JSON JOSE object builder
	 */
	<T> JsonJOSEObjectBuilder<T, ?, ?, ?> jsonBuilder(Type type);
	
	/**
	 * <p>
	 * Returns a new JSON JOSE object reader with the specified payload type.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * 
	 * @return a new JSON JOSE object reader
	 */
	<T> JsonJOSEObjectReader<T, ?, ?> jsonReader(Class<T> type);
	
	/**
	 * <p>
	 * Returns a new JSON JOSE object reader with the specified payload type.
	 * </p>
	 * 
	 * @param <T>  the payload type
	 * @param type the payload type
	 * 
	 * @return a new JSON JOSE object reader
	 */
	<T> JsonJOSEObjectReader<T, ?, ?> jsonReader(Type type);
}
