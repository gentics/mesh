package com.gentics.mesh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * Tests for {@link MeshVersion}.
 */
public class MeshVersionTest {

	/**
	 * Test that the previous version is calculated when Mesh is loaded from a jar file (e.g. in a reactor build), not only from a classes directory.
	 */
	@Test
	public void testPreviousVersionFromJar() throws Exception {
		Path classesDir = Path.of(MeshVersion.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Path jar = classesDir.resolveSibling("meshversion-test").resolve("mesh-api.jar");
		createJar(classesDir, jar);

		String expected = MeshVersion.getPreviousVersion();
		assertNotEquals("Previous version from classes directory must differ from the current one", MeshVersion.getPlainVersion().split("-")[0], expected);

		try (URLClassLoader loader = new ChildFirstClassLoader(new URL[] { jar.toUri().toURL() }, MeshVersionTest.class.getClassLoader())) {
			Class<?> meshVersion = loader.loadClass(MeshVersion.class.getName());
			assertEquals("MeshVersion must be loaded from the jar", loader, meshVersion.getClassLoader());
			assertEquals(expected, meshVersion.getMethod("getPreviousVersion").invoke(null));
		}
	}

	private static void createJar(Path sourceDir, Path jar) throws IOException {
		Files.createDirectories(jar.getParent());
		List<Path> files;
		try (Stream<Path> walk = Files.walk(sourceDir)) {
			files = walk.filter(Files::isRegularFile).collect(Collectors.toList());
		}
		try (OutputStream out = Files.newOutputStream(jar); JarOutputStream jarOut = new JarOutputStream(out)) {
			for (Path file : files) {
				jarOut.putNextEntry(new JarEntry(sourceDir.relativize(file).toString().replace('\\', '/')));
				Files.copy(file, jarOut);
				jarOut.closeEntry();
			}
		}
	}

	/**
	 * Class loader, which prefers the Mesh classes and resources of its own URLs over the ones of the parent.
	 */
	private static class ChildFirstClassLoader extends URLClassLoader {
		ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			synchronized (getClassLoadingLock(name)) {
				Class<?> clazz = findLoadedClass(name);
				if (clazz == null && name.startsWith("com.gentics.mesh.")) {
					try {
						clazz = findClass(name);
					} catch (ClassNotFoundException e) {
						// fall back to the parent
					}
				}
				if (clazz == null) {
					return super.loadClass(name, resolve);
				}
				if (resolve) {
					resolveClass(clazz);
				}
				return clazz;
			}
		}

		@Override
		public URL getResource(String name) {
			URL url = findResource(name);
			return url != null ? url : super.getResource(name);
		}
	}
}
