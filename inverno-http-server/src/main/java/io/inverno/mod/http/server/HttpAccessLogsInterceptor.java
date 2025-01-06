/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.server;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import java.net.InetSocketAddress;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.util.Strings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/**
 * <p>
 * Intercepts exchanges or error exchanges and logs HTTP access.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the handler
 */
public class HttpAccessLogsInterceptor<A extends ExchangeContext, B extends Exchange<A>> implements ExchangeInterceptor<A, B> {

	private static final Logger LOGGER = LogManager.getLogger(Exchange.class);
	
	@Override
	public Mono<? extends B> intercept(B exchange) {
		exchange.response().body().transform(data -> {
			if(data instanceof Mono) {
				return Mono.from(data).doFinally(sig -> {
					if(sig == SignalType.ON_COMPLETE) {
						LOGGER.info(new AccessLogMessage(exchange));
					}
				});
			}
			else {
				return Flux.from(data).doFinally(sig -> {
					if(sig == SignalType.ON_COMPLETE) {
						LOGGER.info(new AccessLogMessage(exchange));
					}
				});
			}
		});
		return Mono.just(exchange);
	}
	
	/**
	 * <p>
	 * Access log message.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class AccessLogMessage implements MultiformatMessage {

		private static final long serialVersionUID = 1L;
		
		private static final String JSON_FORMAT = "JSON";
		
		private final String remoteAddress;
		private final String request;
		private final int statusCode;
		private final int transferredLength;
		private final String referer;
		private final String userAgent;
		
		/**
		 * <p>
		 * Creates an access log message.
		 * </p>
		 * 
		 * @param exchange the exchange
		 */
		public AccessLogMessage(B exchange) {
			this.remoteAddress = ((InetSocketAddress)exchange.request().getRemoteAddress()).getAddress().getHostAddress();
			this.request = new StringBuilder().append(exchange.request().getMethod().name()).append(" ").append(exchange.request().getPath()).toString();
			this.statusCode = exchange.response().headers().getStatusCode();
			this.transferredLength = exchange.response().getTransferredLength();
			this.referer = exchange.request().headers().get(Headers.NAME_REFERER).orElse("");
			this.userAgent = exchange.request().headers().get(Headers.NAME_USER_AGENT).orElse("");
		}

		@Override
		public Object[] getParameters() {
			return new Object[] {
				this.remoteAddress,
				this.request,
				this.statusCode,
				this.transferredLength,
				this.referer,
				this.userAgent
			};
		}

		@Override
		public Throwable getThrowable() {
			return null;
		}
		
		@Override
		public String getFormattedMessage() {
			return this.asString();
		}

		@Override
		public String getFormattedMessage(String[] formats) {
			for(String format : formats) {
				if(format.equalsIgnoreCase(JSON_FORMAT)) {
					return this.asJson();
				}
			}
			return this.asString();
		}

		@Override
		public String[] getFormats() {
			return new String[] { "JSON" };
		}
		
		private String asString() {
			StringBuilder message = new StringBuilder();
			message.append(this.remoteAddress).append(" ");
			message.append("\"").append(this.request).append("\" ");
			message.append(this.statusCode).append(" ");
			message.append(this.transferredLength).append(" ");
			message.append("\"").append(this.referer).append("\" ");
			message.append("\"").append(this.userAgent).append("\" ");
			
			return message.toString();
		}
		
		private String asJson() {
			StringBuilder message = new StringBuilder();
			message.append("{");
			message.append("\"remoteAddress\":\"").append(this.remoteAddress).append("\",");
			message.append("\"request\":\"").append(StringEscapeUtils.escapeJson(this.request)).append("\",");
			message.append("\"status\":").append(this.statusCode).append(",");
			message.append("\"bytes\":").append(this.transferredLength).append(",");
			message.append("\"referer\":\"").append(StringEscapeUtils.escapeJson(this.referer)).append("\",");
			message.append("\"userAgent\":\"").append(StringEscapeUtils.escapeJson(this.userAgent)).append("\"");
			message.append("}");
			
			return message.toString();
		}
	}
}
