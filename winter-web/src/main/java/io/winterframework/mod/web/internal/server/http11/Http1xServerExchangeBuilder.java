/**
 * 
 */
package io.winterframework.mod.web.internal.server.http11;

import java.util.Objects;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslHandler;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Strategy;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import io.winterframework.mod.web.internal.server.AbstractHttpServerExchangeBuilder;
import io.winterframework.mod.web.internal.server.GenericRequestBody;
import io.winterframework.mod.web.internal.server.GenericRequestParameters;
import io.winterframework.mod.web.internal.server.GenericResponse;
import io.winterframework.mod.web.internal.server.GetRequest;
import io.winterframework.mod.web.internal.server.PostRequest;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
@Bean(strategy = Strategy.PROTOTYPE, visibility = Visibility.PRIVATE)
public class Http1xServerExchangeBuilder extends AbstractHttpServerExchangeBuilder<Http1xServerExchange<?>> {

	private HttpRequest request;
	
	public Http1xServerExchangeBuilder(HeaderService headerService, RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, RequestBodyDecoder<Part> multipartBodyDecoder) {
		super(headerService, urlEncodedBodyDecoder, multipartBodyDecoder);
	}
	
	public Http1xServerExchangeBuilder request(HttpRequest request) {
		this.request = request;
		return this;
	}
	
	@Override
	public Mono<Http1xServerExchange<?>> build(ChannelHandlerContext context) {
		Objects.requireNonNull(this.request, "Missing request");
		return Mono.fromSupplier(() -> {
			Http1xRequestHeaders requestHeaders = new Http1xRequestHeaders(this.headerService, this.request, context.pipeline().get(SslHandler.class) != null);
			GenericRequestParameters requestParameters = new GenericRequestParameters(requestHeaders.getPath());
			// TODO Cookie decoder
			// TODO path parameter decoder
			
			Method method = requestHeaders.getMethod();
			
			// It is maybe better to find the handler here
			if(method == Method.POST || method == Method.PUT || method == Method.PATCH) {
				PostRequest request = new PostRequest(context.channel().remoteAddress(), requestHeaders, requestParameters, this.urlEncodedBodyDecoder, this.multipartBodyDecoder, true);
				GenericResponse response = new GenericResponse(this.headerService);
				
				Http1xServerExchange<GenericRequestBody> postServerStream = new Http1xServerExchange<>(request, response, context);
				postServerStream.setHandler(this.findHandler(request));
				
				return postServerStream;
			}
			else {
				GetRequest request = new GetRequest(context.channel().remoteAddress(), requestHeaders, requestParameters);
				GenericResponse response = new GenericResponse(this.headerService);
				
				Http1xServerExchange<RequestBody> getServerStream = new Http1xServerExchange<>(request, response, context);
				getServerStream.setHandler(this.findHandler(request));
				
				return getServerStream;
			}
		});
	}

}
