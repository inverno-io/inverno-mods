/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.Charsets;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractHttpServerExchangeBuilder<A extends HttpServerExchange<?>> {

	protected final HeaderService headerService;
	protected final RequestBodyDecoder<Parameter> urlEncodedBodyDecoder;
	protected final RequestBodyDecoder<Part> multipartBodyDecoder;
	
	public AbstractHttpServerExchangeBuilder(HeaderService headerService, RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, RequestBodyDecoder<Part> multipartBodyDecoder) {
		this.headerService = headerService;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
	}
	
	public abstract Mono<A> build(ChannelHandlerContext context);
	
	// TODO replace with a Route service of some kind
	protected RequestHandler<RequestBody, ResponseBody> findHandler(PostRequest postRequest, ChannelHandlerContext context) {
		RequestHandler<RequestBody, ResponseBody> simple = (request, response) -> response
			.headers(headers -> headers
				.status(200)
				.contentType("text/plain; charset=\"UTF-8\"")
				.add("test", "1235")
			)
			.body().data().data(
				Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Server Response : Version - HTTP/2", Charsets.UTF_8)))
			);
		
		RequestHandler<RequestBody, ResponseBody> printData = (request, response) -> {
			response.headers(headers -> {
				headers.status(200)
					.contentType("text/plain; charset=\"UTF-8\"")
					.add("test", "1235");
			});
			
			request.body().data().data().subscribe(
				buffer -> {
					System.out.println("=================================");
			        System.out.println(buffer.toString(Optional.ofNullable(request.headers().getCharset()).orElse(Charsets.UTF_8)));
			        System.out.println("=================================");
				},
				ex -> {
					ex.printStackTrace();
				},
				() -> {
					System.out.println("Body complete");
					
//						response.body().empty();						
//						response.body().data(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Server Response : Version - HTTP/2", Charsets.UTF_8))));
					
					response.body().data().data(Flux.interval(Duration.ofMillis(500)).map(index -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response " + index + ", \r\n", Charsets.UTF_8))));
					
					/*response.body().data().data(Flux.just(
							Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response A, \r\n", Charsets.UTF_8)),
							Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response B, \r\n", Charsets.UTF_8)),
							Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response C, \r\n", Charsets.UTF_8)),
							Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response D, \r\n", Charsets.UTF_8)),
							Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response E \r\n", Charsets.UTF_8))
						)
						.delayElements(Duration.ofMillis(500)) 
					);*/
					
					// Why do I loose first message? 
					// We are using one single thread, when next is invoked the chunk is submit to the eventloopgroup whose thread is already busy right here so the chunk is placed in a queue until the thread gets back to the eventloopgroup 
					// This is not good for several reasons:
					// - it blocks the event loop group
					// - potentially we can loose messages if the data flux replay buffer in the response gets full => at least we should have a clear error explaining what went terribly wrong 
					/*response.body().data(Flux.create(emitter -> {
						try {
							emitter.next(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response A, \r\n", Charsets.UTF_8)));
							System.out.println("Emit A");
							Thread.sleep(1000);
							emitter.next(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response B, \r\n", Charsets.UTF_8)));
							System.out.println("Emit B");
							Thread.sleep(1000);
							emitter.next(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response C, \r\n", Charsets.UTF_8)));
							System.out.println("Emit C");
							Thread.sleep(1000);
							emitter.next(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response D \r\n", Charsets.UTF_8)));
							System.out.println("Emit D");
							emitter.complete();
						} 
						catch (InterruptedException e) {
							e.printStackTrace();
						}
					}));*/
					
					/*response.body().data(Flux.create(emitter -> {
						new Thread(() -> {
							try {
								for(int i=0;i<100;i++) {
									emitter.next(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response A, \r\n", Charsets.UTF_8)));
									Thread.sleep(100);
								}
								emitter.complete();
							} 
							catch (InterruptedException e) {
								e.printStackTrace();
							}
						}).start();
					}));*/
				});
		};
		
		RequestHandler<RequestBody, ResponseBody> urlEncoded = (request, response) -> response
			.headers(headers -> headers
				.status(200)
				.contentType("text/plain")
				.charset(Charsets.UTF_8)
			)
			.body().data()
			.data(
				request.body()
					.urlEncoded()
					.parameters()
					.collectList()
					.map(parameters -> "Received parameters: " + parameters.stream().map(param -> param.getName() + " = " + param.getValue()).collect(Collectors.joining(", ")))
					.map(result -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(result, Charsets.UTF_8)))
			);
		
		RequestHandler<RequestBody, ResponseBody> echo = (request, response) -> {
			response
				.headers(headers -> headers.status(200).contentType("text/plain"))
//				.body().data().data(request.body().data().data().map(chunk -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(chunk))));
				.body().data().data(request.body().data().data().doOnNext(chunk -> chunk.retain()));
		};
		
		RequestHandler<RequestBody, ResponseBody> multipartEcho = (request, response) -> {
			Flux<ByteBuf> responseData = request.body().multipart().parts().flatMapSequential(part -> {
				ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer(256));
				
				buf.writeCharSequence("================================================================================\n", Charsets.UTF_8);
				buf.writeCharSequence("name: " + part.getName() + "\n", Charsets.UTF_8);
				part.getFilename().ifPresent(filename -> buf.writeCharSequence("filename: " + filename + "\n", Charsets.UTF_8));
				buf.writeCharSequence("content-type: " + part.headers().getContentType() + "\n", Charsets.UTF_8);
				buf.writeCharSequence("charset: " + part.headers().getCharset() + "\n", Charsets.UTF_8);
				buf.writeCharSequence("size: " + part.headers().getSize() + "\n", Charsets.UTF_8);
				String headers = "headers:\n";
				headers += part.headers().getAll().entrySet().stream()
					.flatMap(e -> {
						return e.getValue().stream();
					})
					.map(h -> "  - " + headerService.encode(h))
					.collect(Collectors.joining("\n"));
				buf.writeCharSequence(headers + "\n", Charsets.UTF_8);
				buf.writeCharSequence("data: \n[", Charsets.UTF_8);
				
				return Flux.concat(
					Mono.<ByteBuf>just(buf),
					part.data().doOnNext(chunk -> chunk.retain()),
					Mono.<ByteBuf>just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("]\n================================================================================\r\n", Charsets.UTF_8)))
				);
			});
			
			response
				.headers(headers -> headers
					.status(200)
					.contentType("text/plain")
					.charset(Charsets.UTF_8)
				)
				.body().data().data(responseData);
		};
		
		// TODO we should provide a specific FilePart with adhoc methods to manipulate the File data flux or some utilities to make this simpler (especially regarding error handling, size limits...): a Part to Mono<File> mapper would be interesting as it would allow to chain the flux to the response data
		// TODO progressive upload can also be done: sse can do the trick but we should see other client side tricks for this as well
		// TODO it seems the size of the resulting file doesn't match the source why?
		RequestHandler<RequestBody, ResponseBody> multipartSaveFile = (request, response) -> {
			request.body().multipart().parts().filter(p -> p.getFilename().isPresent()).subscribe(
				filePart -> {
					try {
						SeekableByteChannel byteChannel = Files.newByteChannel(Paths.get("uploads/" + filePart.getFilename().get()), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
						
						filePart.data().subscribe(
							chunk -> {
								System.out.println("File chunk: " + chunk.readableBytes());
								try {
									byteChannel.write(chunk.nioBuffer());
								} catch (IOException e) {
									e.printStackTrace();
								}
							},
							ex -> {
								
							},
							() -> {
								System.out.println("Saved file: " + filePart.getFilename().get());
								try {
									byteChannel.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						);
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				},
				ex -> {
					response
					.headers(headers -> headers
						.status(500)
						.contentType("text/plain")
						.charset(Charsets.UTF_8)
					)
					.body().empty();
				},
				() -> {
					response
					.headers(headers -> headers
						.status(200)
						.contentType("text/plain")
						.charset(Charsets.UTF_8)
					)
					.body().empty();
				}
			);
		};
		
		return multipartEcho;
    }
    
	protected RequestHandler<Void, ResponseBody> findHandler(GetRequest getRequest) {
    	RequestHandler<Void, ResponseBody> simple = (request, response) -> response
			.headers(headers -> headers
				.status(200)
				.contentType("text/plain; charset=\"UTF-8\"")
				.add("test", "1235")
			)
			.body().data().data(
				Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Server Response : Version - HTTP/2", Charsets.UTF_8)))
			);
		
    	RequestHandler<Void, ResponseBody> echoParameters = (request, response) -> {
    		response
			.headers(headers -> headers
				.status(200)
				.contentType("text/plain; charset=UTF-8")
				.add("test", "1235")
			)
			.body().data().data(
//				Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Received parameters: " + request.parameters().getAll().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().stream().map(Parameter::getValue).collect(Collectors.joining(", "))).collect(Collectors.joining(", ")), Charsets.UTF_8)))
				Mono.just(Unpooled.copiedBuffer("Received parameters: " + request.parameters().getAll().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().stream().map(Parameter::getValue).collect(Collectors.joining(", "))).collect(Collectors.joining(", ")), Charsets.UTF_8))
			);
    	};
		RequestHandler<Void, ResponseBody> stream = (request, response) -> response
			.headers(headers -> headers
				.status(200)
				.contentType("text/plain; charset=\"UTF-8\"")
				.add("test", "1235")
			)
			.body().data().data(
				Flux.just(
					Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response A, \r\n", Charsets.UTF_8)),
					Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response B, \r\n", Charsets.UTF_8)),
					Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response C, \r\n", Charsets.UTF_8)),
					Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response D, \r\n", Charsets.UTF_8)),
					Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Response E \r\n", Charsets.UTF_8))
				)
				.delayElements(Duration.ofMillis(500)) 
			);
		
		RequestHandler<Void, ResponseBody> sse = (request, response) -> response
			.body().sse().events(
				Flux.interval(Duration.ofSeconds(1))
					.map(sequence -> response.body().sse().create(configurator -> configurator
							.id(Long.toString(sequence))
							.event("periodic-event")
							.comment("some comment \n on mutliple lines")
							.data(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("SSE - " + LocalTime.now().toString() + "\r\n", Charsets.UTF_8))))
						)
					)
					.doOnNext(evt -> System.out.println("Emit sse"))
			);
		
		RequestHandler<Void, ResponseBody> sse2 = (request, response) -> response
			.body().sse().events(
				Flux.range(0, 10)
					.map(sequence -> response.body().sse().create(configurator -> configurator
							.id(Long.toString(sequence))
							.event("periodic-event")
							.comment("some comment")
							.data(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("SSE - " + LocalTime.now().toString() + "\r\n", Charsets.UTF_8))))
						)
					)
					.doOnNext(evt -> System.out.println("Emit sse"))
					.delayElements(Duration.ofSeconds(1))
			);
		
		RequestHandler<Void, ResponseBody> echo = (request, response) -> {
			ByteBuf buf = Unpooled.unreleasableBuffer(Unpooled.buffer(256));
			
			buf.writeCharSequence("authority: " + request.headers().getAuthority() + "\n", Charsets.UTF_8);
			buf.writeCharSequence("path: " + request.headers().getPath() + "\n", Charsets.UTF_8);
			buf.writeCharSequence("method: " + request.headers().getMethod() + "\n", Charsets.UTF_8);
			buf.writeCharSequence("scheme: " + request.headers().getScheme() + "\n", Charsets.UTF_8);
			buf.writeCharSequence("content-type: " + request.headers().getContentType() + "\n", Charsets.UTF_8);
			buf.writeCharSequence("charset: " + request.headers().getCharset() + "\n", Charsets.UTF_8);
			buf.writeCharSequence("size: " + request.headers().getSize() + "\n", Charsets.UTF_8);
			String headers = "headers:\n";
			headers += request.headers().getAll().entrySet().stream()
				.flatMap(e -> {
					return e.getValue().stream();
				})
				.map(h -> "  - " + headerService.encode(h))
				.collect(Collectors.joining("\n"));
			buf.writeCharSequence(headers + "\n", Charsets.UTF_8);
			
			String cookies = "cookies:\n";
			cookies += request.cookies().getAll().entrySet().stream()
				.flatMap(e -> e.getValue().stream())
				.map(c -> "  - " + c.getName() + "=" + c.getValue())
				.collect(Collectors.joining("\n"));
			buf.writeCharSequence(cookies + "\n", Charsets.UTF_8);
			
			String parameters = "parameters:\n";
			parameters += request.parameters().getAll().entrySet().stream()
				.map(e -> "  - " + e.getKey() + "=" + e.getValue().stream().map(Parameter::getValue).collect(Collectors.joining(", ")))
				.collect(Collectors.joining("\n"));
			buf.writeCharSequence(parameters + "\n", Charsets.UTF_8);
			
			response.headers(configurator -> configurator
					.status(200)
					.contentType("text/plain")
					.charset(Charsets.DEFAULT)
				)
				.body().data().data(Mono.just(buf));
		};
		
		RequestHandler<Void, ResponseBody> echoSetCookie = echo.<Void, ResponseBody>map(handler -> (request, response) -> {
				response.cookies(cookies -> cookies.addCookie("test-cookie", "123465"));
				handler.handle(request, response);
			}
		);
		
		RequestHandler<Void, ResponseBody> echoSetCookie2 = echo.andThen(
			(request, response) -> response.cookies(cookies -> cookies.addCookie("test-cookie", "123465"))
		);
		
		RequestHandler<Void, ResponseBody> setCookie = (request, response) -> response
			.headers(headers -> headers.status(200).contentType("text/plain"))
			.cookies(cookies -> cookies.addCookie("test-cookie", "123465"))
			.body().data().data(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Set cookie", Charsets.DEFAULT))));
		
    	return echoSetCookie2;
    } 
}
