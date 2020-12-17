package com.gentics.mesh.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * Helper class to handle unzipping of the demo content.
 */
public class DemoZipHelper {

	private static final Logger log = LoggerFactory.getLogger(DemoZipHelper.class);

	/**
	 * Unzip the mesh-demo.zip file which contains the content and demo application.
	 * 
	 * @param zipClasspath
	 * @param outdir
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ZipException
	 */
	public static void unzip(String zipClasspath, String outdir) throws FileNotFoundException, IOException, ZipException {
		InputStream ins = DemoZipHelper.class.getResourceAsStream(zipClasspath);
		if (ins != null) {
			File zipFile = new File(System.getProperty("java.io.tmpdir"), "mesh-demo.zip");
			if (zipFile.exists()) {
				zipFile.delete();
			}
			IOUtils.copy(ins, new FileOutputStream(zipFile));
			ZipFile zip = new ZipFile(zipFile);
			zip.extractAll(outdir);
			zipFile.delete();
		} else {
			log.error("The mesh-demo.zip file could not be found within the classpath {" + zipClasspath + "}");
		}
	}
}
