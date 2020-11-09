/**
 * 
 */
package io.winterframework.mod.web;

/**
 * @author jkuhn
 *
 */
public interface HeaderBuilder<A extends Header, B extends HeaderBuilder<A, B>> {

	B headerName(String name);
	
	B headerValue(String value);
	
	A build();
}
