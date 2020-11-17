/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.web.lab.router.base;

import io.winterframework.mod.web.Method;

/**
 * @author jkuhn
 *
 */
public class RoutingContext {
	
	private Method method;
	
	private String consumes;
	
	private String produces;
	
	private String language;
	
	public Method getMethod() {
		return this.method;
	}
	
	public void setMethod(Method method) {
		this.method = method;
	}
	
	public String getConsumes() {
		return this.consumes;
	}
	
	public void setConsumes(String consumes) {
		this.consumes = consumes;
	}
	
	public String getProduces() {
		return this.produces;
	}
	
	public void setProduces(String produces) {
		this.produces = produces;
	}
	
	public String getLanguage() {
		return this.language;
	}
	
	public void setLanguage(String language) {
		this.language = language;
	}
}
