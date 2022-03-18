/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http;

import io.inverno.mod.security.SecurityException;

/**
 *
 * @author jkuhn
 */
public class MalformedCredentialsException extends SecurityException {

	public MalformedCredentialsException() {
	}

	public MalformedCredentialsException(String message) {
		super(message);
	}

	public MalformedCredentialsException(String message, Throwable cause) {
		super(message, cause);
	}

	public MalformedCredentialsException(Throwable cause) {
		super(cause);
	}

	public MalformedCredentialsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
