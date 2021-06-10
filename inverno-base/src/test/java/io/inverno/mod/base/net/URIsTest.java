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

		Assertions.assertEquals("{scheme}:/a/b/c", schemeUriBuilder.toString());
		
		Assertions.assertEquals("http:/a/b/c", schemeUriBuilder.buildString("http"));
		Assertions.assertEquals("http:/a/b/c", schemeUriBuilder.buildString(Map.of("scheme", "http")));
		Assertions.assertEquals("/a/b/c", schemeUriBuilder.buildPath("http"));
		Assertions.assertEquals("/a/b/c", schemeUriBuilder.buildPath(Map.of("scheme", "http")));
		Assertions.assertEquals(new URI("http:/a/b/c"), schemeUriBuilder.build("http"));
		Assertions.assertEquals(new URI("http:/a/b/c"), schemeUriBuilder.build(Map.of("scheme", "http")));
		
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
		
		Assertions.assertEquals("a/1/b/_2/b_3_", pathUriBuilder.buildString(new Object[]{"1", "2/b", "3"}, false));
		Assertions.assertEquals("a/1/b/_2/b_3_", pathUriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", pathUriBuilder.buildPath(new Object[]{"1", "2/b", "3"}, false));
		Assertions.assertEquals("a/1/b/_2/b_3_", pathUriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3), false));
		Assertions.assertEquals(new URI("a/1/b/_2/b_3_"), pathUriBuilder.build(new Object[]{"1", "2/b", "3"}, false));
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
		
		Assertions.assertEquals("a/1/b/_2/b_3_?query_param1=abc&queryParam2=_4_5%7Ce_", queryUriBuilder.buildString(new Object[]{"1", "2/b", "3", "4", "5|e"}, false));
		Assertions.assertEquals("a/1/b/_2/b_3_?query_param1=abc&queryParam2=_4_5%7Ce_", queryUriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", queryUriBuilder.buildPath(new Object[]{"1", "2/b", "3", "4", "5|e"}, false));
		Assertions.assertEquals("a/1/b/_2/b_3_", queryUriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4, "param5", "5|e"), false));
		Assertions.assertEquals(new URI("a/1/b/_2/b_3_?query_param1=abc&queryParam2=_4_5%7Ce_"), queryUriBuilder.build(new Object[]{"1", "2/b", "3", "4", "5|e"}, false));
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
		
		Assertions.assertEquals("a/1/b/_2/b_3_#_4%7Cd_", fragmentUriBuilder.buildString(new Object[]{"1", "2/b", "3", "4|d"}, false));
		Assertions.assertEquals("a/1/b/_2/b_3_#_4%7Cd_", fragmentUriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", "4|d"), false));
		Assertions.assertEquals("a/1/b/_2/b_3_", fragmentUriBuilder.buildPath(new Object[]{"1", "2/b", "3", "4|d"}, false));
		Assertions.assertEquals("a/1/b/_2/b_3_", fragmentUriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", "4|d"), false));
		Assertions.assertEquals(new URI("a/1/b/_2/b_3_#_4%7Cd_"), fragmentUriBuilder.build(new Object[]{"1", "2/b", "3", "4|d"}, false));
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
		
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment", uriBuilder.buildString(new Object[]{"1", "2/b", "3", "4"}, false));
		Assertions.assertEquals("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment", uriBuilder.buildString(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4), false));
		Assertions.assertEquals("/a/1/b/_2/b_3_/4", uriBuilder.buildPath(new Object[]{"1", "2/b", "3", "4"}, false));
		Assertions.assertEquals("/a/1/b/_2/b_3_/4", uriBuilder.buildPath(Map.of("param2", "2/b", "param1", 1, "param3", 3, "param4", 4), false));
		Assertions.assertEquals(new URI("https://127.0.0.1/a/1/b/_2/b_3_/4?foo=bar#!fragment"), uriBuilder.build(new Object[]{"1", "2/b", "3", "4"}, false));
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
		
		Assertions.assertNull(URIs.uri("/a/b/c?this_is_a_test").getRawQueryParameters());
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
	
}
