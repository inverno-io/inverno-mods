package io.winterframework.mod.commons.resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;

public class ResourceTest {

	private void writeResource(Resource resource) throws IOException {
		Assertions.assertFalse(resource.exists());
		Assertions.assertNull(resource.lastModified());
		resource.openWritableByteChannel().ifPresent(ch -> {
			try(ch) {
				int bufferSize = 1024;
		        ByteBuffer buff = ByteBuffer.allocate(bufferSize);

		        buff.put("This is a test".getBytes(), 0, 14);
		        buff.flip();
		        ch.write(buff);
			} 
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	private void readResource(Resource resource) throws IOException {
		Assertions.assertTrue(resource.exists());
		Assertions.assertNotNull(resource.lastModified());
		Assertions.assertTrue(System.currentTimeMillis() - resource.lastModified().toMillis() < 1000);
		resource.openReadableByteChannel().ifPresent(ch -> {
			try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				ch;) {
				int bufferSize = 1024;
		        ByteBuffer buff = ByteBuffer.allocate(bufferSize);
		        
		        while (ch.read(buff) > 0) {
		            out.write(buff.array(), 0, buff.position());
		            buff.clear();
		        }
		        
		        String fileContent = new String(out.toByteArray(), StandardCharsets.UTF_8);
		        Assertions.assertEquals("This is a test", fileContent);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	private void deleteResource(Resource resource) throws IOException {
		Assertions.assertTrue(resource.exists());
		Assertions.assertTrue(resource.delete());
		Assertions.assertFalse(resource.exists());
	}
	
	@Test
	public void testFile() throws URISyntaxException, IOException {
		String pathname = "target/tmp/ign/test.txt";
		File file = new File(pathname);
		URI uri = file.toURI();
		try {
			try (Resource resource = new FileResource(pathname)) {
				this.writeResource(resource);
			}
			
			try (Resource resource = new FileResource(file)) {
				this.readResource(resource);
			}
			
			try (Resource resource = new FileResource(uri)) {
				this.deleteResource(resource);
			}
		}
		finally {
			file.delete();
		}
	}
	
	@Test
	public void testZip() throws URISyntaxException, IOException {
		File zipFile = new File("target/tmp/test.zip");
		URI uri = new URI("zip:" + zipFile.toURI() +"!/ign/test.txt");
		try  {
			try (Resource resource = new ZipResource(uri)) {
				this.writeResource(resource);
			}
			
			try (Resource resource = new ZipResource(uri)) {
				this.readResource(resource);
			}
			
			try (Resource resource = new ZipResource(uri)) {
				this.deleteResource(resource);
			}
		}
		finally {
			zipFile.delete();
		}
	}

	@Test
	public void testJar() throws URISyntaxException, IOException {
		File jarFile = new File("target/tmp/test.jar");
		URI uri = new URI("jar:" + jarFile.toURI() +"!/ign/test.txt");
		try  {
			try (Resource resource = new JarResource(uri)) {
				this.writeResource(resource);
			}
			
			try (Resource resource = new JarResource(uri)) {
				this.readResource(resource);
			}
			
			try (Resource resource = new JarResource(uri)) {
				this.deleteResource(resource);
			}
		}
		finally {
			jarFile.delete();
		}
	}
	
	@Test
	public void testClasspath() throws URISyntaxException, IOException {
		File testJar = new File("src/test/resources/test.jar");
		ClassLoader cl = new URLClassLoader(new URL[] {testJar.toURI().toURL()});
		URI uri = new URI("classpath:/ign/test.txt");
		try (Resource resource = new ClasspathResource(uri, cl)) {
			Assertions.assertTrue(resource.exists());
			Assertions.assertNotNull(resource.lastModified());
			resource.openReadableByteChannel().ifPresent(ch -> {
				try (ByteArrayOutputStream out = new ByteArrayOutputStream();
					ch;) {
					int bufferSize = 1024;
			        ByteBuffer buff = ByteBuffer.allocate(bufferSize);
			        
			        while (ch.read(buff) > 0) {
			            out.write(buff.array(), 0, buff.position());
			            buff.clear();
			        }
			        String fileContent = new String(out.toByteArray(), StandardCharsets.UTF_8);
			        Assertions.assertEquals("This is a test", fileContent);
				}
				catch(IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}
	
	@Test
	public void testUrl() throws URISyntaxException, IOException {
		File testFile = new File("src/test/resources/test.txt");
		try (Resource resource = new UrlResource(testFile.toURI())) {
			resource.openReadableByteChannel().ifPresent(ch -> {
				try (ByteArrayOutputStream out = new ByteArrayOutputStream();
					ch;) {
					int bufferSize = 1024;
			        ByteBuffer buff = ByteBuffer.allocate(bufferSize);
			        
			        while (ch.read(buff) > 0) {
			            out.write(buff.array(), 0, buff.position());
			            buff.clear();
			        }
			        String fileContent = new String(out.toByteArray(), StandardCharsets.UTF_8);
			        Assertions.assertEquals("This is a test", fileContent);
				}
				catch(IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}
	
	@Test
	public void testReactiveRead() throws IllegalArgumentException, IOException {
		Path srcFile = Paths.get("src/test/resources/soufriere.png");
		Path tgtFile = Paths.get("target/tmp/soufriere.png");
		FileChannel out = FileChannel.open(tgtFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		try(PathResource resource = new PathResource(srcFile)) {
			resource.setExecutor(Executors.newFixedThreadPool(5));
			resource.read().ifPresent(data -> {
				data.doOnNext(chunk -> {
					try {
						out.write(chunk.nioBuffer());
					} 
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}).doOnComplete(() -> {
					try {
						out.close();
					} 
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.blockLast();
			});
		}
		Assertions.assertEquals(Files.size(srcFile), Files.size(tgtFile));
		FileChannel srcChannel = FileChannel.open(srcFile, StandardOpenOption.READ);
		FileChannel tgtChannel = FileChannel.open(tgtFile, StandardOpenOption.READ);
		ByteBuffer srcBuffer = ByteBuffer.allocate(8192);
		ByteBuffer tgtBuffer = ByteBuffer.allocate(8192);
		while(srcChannel.read(srcBuffer) != -1 && tgtChannel.read(tgtBuffer) != -1) {
			Assertions.assertEquals(srcBuffer, tgtBuffer);
			srcBuffer.clear();
			tgtBuffer.clear();
		}
		Files.delete(tgtFile);
	}
	
	@Test
	public void testReactiveWrite() throws IllegalArgumentException, IOException {
		Path srcFile = Paths.get("src/test/resources/soufriere.png");
		Path tgtFile = Paths.get("target/tmp/soufriere.png");
		try(PathResource srcResource = new PathResource(srcFile);
			PathResource tgtResource = new PathResource(tgtFile)) {
			
			Flux<ByteBuf> srcData = srcResource.read().get();
			tgtResource.write(srcData).get().blockLast();
		}
		Assertions.assertEquals(Files.size(srcFile), Files.size(tgtFile));
		FileChannel srcChannel = FileChannel.open(srcFile, StandardOpenOption.READ);
		FileChannel tgtChannel = FileChannel.open(tgtFile, StandardOpenOption.READ);
		ByteBuffer srcBuffer = ByteBuffer.allocate(8192);
		ByteBuffer tgtBuffer = ByteBuffer.allocate(8192);
		while(srcChannel.read(srcBuffer) != -1 && tgtChannel.read(tgtBuffer) != -1) {
			Assertions.assertEquals(srcBuffer, tgtBuffer);
			srcBuffer.clear();
			tgtBuffer.clear();
		}
		Files.delete(tgtFile);
	}
}
