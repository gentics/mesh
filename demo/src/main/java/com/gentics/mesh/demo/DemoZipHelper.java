package com.gentics.mesh.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.commons.io.IOUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class DemoZipHelper {

	public static void unzip(String zipClasspath, String outdir) throws FileNotFoundException, IOException, ZipException {
		InputStream ins = DemoZipHelper.class.getResourceAsStream(zipClasspath);
		Objects.requireNonNull(ins, "The file could not be found within the classpath {" + zipClasspath + "}");
		File zipFile = new File(System.getProperty("java.io.tmpdir"), "mesh-demo.zip");
		if (zipFile.exists()) {
			zipFile.delete();
		}
		IOUtils.copy(ins, new FileOutputStream(zipFile));
		ZipFile zip = new ZipFile(zipFile);
		zip.extractAll(outdir);
		zipFile.delete();
	}
}
