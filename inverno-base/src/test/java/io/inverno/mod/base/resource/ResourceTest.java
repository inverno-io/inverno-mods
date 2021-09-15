package io.inverno.mod.base.resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;

public class ResourceTest {

	private void writeResource(Resource resource){
		Assertions.assertFalse(resource.exists().get());
		Assertions.assertFalse(resource.lastModified().isPresent());
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
	
	private void readResource(Resource resource) {
		Assertions.assertTrue(resource.exists().get());
		Assertions.assertTrue(resource.lastModified().isPresent());
		Assertions.assertTrue(System.currentTimeMillis() - resource.lastModified().get().toMillis() < 1000);
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
	
	private void deleteResource(Resource resource) {
		Assertions.assertTrue(resource.exists().get());
		Assertions.assertTrue(resource.delete());
		Assertions.assertFalse(resource.exists().get());
	}
	
	@Test
	public void testFile() throws URISyntaxException {
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
			
			try (Resource resource = new FileResource(uri)) {
				try (Resource resolvedResource = resource.resolve("../foo.txt");) {
					Assertions.assertEquals(new File("target/tmp/ign/foo.txt").toURI(), resolvedResource.getURI());
				}
			}			
		}
		finally {
			file.delete();
		}
	}
	
	@Test
	public void testZip() throws URISyntaxException {
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
			
			try (Resource resource = new ZipResource(uri)) {
				try (Resource resolvedResource = resource.resolve("../foo.txt");) {
					Assertions.assertEquals(URI.create("zip:" + zipFile.toURI() +"!/ign/foo.txt"), resolvedResource.getURI());
				}
			}
		}
		finally {
			zipFile.delete();
		}
	}

	@Test
	public void testJar() throws URISyntaxException {
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
			
			try (Resource resource = new JarResource(uri)) {
				try (Resource resolvedResource = resource.resolve("../foo.txt");) {
					Assertions.assertEquals(URI.create("jar:" + jarFile.toURI() +"!/ign/foo.txt"), resolvedResource.getURI());
				}
			}
		}
		finally {
			jarFile.delete();
		}
	}
	
	@Test
	public void testClasspath() throws URISyntaxException, MalformedURLException {
		File testJar = new File("src/test/resources/test.jar");
		System.out.println(testJar.getAbsolutePath());
		ClassLoader cl = new URLClassLoader(new URL[] {testJar.toURI().toURL()});
		URI uri = new URI("classpath:/ign/test.txt");
		try (Resource resource = new ClasspathResource(uri, cl)) {
			Assertions.assertTrue(resource.exists().get());
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
			
			try (Resource resolvedResource = resource.resolve("../foo.txt");) {
				Assertions.assertEquals(URI.create("classpath:/ign/foo.txt"), resolvedResource.getURI());
			}
		}
	}
	
	@Test
	public void testUrl() throws URISyntaxException {
		try (Resource resource = new URLResource(new File("src/test/resources/test.txt").toURI())) {
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
			
			try (Resource resolvedResource = resource.resolve("../foo.txt");) {
				Assertions.assertEquals(new File("src/test/resources/foo.txt").toURI(), resolvedResource.getURI());
			}
		}
	}
	
	@Test
	public void testReactiveRead() throws IllegalArgumentException, IOException {
		Path srcFile = Paths.get("src/test/resources/soufriere.png");
		Path tgtFile = Paths.get("target/tmp/soufriere.png");
		FileChannel out = FileChannel.open(tgtFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		try(PathResource resource = new PathResource(srcFile)) {
			resource.setExecutor(Executors.newFixedThreadPool(5));
			resource.read().map(Flux::from).ifPresent(data -> {
				data.doOnNext(chunk -> {
					try {
						out.write(chunk.nioBuffer());
					} 
					catch (IOException e) {
						throw new RuntimeException(e);
					}
					finally {
						chunk.release();
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
			
			Flux<ByteBuf> srcData = srcResource.read().map(Flux::from).get();
			tgtResource.write(srcData).map(Flux::from).get().blockLast();
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
	public void testUrlRead() throws IllegalArgumentException {
		try (Resource resource = new URLResource(new File("src/test/resources/test.txt").toURI())) {
			String content = resource.read().map(Flux::from).get()
				.map(chunk -> {
					String s = chunk.toString(Charset.defaultCharset());
					chunk.release();
					return s;
				})
				.collect(Collectors.joining())
				.block();
			Assertions.assertEquals("This is a test", content);
		}
	}
	
	@Test
	public void testUrlWrite() throws IllegalArgumentException, URISyntaxException {
		FakeFtpServer fakeFtpServer = new FakeFtpServer();
		fakeFtpServer.setServerControlPort(0);
		fakeFtpServer.addUserAccount(new UserAccount("user", "password", "/data"));
		FileSystem fileSystem = new UnixFakeFileSystem();
		fileSystem.add(new DirectoryEntry("/data"));
		fileSystem.add(new FileEntry("/data/foobar.txt", "abcdef 1234567890"));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.start();
        
        try (Resource resource = new URLResource(new URI(String.format("ftp://user:password@localhost:%d/test.txt", fakeFtpServer.getServerControlPort())))) {
			int bytesWritten = resource.write(Flux.just(Unpooled.copiedBuffer("This is a write test", Charset.defaultCharset()))).map(Flux::from).get().collect(Collectors.summingInt(Integer::intValue)).block();
			Assertions.assertEquals(20, bytesWritten);
			Assertions.assertTrue(fileSystem.exists("/data/test.txt"));
			Assertions.assertEquals(20, fileSystem.getEntry("/data/test.txt").getSize());
		}
		finally {
			fakeFtpServer.stop();
		}
	}
}
