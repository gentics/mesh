package com.gentics.mesh.image;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.gentics.mesh.core.data.binary.Binary;

public final class ImageTestUtil {
	private ImageTestUtil() {
	}

	public static Binary createMockedBinary(String filePath) {
		Binary mock = mock(Binary.class);
		when(mock.openBlockingStream()).thenReturn(() -> ImageTestUtil.class.getResourceAsStream(filePath));
		when(mock.getSHA512Sum()).thenReturn(filePath);
		return mock;
	}

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
		InputStream is = ImageTestUtil.class.getResourceAsStream("/pictures/" + imageName);
		ImageInputStream ins = ImageIO.createImageInputStream(is);
		Iterator<ImageReader> it = ImageIO.getImageReaders(ins);
		ImageReader reader = it.next();
		reader.setInput(ins, true);
		return reader.read(0);
	}
}
