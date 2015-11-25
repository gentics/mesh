package com.gentics.mesh.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.junit.Test;

public class ImgscalrImageProviderTest {

	@Test
	public void testResize() throws Exception {

		List<String> imageNames = IOUtils.readLines(getClass().getResourceAsStream("/pictures/images.lst"));
		for (String imageName : imageNames) {
			System.out.println("Handling " + imageName);
			InputStream ins = getClass().getResourceAsStream("/pictures/" + imageName);

			//		Iterator readers = ImageIO.getImageReadersByFormatName("JPEG");
			//		ImageReader reader = null;
			//		while (readers.hasNext()) {
			//			reader = (ImageReader) readers.next();
			//			if (reader.canReadRaster()) {
			//				break;
			//			}
			//		}

			BufferedImage bi = null;
			try {
				bi = ImageIO.read(ins);
				if (bi == null) {
					System.out.println("Can't handle {" + imageName + "}");
					continue;
				}
			} catch (Exception e) {
				System.out.println("Can't handle {" + imageName + "}");
				e.printStackTrace();
				continue;
			}

			//		ImageInputStream imageInput = ImageIO.createImageInputStream(ins);
			//		reader.setInput(imageInput);
			//		
			//		//Read the image raster
			//		Raster raster = reader.readRaster(0, null);
			//
			//		//Create a new RGB image
			//		BufferedImage bi = new BufferedImage(raster.getWidth(), raster.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			//
			//		//Fill the new image with the old raster
			//		bi.getRaster().setRect(raster);

			//bi = Scalr.crop(bi, 20, 320, 200, 120);
			//bi = Scalr.resize(bi, 200, 200);

			File outputfile = new File(imageName + ".resized.png");
			ImageIO.write(bi, "jpg", outputfile);
		}
	}
}
