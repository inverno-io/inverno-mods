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
package io.inverno.mod.test.web.client;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.base.annotation.CookieParam;
import io.inverno.mod.web.base.annotation.FormParam;
import io.inverno.mod.web.base.annotation.HeaderParam;
import io.inverno.mod.web.base.annotation.PathParam;
import io.inverno.mod.web.base.annotation.QueryParam;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.client.WebResponse;
import io.inverno.mod.web.client.annotation.PartParam;
import io.inverno.mod.web.client.annotation.WebClient;
import io.inverno.mod.web.client.annotation.WebRoute;
import io.inverno.mod.test.web.client.dto.Message;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * - Exchange context should always be decalred as '?|T extends ContextType' because that's what we'll get eventually
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@WebClient(uri = "conf://testService")
public interface TestClient {

	@WebRoute(path = "/message", method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
	Mono<Void> create(@Body Message message);

	@WebRoute(path = "/message", consumes = MediaTypes.APPLICATION_JSON)
	Flux<Message> list();

	@WebRoute(path = "/message/{id}", consumes = MediaTypes.APPLICATION_JSON)
	Mono<Message> get(@PathParam String id);

	@WebRoute(path = "/message/{id}", method = Method.PUT, produces = MediaTypes.APPLICATION_JSON, consumes = MediaTypes.APPLICATION_JSON)
	Mono<Message> update(@PathParam String id, @Body Message message);

	@WebRoute(path = "/message/{id}", method = Method.DELETE)
	Mono<Void> delete(@PathParam String id);

	@WebRoute(path = "/get_mono_web_exchange", method = Method.GET)
	Mono<WebExchange<? extends ExchangeContext>> get_mono_web_exchange();

	@WebRoute(path = "/get_mono_web_exchange_generic", method = Method.GET)
	<T extends ExchangeContext> Mono<WebExchange<T>> get_mono_web_exchange_generic();

	@WebRoute(path = "/post_mono_web_exchange_with_params_and_body", method = Method.POST, produces = MediaTypes.TEXT_PLAIN)
	Mono<WebExchange<? extends ExchangeContext>> post_mono_web_exchange_with_params_and_body(@HeaderParam String headerparam, @Body String body);

	@WebRoute(path = "/get_mono_web_response", method = Method.GET)
	Mono<WebResponse> get_mono_web_response();

	@WebRoute(path = "/get_exchange", method = Method.GET)
	Mono<String> get_exchange(WebExchange.Configurer<ExchangeContext> exchange);

	@WebRoute(path = "/get_exchange_generic", method = Method.GET)
	<T extends ExchangeContext> Mono<String> get_exchange_generic(WebExchange.Configurer<T> exchange);

	@WebRoute(path = "/get_publisher_void", method = Method.GET)
	Publisher<Void> get_publisher_void();

	@WebRoute(path = "/get_publisher_raw", method = Method.GET)
	Publisher<ByteBuf> get_publisher_raw();

	@WebRoute(path = "/get_publisher_string", method = Method.GET)
	Publisher<String> get_publisher_string();

	@WebRoute(path = "/get_publisher_string_encoded", method = Method.GET, consumes = MediaTypes.TEXT_PLAIN)
	Publisher<String> get_publisher_string_encoded();

	@WebRoute(path = "/get_publisher_encoded", method = Method.GET, consumes = MediaTypes.APPLICATION_JSON)
	Publisher<Message> get_publisher_encoded();

	@WebRoute(path = "/post_publisher_raw", method = Method.POST)
	Mono<String> post_publisher_raw(@Body Publisher<ByteBuf> body);

	@WebRoute(path = "/post_publisher_string", method = Method.POST)
	Mono<String> post_publisher_string(@Body Publisher<String> body);

	@WebRoute(path = "/post_publisher_string_encoded", method = Method.POST, produces = MediaTypes.TEXT_PLAIN)
	Mono<String> post_publisher_string_encoded(@Body Publisher<String> body);

	@WebRoute(path = "/post_publisher_encoded", method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
	Mono<String> post_publisher_encoded(@Body Publisher<Message> body);

	@WebRoute(path = "/get_flux_void", method = Method.GET)
	Flux<Void> get_flux_void();

	@WebRoute(path = "/get_flux_raw", method = Method.GET)
	Flux<ByteBuf> get_flux_raw();

	@WebRoute(path = "/get_flux_string", method = Method.GET)
	Flux<String> get_flux_string();

	@WebRoute(path = "/get_flux_string_encoded", method = Method.GET, consumes = MediaTypes.TEXT_PLAIN)
	Flux<String> get_flux_string_encoded();

	@WebRoute(path = "/get_flux_encoded", method = Method.GET, consumes = MediaTypes.APPLICATION_JSON)
	Flux<Message> get_flux_encoded();

	@WebRoute(path = "/post_flux_raw", method = Method.POST)
	Mono<String> post_flux_raw(@Body Flux<ByteBuf> body);

	@WebRoute(path = "/post_flux_string", method = Method.POST)
	Mono<String> post_flux_string(@Body Flux<String> body);

	@WebRoute(path = "/post_flux_string_encoded", method = Method.POST, produces = MediaTypes.TEXT_PLAIN)
	Mono<String> post_flux_string_encoded(@Body Flux<String> body);

	@WebRoute(path = "/post_flux_encoded", method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
	Mono<String> post_flux_encoded(@Body Flux<Message> body);

	@WebRoute(path = "/get_mono_void", method = Method.GET)
	Mono<Void> get_mono_void();

	@WebRoute(path = "/get_mono_raw", method = Method.GET)
	Mono<ByteBuf> get_mono_raw();

	@WebRoute(path = "/get_mono_string", method = Method.GET)
	Mono<String> get_mono_string();

	@WebRoute(path = "/get_mono_string_encoded", method = Method.GET, consumes = MediaTypes.TEXT_PLAIN)
	Mono<String> get_mono_string_encoded();

	@WebRoute(path = "/get_mono_encoded", method = Method.GET, consumes = MediaTypes.APPLICATION_JSON)
	Mono<Message> get_mono_encoded();

	@WebRoute(path = "/post_mono_raw", method = Method.POST)
	Mono<String> post_mono_raw(@Body Mono<ByteBuf> body);

	@WebRoute(path = "/post_mono_string", method = Method.POST)
	Mono<String> post_mono_string(@Body Mono<String> body);

	@WebRoute(path = "/post_mono_string_encoded", method = Method.POST, produces = MediaTypes.TEXT_PLAIN)
	Mono<String> post_mono_string_encoded(@Body Mono<String> body);

	@WebRoute(path = "/post_mono_encoded", method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
	Mono<String> post_mono_encoded(@Body Mono<Message> body);

	@WebRoute(path = "/get_header_param", method = Method.GET)
	Mono<String> get_header_param(@HeaderParam String param);

	@WebRoute(path = "/get_header_param_collection", method = Method.GET)
	Mono<String> get_header_param_collection(@HeaderParam Collection<String> param);

	@WebRoute(path = "/get_header_param_list", method = Method.GET)
	Mono<String> get_header_param_list(@HeaderParam List<String> param);

	@WebRoute(path = "/get_header_param_set", method = Method.GET)
	Mono<String> get_header_param_set(@HeaderParam Set<String> param);

	@WebRoute(path = "/get_header_param_array", method = Method.GET)
	Mono<String> get_header_param_array(@HeaderParam String[] param);

	@WebRoute(path = "/get_cookie_param", method = Method.GET)
	Mono<String> get_cookie_param(@CookieParam String param);

	@WebRoute(path = "/get_cookie_param_collection", method = Method.GET)
	Mono<String> get_cookie_param_collection(@CookieParam Collection<String> param);

	@WebRoute(path = "/get_cookie_param_list", method = Method.GET)
	Mono<String> get_cookie_param_list(@CookieParam List<String> param);

	@WebRoute(path = "/get_cookie_param_set", method = Method.GET)
	Mono<String> get_cookie_param_set(@CookieParam Set<String> param);

	@WebRoute(path = "/get_cookie_param_array", method = Method.GET)
	Mono<String> get_cookie_param_array(@CookieParam String[] param);

	@WebRoute(path = "/get_query_param", method = Method.GET)
	Mono<String> get_query_param(@QueryParam String param);

	@WebRoute(path = "/get_query_param_collection", method = Method.GET)
	Mono<String> get_query_param_collection(@QueryParam Collection<String> param);

	@WebRoute(path = "/get_query_param_list", method = Method.GET)
	Mono<String> get_query_param_list(@QueryParam List<String> param);

	@WebRoute(path = "/get_query_param_set", method = Method.GET)
	Mono<String> get_query_param_set(@QueryParam Set<String> param);

	@WebRoute(path = "/get_query_param_array", method = Method.GET)
	Mono<String> get_query_param_array(@QueryParam String[] param);

	@WebRoute(path = "/get_path_param/{param}", method = Method.GET)
	Mono<String> get_path_param(@PathParam String param);

	@WebRoute(path = "/get_path_param_collection/{param}", method = Method.GET)
	Mono<String> get_path_param_collection(@PathParam Collection<String> param);

	@WebRoute(path = "/get_path_param_list/{param}", method = Method.GET)
	Mono<String> get_path_param_list(@PathParam List<String> param);

	@WebRoute(path = "/get_path_param_set/{param}", method = Method.GET)
	Mono<String> get_path_param_set(@PathParam Set<String> param);

	@WebRoute(path = "/get_path_param_array/{param}", method = Method.GET)
	Mono<String> get_path_param_array(@PathParam String[] param);

	@WebRoute(path = "/post_form_param", method = Method.POST, produces = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	Mono<String> post_form_param(@FormParam String param);

	@WebRoute(path = "/post_form_param_collection", method = Method.POST, produces = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	Mono<String> post_form_param_collection(@FormParam Collection<String> param);

	@WebRoute(path = "/post_form_param_list", method = Method.POST, produces = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	Mono<String> post_form_param_list(@FormParam List<String> param);

	@WebRoute(path = "/post_form_param_set", method = Method.POST, produces = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	Mono<String> post_form_param_set(@FormParam Set<String> param);

	@WebRoute(path = "/post_form_param_array", method = Method.POST, produces = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	Mono<String> post_form_param_array(@FormParam String[] param);

	@WebRoute(path = "/post_raw", method = Method.POST)
	Mono<String> post_raw(@Body ByteBuf body);

	@WebRoute(path = "/post_string", method = Method.POST)
	Mono<String> post_string(@Body String body);

	@WebRoute(path = "/post_encoded", method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
	Mono<String> post_encoded(@Body Message message);

	@WebRoute(path = "/post_encoded_collection", method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
	Mono<String> post_encoded_collection(@Body Collection<Message> messages);

	@WebRoute(path = "/post_encoded_list", method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
	Mono<String> post_encoded_list(@Body List<Message> messages);

	@WebRoute(path = "/post_encoded_set", method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
	Mono<String> post_encoded_set(@Body Set<Message> messages);

	@WebRoute(path = "/post_encoded_array", method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
	Mono<String> post_encoded_array(@Body Message[] messages);

	@WebRoute(path = "/post_multipart", method = Method.POST, produces = MediaTypes.MULTIPART_FORM_DATA)
	Mono<String> post_multipart(@PartParam String part, @PartParam(filename = "resourceFile") Resource resource, @PartParam(contentType = MediaTypes.APPLICATION_JSON) Message message);

	@WebRoute(path = "/post_multipart_flux_encoded", method = Method.POST, produces = MediaTypes.MULTIPART_FORM_DATA)
	Mono<String> post_multipart_flux_encoded(@PartParam(contentType = MediaTypes.APPLICATION_JSON) Flux<Message> messages);

	@WebRoute(path = "/post_multipart_mono_encoded", method = Method.POST, produces = MediaTypes.MULTIPART_FORM_DATA)
	Mono<String> post_multipart_mono_encoded(@PartParam(contentType = MediaTypes.APPLICATION_JSON) Mono<Message> message);

	@WebRoute(path = "/post_resource", method = Method.POST)
	Mono<String> post_resource(@Body Resource resource);

	@WebRoute(path = "/get_not_found", method = Method.GET)
	Mono<Void> get_not_found();

	@WebRoute(path = "/get_internal_server_error", method = Method.GET)
	Mono<String> get_internal_server_error();

	@WebRoute(path = "/post_mixed_parameters_body/{pathparam}", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON)
	Mono<String> post_mixed_parameters_body(@PathParam String pathparam, @QueryParam String queryparam, @HeaderParam String headerparam, @CookieParam String cookieparam, @Body Message message);

	@WebRoute(path = "/post_mixed_parameters_form/{pathparam}", method = Method.POST, produces = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	Mono<String> post_mixed_parameters_form(@PathParam String pathparam, @QueryParam String queryparam, @HeaderParam String headerparam, @CookieParam String cookieparam, @FormParam String formparam);

	@WebRoute(path = "/post_mixed_parameters_multipart/{pathparam}", method = Method.POST, produces = MediaTypes.MULTIPART_FORM_DATA)
	Mono<String> post_mixed_parameters_multipart(@PathParam String pathparam, @QueryParam String queryparam, @HeaderParam String headerparam, @CookieParam String cookieparam, @PartParam String partparam);
}
