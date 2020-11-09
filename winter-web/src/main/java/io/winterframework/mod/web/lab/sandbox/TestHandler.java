package io.winterframework.mod.web.lab.sandbox;

import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;

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
			response.body().value(new Tata("Rsponse to: " + request.body().getField()));
		};
		h3.<RequestBody, ResponseBody>map(h -> (request, response) -> h.handle(request.map(TestHandler::decode), response.map(ResponseEntity::new)));
		//h1.map(TestHandler::<Toto>decoderHandler);
		
		addRequestHandler(h3.map(h -> (request, response) -> h.handle(request.map(TestHandler::decode), response.map(ResponseEntity::new))));
	}

}
