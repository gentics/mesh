package com.gentics.mesh.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

public class AbstractImageTest {

	protected File cacheDir;

	@Before
	public void setup() {
		cacheDir = new File("target/cacheDir_" + System.currentTimeMillis());
	}

	@After
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(cacheDir);
		FileUtils.deleteDirectory(new File("data"));
		FileUtils.deleteDirectory(new File("target/data"));
	}

	public BufferedImage getImage(String name) throws IOException {
		InputStream ins = getClass().getResourceAsStream("/pictures/" + name);
		if (ins == null) {
			throw new RuntimeException("Could not find image {" + name + "}");
		}
		return ImageIO.read(ins);
	}

}
