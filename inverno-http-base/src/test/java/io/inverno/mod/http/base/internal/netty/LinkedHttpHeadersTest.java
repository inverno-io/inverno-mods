package io.inverno.mod.http.base.internal.netty;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class LinkedHttpHeadersTest {

	@Test
	public void test() {
		LinkedHttpHeaders headers =  new LinkedHttpHeaders();

		headers.add("test", "abc");
		headers.add("test", "def");
		headers.add("test2", "ghi");
		Assertions.assertEquals(3, headers.size());

		headers.set("test", "123");
		Assertions.assertEquals(2, headers.size());

		headers.remove("test2");
		Assertions.assertEquals(1, headers.size());
		Assertions.assertEquals("123", headers.get("test"));
	}
}
