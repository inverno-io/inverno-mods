/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.util;

/**
 *
 * @author jkuhn
 */
public interface AbstractScanResult {

	String getCursor();
	
	boolean isFinished();
}
