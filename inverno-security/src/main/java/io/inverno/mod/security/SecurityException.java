/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security;

/**
 *
 * @author jkuhn
 */
public class SecurityException extends RuntimeException {

	public SecurityException() {
	}

	public SecurityException(String message) {
		super(message);
	}

	public SecurityException(String message, Throwable cause) {
		super(message, cause);
	}

	public SecurityException(Throwable cause) {
		super(cause);
	}

	public SecurityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
}
