package com.gentics.mesh.test.assertj;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import com.gentics.mesh.test.context.MeshTestContext;

public class MeshTestContextAssert extends AbstractAssert<MeshTestContextAssert, MeshTestContext> {

	public MeshTestContextAssert(MeshTestContext actual) {
		super(actual, MeshTestContextAssert.class);
	}

	/**
	 * Assert the upload files and folders.
	 * 
	 * @param files
	 * @param folders
	 * @return
	 */
	public MeshTestContextAssert hasUploads(long files, long folders) {
		hasUploadFiles(files);
		hasUploadFolders(folders);
		return this;
	}

	/**
	 * Assert that the upload folder only contained the expected amount of files.
	 * 
	 * @param expected
	 * @return
	 */
	public MeshTestContextAssert hasUploadFiles(long expected) {
		String dir = actual.getOptions().getUploadOptions().getDirectory();
		assertCount("The upload folder did not contain the expected amount of files.", dir, Files::isRegularFile, expected);
		return this;
	}

	/**
	 * Assert that the upload folder only contained the expected amount of subfolders.
	 * 
	 * @param expected
	 * @return
	 * @return Fluent API
	 */
	public MeshTestContextAssert hasUploadFolders(long expected) {
		String dir = actual.getOptions().getUploadOptions().getDirectory();

		long count = listFolders(dir).count();
		if (count != expected) {
			String msg = "The upload folder did not contain the expected amount of folders.";
			String info = listFolders(dir)
				.map(p -> p.toAbsolutePath().toString())
				.collect(Collectors.joining("\n"));
			assertEquals(msg + "\nFound:\n" + info + "\n\n", expected, count);
		}
		return this;
	}

	/**
	 * Assert that the temporary directory only contains the expected amount of files.
	 * 
	 * @param expected
	 * @return
	 * @throws IOException
	 * @return Fluent API
	 */
	public MeshTestContextAssert hasTempFiles(long expected) {
		String dir = actual.getOptions().getTempDirectory();
		assertCount("The tempdirectory did not contain the expected amount of files.", dir, Files::isRegularFile, expected);
		return this;
	}

	/**
	 * Asserts that the upload temporary directory only contains the expected amount of files.
	 * 
	 * @param expected
	 * @return
	 * @throws IOException
	 * @return Fluent API
	 */
	public MeshTestContextAssert hasTempUploads(long expected) {
		String dir = actual.getOptions().getUploadOptions().getTempDirectory();
		assertCount("The upload tempdirectory did not contain the expected amount of files.", dir, Files::isRegularFile, expected);
		return this;
	}

	private long count(String path, Predicate<? super Path> filter) {
		try {
			return Files.walk(Paths.get(path))
				.filter(filter)
				.count();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String list(String path, Predicate<? super Path> filter) {
		try {
			return Files.walk(Paths.get(path))
				.filter(filter).map(p -> p.toAbsolutePath().toString())
				.collect(Collectors.joining("\n"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Stream<Path> listFolders(String path) {
		try {
			return Files.walk(Paths.get(path))
				.filter(p -> !p.endsWith("temp"))
				.filter(p -> !p.equals(Paths.get(path)))
				.filter(Files::isDirectory)
				.filter(p -> p.toFile().listFiles(File::isDirectory).length == 0);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void assertCount(String msg, String path, Predicate<? super Path> filter, long expected) {
		long count = count(path, filter);
		assertEquals(msg + "\nFound:\n" + list(path, filter) + "\n\n", expected, count);

	}

}
