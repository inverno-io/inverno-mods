/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security;

/**
 *
 * @author jkuhn
 */
public class AuthenticationException extends io.inverno.mod.security.SecurityException {

	public AuthenticationException() {
	}

	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthenticationException(Throwable cause) {
		super(cause);
	}

	public AuthenticationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
