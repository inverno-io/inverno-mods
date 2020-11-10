package io.winterframework.mod.web.lab.sandbox;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import io.netty.buffer.Unpooled;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.RequestBody;
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
	
	public static abstract class ContextWrapper implements Context {
		
		private Optional<Context> previous;
		
		public ContextWrapper(Context previous) {
			this.previous = Optional.ofNullable(previous);
		}
		
		private Optional<Context> previous() {
			return this.previous;
		}
	}
	
	
	public static class SecurityContext extends ContextWrapper {
		
		public SecurityContext(Context previous) {
			super(previous);
		}
		
		boolean hasPermission(String permission) {
			return true;
		}
	}
	
	public static class SessionContext extends ContextWrapper {
		
		private String sessionId;
		
		public SessionContext(Context previous) {
			this(previous, "new generated");
		}
		
		public SessionContext(Context previous, String sessionId) {
			super(previous);
		}
		
		public void putValue(String key, String value) {
			
		}
		
		public String getValue(String key) {
			return "";
		}
	}
	
	public static class ApplicationContext implements Context {
		
		private Optional<SecurityContext> securityContext = Optional.empty();
		
		private Optional<SessionContext> sessionContext = Optional.empty();
		
		public ApplicationContext(Context previousContext) {
			Context current = previousContext;
			while(current != null) {
				if(current instanceof SecurityContext && !this.securityContext.isPresent()) {
					this.securityContext = Optional.of((SecurityContext)current);
				}
				else if(current instanceof SessionContext && !this.securityContext.isPresent()) {
					this.sessionContext = Optional.of((SessionContext)current);
				}
				else if(current instanceof ContextWrapper && ((ContextWrapper)current).previous().isPresent()) {
					current = ((ContextWrapper)current).previous().get();
				}
				else {
					break;
				}
			};
		}
		
		public Optional<SecurityContext> securityContext() {
			return this.securityContext;
		}
		
		public Optional<SessionContext> sessionContext() {
			return this.sessionContext;
		}
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
	public static <T> RequestHandler<RequestBody, ResponseBody> decoderHandler(RequestHandler<T, ResponseBody> sourceHandler) {
		return (request, response) -> sourceHandler.handle(request.map(TestHandler::decode), response);
	}
	
	// This what the server expect
	public static void addRequestHandler(RequestHandler<RequestBody, ResponseBody> handler) {
		
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
	
	public static void main(String[] args) {
		
		RequestHandler<Toto, ResponseBody> h1 = (request, response) -> {};
		h1.<RequestBody, ResponseBody>map(h -> (request, response) -> h.handle(request.map(TestHandler::decode), response));
//		h1.map(TestHandler::<Toto>decoderHandler);
		
		RequestHandler<RequestBody, ResponseEntity<Toto>> h2 = (request, response) -> {
			response.body().value(new Toto("abc"));
		};
		h2.<RequestBody, ResponseBody>map(h -> (request, response) -> h.handle(request, response.map(ResponseEntity::new)));
		
		// Put it together
		RequestHandler<Toto, ResponseEntity<Tata>> h3 = (request, response) -> {
			response.body().value(new Tata("Response to: " + request.body().map(toto -> toto.getField()).orElse("")));
		};
		h3.<RequestBody, ResponseBody>map(h -> (request, response) -> h.handle(request.map(TestHandler::decode), response.map(ResponseEntity::new)));
		//h1.map(TestHandler::<Toto>decoderHandler);
		
		addRequestHandler(h3.map(h -> (request, response) -> h.handle(request.map(TestHandler::decode), response.map(ResponseEntity::new))));
	}

}
