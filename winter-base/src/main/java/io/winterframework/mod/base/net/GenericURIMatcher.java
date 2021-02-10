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
package io.winterframework.mod.base.net;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @author jkuhn
 *
 */
class GenericURIMatcher implements URIMatcher {

	private final Matcher matcher;
	
	private final List<String> groupNames;
	
	private Map<String, String> parameters;
	
	public GenericURIMatcher(Matcher matcher, List<String> groupNames) {
		this.matcher = matcher;
		this.groupNames = groupNames;
	}

	@Override
	public boolean matches() {
		return this.matcher.matches();
	}

	@Override
	public Matcher getMatcher() {
		return this.matcher;
	}

	@Override
	public String getParameterValue(String name) {
		return this.getParameters().get(name);
	}

	@Override
	public Map<String, String> getParameters() {
		if(this.parameters == null) {
			this.parameters = new LinkedHashMap<>();
		}
		for(int i=1;i<=this.matcher.groupCount();i++) {
			String parameterName = this.groupNames.get(i-1);
			if(parameterName != null) {
				this.parameters.put(parameterName, this.matcher.group(i));
			}
		}
		return this.parameters;
	}

	@Override
	public int compareTo(URIMatcher o) {
		// Rule: the most specific path wins from left to right
		// /toto/tata/titi/{}... > /toto/tata/{}...
		// /toto/tata/titi/{}... > /toto/tata/{}
		// /toto/{}/tutu > /toto/{}/{}
		// /toto/tutu > /toto/{:.*}
		// /toto/{}/tata > /toto/{:.*}
		// /toto/{}/{} > /toto/{:.*} 
		// /toto/{}_{}... > /toto/{}... 
		// /toto/abc_{}... > /toto/{}... 
		
		if(!this.matches() || !o.matches()) {
			throw new IllegalStateException("Matchers don't match the input");
		}
		if(!GenericURIMatcher.class.isAssignableFrom(o.getClass())) {
			throw new IllegalStateException("Can't compare " + GenericURIMatcher.class + " to " + o.getClass());
		}
		
		GenericURIMatcher other = (GenericURIMatcher)o;
		
		int groupIndex = 1;
		while (groupIndex <= Math.min(this.matcher.groupCount(), other.getMatcher().groupCount())) {
			String thisGroup = this.matcher.group(groupIndex);
			boolean thisParameterized = this.groupNames.get(groupIndex - 1) != null;
			
			String otherGroup = other.matcher.group(groupIndex);
			boolean otherParameterized = other.groupNames.get(groupIndex - 1) != null;

			if(thisGroup.length() < otherGroup.length()) {
				if(!otherParameterized) {
					return -1;
				}
				else {
					return 1;
				}
			}
			else if(thisGroup.length() > otherGroup.length()) {
				if(!thisParameterized) {
					return 1;
				}
				else {
					return -1;
				}
			}
			else {
				if(!thisParameterized && otherParameterized) {
					return 1;
				}
				else if(thisParameterized && !otherParameterized) {
					return -1;
				}
			}
			groupIndex++;
		}
		
		if(this.matcher.groupCount() < other.matcher.groupCount()) {
			return -1;
		}
		else if(this.matcher.groupCount() > other.matcher.groupCount()) {
			return 1;
		}
		return 0;
	}
}
