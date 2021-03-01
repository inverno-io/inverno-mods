/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.mod.web.router.internal.compiler.spi;

import java.util.Optional;

import javax.lang.model.element.ExecutableElement;

import io.winterframework.core.compiler.spi.Info;
import io.winterframework.mod.web.Method;

/**
 * @author jkuhn
 *
 */
public interface WebRouteInfo extends Info {

	Optional<WebControllerInfo> getController();
	
	Optional<ExecutableElement> getElement();
	
	@Override
	WebRouteQualifiedName getQualifiedName();
	
	String[] getPaths();
	
	boolean isMatchTrailingSlash();
	
	Method[] getMethods();
	
	String[] getConsumes();
	
	String[] getProduces();
	
	String[] getLanguages();
	
	WebParameterInfo[] getParameters();
	
	WebResponseBodyInfo getResponseBody();
}
