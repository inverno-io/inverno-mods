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
package io.inverno.mod.grpc.base;

import io.inverno.mod.grpc.base.internal.GenericGrpcServiceName;

/**
 * <p>
 * Represents a gRPC service name.
 * </p>
 * 
 * <p>
 * A gRPC service is uniquely identified by its fully qualified name composed of a package and a service name: {@code <package>.<service_name>}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcServiceName {
	
	/**
	 * <p>
	 * Creates a gRPC service name from the specified fully qualified name.
	 * </p>
	 * 
	 * @param fullyQualifiedName a fully qualified name
	 * 
	 * @return a gRPC service name
	 * 
	 * @throws IllegalArgumentException if the specified name is not valid gRPC full identifier
	 */
	static GrpcServiceName of(String fullyQualifiedName) throws IllegalArgumentException {
		return new GenericGrpcServiceName(fullyQualifiedName);
	}
	
	/**
	 * <p>
	 * Creates a gRPC service name from the specified package and service names.
	 * </p>
	 * 
	 * @param packageName the package name
	 * @param serviceName the service name
	 * 
	 * @return a gRPC service name
	 * 
	 * @throws IllegalArgumentException if the specified names are not valid gRPC identifier
	 */
	static GrpcServiceName of(String packageName, String serviceName) throws IllegalArgumentException {
		return new GenericGrpcServiceName(packageName, serviceName);
	}

	/**
	 * <p>
	 * Returns the name of the service.
	 * </p>
	 * 
	 * @return the service name
	 */
	String getServiceName();
	
	/**
	 * <p>
	 * Returns the package of the service.
	 * </p>
	 * 
	 * @return the package name
	 */
	String getPackageName();

	/**
	 * <p>
	 * Returns the fully qualified name of the service.
	 * </p>
	 * 
	 * @return the service fully qualified name
	 */
	String getFullyQualifiedName();
	
	/**
	 * <p>
	 * Returns the path to the specified service method.
	 * </p>
	 * 
	 * <p>
	 * The path to a service method is always absolute and obtained from the service fully qualified name and the method name: {@code /<package>.<service_name>/<method_name>}.
	 * </p>
	 * 
	 * @param method a service method
	 * 
	 * @return a path to a service method
	 */
	String methodPath(String method);
}
