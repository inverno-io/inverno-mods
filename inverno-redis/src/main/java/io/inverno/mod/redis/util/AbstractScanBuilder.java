/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.util;

import java.nio.charset.Charset;

/**
 *
 * @author jkuhn
 */
public interface AbstractScanBuilder<A extends AbstractScanBuilder<A>> {

	A count(long count);
	
	A pattern(String pattern);
	A pattern(String pattern, Charset charset);
}
