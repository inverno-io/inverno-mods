/*
 * Copyright 2024 Jeremy KUHN
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
package io.inverno.mod.web.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.client.WebClient;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericWebClientTest {

	@Test
	public void testWebExchangeBuilder() {
		WebClient<ExchangeContext> webClient = Mockito.mock(WebClient.class);
		ObjectConverter<String> parameterConverter = new StringConverter();


		WebClient.WebExchangeBuilder<ExchangeContext> webExchangeBuilder = new GenericWebClient.GenericWebExchangeBuilder<>(webClient, parameterConverter, "https://test.com");

		LocalDate date = LocalDate.now();
		String dateString = date.toString();

		webExchangeBuilder
			.method(Method.POST)
			.path("/path/{pathParam1}/to/{pathParam2}")
			.pathParameter("pathParam1", "p1")
			.pathParameter("pathParam2", "p2", String.class)
			.queryParameter("queryParam1", date, LocalDate.class)
			.queryParameter("queryParam2", List.of("value21", "value22", "value23"))
			.build();

		Mockito.verify(webClient).exchange(Method.POST, URI.create("https://test.com/path/p1/to/p2?queryParam1=" + dateString + "&queryParam2=value21,value22,value23"));
	}
}
