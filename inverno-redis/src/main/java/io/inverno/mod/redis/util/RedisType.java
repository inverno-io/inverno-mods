/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package io.inverno.mod.redis.util;

/**
 *
 * @author jkuhn
 */
public enum RedisType {
	
	STRING("string"),
	LIST("list"),
	SET("set"),
	ZSET("zset"),
	HASH("hash"),
	STREAM("stream");
	
	private final String value;
	
	private RedisType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
