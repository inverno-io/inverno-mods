/**
 * 
 */
package io.winterframework.mod.web;

/**
 * @author jkuhn
 *
 */
public interface SetCookie extends Cookie {

	public static interface Configurator {

		Configurator name(String name);
		
		Configurator value(String value);
		
		Configurator maxAge(int maxAge);
		
		Configurator domain(String domain);
		
		Configurator path(String path);
		
		Configurator secure(boolean secure);
		
		Configurator httpOnly(boolean httpOnly);
	}
	
	Integer getMaxAge();
	
	String getDomain();
	
	String getPath();
	
	Boolean isSecure();
	
	Boolean isHttpOnly();
}
