/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security;

/**
 *
 * @author jkuhn
 */
public class TokenCredentials implements Credentials {
	
	private final String token;

	public TokenCredentials(String token) {
		this.token = token;
	}

	public String getToken() {
		return token;
	}
}
