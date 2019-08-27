package com.gentics.mesh.image;

import static com.gentics.mesh.image.ImgscalrImageManipulatorTest.getReferenceFilename;
import static com.gentics.mesh.test.util.ImageTestUtil.createMockedBinary;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;

import io.reactivex.Observable;
import io.vertx.core.file.FileSystem;
import io.vertx.reactivex.core.Vertx;

public class ReferenceImageCreator {
	public static void main(String[] args) throws Exception {
		new ReferenceImageCreator().run();
	}

	private final String BASE_PATH = "services/image-imgscalr/src/test/resources/";
	private void run() throws Exception {
		Vertx vertx = Vertx.vertx();
		FileSystem fs = vertx.getDelegate().fileSystem();
		ImageManipulatorOptions options = new ImageManipulatorOptions();
		String tmpDir = new File("target", "tmp_" + System.currentTimeMillis()).getAbsolutePath();
		options.setImageCacheDirectory(tmpDir);
		ImgscalrImageManipulator manipulator = new ImgscalrImageManipulator(vertx, options);

		readImageConfig().blockingForEach(image -> {
			String imageName = image.getString("name");
			System.out.println("Transforming image " + imageName);
			String path = "/pictures/" + imageName;

			String file = manipulator.handleResize(createMockedBinary(path), new ImageManipulationParametersImpl().setWidth(150).setHeight(180)).blockingGet();
			String targetPath = BASE_PATH + getReferenceFilename(imageName);

			if (fs.existsBlocking(targetPath)) {
				fs.deleteBlocking(targetPath);
			}
			fs.moveBlocking(file, targetPath);
		});

		fs.deleteRecursiveBlocking(tmpDir, true);

		vertx.close();
	}

	private Observable<JSONObject> readImageConfig() throws Exception {
		JSONObject json = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/pictures/images.json"), StandardCharsets.UTF_8));
		JSONArray array = json.getJSONArray("images");
		return Observable.range(0, array.length())
			.map(array::getJSONObject);
	}


}
