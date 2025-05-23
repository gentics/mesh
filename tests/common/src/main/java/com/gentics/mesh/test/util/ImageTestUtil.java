package com.gentics.mesh.test.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.util.UUIDUtil;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpImageReader;


public final class ImageTestUtil {
	private ImageTestUtil() {
	}

	public static HibBinary createMockedBinary(String filePath) {
		HibBinary mock = mock(HibBinary.class);
		when(mock.openBlockingStream()).thenReturn(() -> ImageTestUtil.class.getResourceAsStream(filePath));
		when(mock.getSHA512Sum()).thenReturn(filePath);
		when(mock.getUuid()).thenReturn(UUIDUtil.randomUUID());
		return mock;
	}

	/**
	 * Displays the given image in a window and waits for a keypress. Don't use this in CI.
	 * 
	 * @param image
	 * @throws IOException
	 */
	public static void displayImage(BufferedImage image) throws IOException {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					createImageFrame(image);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		System.out.println("Press Enter to continue ...");
		System.in.read();
	}

	private static void createImageFrame(BufferedImage image) throws IOException {
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 0, (int)(image.getHeight()*0.025)));
		frame.getContentPane().add(new JLabel(new ImageIcon(image)));
		frame.setPreferredSize(new Dimension((int)(image.getWidth()*1.05), (int)(image.getHeight()*1.05+22)));
		frame.repaint();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.pack();

	}

	public static BufferedImage readImage(String imageName) throws IOException {
		return readImage(imageName, "/pictures");
	}

	public static BufferedImage readImage(String imageName, String fromResourcePath) throws IOException {
		BufferedImage image = null;
		try (InputStream is = ImageTestUtil.class.getResourceAsStream(fromResourcePath + "/" + imageName)) {
			image = ImageIO.read(is);
		}

		if (image == null) {
			try (InputStream is = ImageTestUtil.class.getResourceAsStream(fromResourcePath + "/" + imageName)) {
				ImmutableImage immutableImage = ImmutableImage.loader().withImageReaders(Arrays.asList(new WebpImageReader())).fromStream(is);
				if (immutableImage != null) {
					image = immutableImage.awt();
				}
			}
		}

		return image;
	}

	/**
	 * Read the given input stream into a buffered image
	 * @param is input stream
	 * @return buffered image
	 * @throws IOException
	 */
	public static BufferedImage read(InputStream is) throws IOException {
		BufferedImage bufferedImage = ImageIO.read(is);
		if (bufferedImage == null) {
			is.reset();
			ImmutableImage immutableImage = ImmutableImage.loader().fromStream(is);
			if (immutableImage != null) {
				bufferedImage = immutableImage.awt();
			}
		}
		return bufferedImage;
	}

	/**
	 * Read the given file into a buffered image
	 * @param imageFile image file
	 * @return buffered image
	 * @throws IOException
	 */
	public static BufferedImage read(File imageFile) throws IOException {
		BufferedImage bufferedImage = ImageIO.read(imageFile);
		if (bufferedImage == null) {
			ImmutableImage immutableImage = ImmutableImage.loader().fromFile(imageFile);
			if (immutableImage != null) {
				bufferedImage = immutableImage.awt();
			}
		}
		return bufferedImage;
	}

	public static void writePngImage(BufferedImage output, File target) throws IOException {
		try(ImageOutputStream out = new FileImageOutputStream(target)) {
			ImageWriter imageWriter = ImageIO.getImageWritersByFormatName("png").next();
			imageWriter.setOutput(out);
			imageWriter.write(null, new IIOImage(output, null, null), null);
		}
		
	}
}
