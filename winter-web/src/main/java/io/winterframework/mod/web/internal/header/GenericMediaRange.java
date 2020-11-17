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
package io.winterframework.mod.web.internal.header;

import java.util.Collections;
import java.util.Map;

import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;

/**
 * @author jkuhn
 *
 */
public class GenericMediaRange implements Headers.MediaRange {

	private String mediaType;
	
	private String type;
	
	private String subType;
	
	private float weight;
	
	private Map<String, String> parameters;
	
	private int score;
	
	public GenericMediaRange(String mediaType, float weight, Map<String, String> parameters) {
		this.mediaType = mediaType.toLowerCase();
		String[] splitMediaType = this.mediaType.split("/");
		if(splitMediaType.length == 2) {
			this.type = splitMediaType[0];
			this.subType = splitMediaType[1];
		}
		else if(splitMediaType.length == 1) {
			this.type = splitMediaType[0];
			this.subType = "*";
		}
		else {
			// TODO => Not Acceptable
			throw new RuntimeException("Not Acceptable: empty media type");
		}
		
		if(!HeaderService.isToken(this.type) || !HeaderService.isToken(this.subType)) {
			// TODO Not Acceptable
			throw new RuntimeException("Not Acceptable: invalid media type");
		}
		
		this.weight = weight;
		this.parameters = parameters != null ? Collections.unmodifiableMap(parameters) : Map.of();
		
		this.score = Headers.MediaRange.super.getScore();
	}
	
	public GenericMediaRange(String type, String subType, float weight, Map<String, String> parameters) {
		this.type = type.toLowerCase();
		this.subType = subType.toLowerCase();
		if(!HeaderService.isToken(this.type) || !HeaderService.isToken(this.subType)) {
			// TODO Not Acceptable
			throw new RuntimeException("Not Acceptable: invalid media type");
		}
		this.weight = weight;
		this.parameters = parameters != null ? Collections.unmodifiableMap(parameters) : Map.of();
		
		this.score = Headers.MediaRange.super.getScore();
	}
	
	@Override
	public String getMediaType() {
		if(this.mediaType == null) {
			this.mediaType = this.type + "/" + this.subType;
		}
		return this.mediaType;
	}
	
	@Override
	public String getType() {
		return this.type;
	}
	
	@Override
	public String getSubType() {
		return this.subType;
	}

	@Override
	public float getWeight() {
		return this.weight;
	}
	
	@Override
	public int getScore() {
		return this.score;
	}
	
	@Override
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mediaType == null) ? 0 : mediaType.hashCode());
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
		result = prime * result + Float.floatToIntBits(weight);
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
		GenericMediaRange other = (GenericMediaRange) obj;
		if (mediaType == null) {
			if (other.mediaType != null)
				return false;
		} else if (!mediaType.equals(other.mediaType))
			return false;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters))
			return false;
		if (Float.floatToIntBits(weight) != Float.floatToIntBits(other.weight))
			return false;
		return true;
	}
}
