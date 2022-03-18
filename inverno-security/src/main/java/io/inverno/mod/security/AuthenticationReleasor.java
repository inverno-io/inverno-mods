/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.security;

import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
@FunctionalInterface
public interface AuthenticationReleasor<A extends Authentication> {
	
	Mono<Void> release(A authentication);
}
