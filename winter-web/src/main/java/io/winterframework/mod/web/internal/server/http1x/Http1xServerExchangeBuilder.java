/**
 * 
 */
package io.winterframework.mod.web.internal.server.http1x;

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
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import io.winterframework.mod.web.internal.server.AbstractHttpServerExchangeBuilder;
import io.winterframework.mod.web.internal.server.AbstractRequest;
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
public class Http1xServerExchangeBuilder extends AbstractHttpServerExchangeBuilder<Http1xServerExchange> {

	private HttpRequest request;
	
	public Http1xServerExchangeBuilder(RequestHandler<RequestBody, ResponseBody, Void> rootHandler, RequestHandler<Void, ResponseBody, Throwable> errorHandler, HeaderService headerService, RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, RequestBodyDecoder<Part> multipartBodyDecoder) {
		super(rootHandler, errorHandler, headerService, urlEncodedBodyDecoder, multipartBodyDecoder);
	}
	
	public Http1xServerExchangeBuilder request(HttpRequest request) {
		this.request = request;
		return this;
	}
	
	@Override
	public Mono<Http1xServerExchange> build(ChannelHandlerContext context) {
		Objects.requireNonNull(this.request, "Missing request");
		return Mono.fromSupplier(() -> {
			Http1xRequestHeaders requestHeaders = new Http1xRequestHeaders(this.headerService, this.request, context.pipeline().get(SslHandler.class) != null);
			GenericRequestParameters requestParameters = new GenericRequestParameters(requestHeaders.getPath());
			Method method = requestHeaders.getMethod();
			
			AbstractRequest request;
			if(method == Method.POST || method == Method.PUT || method == Method.PATCH) {
				request = new PostRequest(context.channel().remoteAddress(), requestHeaders, requestParameters, this.urlEncodedBodyDecoder, this.multipartBodyDecoder, true);
			}
			else {
				request = new GetRequest(context.channel().remoteAddress(), requestHeaders, requestParameters);
			}
			GenericResponse response = new GenericResponse(this.headerService);
			
			return new Http1xServerExchange(context, this.rootHandler, this.errorHandler, request, response);
		});
	}

}
