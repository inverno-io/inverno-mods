package io.winterframework.mod.web.lab;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import io.netty.buffer.Unpooled;
import io.winterframework.mod.web.Cookie;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestCookies;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.Charsets;
import reactor.core.publisher.Mono;

public class TestHandler {

	public TestHandler() {
		// TODO Auto-generated constructor stub
	}
	
	public static class Toto {
		
		private String field;
		
		public Toto(String field) {
			this.field = field;
		}
		
		public String getField() {
			return field;
		}
	}
	
	public static class Tata {
		
		private String field;
		
		public Tata(String field) {
			this.field = field;
		}
		
		public String getField() {
			return field;
		}
	}
	
	public static class ResponseEntity<T> {
		
		private ResponseBody responseBody;
		
		public ResponseEntity(ResponseBody responseBody) {
			this.responseBody = responseBody;
		}
		
		public void value(T value) {
			// We should not be able to call this twice
			TestHandler.encode(this.responseBody, value);
		}
	}
	
	public static interface Context {
		
	}
	
	public static interface AttributeContext extends Context {
		
		Set<String> getAttributeNames();
		
		<T> T getAttribute(String name);
		
		<T> void setAttribute(String name, T attribute);
		
		void removeAttribute(String name);
		
		Map<String, Object> getAllAttributes();
	}
	
	public static interface ApplicationContext extends AttributeContext {
		
		Optional<SecurityContext> getSecurityContext();
		
		Optional<Session> getSession();
	}
	
	public static interface SecurityContext {
		
		boolean hasPermission(String permission);
	}
	
	public interface Session {
		
		String getId();
	}
	
	public static <T> T decode(RequestBody data) {
		return null;
	}
	
	// Not good: je dois injecter le body serialisé dans le body fourni
	public static <T> void encode(ResponseBody responseBody, T entity) {
//			responseBody.data().data(...) // encode body to Flux<ByteBuf>
		// Here we can basically do whatever we need since we have access to the responseBody and the entity which can be many things like a builder: response.body().data1(...).data2(...).data3(...).build() 
		
	}
	
	// This calls whatever decoder based on content-type we might need the actual target class to decode data? 
	public static <T> RequestHandler<RequestBody, Void, ResponseBody> decoderHandler(RequestHandler<T, Void, ResponseBody> sourceHandler) {
		return (request, response) -> sourceHandler.handle(request.map(TestHandler::decode, Function.identity()), response);
	}
	
	// This what the server expect
	public static void addRequestHandler(RequestHandler<RequestBody, Void, ResponseBody> handler) {
		
	}

/*
	@FunctionalInterface
	public static interface InterceptorMapper<A,B,C, D,E,F> {
		
		RequestHandler<D,E,F> map(RequestHandler<A,B,C> source);
		
		default <G,H,I> InterceptorMapper<A,B,C, G,H,I> combine(InterceptorMapper<D,E,F, G,H,I> source) {
			return h -> {
				return source.map(this.map(h));
			};
		}
	}
	
	
	public static Optional<SecurityContext> authenticate(Context currentContext, String credentials) {
		return Optional.of(new SecurityContext(currentContext));
	}
	
	public static <T, U extends Context> RequestHandler<T, U, ResponseBody> authenticationInterceptor(RequestHandler<T, SecurityContext, ResponseBody> handler) {
		return (request, response) -> {
			request.headers().get("Authorization")
				.flatMap(authorization -> authenticate(request.context(), authorization.getHeaderValue()))
				.ifPresentOrElse(
					securityContext -> {
						handler.handle(request.map(Function.identity(), ign -> securityContext), response);
					},
					() -> {
						// 401 handler
						// This basically impose that we have a responseBody... so definitely handlers can't be chained that easily
						response.headers(headers -> headers.status(401)).body().empty();
					}
				);
		};
	}
	
	public static <T> RequestHandler<T, SecurityContext, ResponseBody> authorizationInterceptor(RequestHandler<T, SecurityContext, ResponseBody> handler, String permission) {
		return (request, response) -> {
			// How to pass this?
			// We should have an interceptor factory 
			if(request.context().hasPermission(permission)) {
				// proceed
				handler.handle(request, response);
			}
			else {
				// 403 handler
				response.headers(headers -> headers.status(403)).body().empty();
			}
		};
	}
	
	public static SessionContext loadSession(Context currentContext, String sessionId) {
		// We return eithier the session we found or a nex one
		return new SessionContext(currentContext, sessionId);
	}
	
	// Here we have an issue:
	// - we expect a securityContext for no reason since I can want to have session without security
	// - I can declare that I don't care using generics but then how can I know how to chain interceptor unless I do it explictly (which is actually the case for filters, handlers in a pipeline...)
	// - this greatly limits static validation...
	// - one solution would be to wrap contexts: at the end of the chain we can then retrieve all contexts do some instanceof and expose a consolidated view to the end user (this is comparable to an attribute map 
	// - this allows for interceptor chaining with static check to force a proper ordering but again not quite flexible
	// - one example: what if we want to change a context in a subsequent interceptor for some reason
	// - untyped attributes is the only way
	//
	// This should be enhanced: session ids should roll every x minutes so existing sessions should be recreated from time to time
	// This can be tricky especially on multi nodes configuration
	public static <T, U extends Context, V> RequestHandler<T, U, V> sessionInterceptor(RequestHandler<T, SessionContext, V> handler) {
		return (request, response) -> {
			request.cookies().get("SESSION").ifPresentOrElse(
				sessionCookie -> {
					handler.handle(request.map(Function.identity(), ctx -> loadSession(ctx, sessionCookie.getValue())), response);
				},
				() -> {
					// We definitely return a new one
					handler.handle(request.map(Function.identity(), SessionContext::new), response);
				}
			);
		};
	}
	
	public static <T, U extends Context, V> RequestHandler<T, U, V> applicationInterceptor(RequestHandler<T, ApplicationContext, V> handler) {
		return (request, response) -> {
			handler.handle(request.map(Function.identity(), ApplicationContext::new), response);
		};
	}*/
	
	public static void bodyMapping() {
		
		RequestHandler<Toto, Void, ResponseBody> h1 = (request, response) -> {};
		RequestHandler<RequestBody, Void, ResponseBody> h11 = h1.map(h -> (request, response) -> h.handle(request.map(TestHandler::decode, Function.identity()), response));
		
		RequestHandler<RequestBody, Void, ResponseEntity<Toto>> h2 = (request, response) -> {
			response.body().value(new Toto("abc"));
		};
		RequestHandler<RequestBody, Void, ResponseBody> h22 = h2.map(h -> (request, response) -> h.handle(request, response.map(ResponseEntity::new)));
		
		// Put it together
		RequestHandler<Toto, Void, ResponseEntity<Tata>> h3 = (request, response) -> {
			response.body().value(new Tata("Response to: " + request.body().map(toto -> toto.getField()).orElse("")));
		};
		RequestHandler<RequestBody, Void, ResponseBody> h33 = h3.map(h -> (request, response) -> h.handle(request.map(TestHandler::decode, Function.identity()), response.map(ResponseEntity::new)));
		//h1.map(TestHandler::<Toto>decoderHandler);
		
		addRequestHandler(h3.map(h -> (request, response) -> h.handle(request.map(TestHandler::decode, Function.identity()), response.map(ResponseEntity::new))));
	}
	
	public static void requestInterceptor() {
		// This is what I want to have as a base for my application handler
		RequestHandler<RequestBody, ApplicationContext, ResponseBody> applicationHandler = (request, response) -> {};
		
		// This is what is expected by the server
		RequestHandler<RequestBody, Void, ResponseBody> serverHandler = (request, response) -> {};
		
		// I want to be able to invoke as many pre/post processors handlers I want in between 
		// These processors must all have the same input output signatures otherwise we don't have enough flexibility to compose them
		// However it is still possible to do such things at the cost of complexity
		
		// I must got from the applicationHandler to the serverHandler
		
		Function<RequestHandler<RequestBody, AttributeContext, ResponseBody>, RequestHandler<RequestBody, Void, ResponseBody>> attributeContextInterceptor = handler -> {
			return (request, response) -> {
				Function<Void, AttributeContext> attributeContextMapper = noContext -> {
					return new AttributeContext() {
						
						@Override
						public <T> void setAttribute(String name, T attribute) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void removeAttribute(String name) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public Set<String> getAttributeNames() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public <T> T getAttribute(String name) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public Map<String, Object> getAllAttributes() {
							// TODO Auto-generated method stub
							return null;
						}
					};
				};
				
				handler.handle(request.map(Function.identity(), attributeContextMapper), response);
			};
		};
		
		// TODO this is where we should create something: we can create various kind of interceptors to change the context, request body or response body
		// An interceptor is typically (request, response, handler) -> {...} where handler is the wrapped handler and return a RequestHandler
		Function<RequestHandler<RequestBody, AttributeContext, ResponseBody>, RequestHandler<RequestBody, AttributeContext, ResponseBody>> authenticationInterceptor = handler -> {
			return (request, response) -> {
				request.headers().get("Authorization")
					.flatMap(authorization -> authenticate(authorization.getHeaderValue()))
					.ifPresentOrElse(
						securityContext -> {
							request.context().setAttribute("SECURITY_CONTEXT", securityContext);
							// Chain the request
							handler.handle(request, response);
						},
						() -> {
							// Interupt the chain
							//   - interesting thought here: what if we perform authentication in a non blocking way (most likely to happen)
							//   => actually easy: we do everything in publishers on success we invoke the handler on error we invoke the 401 handler. This basically means authenticate() return a Mono instead of an Optional
							// 401 handler
							// This basically impose that we have a responseBody... so definitely handlers can't be chained that easily
							response.headers(headers -> headers.status(401)).body().empty();
						}
					);
			};
		};
		
		Function<RequestHandler<RequestBody, AttributeContext, ResponseBody>, RequestHandler<RequestBody, AttributeContext, ResponseBody>> sessionInterceptor = handler -> {
			return (request, response) -> {
				request.cookies().get("SESSION")
					.map(Cookie::getValue)
					.map(TestHandler::loadSession)
					.ifPresent(session -> request.context().setAttribute("SESSION", session));
				handler.handle(request, response);
			};
		};

		// This typically requires an interceptorBuilder of some sort 
		final String permission = "somePermission"; 
		
		Function<RequestHandler<RequestBody, AttributeContext, ResponseBody>, RequestHandler<RequestBody, AttributeContext, ResponseBody>> authorizationInterceptor = handler -> {
			return (request, response) -> {
				// We should have an interceptor factory 
				if(request.context().<SecurityContext>getAttribute("SECURITY").hasPermission(permission)) { // This could throw NPE
					// proceed
					handler.handle(request, response);
				}
				else {
					// 403 handler
					response.headers(headers -> headers.status(403)).body().empty();
				}
			};
		};
		
		Function<RequestHandler<RequestBody, ApplicationContext, ResponseBody>, RequestHandler<RequestBody, AttributeContext, ResponseBody>> attributeContextToApplicationContext = handler -> {
			return (request, response) -> {
				
				Function<AttributeContext, ApplicationContext> applicationContextMapper = attributeContext -> {
					return new ApplicationContext() {
						
						@Override
						public <T> void setAttribute(String name, T attribute) {
							request.context().setAttribute(name, attribute);
						}
						
						@Override
						public void removeAttribute(String name) {
							request.context().removeAttribute(name);
						}
						
						@Override
						public Set<String> getAttributeNames() {
							return request.context().getAttributeNames();
						}
						
						@Override
						public <T> T getAttribute(String name) {
							return request.context().getAttribute(name);
						}
						
						@Override
						public Map<String, Object> getAllAttributes() {
							return request.context().getAllAttributes();
						}
						
						@Override
						public Optional<Session> getSession() {
							return Optional.ofNullable(request.context().<Session>getAttribute("SESSION"));
						}
						
						@Override
						public Optional<SecurityContext> getSecurityContext() {
							return Optional.ofNullable(request.context().<SecurityContext>getAttribute("SECURITY_CONTEXT"));
						}
					};
				};
				handler.handle(request.map(Function.identity(), applicationContextMapper), response);
			};
		};
		
		// chain everything
		
		serverHandler = applicationHandler
			.map(attributeContextToApplicationContext)
			.map(authorizationInterceptor)
			.map(sessionInterceptor)
			.map(authenticationInterceptor)
			.map(attributeContextInterceptor);
	}
	
	public static void router() {
		
		RequestHandler<RequestBody, Void, ResponseBody> router = (request, response) -> {
		};
		
	}
	
	public static Optional<SecurityContext> authenticate(String credentials) {
		return Optional.empty();
	}
	
	public static Session loadSession(String sessionId) {
		// We return either the session we found or a nex one
		return null;
	}
}
