/*
 * Copyright 2021 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * <p>
 * The Inverno framework Reactive Templating module provides the base API used by Inverno Reactive Template classes to render a data model into a serialized representation (eg. HTML, XML, JSON...) in
 * a procedural or reactive way.
 * </p>
 * 
 * <p>
 * The API should not be used directly as it is intended to be used in template classes resulting from the compilation of a Inverno Reactive Template source file by the Inverno Reactive Template
 * compiler.
 * </p>
 * 
 * <p>
 * The API defines the {@link io.inverno.mod.irt.TemplateSet} interface which provides the basic building blocks used within a template class to render data models. It makes it possible to render data
 * in a reactive way by taking a non-blocking approach. The objects to render are considered as a flow of events that are rendered using multiple templates in sequence. As a result, a publisher is
 * rendered in the same way as any other object and the rendering process is never blocked.
 * </p>
 * 
 * <p>
 * The module also provides several base {@link io.inverno.mod.irt.TemplateSet} implementations that are used to generate different template classes depending on the desired output: String, ByteBuf,
 * Stream, String or ByteBuf publishers. This basically allows to optimize the rendering process for a particular output.
 * </p>
 * 
 * <p>
 * It also defines the {@link io.inverno.mod.irt.Pipe} API, which allows a template implementation to transform data before applying a template. There are three kinds of pipes: regular pipes used to
 * transform raw object (eg. formatting), stream pipes that perform transformation on a stream of data and publisher pipes that perform transformation on publishers. Pipes can be combined to create a
 * single pipe that applies the pipes in sequence.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
module io.inverno.mod.irt {
	requires static io.netty.common;
	requires static transitive io.netty.buffer;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	
	exports io.inverno.mod.irt;
}