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
package io.inverno.mod.grpc.base.internal;

import io.inverno.mod.grpc.base.GrpcServiceName;
import java.util.Objects;

/**
 * <p>
 * Generic {@link GrpcServiceName} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GenericGrpcServiceName implements GrpcServiceName {

	/**
	 * The package name.
	 */
	private String packageName;
	
	/**
	 * The service name.
	 */
	private String serviceName;
	
	/**
	 * The fully qualified name.
	 */
	private String fullyQualifiedName;

	/**
	 * <p>
	 * Creates a generic gRPC service name with the specified fully qualified name.
	 * </p>
	 * 
	 * @param fullyQualifiedName a gRPC full identifier
	 * 
	 * @throws IllegalArgumentException if the specified name is not valid gRPC full identifier
	 */
	public GenericGrpcServiceName(String fullyQualifiedName) throws IllegalArgumentException {
		this.setFullyQualifiedName(fullyQualifiedName);
	}
	
	/**
	 * <p>
	 * Creates a generic gRPC service name with the specified package and service names.
	 * </p>
	 * 
	 * @param packageName a gRPC package name
	 * @param serviceName a gRPC service name
	 * 
	 * @throws IllegalArgumentException if the specified names are not valid gRPC package or service names
	 */
	public GenericGrpcServiceName(String packageName, String serviceName) throws IllegalArgumentException {
		this.setPackageName(packageName);
		this.setServiceName(serviceName);
	}
	
	/**
	 * <p>
	 * Validates and sets the fully qualified service name.
	 * </p>
	 * 
	 * @param fullyQualifiedName a gRPC full identifier 
	 * 
	 * @throws IllegalArgumentException if the specified name is not valid gRPC full identifier
	 */
	private void setFullyQualifiedName(String fullyQualifiedName) throws IllegalArgumentException {
		if(fullyQualifiedName == null || fullyQualifiedName.isBlank()) {
			throw new IllegalArgumentException("Blank fully qualified name");
		}
		char[] chars = fullyQualifiedName.toCharArray();
		int serviceNameIndex = -1;
		for(int i=0;i<chars.length;i++) {
			if(chars[i] == '.') {
				serviceNameIndex = i+1;
			}
			else if(chars[i] != '_' && !Character.isLetterOrDigit(chars[i])) {
				throw new IllegalArgumentException("Invalid gRPC identifier");
			}
		}
		this.fullyQualifiedName = fullyQualifiedName;
		if(serviceNameIndex < 0) {
			this.serviceName = fullyQualifiedName;
		}
		else {
			this.packageName = this.fullyQualifiedName.substring(0, serviceNameIndex - 1);
			this.serviceName = this.fullyQualifiedName.substring(serviceNameIndex);
		}
	}
	
	/**
	 * <p>
	 * Validates and sets the service package name.
	 * </p>
	 * 
	 * @param packageName a gRPC full identifier
	 * 
	 * @throws IllegalArgumentException if the specified name is not valid gRPC full identifier
	 */
	private void setPackageName(String packageName) throws IllegalArgumentException {
		if(packageName == null || packageName.isBlank()) {
			this.packageName = packageName;
			return;
		}
		for(char c : packageName.toCharArray()) {
			if(c != '.' && c != '_' && !Character.isLetterOrDigit(c)) {
				throw new IllegalArgumentException("Invalid gRPC identifier");
			}
		}
		this.packageName = packageName;
	}
	
	/**
	 * <p>
	 * Validates and sets the service name.
	 * </p>
	 * 
	 * @param serviceName a service name
	 * 
	 * @throws IllegalArgumentException if the specified name is not valid gRPC identifier
	 */
	private void setServiceName(String serviceName) throws IllegalArgumentException {
		if(serviceName == null || serviceName.isBlank()) {
			throw new IllegalArgumentException("Blank service name");
		}
		for(char c : serviceName.toCharArray()) {
			if(c != '_' && !Character.isLetterOrDigit(c)) {
				throw new IllegalArgumentException("Invalid gRPC identifier");
			}
		}
		this.serviceName = serviceName;
	}
	
	@Override
	public String getServiceName() {
		return this.serviceName;
	}

	@Override
	public String getPackageName() {
		return this.packageName;
	}

	@Override
	public String getFullyQualifiedName() {
		if(this.fullyQualifiedName == null) {
			this.fullyQualifiedName = (this.packageName != null ? this.packageName + "." : "") + this.serviceName;
		}
		return this.fullyQualifiedName;
	}

	@Override
	public String methodPath(String method) {
		return "/" + this.getFullyQualifiedName() + "/" + Objects.requireNonNull(method);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GenericGrpcServiceName that = (GenericGrpcServiceName) o;
		return Objects.equals(packageName, that.packageName) && Objects.equals(serviceName, that.serviceName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(packageName, serviceName);
	}

	@Override
	public String toString() {
		return this.getFullyQualifiedName();
	}
}
