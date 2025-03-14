/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.base.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.inverno.mod.base.Charsets;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class URIsTest {

	@Test
	public void testFromURI() throws URISyntaxException {
		URI baseURI = new URI("tata/titi/tutu?a=b&c=d&e=5631,569#!/tata/yoyo");
		URI newURI = URIs.uri(baseURI, URIs.Option.PARAMETERIZED).path("foo/bar/123").build();
		
		Assertions.assertEquals(new URI("tata/titi/tutu/foo/bar/123?a=b&c=d&e=5631,569#!/tata/yoyo"), newURI);
		
		baseURI = new URI("tata/titi/tutu/?a=b&c=d&e=5631,569#!/tata/yoyo");
		newURI = URIs.uri(baseURI, URIs.Option.PARAMETERIZED).path("foo/bar/123").build();
		
		Assertions.assertEquals(new URI("tata/titi/tutu/foo/bar/123?a=b&c=d&e=5631,569#!/tata/yoyo"), newURI);
		
		baseURI = new URI("/tata/titi/tutu?a=b&c=d&e=5631,569#!/tata/yoyo");
		newURI = URIs.uri(baseURI, URIs.Option.PARAMETERIZED).path("foo/bar/123").build();
		
		Assertions.assertEquals(new URI("/tata/titi/tutu/foo/bar/123?a=b&c=d&e=5631,569#!/tata/yoyo"), newURI);
		
		baseURI = new URI("/tata/titi/tutu/?a=b&c=d&e=5631,569#!/tata/yoyo");
		newURI = URIs.uri(baseURI, URIs.Option.PARAMETERIZED).path("foo/bar/123").build();
		
		Assertions.assertEquals(new URI("/tata/titi/tutu/foo/bar/123?a=b&c=d&e=5631,569#!/tata/yoyo"), newURI);
		
		baseURI = new URI("https://toto@127.0.0.1:1324/tata/titi/tutu?a%7Cb=b&c=d&e=5631,569#!/ta%7Cta/yoyo");
		newURI = URIs.uri(baseURI, URIs.Option.PARAMETERIZED).path("foo/bar/123").queryParameter("tutu€tata", "1322").build();
		
		Assertions.assertEquals(new URI("https://toto@127.0.0.1:1324/tata/titi/tutu/foo/bar/123?a%7Cb=b&c=d&e=5631,569&tutu%E2%82%ACtata=1322#!/ta%7Cta/yoyo"), newURI);
		
		baseURI = new URI("https://toto@127.0.0.1:1324/tata/titi/tutu/?a%7Cb=b&c=d&e=5631,569#!/ta%7Cta/yoyo");
		newURI = URIs.uri(baseURI, URIs.Option.PARAMETERIZED).path("foo/bar/123").queryParameter("tutu€tata", "1322").build();
		
		Assertions.assertEquals(new URI("https://toto@127.0.0.1:1324/tata/titi/tutu/foo/bar/123?a%7Cb=b&c=d&e=5631,569&tutu%E2%82%ACtata=1322#!/ta%7Cta/yoyo"), newURI);
		
		Assertions.assertEquals(URI.create("classpath:META-INF/inverno/web/openapi.yml"), URIs.uri(URI.create("resource:META-INF/inverno/web/openapi.yml")).scheme("classpath").build());
	}

	@Test
	public void testFromPath() throws URISyntaxException {
		URI newURI = URIs.uri("tata/titi/tutu", URIs.Option.PARAMETERIZED).queryParameter("a", "b").queryParameter("c", "d").queryParameter("e", "5631,569").fragment("!/tata/yoyo").build();
		Assertions.assertEquals(new URI("tata/titi/tutu?a=b&c=d&e=5631,569#!/tata/yoyo"), newURI);
		
		newURI = URIs.uri("tata/titi/tutu", URIs.Option.PARAMETERIZED).queryParameter("a", "b").queryParameter("c", "d").queryParameter("e", "5631,569").fragment("!/tata/yoyo").path("foo/bar/123").build();
		Assertions.assertEquals(new URI("tata/titi/tutu/foo/bar/123?a=b&c=d&e=5631,569#!/tata/yoyo"), newURI);
		
		newURI = URIs.uri("tata/titi/tutu/", URIs.Option.PARAMETERIZED).queryParameter("a", "b").queryParameter("c", "d").queryParameter("e", "5631,569").fragment("!/tata/yoyo").path("foo/bar/123").build();
		Assertions.assertEquals(new URI("tata/titi/tutu/foo/bar/123?a=b&c=d&e=5631,569#!/tata/yoyo"), newURI);
	
		newURI = URIs.uri("/tata/titi/tutu", URIs.Option.PARAMETERIZED).queryParameter("a", "b").queryParameter("c", "d").queryParameter("e", "5631,569").fragment("!/tata/yoyo").path("foo/bar/123").build();
		Assertions.assertEquals(new URI("/tata/titi/tutu/foo/bar/123?a=b&c=d&e=5631,569#!/tata/yoyo"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).queryParameter("a", "b").queryParameter("c", "d").queryParameter("e", "5631,569").fragment("!/tata/yoyo").path("foo/bar/123").build();
		Assertions.assertEquals(new URI("/tata/titi/tutu/foo/bar/123?a=b&c=d&e=5631,569#!/tata/yoyo"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).queryParameter("a", "b").segment("foo/bar/").build();
		Assertions.assertEquals(new URI("/tata/titi/tutu/foo%2Fbar%2F?a=b"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).scheme("https").build();
		Assertions.assertEquals(new URI("https:/tata/titi/tutu"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).host("127.0.0.1").build();
		Assertions.assertEquals(new URI("//127.0.0.1/tata/titi/tutu"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).scheme("https").host("127.0.0.1").build();
		Assertions.assertEquals(new URI("https://127.0.0.1/tata/titi/tutu"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).segment("").port(8080).build();
		Assertions.assertEquals(new URI("/tata/titi/tutu/"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).scheme("https").segment("").port(8080).build();
		Assertions.assertEquals(new URI("https:/tata/titi/tutu/"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).host("127.0.0.1").segment("").port(8080).build();
		Assertions.assertEquals(new URI("//127.0.0.1:8080/tata/titi/tutu/"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).scheme("https").host("127.0.0.1").segment("").port(8080).build();
		Assertions.assertEquals(new URI("https://127.0.0.1:8080/tata/titi/tutu/"), newURI);
		
		newURI = URIs.uri("/ta|ta/ti]ti/tu(tu/", URIs.Option.PARAMETERIZED).scheme("https").host("127.0.0.1").segment("").port(8080).build();
		Assertions.assertEquals(new URI("https://127.0.0.1:8080/ta%7Cta/ti%5Dti/tu(tu/"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).userInfo("user").build();
		Assertions.assertEquals(new URI("/tata/titi/tutu"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).scheme("https").userInfo("user").build();
		Assertions.assertEquals(new URI("https:/tata/titi/tutu"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).host("127.0.0.1").userInfo("user").build();
		Assertions.assertEquals(new URI("//user@127.0.0.1/tata/titi/tutu"), newURI);
		
		newURI = URIs.uri("/tata/titi/tutu/", URIs.Option.PARAMETERIZED).scheme("https").host("127.0.0.1").userInfo("user").build();
		Assertions.assertEquals(new URI("https://user@127.0.0.1/tata/titi/tutu"), newURI);
		
		newURI = URIs.uri("/ta|ta/ti]ti/tu(tu/", URIs.Option.PARAMETERIZED).scheme("https").host("127.0.0.1").userInfo("us@er").build();
		Assertions.assertEquals(new URI("https://us%40er@127.0.0.1/ta%7Cta/ti%5Dti/tu(tu"), newURI);
	}
	
	@Test
	public void testTemplate() throws URISyntaxException {
		URIBuilder portUriBuilder = URIs.uri("a/b/c", URIs.Option.PARAMETERIZED).host("127.0.0.1").port("{port}");

		Assertions.assertEquals("//127.0.0.1:{port}/a/b/c", portUriBuilder.toString());
		
		Assertions.assertEquals("//127.0.0.1:8080/a/b/c", portUriBuilder.buildString("8080"));
		Assertions.assertEquals("//127.0.0.1:8080/a/b/c", portUriBuilder.buildString(Map.of("port", "8080")));
		Assertions.assertEquals("/a/b/c", portUriBuilder.buildPath("http"));
		Assertions.assertEquals("/a/b/c", portUriBuilder.buildPath(Map.of("scheme", "http")));
		Assertions.assertEquals(new URI("//127.0.0.1:8080/a/b/c"), portUriBuilder.build("8080"));
		Assertions.assertEquals(new URI("//127.0.0.1:8080/a/b/c"), portUriBuilder.build(Map.of("port", "8080")));
		
		try {
			portUriBuilder.build("abc");
			Assertions.fail("Should throw a " + URIBuilderException.class);
		}
		catch(URIBuilderException e) {
			Assertions.assertEquals("Invalid character a found in URI component", e.getMessage());
		}
		
		URIBuilder schemeUriBuilder = URIs.uri("a/b/c", URIs.Option.PARAMETERIZED).scheme("{scheme}");

		Assertions.assertEquals("{scheme}:a/b/c", schemeUriBuilder.toString());
		
		Assertions.assertEquals("http:a/b/c", schemeUriBuilder.buildString("http"));
		Assertions.assertEquals("http:a/b/c", schemeUriBuilder.buildString(Map.of("scheme", "http")));
		Assertions.assertEquals("a/b/c", schemeUriBuilder.buildPath("http"));
		Assertions.assertEquals("a/b/c", schemeUriBuilder.buildPath(Map.of("scheme", "http")));
		Assertions.assertEquals(new URI("http:a/b/c"), schemeUriBuilder.build("http"));
		Assertions.assertEquals(new URI("http:a/b/c"), schemeUriBuilder.build(Map.of("scheme", "http")));
		
		try {
			schemeUriBuilder.build("#@55");
			Assertions.fail("Should throw a " + URIBuilderException.class);
		}
		catch(URIBuilderException e) {
			Assertions.assertEquals("Invalid character # found in URI component", e.getMessage());
		}
		
		URIBuilder userInfoUriBuilder = URIs.uri("a/b/c", URIs.Option.PARAMETERIZED).host("127.0.0.1").userInfo("_{param1}_{param2}_");

		Assertions.assertEquals("//_{param1}_{param2}_@127.0.0.1/a/b/c", userInfoUriBuilder.toString());
		
		Assertions.assertEquals("//_1_2_@127.0.0.1/a/b/c", userInfoUriBuilder.buildString("1", "2"));
		Assertions.assertEquals("//_1_2_@127.0.0.1/a/b/c", userInfoUriBuilder.buildString(Map.of("param2", 2, "param1", 1)));
		Assertions.assertEquals("/a/b/c", userInfoUriBuilder.buildPath("1", "2"));
		Assertions.assertEquals("/a/b/c", userInfoUriBuilder.buildPath(Map.of("param2", 2, "param1", 1)));
		Assertions.assertEquals(new URI("//_1_2_@127.0.0.1/a/b/c"), userInfoUriBuilder.build("1", "2"));
		Assertions.assertEquals(new URI("//_1_2_@127.0.0.1/a/b/c"), userInfoUriBuilder.build(Map.of("param2", 2, "param1", 1)));
		
		Assertions.assertEquals("//_1_2%7Cb_@127.0.0.1/a/b/c", userInfoUriBuilder.buildString("1", "2|b"));
		Assertions.assertEquals("//_1_2%7Cb_@127.0.0.1/a/b/c", userInfoUriBuilder.buildString(Map.of("param2", "2|b", "param1", 1)));
		Assertions.assertEquals("/a/b/c", userInfoUriBuilder.buildPath("1", "2|b"));
		Assertions.assertEquals("/a/b/c", userInfoUriBuilder.buildPath(Map.of("param2", "2|b", "param1", 1)));
		Assertions.assertEquals(new URI("//_1_2%7Cb_@127.0.0.1/a/b/c"), userInfoUriBuilder.build("1", "2|b"));
		Assertions.assertEquals(new URI("//_1_2%7Cb_@127.0.0.1/a/b/c"), userInfoUriBuilder.build(Map.of("param2", "2|b", "param1", 1)));
		
		URIBuilder hostUriBuilder = URIs.uri("a/b/c", URIs.Option.PARAMETERIZED).host("{hostname}{domain}");

		Assertions.assertEquals("//{hostname}{domain}/a/b/c", hostUriBuilder.toString());
		
		Assertions.assertEquals("//foo.example.com/a/b/c", hostUriBuilder.buildString("foo", ".example.com"));
		Assertions.assertEquals("//foo.example.com/a/b/c", hostUriBuilder.buildString(Map.of("hostname", "foo", "domain", ".example.com")));
		Assertions.assertEquals("/a/b/c", hostUriBuilder.buildPath("foo", ".example.com"));
		Assertions.assertEquals("/a/b/c", hostUriBuilder.buildPath(Map.of("hostname", "foo", "domain", ".example.com")));
		Assertions.assertEquals(new URI("//foo.example.com/a/b/c"), hostUriBuilder.build("foo", ".example.com"));
		Assertions.assertEquals(new URI("//foo.example.com/a/b/c"), hostUriBuilder.build(Map.of("hostname", "foo", "domain", ".example.com")));
		
		URIBuilder pathUriBuilder = URIs.uri("a/{param1}/b/_{param2:.*}_{param3}_", URIs.Option.PARAMETERIZED);

		Assertions.assertEquals("a/{param1}/b/_{param2:.*}_{param3}_", pathUriBuilder.toString());
		
		Assertions.assertEquals("a/1/b/_2_3_", pathUriBuilder.buildString("1", "2", "3"));
		Assertions.assertEquals("a/1/b/_2_3_", pathUriBuilder.buildString(Map.of("param2", 2, "param1", 1, "param3", 3)));
		Assertions.assertEquals("a/1/b/_2_3_", pathUriBuilder.buildPath("1", "2", "3"));
		Assertions.assertEquals("a/1/b/_2_3_", pathUriBuilder.buildPath(Map.of("param2", 2, "param1", 1, "param3", 3)));
		Assertions.assertEquals(new URI("a/1/b/_2_3_"), pathUriBuilder.build("1", "2", "3"));
		Assertions.assertEquals(new URI("a/1/b/_2_3_"), pathUriBuilder.build(Map.of("param2", 2, "param1", 1, "param3", 3)));
		
		Assertions.assertEquals("a/1/b/_2%7Cb_3_", pathUriBuilder.buildString("1", "2|b", "3"));
		Assertions.assertEquals("a/1/b/_2%7Cb_3_", pathUriBuilder.buildString(Map.of("param2", "2|b", "param1", 1, "param3", 3)));
		Assertions.assertEquals("a/1/b/_2%7Cb_3_", pathUriBuilder.buildPath("1", "2|b", "3"));
		Assertions.assertEquals("a/1/b/_2%7Cb_3_", pathUriBuilder.buildPath(Map.of("param2", "2|b", "param1", 1, "param3", 3)));
		Assertions.assertEquals(new URI("a/1/b/_2%7Cb_3_"), pathUriBuilder.build("1", "2|b", "3"));
		Assertions.assertEquals(new URI("a/1/b/_2%7Cb_3_"), pathUriBuilder.build(Map.of("param2", "2|b", "param1", 1, "param3", 3)));
		
		Assertions.assertEquals("a/1/b/_2%2Fb_3_", pathUriBuilder.buildString("1", "2/b", "3"));
		Assertions.assertEquals("a/1/b/_2%2Fb_3_", pathUriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3)));
		Assertions.assertEquals("a/1/b/_2%2Fb_3_", pathUriBuilder.buildPath("1", "2/b", "3"));
		Assertions.assertEquals("a/1/b/_2%2Fb_3_", pathUriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3)));
		Assertions.assertEquals(new URI("a/1/b/_2%2Fb_3_"), pathUriBuilder.build("1", "2/b", "3"));
		Assertions.assertEquals(new URI("a/1/b/_2%2Fb_3_"), pathUriBuilder.build(Map.of("param2", "2/b", "param1", 1, "param3", 3)));
		
		Assertions.assertEquals("a/1/b/_2/b_3_", pathUriBuilder.buildString(List.of("1", "2/b", "3"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", pathUriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", pathUriBuilder.buildPath(List.of("1", "2/b", "3"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", pathUriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3), false));
		Assertions.assertEquals(new URI("a/1/b/_2/b_3_"), pathUriBuilder.build(List.of("1", "2/b", "3"), false));
		Assertions.assertEquals(new URI("a/1/b/_2/b_3_"), pathUriBuilder.build(Map.of("param2", "2/b", "param1", 1, "param3", 3), false));
		
		URIBuilder queryUriBuilder = URIs.uri("a/{param1}/b/_{param2:.*}_{param3}_", URIs.Option.PARAMETERIZED).queryParameter("query_param1", "abc").queryParameter("queryParam2", "_{param4}_{param5}_");
		
		Assertions.assertEquals("a/{param1}/b/_{param2:.*}_{param3}_?query_param1=abc&queryParam2=_{param4}_{param5}_", queryUriBuilder.toString());
		
		Assertions.assertEquals("a/1/b/_2_3_?query_param1=abc&queryParam2=_4_5_", queryUriBuilder.buildString("1", "2", "3", "4", "5"));
		Assertions.assertEquals("a/1/b/_2_3_?query_param1=abc&queryParam2=_4_5_", queryUriBuilder.buildString(Map.of("param2", 2, "param1", 1, "param3", 3, "param4", 4, "param5", 5)));
		Assertions.assertEquals("a/1/b/_2_3_", queryUriBuilder.buildPath("1", "2", "3", "4", "5"));
		Assertions.assertEquals("a/1/b/_2_3_", queryUriBuilder.buildPath(Map.of("param2", 2, "param1", 1, "param3", 3, "param4", 4, "param5", 5)));
		Assertions.assertEquals(new URI("a/1/b/_2_3_?query_param1=abc&queryParam2=_4_5_"), queryUriBuilder.build("1", "2", "3", "4", "5"));
		Assertions.assertEquals(new URI("a/1/b/_2_3_?query_param1=abc&queryParam2=_4_5_"), queryUriBuilder.build(Map.of("param2", 2, "param1", 1, "param3", 3, "param4", 4, "param5", 5)));
		
		Assertions.assertEquals("a/1/b/_2%7Cb_3_?query_param1=abc&queryParam2=_4_5%7Ce_", queryUriBuilder.buildString("1", "2|b", "3", "4", "5|e"));
		Assertions.assertEquals("a/1/b/_2%7Cb_3_?query_param1=abc&queryParam2=_4_5%7Ce_", queryUriBuilder.buildString(Map.of("param2", "2|b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e")));
		Assertions.assertEquals("a/1/b/_2%7Cb_3_", queryUriBuilder.buildPath("1", "2|b", "3", "4", "5|e"));
		Assertions.assertEquals("a/1/b/_2%7Cb_3_", queryUriBuilder.buildPath(Map.of("param2", "2|b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e")));
		Assertions.assertEquals(new URI("a/1/b/_2%7Cb_3_?query_param1=abc&queryParam2=_4_5%7Ce_"), queryUriBuilder.build("1", "2|b", "3", "4", "5|e"));
		Assertions.assertEquals(new URI("a/1/b/_2%7Cb_3_?query_param1=abc&queryParam2=_4_5%7Ce_"), queryUriBuilder.build(Map.of("param2", "2|b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e")));
		
		Assertions.assertEquals("a/1/b/_2%2Fb_3_?query_param1=abc&queryParam2=_4_5%7Ce_", queryUriBuilder.buildString("1", "2/b", "3", "4", "5|e"));
		Assertions.assertEquals("a/1/b/_2%2Fb_3_?query_param1=abc&queryParam2=_4_5%7Ce_", queryUriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e")));
		Assertions.assertEquals("a/1/b/_2%2Fb_3_", queryUriBuilder.buildPath("1", "2/b", "3", "4", "5|e"));
		Assertions.assertEquals("a/1/b/_2%2Fb_3_", queryUriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e")));
		Assertions.assertEquals(new URI("a/1/b/_2%2Fb_3_?query_param1=abc&queryParam2=_4_5%7Ce_"), queryUriBuilder.build("1", "2/b", "3", "4", "5|e"));
		Assertions.assertEquals(new URI("a/1/b/_2%2Fb_3_?query_param1=abc&queryParam2=_4_5%7Ce_"), queryUriBuilder.build(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e")));
		
		Assertions.assertEquals("a/1/b/_2/b_3_?query_param1=abc&queryParam2=_4_5%7Ce_", queryUriBuilder.buildString(List.of("1", "2/b", "3", "4", "5|e"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_?query_param1=abc&queryParam2=_4_5%7Ce_", queryUriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", queryUriBuilder.buildPath(List.of("1", "2/b", "3", "4", "5|e"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", queryUriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e"), false));
		Assertions.assertEquals(new URI("a/1/b/_2/b_3_?query_param1=abc&queryParam2=_4_5%7Ce_"), queryUriBuilder.build(List.of("1", "2/b", "3", "4", "5|e"), false));
		Assertions.assertEquals(new URI("a/1/b/_2/b_3_?query_param1=abc&queryParam2=_4_5%7Ce_"), queryUriBuilder.build(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e"), false));
		
		URIBuilder fragmentUriBuilder = URIs.uri("a/{param1}/b/_{param2:.*}_{param3}_", URIs.Option.PARAMETERIZED).fragment("_{param4}_");

		Assertions.assertEquals("a/{param1}/b/_{param2:.*}_{param3}_#_{param4}_", fragmentUriBuilder.toString());
		
		Assertions.assertEquals("a/1/b/_2_3_#_4_", fragmentUriBuilder.buildString("1", "2", "3", "4"));
		Assertions.assertEquals("a/1/b/_2_3_#_4_", fragmentUriBuilder.buildString(Map.of("param2", 2, "param1", 1, "param3", 3, "param4", 4)));
		Assertions.assertEquals("a/1/b/_2_3_", fragmentUriBuilder.buildPath("1", "2", "3", "4"));
		Assertions.assertEquals("a/1/b/_2_3_", fragmentUriBuilder.buildPath(Map.of("param2", 2, "param1", 1, "param3", 3, "param4", 4)));
		Assertions.assertEquals(new URI("a/1/b/_2_3_#_4_"), fragmentUriBuilder.build("1", "2", "3", "4"));
		Assertions.assertEquals(new URI("a/1/b/_2_3_#_4_"), fragmentUriBuilder.build(Map.of("param2", 2, "param1", 1, "param3", 3, "param4", 4)));
		
		Assertions.assertEquals("a/1/b/_2%7Cb_3_#_4%7Cd_", fragmentUriBuilder.buildString("1", "2|b", "3", "4|d"));
		Assertions.assertEquals("a/1/b/_2%7Cb_3_#_4%7Cd_", fragmentUriBuilder.buildString(Map.of("param2", "2|b", "param1", 1, "param3", 3, "param4", "4|d")));
		Assertions.assertEquals("a/1/b/_2%7Cb_3_", fragmentUriBuilder.buildPath("1", "2|b", "3", "4|d"));
		Assertions.assertEquals("a/1/b/_2%7Cb_3_", fragmentUriBuilder.buildPath(Map.of("param2", "2|b", "param1", 1, "param3", 3, "param4", "4|d")));
		Assertions.assertEquals(new URI("a/1/b/_2%7Cb_3_#_4%7Cd_"), fragmentUriBuilder.build("1", "2|b", "3", "4|d"));
		Assertions.assertEquals(new URI("a/1/b/_2%7Cb_3_#_4%7Cd_"), fragmentUriBuilder.build(Map.of("param2", "2|b", "param1", 1, "param3", 3, "param4", "4|d")));
		
		Assertions.assertEquals("a/1/b/_2%2Fb_3_#_4%7Cd_", fragmentUriBuilder.buildString("1", "2/b", "3", "4|d"));
		Assertions.assertEquals("a/1/b/_2%2Fb_3_#_4%7Cd_", fragmentUriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", "4|d")));
		Assertions.assertEquals("a/1/b/_2%2Fb_3_", fragmentUriBuilder.buildPath("1", "2/b", "3", "4|d"));
		Assertions.assertEquals("a/1/b/_2%2Fb_3_", fragmentUriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", "4|d")));
		Assertions.assertEquals(new URI("a/1/b/_2%2Fb_3_#_4%7Cd_"), fragmentUriBuilder.build("1", "2/b", "3", "4|d"));
		Assertions.assertEquals(new URI("a/1/b/_2%2Fb_3_#_4%7Cd_"), fragmentUriBuilder.build(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", "4|d")));
		
		Assertions.assertEquals("a/1/b/_2/b_3_#_4%7Cd_", fragmentUriBuilder.buildString(List.of("1", "2/b", "3", "4|d"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_#_4%7Cd_", fragmentUriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", "4|d"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", fragmentUriBuilder.buildPath(List.of("1", "2/b", "3", "4|d"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", fragmentUriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", "4|d"), false));
		Assertions.assertEquals(new URI("a/1/b/_2/b_3_#_4%7Cd_"), fragmentUriBuilder.build(List.of("1", "2/b", "3", "4|d"), false));
		Assertions.assertEquals(new URI("a/1/b/_2/b_3_#_4%7Cd_"), fragmentUriBuilder.build(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", "4|d"), false));
		
		URIBuilder uriBuilder = URIs.uri("a/{param1}/b/_{param2:.*}_{param3}_", URIs.Option.PARAMETERIZED).scheme("https").host("127.0.0.1").segment("{param4}").queryParameter("foo", "bar").fragment("!fragment");
		
		Assertions.assertEquals("https://127.0.0.1/a/{param1}/b/_{param2:.*}_{param3}_/{param4}?foo=bar#!fragment", uriBuilder.toString());
		
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2_3_/4?foo=bar#!fragment", uriBuilder.buildString("1", "2", "3", "4"));
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2_3_/4?foo=bar#!fragment", uriBuilder.buildString(Map.of("param2", 2, "param1", 1, "param3", 3, "param4", 4)));
		Assertions.assertEquals("/a/1/b/_2_3_/4", uriBuilder.buildPath("1", "2", "3", "4"));
		Assertions.assertEquals("/a/1/b/_2_3_/4", uriBuilder.buildPath(Map.of("param2", 2, "param1", 1, "param3", 3, "param4", 4)));
		Assertions.assertEquals(new URI("https://127.0.0.1/a/1/b/_2_3_/4?foo=bar#!fragment"), uriBuilder.build("1", "2", "3", "4"));
		Assertions.assertEquals(new URI("https://127.0.0.1/a/1/b/_2_3_/4?foo=bar#!fragment"), uriBuilder.build(Map.of("param2", 2, "param1", 1, "param3", 3, "param4", 4)));
		
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2%7Cb_3_/4?foo=bar#!fragment", uriBuilder.buildString("1", "2|b", "3", "4"));
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2%7Cb_3_/4?foo=bar#!fragment", uriBuilder.buildString(Map.of("param2", "2|b", "param1", 1, "param3", 3, "param4", 4)));
		Assertions.assertEquals("/a/1/b/_2%7Cb_3_/4", uriBuilder.buildPath("1", "2|b", "3", "4"));
		Assertions.assertEquals("/a/1/b/_2%7Cb_3_/4", uriBuilder.buildPath(Map.of("param2", "2|b", "param1", 1, "param3", 3, "param4", 4)));
		Assertions.assertEquals(new URI("https://127.0.0.1/a/1/b/_2%7Cb_3_/4?foo=bar#!fragment"), uriBuilder.build("1", "2|b", "3", "4"));
		Assertions.assertEquals(new URI("https://127.0.0.1/a/1/b/_2%7Cb_3_/4?foo=bar#!fragment"), uriBuilder.build(Map.of("param2", "2|b", "param1", 1, "param3", 3, "param4", 4)));
		
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2%2Fb_3_/4?foo=bar#!fragment", uriBuilder.buildString("1", "2/b", "3", "4"));
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2%2Fb_3_/4?foo=bar#!fragment", uriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4)));
		Assertions.assertEquals("/a/1/b/_2%2Fb_3_/4", uriBuilder.buildPath("1", "2/b", "3", "4"));
		Assertions.assertEquals("/a/1/b/_2%2Fb_3_/4", uriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4)));
		Assertions.assertEquals(new URI("https://127.0.0.1/a/1/b/_2%2Fb_3_/4?foo=bar#!fragment"), uriBuilder.build("1", "2/b", "3", "4"));
		Assertions.assertEquals(new URI("https://127.0.0.1/a/1/b/_2%2Fb_3_/4?foo=bar#!fragment"), uriBuilder.build(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4)));
		
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment", uriBuilder.buildString(List.of("1", "2/b", "3", "4"), false));
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment", uriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4), false));
		Assertions.assertEquals("/a/1/b/_2/b_3_/4", uriBuilder.buildPath(List.of("1", "2/b", "3", "4"), false));
		Assertions.assertEquals("/a/1/b/_2/b_3_/4", uriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4), false));
		Assertions.assertEquals(new URI("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment"), uriBuilder.build(List.of("1", "2/b", "3", "4"), false));
		Assertions.assertEquals(new URI("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment"), uriBuilder.build(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4), false));
	}
	
	@Test
	public void testPattern() {
		URIBuilder uriBuilder = URIs.uri("a/{param1}/b/_{param2:.*}_{param3}_", URIs.Option.PARAMETERIZED).scheme("https").host("127.0.0.1").segment("{param4}").queryParameter("foo", "bar").fragment("!fragment");
		
		URIPattern uriPattern = uriBuilder.buildPattern();
		URIMatcher uriMatcher = uriPattern.matcher("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment");
		
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param1", "1", "param2", "2/b", "param3", "3", "param4", "4"), uriMatcher.getParameters());
		Assertions.assertEquals("1", uriMatcher.getParameterValue("param1").get());
		Assertions.assertEquals("2/b", uriMatcher.getParameterValue("param2").get());
		Assertions.assertEquals("3", uriMatcher.getParameterValue("param3").get());
		Assertions.assertEquals("4", uriMatcher.getParameterValue("param4").get());
		
		Assertions.assertTrue(uriMatcher.getMatcher().matches());
		Assertions.assertEquals(12, uriMatcher.getMatcher().groupCount());
		Assertions.assertEquals("1", uriMatcher.getMatcher().group(4));
		Assertions.assertEquals("2/b", uriMatcher.getMatcher().group(7));
		Assertions.assertEquals("3", uriMatcher.getMatcher().group(9));
		Assertions.assertEquals("4", uriMatcher.getMatcher().group(11));
		Assertions.assertEquals("1", uriMatcher.getMatcher().group("param1"));
		Assertions.assertEquals("2/b", uriMatcher.getMatcher().group("param2"));
		Assertions.assertEquals("3", uriMatcher.getMatcher().group("param3"));
		Assertions.assertEquals("4", uriMatcher.getMatcher().group("param4"));
		
		Assertions.assertTrue("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment".matches(uriPattern.getPatternString()));
		Assertions.assertFalse("https://127.0.0.1/A/1/b/_2/b_3_/4?foo=bar#!fragment".matches(uriPattern.getPatternString()));
		
		URIPattern uriPathPattern = uriBuilder.buildPathPattern();
		URIMatcher uriPathMatcher = uriPathPattern.matcher("/a/1/b/_2/b_3_/4");
		
		Assertions.assertTrue(uriPathMatcher.matches());
		Assertions.assertEquals(Map.of("param1", "1", "param2", "2/b", "param3", "3", "param4", "4"), uriPathMatcher.getParameters());
		Assertions.assertEquals("1", uriPathMatcher.getParameterValue("param1").get());
		Assertions.assertEquals("2/b", uriPathMatcher.getParameterValue("param2").get());
		Assertions.assertEquals("3", uriPathMatcher.getParameterValue("param3").get());
		Assertions.assertEquals("4", uriPathMatcher.getParameterValue("param4").get());
		
		Assertions.assertTrue(uriPathMatcher.getMatcher().matches());
		Assertions.assertEquals(9, uriPathMatcher.getMatcher().groupCount());
		Assertions.assertEquals("1", uriPathMatcher.getMatcher().group(2));
		Assertions.assertEquals("2/b", uriPathMatcher.getMatcher().group(5));
		Assertions.assertEquals("3", uriPathMatcher.getMatcher().group(7));
		Assertions.assertEquals("4", uriPathMatcher.getMatcher().group(9));
		Assertions.assertEquals("1", uriPathMatcher.getMatcher().group("param1"));
		Assertions.assertEquals("2/b", uriPathMatcher.getMatcher().group("param2"));
		Assertions.assertEquals("3", uriPathMatcher.getMatcher().group("param3"));
		Assertions.assertEquals("4", uriPathMatcher.getMatcher().group("param4"));
		
		Assertions.assertFalse("/A/1/b/_2/b_3_/4".matches(uriPathPattern.getPatternString()));
		
		uriBuilder = URIs.uri("/a/{p:[1-9][0-9]{0,2}}", URIs.Option.PARAMETERIZED);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriPathMatcher = uriPathPattern.matcher("/a/123");
		
		Assertions.assertTrue(uriPathMatcher.matches());
		Assertions.assertEquals(Map.of("p", "123"), uriPathMatcher.getParameters());
		Assertions.assertEquals("123", uriPathMatcher.getParameterValue("p").get());
		
		uriPathMatcher = uriPathPattern.matcher("/a/1000");
		
		Assertions.assertFalse(uriPathMatcher.matches());
		
		// Comparison
		
		URIBuilder p1 = URIs.uri("/a/{p1}_{p2}", URIs.Option.PARAMETERIZED);
		URIMatcher m1 = p1.buildPathPattern().matcher("/a/1_2");		
		URIBuilder p2 = URIs.uri("/a/{p}", URIs.Option.PARAMETERIZED);
		URIMatcher m2 = p2.buildPathPattern().matcher("/a/1_2");
		Assertions.assertTrue(m1.compareTo(m2) > 0);
		
		p1 = URIs.uri("/a/b_{p1}", URIs.Option.PARAMETERIZED);
		m1 = p1.buildPathPattern().matcher("/a/b_1");		
		p2 = URIs.uri("/a/{p}", URIs.Option.PARAMETERIZED);
		m2 = p2.buildPathPattern().matcher("/a/b_1");
		Assertions.assertTrue(m1.compareTo(m2) > 0);
		
		p1 = URIs.uri("/a/b/{p}", URIs.Option.PARAMETERIZED);
		m1 = p1.buildPathPattern().matcher("/a/b/c");
		p2 = URIs.uri("/a/{p}/c", URIs.Option.PARAMETERIZED);
		m2 = p2.buildPathPattern().matcher("/a/b/c");
		Assertions.assertTrue(m1.compareTo(m2) > 0);
		
		p1 = URIs.uri("/a/b/{p}", URIs.Option.PARAMETERIZED);
		m1 = p1.buildPathPattern().matcher("/a/b/c");
		p2 = URIs.uri("/a/{p:.*}", URIs.Option.PARAMETERIZED);
		m2 = p2.buildPathPattern().matcher("/a/b/c");
		Assertions.assertTrue(m1.compareTo(m2) > 0);
		
		p1 = URIs.uri("/a/{p}/c", URIs.Option.PARAMETERIZED);
		m1 = p1.buildPathPattern().matcher("/a/b/c");
		p2 = URIs.uri("/a/{p1}/{p2}", URIs.Option.PARAMETERIZED);
		m2 = p2.buildPathPattern().matcher("/a/b/c");
		Assertions.assertTrue(m1.compareTo(m2) > 0);
		
		p1 = URIs.uri("/a/b", URIs.Option.PARAMETERIZED);
		m1 = p1.buildPathPattern().matcher("/a/b");
		p2 = URIs.uri("/a/{p:.*}", URIs.Option.PARAMETERIZED);
		m2 = p2.buildPathPattern().matcher("/a/b");
		Assertions.assertTrue(m1.compareTo(m2) > 0);
		
		p1 = URIs.uri("/a/{p}/c", URIs.Option.PARAMETERIZED);
		m1 = p1.buildPathPattern().matcher("/a/b/c");
		p2 = URIs.uri("/a/{p:.*}", URIs.Option.PARAMETERIZED);
		m2 = p2.buildPathPattern().matcher("/a/b/c");
		Assertions.assertTrue(m1.compareTo(m2) > 0);
		
		p1 = URIs.uri("/a/{p1}/{p2}", URIs.Option.PARAMETERIZED);
		m1 = p1.buildPathPattern().matcher("/a/b/c");
		p2 = URIs.uri("/a/{p:.*}", URIs.Option.PARAMETERIZED);
		m2 = p2.buildPathPattern().matcher("/a/b/c");
		Assertions.assertTrue(m1.compareTo(m2) > 0);
	}
	
	@Test
	public void testNormalized() throws URISyntaxException {
		Assertions.assertEquals("/a/b//e", URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/../d/").segment(".").segment("..").segment("").segment("e").buildString());
		Assertions.assertEquals("/a/b//e", URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/../d/").segment(".").segment("..").segment("").segment("e").buildPath());
		Assertions.assertEquals(new URI("/a/b//e"), URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/../d/").segment(".").segment("..").segment("").segment("e").build());
		Assertions.assertEquals("/a/b//e", URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/../d/").segment(".").segment("..").segment("").segment("e").buildRawString());
		Assertions.assertEquals("/a/b//e", URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/../d/").segment(".").segment("..").segment("").segment("e").buildRawPath());
		Assertions.assertTrue("/a/b//e".matches(URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/../d/").segment(".").segment("..").segment("").segment("e").buildPattern().getPatternString()));
		
		Assertions.assertEquals("/../..", URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildString());
		Assertions.assertEquals("/../..", URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildPath());
		Assertions.assertEquals(new URI("/../.."), URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").build());
		Assertions.assertEquals("/../..", URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildRawString());
		Assertions.assertEquals("/../..", URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildRawPath());
		Assertions.assertTrue("/../..".matches(URIs.uri(URIs.Option.NORMALIZED).path("/a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildPattern().getPatternString()));
		
		
		Assertions.assertEquals("../..", URIs.uri(URIs.Option.NORMALIZED).path("a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildString());
		Assertions.assertEquals("../..", URIs.uri(URIs.Option.NORMALIZED).path("a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildPath());
		Assertions.assertEquals(new URI("../.."), URIs.uri(URIs.Option.NORMALIZED).path("a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").build());
		Assertions.assertEquals("../..", URIs.uri(URIs.Option.NORMALIZED).path("a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildRawString());
		Assertions.assertEquals("../..", URIs.uri(URIs.Option.NORMALIZED).path("a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildRawPath());
		Assertions.assertTrue("../..".matches(URIs.uri(URIs.Option.NORMALIZED).path("a/b/c/").segment("..").segment("..").segment("..").segment("..").segment("..").buildPattern().getPatternString()));
		
		Assertions.assertEquals("/a/b/f/e", URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").buildString("f"));
		Assertions.assertEquals("/a/b/e", URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").buildString("."));
		Assertions.assertEquals("/a/e", URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").buildString(".."));
		Assertions.assertEquals("/a/b/f/e", URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").buildPath("f"));
		Assertions.assertEquals("/a/b/e", URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").buildPath("."));
		Assertions.assertEquals("/a/e", URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").buildPath(".."));
		Assertions.assertEquals(new URI("/a/b/f/e"), URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").build("f"));
		Assertions.assertEquals(new URI("/a/b/e"), URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").build("."));
		Assertions.assertEquals(new URI("/a/e"), URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").build(".."));
		Assertions.assertEquals("/a/b/{param1}/e", URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").buildRawString());
		Assertions.assertEquals("/a/b/{param1}/e", URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").buildRawPath());

		// {param1} is considered as a regular segment that must be removed if followed by '..'
		Assertions.assertTrue(URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("..").segment("e").getParameterNames().isEmpty());
		Assertions.assertTrue("/a/b/f/e".matches(URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("e").buildPattern().getPatternString()));
		Assertions.assertTrue("/a/b/e".matches(URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).path("/a/b/{param1}/d/").segment("..").segment("..").segment("e").buildPattern().getPatternString()));
	}
	
	@Test
	public void testSlash() {
		Assertions.assertEquals("", URIs.uri().path("").buildPath());
		Assertions.assertEquals("/", URIs.uri().path("/").buildPath());
		
		Assertions.assertEquals("/a/b", URIs.uri().path("/").path("/a/b").buildPath());
		
		Assertions.assertEquals("/a/b/c/d", URIs.uri().path("/a/b").path("/c/d").buildPath());
		Assertions.assertEquals("/a/b/c/d", URIs.uri().path("/a/b").path("/c/d/").buildPath());
		Assertions.assertEquals("/a/b/c/d", URIs.uri().path("/a/b").path("/c/d", false).buildPath());
		Assertions.assertEquals("/a/b/c/d/", URIs.uri().path("/a/b").path("/c/d/", false).buildPath());
		
		Assertions.assertEquals("/a/b/", URIs.uri().path("/a/b").path("/", false).buildPath());
	}
	
	@Test
	public void testQuery() {
		Assertions.assertEquals("p1=v1&p2=v2&p3=v3", URIs.uri().queryParameter("p1", "v1").queryParameter("p2", "v2").queryParameter("p3", "v3").buildQuery());
		Assertions.assertEquals("p1=v1&p2=v2&p3=v3", URIs.uri("/a/b/c?p1=v1&p2=v2").queryParameter("p3", "v3").buildQuery());
		Assertions.assertEquals("p1=v1&p2=v2&p3=v3", URIs.uri("/a/b/c?p1={param1}&p2=v2", URIs.Option.PARAMETERIZED).queryParameter("p3", "{param3}").buildQuery("v1", "v3"));
		
		Assertions.assertEquals("p%201=v1&p2=v%7C2&p3=v3", URIs.uri().queryParameter("p 1", "v1").queryParameter("p2", "v|2").queryParameter("p3", "v3").buildQuery());
		Assertions.assertEquals("p1=v1&p2=v|2&p3=v3", URIs.uri().queryParameter("p1", "v1").queryParameter("p2", "v|2").queryParameter("p3", "v3").buildRawQuery());
		
		Assertions.assertEquals(Map.of("p1", List.of("v1"), "p2", List.of("v2"), "p3", List.of("v3")), URIs.uri().queryParameter("p1", "v1").queryParameter("p2", "v2").queryParameter("p3", "v3").getQueryParameters());
		Assertions.assertEquals(Map.of("p1", List.of("v1", "v12"), "p2", List.of("v2"), "p3", List.of("v3")), URIs.uri("/a/b/c?p1=v1&p2=v2").queryParameter("p1", "v12").queryParameter("p3", "v3").getQueryParameters());
		Assertions.assertEquals(Map.of("p1", List.of("v1"), "p2", List.of("v2"), "p3", List.of("v3")), URIs.uri("/a/b/c?p1={param1}&p2=v2", URIs.Option.PARAMETERIZED).queryParameter("p3", "{param3}").getQueryParameters("v1", "v3"));
		
		Assertions.assertTrue(URIs.uri("/a/b/c?this_is_a_test").getRawQueryParameters().isEmpty());
		Assertions.assertEquals("this_is_a_test", URIs.uri("/a/b/c?this_is_a_test").buildQuery());
		
		Assertions.assertEquals("this_{param1}_a_{param2}", URIs.uri("/a/b/c?this_{param1}_a_{param2}", URIs.Option.PARAMETERIZED).buildRawQuery());
		Assertions.assertEquals("this_is_a_test", URIs.uri("/a/b/c?this_{param1}_a_{param2}", URIs.Option.PARAMETERIZED).buildQuery("is", "test"));
		Assertions.assertEquals("te%7Cst", URIs.uri("/a/b/c?a=b").query("te|st").buildQuery());
		Assertions.assertEquals("te|st", URIs.uri("/a/b/c?a=b").query("te|st").buildRawQuery());
	}
	
	@Test
	public void testScanParameters() {
		List<URIParameter> parameters = null;
		Iterator<URIParameter> parametersIterator = null;
		URIParameter parameter = null;
		
		parameters = new LinkedList<>();
		URIs.scanURIComponent("/book/{id:[1-9][0-9]{0,2}}/tata/{name:[A-Z][a-z]{0,50}}/{action}/{:.*}", null, Charsets.DEFAULT, parameters::add, null);
		
		Assertions.assertEquals(4, parameters.size());
		parametersIterator = parameters.iterator();
		parameter = parametersIterator.next();
		Assertions.assertEquals("id", parameter.getName());
		Assertions.assertEquals("(?<id>[1-9][0-9]{0,2})", parameter.getPattern());
		parameter = parametersIterator.next();
		Assertions.assertEquals("name", parameter.getName());
		Assertions.assertEquals("(?<name>[A-Z][a-z]{0,50})", parameter.getPattern());
		parameter = parametersIterator.next();
		Assertions.assertEquals("action", parameter.getName());
		Assertions.assertEquals("(?<action>[^/]*)", parameter.getPattern());
		parameter = parametersIterator.next();
		Assertions.assertNull(parameter.getName());
		Assertions.assertEquals("(.*)", parameter.getPattern());
		
		parameters = new LinkedList<>();
		URIs.scanURIComponent("/{patternWithBrace:a\\{b\\}c}", null, Charsets.DEFAULT, parameters::add, null);
		Assertions.assertEquals(1, parameters.size());
		parametersIterator = parameters.iterator();
		parameter = parametersIterator.next();
		Assertions.assertEquals("patternWithBrace", parameter.getName());
		Assertions.assertEquals("(?<patternWithBrace>a\\{b\\}c)", parameter.getPattern());
		
		parameters = new LinkedList<>();
		try {
			URIs.scanURIComponent("/book/{#invalid}", null, Charsets.DEFAULT, parameters::add, null);
			Assertions.fail("Should throw a URIBuilderException");
		} 
		catch (URIBuilderException e) {}
		
		try {
			URIs.scanURIComponent("/book/{invalid-id}", null, Charsets.DEFAULT, parameters::add, null);
			Assertions.fail("Should throw a URIBuilderException");
		} 
		catch (URIBuilderException e) {}
	}
	
	@Test
	public void testEmptySegment() {
		// We need to add four emtpy segment to represent "////..." because the first empty segment indicates an absolute path and the next three ones indicate the three empty segments we want to create
		Assertions.assertEquals("////a2billing/customer/templates/default/footer.tpl", URIs.uri(URIs.Option.NORMALIZED).segment("").segment("").segment("").segment("").segment("a2billing").segment("customer").segment("templates").segment("default").segment("footer.tpl").buildString());
		
		// We can wonder whether we should keep it that way if the URI has a host component, in which case we'll necessarily have an absolute path...
		Assertions.assertEquals("http://localhost////a2billing/customer/templates/default/footer.tpl", URIs.uri(URIs.Option.NORMALIZED).scheme("http").host("localhost").segment("").segment("").segment("").segment("").segment("a2billing").segment("customer").segment("templates").segment("default").segment("footer.tpl").buildString());

		Assertions.assertEquals("/a2billing/customer/templates/default/footer.tpl", URIs.uri(URIs.Option.NORMALIZED).scheme("http").host("localhost").segment("a2billing").segment("customer").segment("templates").segment("default").segment("footer.tpl").buildPath());
		Assertions.assertEquals("a2billing/customer/templates/default/footer.tpl", URIs.uri(URIs.Option.NORMALIZED).segment("a2billing").segment("customer").segment("templates").segment("default").segment("footer.tpl").buildPath());
		
		Assertions.assertEquals("////a2billing/customer/templates/default/footer.tpl", URIs.uri(URIs.Option.NORMALIZED).scheme("http").host("localhost").segment("").segment("").segment("").segment("").segment("a2billing").segment("customer").segment("templates").segment("default").segment("footer.tpl").buildPath());
		
		Assertions.assertEquals("////a2billing/customer/templates/default/footer.tpl", URIs.uri(URIs.Option.NORMALIZED).path("////a2billing/customer/templates/default/footer.tpl").buildString());
		
		Assertions.assertEquals("////a2billing/customer/templates/default/footer.tpl", URIs.uri("////a2billing/customer/templates/default/footer.tpl", URIs.Option.NORMALIZED).buildString());
	}
	
	@Test
	public void testPathPattern() {
		SegmentComponent sc = new SegmentComponent(new URIFlags(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN), Charsets.UTF_8, "abc*def?ghi*klm");
		
		Assertions.assertEquals("abc*def?ghi*klm", sc.getRawValue());
		Assertions.assertEquals("(\\Qabc\\E)([^/]*)(\\Qdef\\E)([^/])(\\Qghi\\E)([^/]*)(\\Qklm\\E)", sc.getPattern());
		Assertions.assertEquals("abcXXXdefYghiZZZklm", sc.getValue(List.of("XXX", "Y", "ZZZ")));
		
		sc = new SegmentComponent(new URIFlags(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN), Charsets.UTF_8, "abc*def{test}ghi?klm");
		
		Assertions.assertEquals("abc*def{test}ghi?klm", sc.getRawValue());
		Assertions.assertEquals("(\\Qabc\\E)([^/]*)(\\Qdef\\E)(?<test>[^/]*)(\\Qghi\\E)([^/])(\\Qklm\\E)", sc.getPattern());
		Assertions.assertEquals("abcXXXdefYYYghiZklm", sc.getValue(List.of("XXX", "YYY", "Z")));
		
		sc = new SegmentComponent(new URIFlags(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN), Charsets.UTF_8, "**");
		
		Assertions.assertEquals("([^/]*(?:/[^/]*)*)", sc.getPattern());
		Assertions.assertEquals("a/b/c", sc.getValue(List.of("a/b/c"), false));
		
		try {
			new SegmentComponent(new URIFlags(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN), Charsets.UTF_8, "a**");
			Assertions.fail("Should throw a URIBuilderException");
		}
		catch(URIBuilderException e) {
			Assertions.assertEquals("Invalid usage of path pattern '**' which is exclusive: /a**", e.getMessage());
		}
		
		try {
			new SegmentComponent(new URIFlags(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN), Charsets.UTF_8, "**b");
			Assertions.fail("Should throw a URIBuilderException");
		}
		catch(URIBuilderException e) {
			Assertions.assertEquals("Invalid usage of path pattern '**' which is exclusive: /**b", e.getMessage());
		}
		
		try {
			URIs.uri("/a/**/*", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
			Assertions.fail("Should throw a URIBuilderException");
		} catch (URIBuilderException e) {
			Assertions.assertEquals("PATH_QUERY form request-target is incompatible with PATH_PATTERN option", e.getMessage());
		}
		
		URIBuilder uriBuilder = URIs.uri("/a/**/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		
		URIPattern uriPathPattern = uriBuilder.buildPathPattern();
		
		URIMatcher uriMatcher = uriPathPattern.matcher("/a/b/c/toto.jsp");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertTrue(uriMatcher.getParameters().isEmpty());
		
		uriBuilder = URIs.uri("/a/**/*.jsp", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriMatcher = uriPathPattern.matcher("/a/b/c/toto.jsp");
		Assertions.assertTrue(uriMatcher.matches());
		uriMatcher = uriPathPattern.matcher("/a/b/c/toto.png");
		Assertions.assertFalse(uriMatcher.matches());
		
		uriBuilder = URIs.uri("/a/**/{file}.jsp", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriMatcher = uriPathPattern.matcher("/a/b/c/toto.jsp");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("file", "toto"), uriMatcher.getParameters());
		uriMatcher = uriPathPattern.matcher("/a/b/c/toto.png");
		Assertions.assertFalse(uriMatcher.matches());
		
		uriBuilder = URIs.uri("/a/**/test*/toto", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriMatcher = uriPathPattern.matcher("/a/b/c/test123/toto");
		Assertions.assertTrue(uriMatcher.matches());
		
		uriBuilder = URIs.uri("/a/test_?/*.jsp", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriMatcher = uriPathPattern.matcher("/a/test_1/toto.jsp");
		Assertions.assertTrue(uriMatcher.matches());
		uriMatcher = uriPathPattern.matcher("/a/test_12/toto.jsp");
		Assertions.assertFalse(uriMatcher.matches());
		uriMatcher = uriPathPattern.matcher("/a/test_1/toto.png");
		Assertions.assertFalse(uriMatcher.matches());
		
		uriBuilder = URIs.uri("/a/test_?/{file}.jsp", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriMatcher = uriPathPattern.matcher("/a/test_1/toto.jsp");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("file", "toto"), uriMatcher.getParameters());
		uriMatcher = uriPathPattern.matcher("/a/test_1/toto.png");
		Assertions.assertFalse(uriMatcher.matches());
		
		uriBuilder = URIs.uri("/a/test_*_test/folder_?_?_*/**/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriMatcher = uriPathPattern.matcher("/a/test_abc_test/folder_1_2_name/a/b/c");
		Assertions.assertTrue(uriMatcher.matches());
		
		uriBuilder = URIs.uri("/a/test_*_test/folder_?_?_*/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriMatcher = uriPathPattern.matcher("/a/test_abc_test/folder_1_2_name");
		Assertions.assertTrue(uriMatcher.matches());
		uriMatcher = uriPathPattern.matcher("/a/test_abc_test/folder_1_2_name/");
		Assertions.assertTrue(uriMatcher.matches());
		uriMatcher = uriPathPattern.matcher("/a/test_abc_test/folder_1_2_name/a/b/c");
		Assertions.assertTrue(uriMatcher.matches());
		
		uriBuilder = URIs.uri("/a/test_*_test/folder_?_?_*/**/{end}", URIs.RequestTargetForm.PATH, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriMatcher = uriPathPattern.matcher("/a/test_abc_test/folder_1_2_name/a/b/c/{end}");
		Assertions.assertTrue(uriMatcher.matches());
		
		uriBuilder = URIs.uri("/a/test_*_test/folder_?_?_*/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED);
		uriPathPattern = uriBuilder.buildPathPattern();
		uriMatcher = uriPathPattern.matcher("/a/test_abc_test/folder_1_2_name/a/b/c");
		Assertions.assertFalse(uriMatcher.matches());
		uriMatcher = uriPathPattern.matcher("/a/test_*_test/folder_?_?_*/**");
		Assertions.assertTrue(uriMatcher.matches());
		
		uriBuilder = URIs.uri("/a/b/**/{param1}/c/{param2}/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		
		uriMatcher = uriPathPattern.matcher("/a/b/value1/c/value2/d");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param1", "value1", "param2", "value2"), uriMatcher.getParameters());
		
		uriMatcher = uriPathPattern.matcher("/a/b/1/value1/c/value2/d");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param1", "value1", "param2", "value2"), uriMatcher.getParameters());
		
		uriMatcher = uriPathPattern.matcher("/a/b/1/2/3/value1/c/value2/d");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param1", "value1", "param2", "value2"), uriMatcher.getParameters());
		
		uriBuilder = URIs.uri("/a/b/**/*/*/{param1}/c/{param2}/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();
		
		uriMatcher = uriPathPattern.matcher("/a/b/value1/c/value2/d");
		Assertions.assertFalse(uriMatcher.matches());
		
		uriMatcher = uriPathPattern.matcher("/a/b/1/value1/c/value2/d");
		Assertions.assertFalse(uriMatcher.matches());
		
		uriMatcher = uriPathPattern.matcher("/a/b/1/2/value1/c/value2/d");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param1", "value1", "param2", "value2"), uriMatcher.getParameters());
		
		uriMatcher = uriPathPattern.matcher("/a/b/1/2/3/4/value1/c/value2/d");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param1", "value1", "param2", "value2"), uriMatcher.getParameters());

		uriBuilder = URIs.uri("/a/b/{param:**}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();

		uriMatcher = uriPathPattern.matcher("/a/b");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param", ""), uriMatcher.getParameters());

		uriMatcher = uriPathPattern.matcher("/a/b/c");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param", "c"), uriMatcher.getParameters());

		uriMatcher = uriPathPattern.matcher("/a/b/c/d/e");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param", "c/d/e"), uriMatcher.getParameters());

		Assertions.assertEquals(
			"Invalid usage of path pattern '**' which is exclusive: /_{param:**}",
			Assertions.assertThrows(URIBuilderException.class, () -> URIs.uri("/a/b/_{param:**}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN)).getMessage()
		);

		Assertions.assertEquals(
			"Invalid usage of path pattern '**' which is exclusive: /{param:**}_",
			Assertions.assertThrows(URIBuilderException.class, () -> URIs.uri("/a/b/{param:**}_", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN)).getMessage()
		);

		uriBuilder = URIs.uri("/a/b/{param:*}/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();

		uriMatcher = uriPathPattern.matcher("/a/b/c");
		Assertions.assertFalse(uriMatcher.matches());

		uriMatcher = uriPathPattern.matcher("/a/b//c");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param", ""), uriMatcher.getParameters());

		uriMatcher = uriPathPattern.matcher("/a/b/segment/c");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param", "segment"), uriMatcher.getParameters());

		uriBuilder = URIs.uri("/a/b/_{param:*}_/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();

		uriMatcher = uriPathPattern.matcher("/a/b/c");
		Assertions.assertFalse(uriMatcher.matches());

		uriMatcher = uriPathPattern.matcher("/a/b/__/c");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param", ""), uriMatcher.getParameters());

		uriMatcher = uriPathPattern.matcher("/a/b/_segment_/c");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param", "segment"), uriMatcher.getParameters());

		uriBuilder = URIs.uri("/a/b/{param:?}/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		uriPathPattern = uriBuilder.buildPathPattern();

		uriMatcher = uriPathPattern.matcher("/a/b/c");
		Assertions.assertFalse(uriMatcher.matches());

		uriMatcher = uriPathPattern.matcher("/a/b//c");
		Assertions.assertFalse(uriMatcher.matches());

		uriMatcher = uriPathPattern.matcher("/a/b/x/c");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param", "x"), uriMatcher.getParameters());
	}
	
	@Test
	public void testInclusion() {
		URIPattern routePattern = URIs.uri("/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		URIPattern interceptorPattern = URIs.uri("/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/?/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/?*/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*?/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/x?/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/**/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/*/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/*/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/?/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/?/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b/?", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b/*/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b*/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b*x/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/c/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/{toto:(?:abc|def)}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/{:(?:abc|def)}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/{toto:(?:abc|def)}/b", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/abc/b", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/{:(?:abc|def)}/b/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/x_{toto:(?:abc|def)}_y/b", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/abc/b", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/x_abc_y/b", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/abc/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/{:(?:abc|def)}/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/x_{:(?:abc|def)}_y/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/{toto:(?:abc|def)}/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/*/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/x/**/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/{toto:(?:abc|def)}/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/c/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b/c/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b/c/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
	
		interceptorPattern = URIs.uri("/a/b/c/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b/c/**/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/**/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/**/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/**/c/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/**/x?y/c/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/*/*/*/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/**/exit/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/**/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/b/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/*/b/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/x/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/x/y/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/**/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/**/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/*/**/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		interceptorPattern = URIs.uri("/a/*/**/b/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
	
		routePattern = URIs.uri("/a/{toto:(?:abc|def)}/{tata:(?:ghi|klm)}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/*/{toto:(?:abc|def)}/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		
		/* ---- */
		routePattern = URIs.uri("/*/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/*/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/*/c/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/**/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/{toto:(?:abc|def)}/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/{toto:(?:abc|def)}/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/{toto:(?:abc|def)}/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/{toto:(?:abc|def)}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/c/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c/**", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/**/d/e/f/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/**/d/e", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/**/a/a/b", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/a/a/b", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/x/y/z/**/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/x/y/z/**/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.DISJOINT, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/x/y/z/**/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c/**/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/**/b/**/c/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/**/c/**/c/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/b/c/b/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/b/*", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INCLUDED, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/{toto:(?:abc|def)}/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/x/**/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/*/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/x/**/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/*/*/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/**/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/a/**/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/a/{toto:(?:abc|def)}/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		// We don't know what is matched by the custom pattern, we only know it matches at least one segment, since both path ends with /b/c we are indeterminate at best
		interceptorPattern = URIs.uri("/a/{:(?:abc|def)}/a/b/c", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/x/y/z/**/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c/{:(?:abc|def)}/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/x/y/z/{:(?:abc|def)}/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c/**/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
		
		routePattern = URIs.uri("/x/y/z/{:(?:abc|def)}/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		
		interceptorPattern = URIs.uri("/**/a/b/c/{:(?:abc|def|ghi)}/d", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern();
		Assertions.assertEquals(URIPattern.Inclusion.INDETERMINATE, interceptorPattern.includes(routePattern));
	}

	@Test
	public void testUriPattern() throws URISyntaxException {
		URIBuilder uriBuilder = URIs.uri("/a/{param1}/b/_{param2:.*}_{param3}_", URIs.Option.PARAMETERIZED).scheme("https").host("127.0.0.1").segment("{param4}").queryParameter("foo", "bar").fragment("!fragment");
		URIPattern uriPattern = uriBuilder.buildPattern();
		URIMatcher uriMatcher = uriPattern.matcher("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment");
		Assertions.assertTrue(uriMatcher.matches());

		uriBuilder = URIs.uri("/{:.*}", URIs.Option.PARAMETERIZED).scheme("service").host("{:.*}");
		uriPattern = uriBuilder.buildPattern();
		uriMatcher = uriPattern.matcher("service://host/any/path");
		Assertions.assertTrue(uriMatcher.matches());

		uriBuilder = URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).scheme("{:(http|https)}").host("{:*}");
		uriPattern = uriBuilder.buildPattern();

		Assertions.assertTrue(uriPattern.matcher("https://test").matches());
		Assertions.assertTrue(uriPattern.matcher("http://other-test").matches());
		Assertions.assertTrue(uriPattern.matcher("http://other-test").matches());
	}

	@Test
	public void testOpaque() {
		URIBuilder uriBuilder = URIs.uri("/a/b/c", URIs.Option.PARAMETERIZED).scheme("urn").authority("{authority:book}").queryParameter("foo", "bar").fragment("!fragment");
		Assertions.assertEquals(URI.create("urn:book/a/b/c?foo=bar#!fragment"), uriBuilder.build("book"));

		URIPattern uriPattern = uriBuilder.buildPattern();
		URIMatcher uriMatcher = uriPattern.matcher("urn:book/a/b/c?foo=bar#!fragment");
		Assertions.assertTrue(uriMatcher.matches());

		uriMatcher = uriPattern.matcher("urn:tool/a/b/c?foo=bar#!fragment");
		Assertions.assertTrue(!uriMatcher.matches());

		uriBuilder = URIs.uri("/a/b/c", URIs.Option.PARAMETERIZED).scheme("urn").authority("/{authority}").queryParameter("foo", "bar").fragment("!fragment");
		Assertions.assertEquals(URI.create("urn://book/a/b/c?foo=bar#!fragment"), uriBuilder.build("book"));
	}

	@Test
	public void testPathPatternParameters() {
		URIBuilder uriBuilder = URIs.uri("/a/b/{param1:**}/c/{param2}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		URIPattern uriPathPattern = uriBuilder.buildPathPattern();

		URIMatcher uriMatcher = uriPathPattern.matcher("/a/b/x/y/c/value");
		Assertions.assertTrue(uriMatcher.matches());
		Assertions.assertEquals(Map.of("param1", "x/y", "param2", "value"), uriMatcher.getParameters());

		Map<String, String> parameters = uriMatcher.getParameters();

		uriBuilder = URIs.uri("/a/b/{param2}/c/{param1:**}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		Assertions.assertEquals("/a/b/value/c/x/y",  uriBuilder.buildPath(parameters));
	}

	@Test
	public void testUnnamedPathPatternParameters() {
		URIBuilder uriBuilder = URIs.uri("/a/b/{:[a-z]*}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);

		List<String> parameterNames = uriBuilder.getParameterNames();
		Assertions.assertEquals(1, parameterNames.size());
		Assertions.assertNull(parameterNames.getFirst());

		uriBuilder = URIs.uri("/a/b/{:[a-z]*}/{p1}", URIs.RequestTargetForm.PATH, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);

		parameterNames = uriBuilder.getParameterNames();
		Assertions.assertEquals(2, parameterNames.size());
		Assertions.assertNull(parameterNames.getFirst());
		Assertions.assertEquals("p1", parameterNames.get(1));
	}

	@Test
	public void testPathAndQueryParameters() {
		URIBuilder uriBuilder = URIs.uri("/a/b/{v1}/c/{v2}?p1={v3}&p2={v4}", URIs.RequestTargetForm.PATH_QUERY, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED);
		Assertions.assertEquals("/a/b/A%20X/c/B?p1=C&p2=%26D", uriBuilder.buildString(Map.of("v1", "A X", "v2", "B", "v3", "C", "v4", "&D")));

		uriBuilder = URIs.uri("/", URIs.RequestTargetForm.PATH_QUERY, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED);
		uriBuilder.path("/test");
		uriBuilder.queryParameter("param", "value");

		Assertions.assertEquals("/test?param=value", uriBuilder.buildString());
	}

	@Test
	public void testTranslatePath() {
		URIBuilder matcherPathBuilder = URIs.uri("/v1/{path:**}", URIs.RequestTargetForm.PATH, false, new URIs.Option[]{URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN});
		URIPattern pathPattern = matcherPathBuilder.buildPathPattern(false);
		URIMatcher pathMatcher = pathPattern.matcher("/v1/b/c");

		Assertions.assertTrue(pathMatcher.matches());

		URIBuilder translatedPathBuilder = URIs.uri("/a/{path:**}/d", URIs.RequestTargetForm.PATH, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		String translatedPath = translatedPathBuilder.buildPath(pathMatcher.getParameters());

		Assertions.assertEquals("/a/b/c/d", translatedPath);


		matcherPathBuilder = URIs.uri("/api/{version}/{resource}/{path:**}", URIs.RequestTargetForm.PATH, false, new URIs.Option[]{URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN});
		pathPattern = matcherPathBuilder.buildPathPattern(false);
		pathMatcher = pathPattern.matcher("/api/v1/fruits/apple/gala");

		Assertions.assertTrue(pathMatcher.matches());

		translatedPathBuilder = URIs.uri("/{resource}/{version}/{path:**}", URIs.RequestTargetForm.PATH, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN);
		translatedPath = translatedPathBuilder.buildPath(pathMatcher.getParameters());

		Assertions.assertEquals("/fruits/v1/apple/gala", translatedPath);
	}

	@Test
	public void testTranslatePathAndQuery() {
		URIBuilder matcherPathBuilder = URIs.uri("/{version}/{resource}", URIs.RequestTargetForm.PATH, false, new URIs.Option[]{URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN});
		URIPattern pathPattern = matcherPathBuilder.buildPathPattern(false);
		URIMatcher pathMatcher = pathPattern.matcher("/v1/b");

		Assertions.assertTrue(pathMatcher.matches());

		// PATH_QUERY can't be used with PATH_PATTERN option
		URIBuilder translatedPathBuilder = URIs.uri("/a/{resource}?version={version}", URIs.RequestTargetForm.PATH_QUERY, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED);
		String translatedPath = translatedPathBuilder.buildPath(pathMatcher.getParameters());
		String translatedQuery = translatedPathBuilder.buildQuery(pathMatcher.getParameters());

		Assertions.assertEquals("/a/b?version=v1", translatedPath + "?" + translatedQuery);
	}

	@Test
	public void testParameterizedQueryParameter() {
		URIBuilder uriBuilder = URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).scheme("http").host("service").path("/path/to/resource").query("p1={p1}&p2={p2}");
		URI uri = uriBuilder.build(Map.of("p1", "v1", "p2", "v2"));

		Assertions.assertEquals(URI.create("http://service/path/to/resource?p1=v1&p2=v2"), uri);

		uriBuilder = URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).scheme("http").host("service").path("/path/to/resource").queryParameter("p1", "{p1}").queryParameter("p2", "{p2}");
		uri = uriBuilder.build(Map.of("p1", "v1", "p2", "v2"));
	}
}
