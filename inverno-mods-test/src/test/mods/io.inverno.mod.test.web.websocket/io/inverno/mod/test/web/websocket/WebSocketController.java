/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.test.web.websocket;

import org.reactivestreams.Publisher;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.test.web.websocket.dto.GenericMessage;
import io.inverno.mod.test.web.websocket.dto.Message;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.server.annotation.WebSocketRoute;
import io.inverno.mod.web.server.ws.Web2SocketExchange;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean(visibility = Visibility.PUBLIC)
@WebController
public class WebSocketController {

	@WebRoute(path = "/no_ws", method = Method.GET)
	public String no_ws() {
	  return "no_ws";
	}
	
	@WebSocketRoute( path = "/ws1", messageType = WebSocketMessage.Kind.TEXT )
	public void ws1(Web2SocketExchange<? extends ExchangeContext> exchange) {
		exchange.outbound().frames(factory -> Flux.concat(Mono.just(factory.text("ws1")), exchange.inbound().frames()));
	}
	
	@WebSocketRoute( path = "/ws2", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws2(Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws2", Charsets.DEFAULT))), Flux.from(exchange.inbound().frames()).map(WebSocketFrame::getRawData));
	}
	
	@WebSocketRoute( path = "/ws3", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws3(Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws3")), exchange.inbound().decodeTextMessages(Message.class));
	}
	
	@WebSocketRoute( path = "/ws4", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws4(Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws4", Charsets.DEFAULT))), Flux.from(exchange.inbound().frames()).map(WebSocketFrame::getRawData));
	}
	
	@WebSocketRoute( path = "/ws5", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws5(Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws5")), exchange.inbound().decodeTextMessages(Message.class));
	}
	
	@WebSocketRoute( path = "/ws6", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws6(Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(exchange.inbound().frames()).map(frame -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws6", Charsets.DEFAULT);
			buf.writeBytes(frame.getRawData());
			frame.release();
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws7", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws7(Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(exchange.inbound()	.decodeTextMessages(Message.class)).doOnNext(message -> message.setMessage("ws7" + message.getMessage()));
	}
	
	public StringBuilder ws8 = new StringBuilder();
	
	@WebSocketRoute( path = "/ws8", messageType = WebSocketMessage.Kind.TEXT )
	public void ws8(BaseWeb2SocketExchange.Inbound inbound) {
		Flux.from(inbound.frames()).subscribe(frame -> {
			this.ws8.append(frame.getStringData());
			frame.release();
		});
	}
	
	public StringBuilder ws9 = new StringBuilder();
	
	@WebSocketRoute( path = "/ws9", messageType = WebSocketMessage.Kind.TEXT )
	public void ws9(Publisher<ByteBuf> inbound) {
		Flux.from(inbound).subscribe(chunk -> {
			this.ws9.append(chunk.toString(Charsets.DEFAULT));
			chunk.release();
		});
	}
	
	public StringBuilder ws10 = new StringBuilder();
	
	@WebSocketRoute( path = "/ws10", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws10(Publisher<Message> inbound) {
		Flux.from(inbound).subscribe(message -> {
			this.ws10.append(message.getMessage());
		});
	}
	
	public StringBuilder ws11 = new StringBuilder();
	
	@WebSocketRoute( path = "/ws11", messageType = WebSocketMessage.Kind.TEXT )
	public void ws11(Flux<ByteBuf> inbound) {
		inbound.subscribe(chunk -> {
			this.ws11.append(chunk.toString(Charsets.DEFAULT));
			chunk.release();
		});
	}
	
	public StringBuilder ws12 = new StringBuilder();
	
	@WebSocketRoute( path = "/ws12", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws12(Flux<Message> inbound) {
		inbound.subscribe(message -> {
			this.ws12.append(message.getMessage());
		});
	}
	
	public String ws13;
	
	@WebSocketRoute( path = "/ws13", messageType = WebSocketMessage.Kind.TEXT )
	public void ws13(Mono<ByteBuf> inbound) {
		inbound.subscribe(buf -> {
			this.ws13 = buf.toString(Charsets.DEFAULT);
			buf.release();
		});
	}
	
	public String ws14;
	
	@WebSocketRoute( path = "/ws14", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws14(Mono<Message> inbound) {
		inbound.subscribe(message -> {
			this.ws14 = message.getMessage();
		});
	}
	
	@WebSocketRoute( path = "/ws15", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws15(BaseWeb2SocketExchange.Inbound inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws15", Charsets.DEFAULT))), Flux.from(inbound.frames()).map(WebSocketFrame::getRawData));
	}
	
	@WebSocketRoute( path = "/ws16", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws16(Publisher<ByteBuf> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws16", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws17", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws17(Publisher<Message> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws17", Charsets.DEFAULT))), Flux.from(inbound).map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws18", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws18(Flux<ByteBuf> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws18", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws19", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws19(Flux<Message> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws19", Charsets.DEFAULT))), inbound.map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws20", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws20(Mono<ByteBuf> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws20", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws21", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws21(Mono<Message> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws21", Charsets.DEFAULT))), inbound.map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws22", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws22(BaseWeb2SocketExchange.Inbound inbound) {
		return Flux.concat(Mono.just(new Message("ws22")), inbound.decodeTextMessages(Message.class));
	}
	
	@WebSocketRoute( path = "/ws23", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws23(Publisher<ByteBuf> inbound) {
		return Flux.concat(Mono.just(new Message("ws23")), Flux.from(inbound).map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws24", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws24(Publisher<Message> inbound) {
		return Flux.concat(Mono.just(new Message("ws24")), inbound);
	}
	
	@WebSocketRoute( path = "/ws25", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws25(Flux<ByteBuf> inbound) {
		return Flux.concat(Mono.just(new Message("ws25")), inbound.map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws26", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws26(Flux<Message> inbound) {
		return Flux.concat(Mono.just(new Message("ws26")), inbound);
	}
	
	@WebSocketRoute( path = "/ws27", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws27(Mono<ByteBuf> inbound) {
		return Flux.concat(Mono.just(new Message("ws27")), inbound.map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws28", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws28(Mono<Message> inbound) {
		return Flux.concat(Mono.just(new Message("ws28")), inbound);
	}
	
	@WebSocketRoute( path = "/ws29", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws29(BaseWeb2SocketExchange.Inbound inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws29", Charsets.DEFAULT))), Flux.from(inbound.textMessages()).flatMap(WebSocketMessage::raw));
	}
	
	@WebSocketRoute( path = "/ws30", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws30(Publisher<ByteBuf> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws30", Charsets.DEFAULT))), Flux.from(inbound));
	}
	
	@WebSocketRoute( path = "/ws31", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws31(Publisher<Message> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws31", Charsets.DEFAULT))), Flux.from(inbound).map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws32", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws32(Flux<ByteBuf> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws32", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws33", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws33(Flux<Message> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws33", Charsets.DEFAULT))), Flux.from(inbound).map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws34", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws34(Mono<ByteBuf> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws34", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws35", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws35(Mono<Message> inbound) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws35", Charsets.DEFAULT))), inbound.map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws36", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws36(BaseWeb2SocketExchange.Inbound inbound) {
		return Flux.concat(Mono.just(new Message("ws36")), inbound.decodeTextMessages(Message.class));
	}
	
	@WebSocketRoute( path = "/ws37", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws37(Publisher<ByteBuf> inbound) {
		return Flux.concat(Mono.just(new Message("ws37")), Flux.from(inbound).map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws38", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws38(Publisher<Message> inbound) {
		return Flux.concat(Mono.just(new Message("ws38")), inbound);
	}
	
	@WebSocketRoute( path = "/ws39", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws39(Flux<ByteBuf> inbound) {
		return Flux.concat(Mono.just(new Message("ws39")), inbound.map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws40", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws40(Flux<Message> inbound) {
		return Flux.concat(Mono.just(new Message("ws40")), inbound);
	}
	
	@WebSocketRoute( path = "/ws41", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws41(Mono<ByteBuf> inbound) {
		return Flux.concat(Mono.just(new Message("ws41")), inbound.map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws42", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws42(Mono<Message> inbound) {
		return Flux.concat(Mono.just(new Message("ws42")), inbound);
	}
	
	@WebSocketRoute( path = "/ws43", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws43(BaseWeb2SocketExchange.Inbound inbound) {
		return Mono.from(inbound.frames()).map(frame -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws43", Charsets.DEFAULT);
			buf.writeBytes(frame.getRawData());
			frame.release();
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws44", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws44(Publisher<ByteBuf> inbound) {
		return Mono.from(inbound).map(frame -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws44", Charsets.DEFAULT);
			buf.writeBytes(frame);
			frame.release();
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws45", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws45(Publisher<Message> inbound) {
		return Mono.from(inbound).map(message -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws45", Charsets.DEFAULT);
			buf.writeCharSequence(message.getMessage(), Charsets.DEFAULT);
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws46", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws46(Flux<ByteBuf> inbound) {
		return Mono.from(inbound).map(frame -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws46", Charsets.DEFAULT);
			buf.writeBytes(frame);
			frame.release();
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws47", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws47(Flux<Message> inbound) {
		return Mono.from(inbound).map(message -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws47", Charsets.DEFAULT);
			buf.writeCharSequence(message.getMessage(), Charsets.DEFAULT);
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws48", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws48(Mono<ByteBuf> inbound) {
		return inbound.map(frame -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws48", Charsets.DEFAULT);
			buf.writeBytes(frame);
			frame.release();
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws49", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws49(Mono<Message> inbound) {
		return inbound.map(message -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws49", Charsets.DEFAULT);
			buf.writeCharSequence(message.getMessage(), Charsets.DEFAULT);
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws50", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws50(BaseWeb2SocketExchange.Inbound inbound) {
		return Mono.from(inbound.decodeTextMessages(Message.class)).doOnNext(message -> message.setMessage("ws50" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws51", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws51(Publisher<ByteBuf> inbound) {
		return Mono.from(inbound).map(buf -> {
			try {
				return new Message("ws51" + buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		});
	}
	
	@WebSocketRoute( path = "/ws52", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws52(Publisher<Message> inbound) {
		return Mono.from(inbound).doOnNext(message -> message.setMessage("ws52" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws53", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws53(Flux<ByteBuf> inbound) {
		return Mono.from(inbound).map(buf -> {
			try {
				return new Message("ws53" + buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		});
	}
	
	@WebSocketRoute( path = "/ws54", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws54(Flux<Message> inbound) {
		return Mono.from(inbound).doOnNext(message -> message.setMessage("ws54" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws55", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws55(Mono<ByteBuf> inbound) {
		return inbound.map(buf -> {
			try {
				return new Message("ws55" + buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		});
	}
	
	@WebSocketRoute( path = "/ws56", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws56(Mono<Message> inbound) {
		return inbound.doOnNext(message -> message.setMessage("ws56" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws57", messageType = WebSocketMessage.Kind.TEXT )
	public void ws57(BaseWeb2SocketExchange.Inbound inbound, BaseWeb2SocketExchange.Outbound outbound) {
		outbound.frames(factory -> Flux.concat(Mono.just(factory.text("ws57")), inbound.frames()));
	}
	
	@WebSocketRoute( path = "/ws58", messageType = WebSocketMessage.Kind.TEXT )
	public void ws58(Publisher<ByteBuf> inbound, BaseWeb2SocketExchange.Outbound outbound) {
		outbound.frames(factory -> Flux.concat(Mono.just(factory.text("ws58")), Flux.from(inbound).map(factory::text)));
	}
	
	@WebSocketRoute( path = "/ws59", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws59(Publisher<Message> inbound, BaseWeb2SocketExchange.Outbound outbound) {
		outbound.encodeTextMessages(Flux.concat(Mono.just(new Message("ws59")), inbound), Message.class);
	}
	
	@WebSocketRoute( path = "/ws60", messageType = WebSocketMessage.Kind.TEXT )
	public void ws60(Flux<ByteBuf> inbound, BaseWeb2SocketExchange.Outbound outbound) {
		outbound.frames(factory -> Flux.concat(Mono.just(factory.text("ws60")), inbound.map(factory::text)));
	}
	
	@WebSocketRoute( path = "/ws61", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws61(Flux<Message> inbound, BaseWeb2SocketExchange.Outbound outbound) {
		outbound.encodeTextMessages(Flux.concat(Mono.just(new Message("ws61")), inbound), Message.class);
	}
	
	@WebSocketRoute( path = "/ws62", messageType = WebSocketMessage.Kind.TEXT )
	public void ws62(Mono<ByteBuf> inbound, BaseWeb2SocketExchange.Outbound outbound) {
		outbound.frames(factory -> Flux.concat(Mono.just(factory.text("ws62")), inbound.map(factory::text)));
	}
	
	@WebSocketRoute( path = "/ws63", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws63(Mono<Message> inbound, BaseWeb2SocketExchange.Outbound outbound) {
		outbound.encodeTextMessages(Flux.concat(Mono.just(new Message("ws63")), inbound), Message.class);
	}
	
	@WebSocketRoute( path = "/ws64", messageType = WebSocketMessage.Kind.TEXT )
	public void ws64(BaseWeb2SocketExchange.Inbound inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		exchange.outbound().frames(factory -> Flux.concat(Mono.just(factory.text("ws64")), inbound.frames()));
	}
	
	@WebSocketRoute( path = "/ws65", messageType = WebSocketMessage.Kind.TEXT )
	public void ws65(Publisher<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		exchange.outbound().frames(factory -> Flux.concat(Mono.just(factory.text("ws65")), Flux.from(inbound).map(factory::text)));
	}
	
	@WebSocketRoute( path = "/ws66", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws66(Publisher<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		exchange.outbound().encodeTextMessages(Flux.concat(Mono.just(new Message("ws66")), inbound), Message.class);
	}
	
	@WebSocketRoute( path = "/ws67", messageType = WebSocketMessage.Kind.TEXT )
	public void ws67(Flux<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		exchange.outbound().frames(factory -> Flux.concat(Mono.just(factory.text("ws67")), inbound.map(factory::text)));
	}
	
	@WebSocketRoute( path = "/ws68", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws68(Flux<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		exchange.outbound().encodeTextMessages(Flux.concat(Mono.just(new Message("ws68")), inbound), Message.class);
	}
	
	@WebSocketRoute( path = "/ws69", messageType = WebSocketMessage.Kind.TEXT )
	public void ws69(Mono<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		exchange.outbound().frames(factory -> Flux.concat(Mono.just(factory.text("ws69")), inbound.map(factory::text)));
	}
	
	@WebSocketRoute( path = "/ws70", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws70(Mono<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		exchange.outbound().encodeTextMessages(Flux.concat(Mono.just(new Message("ws70")), inbound), Message.class);
	}
	
	@WebSocketRoute( path = "/ws71", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws71(BaseWeb2SocketExchange.Inbound inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws71", Charsets.DEFAULT))), Flux.from(inbound.frames()).map(WebSocketFrame::getRawData));
	}
	
	@WebSocketRoute( path = "/ws72", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws72(Publisher<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws72", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws73", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws73(Publisher<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws73", Charsets.DEFAULT))), Flux.from(inbound).map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws74", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws74(Flux<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws74", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws75", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws75(Flux<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws75", Charsets.DEFAULT))), Flux.from(inbound).map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws76", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws76(Mono<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws76", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws77", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws77(Mono<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws77", Charsets.DEFAULT))), Flux.from(inbound).map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws78", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws78(BaseWeb2SocketExchange.Inbound inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws78")), inbound.decodeTextMessages(Message.class));
	}
	
	@WebSocketRoute( path = "/ws79", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws79(Publisher<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws79")), Flux.from(inbound).map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws80", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws80(Publisher<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws80")), inbound);
	}
	
	@WebSocketRoute( path = "/ws81", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws81(Flux<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws81")), inbound.map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws82", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws82(Flux<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws82")), inbound);
	}
	
	@WebSocketRoute( path = "/ws83", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws83(Mono<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws83")), inbound.map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws84", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Message> ws84(Mono<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws84")), inbound);
	}
	
	@WebSocketRoute( path = "/ws85", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws85(BaseWeb2SocketExchange.Inbound inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws85", Charsets.DEFAULT))), Flux.from(inbound.frames()).map(WebSocketFrame::getRawData));
	}
	
	@WebSocketRoute( path = "/ws86", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws86(Publisher<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws86", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws87", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws87(Publisher<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws87", Charsets.DEFAULT))), Flux.from(inbound).map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws88", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws88(Flux<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws88", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws89", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws89(Flux<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws89", Charsets.DEFAULT))), Flux.from(inbound).map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws90", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws90(Mono<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws90", Charsets.DEFAULT))), inbound);
	}
	
	@WebSocketRoute( path = "/ws91", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws91(Mono<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws91", Charsets.DEFAULT))), Flux.from(inbound).map(message -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(message.getMessage(), Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws92", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws92(BaseWeb2SocketExchange.Inbound inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws92")), inbound.decodeTextMessages(Message.class));
	}
	
	@WebSocketRoute( path = "/ws93", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws93(Publisher<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws93")), Flux.from(inbound).map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws94", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws94(Publisher<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws94")), inbound);
	}
	
	@WebSocketRoute( path = "/ws95", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws95(Flux<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws95")), inbound.map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws96", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws96(Flux<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws96")), inbound);
	}
	
	@WebSocketRoute( path = "/ws97", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws97(Mono<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws97")), inbound.map(buf -> {
			try {
				return new Message(buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		}));
	}
	
	@WebSocketRoute( path = "/ws98", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Message> ws98(Mono<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Flux.concat(Mono.just(new Message("ws98")), inbound);
	}
	
	@WebSocketRoute( path = "/ws99", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws99(BaseWeb2SocketExchange.Inbound inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound.frames()).map(frame -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws99", Charsets.DEFAULT);
			buf.writeBytes(frame.getRawData());
			frame.release();
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws100", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws100(Publisher<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound).map(frame -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws100", Charsets.DEFAULT);
			buf.writeBytes(frame);
			frame.release();
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws101", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws101(Publisher<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound).map(message -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws101", Charsets.DEFAULT);
			buf.writeCharSequence(message.getMessage(), Charsets.DEFAULT);
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws102", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws102(Flux<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound).map(frame -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws102", Charsets.DEFAULT);
			buf.writeBytes(frame);
			frame.release();
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws103", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws103(Flux<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound).map(message -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws103", Charsets.DEFAULT);
			buf.writeCharSequence(message.getMessage(), Charsets.DEFAULT);
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws104", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws104(Mono<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return inbound.map(frame -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws104", Charsets.DEFAULT);
			buf.writeBytes(frame);
			frame.release();
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws105", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws105(Mono<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return inbound.map(message -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buf.writeCharSequence("ws105", Charsets.DEFAULT);
			buf.writeCharSequence(message.getMessage(), Charsets.DEFAULT);
			return buf;
		});
	}
	
	@WebSocketRoute( path = "/ws106", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws106(BaseWeb2SocketExchange.Inbound inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound.decodeTextMessages(Message.class)).doOnNext(message -> message.setMessage("ws106" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws107", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws107(Publisher<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound).map(buf -> {
			try {
				return new Message("ws107" + buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		});
	}
	
	@WebSocketRoute( path = "/ws108", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws108(Publisher<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound).doOnNext(message -> message.setMessage("ws108" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws109", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws109(Flux<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound).map(buf -> {
			try {
				return new Message("ws109" + buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		});
	}
	
	@WebSocketRoute( path = "/ws110", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws110(Flux<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound).doOnNext(message -> message.setMessage("ws110" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws111", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws111(Mono<ByteBuf> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return inbound.map(buf -> {
			try {
				return new Message("ws111" + buf.toString(Charsets.DEFAULT));
			}
			finally {
				buf.release();
			}
		});
	}
	
	@WebSocketRoute( path = "/ws112", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Message> ws112(Mono<Message> inbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		return Mono.from(inbound).doOnNext(message -> message.setMessage("ws112" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws113", messageType = WebSocketMessage.Kind.TEXT )
	public void ws113(BaseWeb2SocketExchange.Inbound inbound, BaseWeb2SocketExchange.Outbound outbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		outbound.frames(factory -> Flux.concat(Mono.just(factory.text("ws113")), inbound.frames()));
	}
	
	@WebSocketRoute( path = "/ws114", messageType = WebSocketMessage.Kind.TEXT )
	public void ws114(Publisher<ByteBuf> inbound, BaseWeb2SocketExchange.Outbound outbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		outbound.frames(factory -> Flux.concat(Mono.just(factory.text("ws114")), Flux.from(inbound).map(factory::text)));
	}
	
	@WebSocketRoute( path = "/ws115", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws115(Publisher<Message> inbound, BaseWeb2SocketExchange.Outbound outbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		outbound.encodeTextMessages(Flux.concat(Mono.just(new Message("ws115")), inbound), Message.class);
	}
	
	@WebSocketRoute( path = "/ws116", messageType = WebSocketMessage.Kind.TEXT )
	public void ws116(Flux<ByteBuf> inbound, BaseWeb2SocketExchange.Outbound outbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		outbound.frames(factory -> Flux.concat(Mono.just(factory.text("ws116")), inbound.map(factory::text)));
	}
	
	@WebSocketRoute( path = "/ws117", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws117(Flux<Message> inbound, BaseWeb2SocketExchange.Outbound outbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		outbound.encodeTextMessages(Flux.concat(Mono.just(new Message("ws117")), inbound), Message.class);
	}
	
	@WebSocketRoute( path = "/ws118", messageType = WebSocketMessage.Kind.TEXT )
	public void ws118(Mono<ByteBuf> inbound, BaseWeb2SocketExchange.Outbound outbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		outbound.frames(factory -> Flux.concat(Mono.just(factory.text("ws118")), inbound.map(factory::text)));
	}
	
	@WebSocketRoute( path = "/ws119", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws119(Mono<Message> inbound, BaseWeb2SocketExchange.Outbound outbound, Web2SocketExchange<? extends ExchangeContext> exchange) {
		outbound.encodeTextMessages(Flux.concat(Mono.just(new Message("ws119")), inbound), Message.class);
	}
	
	public boolean ws120;
	
	@WebSocketRoute( path = "/ws120", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Void> ws120() {
		this.ws120 = true;
		return Mono.empty();
	}
	
	public boolean ws121;
	
	@WebSocketRoute( path = "/ws121", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Void> ws121() {
		this.ws121 = true;
		return Flux.empty();
	}
	
	public boolean ws122;
	
	@WebSocketRoute( path = "/ws122", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Void> ws122() {
		this.ws122 = true;
		return Mono.empty();
	}
	
	@WebSocketRoute( path = "/ws123", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws123() {
		return Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("123", Charsets.DEFAULT)));
	}
	
	@WebSocketRoute( path = "/ws124", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<ByteBuf> ws124() {
		return Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("124", Charsets.DEFAULT)));
	}
	
	@WebSocketRoute( path = "/ws125", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<ByteBuf> ws125() {
		return Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws125", Charsets.DEFAULT)));
	}
	
	@WebSocketRoute( path = "/ws126", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Publisher<ByteBuf>> ws126() {
		return Flux.just(Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("126", Charsets.DEFAULT))), Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("126", Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws127", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Flux<ByteBuf>> ws127() {
		return Flux.just(Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("127", Charsets.DEFAULT))), Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("127", Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws128", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<Mono<ByteBuf>> ws128() {
		return Flux.just(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws128", Charsets.DEFAULT))), Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws128", Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws129", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Publisher<ByteBuf>> ws129() {
		return Flux.just(Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("129", Charsets.DEFAULT))), Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("129", Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws130", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Flux<ByteBuf>> ws130() {
		return Flux.just(Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("130", Charsets.DEFAULT))), Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("130", Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws131", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<Mono<ByteBuf>> ws131() {
		return Flux.just(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws131", Charsets.DEFAULT))), Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws131", Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws132", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Publisher<ByteBuf>> ws132() {
		return Mono.just(Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("132", Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws133", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Flux<ByteBuf>> ws133() {
		return Mono.just(Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws", Charsets.DEFAULT)), Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("133", Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws134", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<Mono<ByteBuf>> ws134() {
		return Mono.just(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("ws134", Charsets.DEFAULT))));
	}
	
	@WebSocketRoute( path = "/ws135", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<GenericMessage<String>> ws135() {
		return Flux.just(new GenericMessage<String>(1, "ws"), new GenericMessage<String>(2, "135"));
	}
	
	@WebSocketRoute( path = "/ws136", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<GenericMessage<String>> ws136() {
		return Flux.just(new GenericMessage<String>(1, "ws"), new GenericMessage<String>(2, "136"));
	}
	
	@WebSocketRoute( path = "/ws137", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<GenericMessage<String>> ws137() {
		return Mono.just(new GenericMessage<String>(1, "ws137"));
	}
	
	public StringBuilder ws138 = new StringBuilder();
	
	@WebSocketRoute( path = "/ws138", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws138(Publisher<GenericMessage<String>> inbound) {
		Flux.from(inbound).subscribe(message -> this.ws138.append(message.toString()));
	}
	
	public StringBuilder ws139 = new StringBuilder();
	
	@WebSocketRoute( path = "/ws139", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws139(Flux<GenericMessage<String>> inbound) {
		inbound.subscribe(message -> this.ws139.append(message.toString()));
	}
	
	public String ws140;
	
	@WebSocketRoute( path = "/ws140", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws140(Mono<GenericMessage<String>> inbound) {
		inbound.subscribe(message -> this.ws140 = message.toString());
	}
	
	@WebSocketRoute( path = "/ws141", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<GenericMessage<String>> ws141(Publisher<GenericMessage<String>> inbound) {
		return Flux.concat(Mono.just(new GenericMessage<>(0, "ws141")), inbound);
	}
	
	@WebSocketRoute( path = "/ws142", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<GenericMessage<String>> ws142(Publisher<GenericMessage<String>> inbound) {
		return Flux.concat(Mono.just(new GenericMessage<>(0, "ws142")), inbound);
	}
	
	@WebSocketRoute( path = "/ws143", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<GenericMessage<String>> ws143(Publisher<GenericMessage<String>> inbound) {
		return Mono.from(inbound).doOnNext(message -> message.setMessage("ws143" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws144", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<GenericMessage<String>> ws144(Flux<GenericMessage<String>> inbound) {
		return Flux.concat(Mono.just(new GenericMessage<>(0, "ws144")), inbound);
	}
	
	@WebSocketRoute( path = "/ws145", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<GenericMessage<String>> ws145(Flux<GenericMessage<String>> inbound) {
		return Flux.concat(Mono.just(new GenericMessage<>(0, "ws145")), inbound);
	}
	
	@WebSocketRoute( path = "/ws146", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<GenericMessage<String>> ws146(Flux<GenericMessage<String>> inbound) {
		return Mono.from(inbound).doOnNext(message -> message.setMessage("ws146" + message.getMessage()));
	}
	
	@WebSocketRoute( path = "/ws147", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<GenericMessage<String>> ws147(Mono<GenericMessage<String>> inbound) {
		return Flux.concat(Mono.just(new GenericMessage<>(0, "ws147")), inbound);
	}
	
	@WebSocketRoute( path = "/ws148", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Flux<GenericMessage<String>> ws148(Mono<GenericMessage<String>> inbound) {
		return Flux.concat(Mono.just(new GenericMessage<>(0, "ws148")), inbound);
	}
	
	@WebSocketRoute( path = "/ws149", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Mono<GenericMessage<String>> ws149(Mono<GenericMessage<String>> inbound) {
		return inbound.doOnNext(message -> message.setMessage("ws149" + message.getMessage()));
	}
	

	// Things that should not compile:
	/*@WebSocketRoute( path = "/ws135", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws135(Web2SocketExchange<? extends ExchangeContext> exchange1, Web2SocketExchange<? extends ExchangeContext> exchange2) {
	}
	
	@WebSocketRoute( path = "/ws136", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws136(BaseWeb2SocketExchange.Outbound outbound) {
		return null;
	}
	
	@WebSocketRoute( path = "/ws137", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public Publisher<ByteBuf> ws137(BaseWeb2SocketExchange.Inbound inbound1, Publisher<ByteBuf> inbound2) {
		return null;
	}
	
	@WebSocketRoute( path = "/ws138", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws138(BaseWeb2SocketExchange.Outbound oubound1, BaseWeb2SocketExchange.Outbound oubound2) {
	}
	
	@WebSocketRoute( path = "/ws_clash", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws139(BaseWeb2SocketExchange.Inbound inbound, BaseWeb2SocketExchange.Outbound oubound) {
	}
	
	@WebSocketRoute( path = "/ws_clash", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
	public void ws140(BaseWeb2SocketExchange.Inbound inbound, BaseWeb2SocketExchange.Outbound oubound) {
	}*/
}
