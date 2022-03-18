/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http.digest;

import io.inverno.mod.security.AuthenticationException;

/**
 *
 * @author jkuhn
 */
public class ExpiredNonceException extends AuthenticationException {

	public ExpiredNonceException() {
	}

	public ExpiredNonceException(String message) {
		super(message);
	}

	public ExpiredNonceException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExpiredNonceException(Throwable cause) {
		super(cause);
	}

	public ExpiredNonceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
