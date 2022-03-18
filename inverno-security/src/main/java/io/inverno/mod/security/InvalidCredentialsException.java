/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security;

/**
 *
 * @author jkuhn
 */
public class InvalidCredentialsException extends AuthenticationException {

	public InvalidCredentialsException() {
	}

	public InvalidCredentialsException(String message) {
		super(message);
	}

	public InvalidCredentialsException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidCredentialsException(Throwable cause) {
		super(cause);
	}

	public InvalidCredentialsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
	
}
