/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security;

/**
 *
 * @author jkuhn
 */
public class CredentialsNotFoundException extends AuthenticationException {

	public CredentialsNotFoundException() {
	}

	public CredentialsNotFoundException(String message) {
		super(message);
	}

	public CredentialsNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public CredentialsNotFoundException(Throwable cause) {
		super(cause);
	}

	public CredentialsNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
}
