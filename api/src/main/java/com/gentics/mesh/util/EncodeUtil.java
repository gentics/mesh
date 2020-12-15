package com.gentics.mesh.util;

import static java.nio.charset.CodingErrorAction.REPLACE;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

/**
 * Utility to help with UTF8 and ISO encoding operations 
 */
public final class EncodeUtil {

	private static final Charset ISO88591_CHARSET = Charset.forName("ISO-8859-1");
	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	public static final String UTF8_BOM = "\uFEFF";

	public static final String REPLACEMENT = "?";

	/**
	 * Encode the given string to ISO-8859-1.
	 * 
	 * @param text
	 *            UTF8 encoded text
	 * @return
	 */
	public static String toISO88591(String text) {

		try {
			ByteBuffer inputBuffer = ByteBuffer.wrap(text.getBytes(UTF8_CHARSET));

			// Decode UTF-8
			CharBuffer data = UTF8_CHARSET.newDecoder()
				.onMalformedInput(REPLACE)
				.onUnmappableCharacter(REPLACE)
				.replaceWith(REPLACEMENT)
				.decode(inputBuffer);

			// Encode ISO-8559-1
			ByteBuffer outputBuffer = ISO88591_CHARSET.newEncoder()
				.onMalformedInput(REPLACE)
				.onUnmappableCharacter(REPLACE)
				.replaceWith(REPLACEMENT.getBytes())
				.encode(data);
			String encoded = new String(outputBuffer.array(), ISO88591_CHARSET);
			return encoded.replaceAll("\0", "");
		} catch (CharacterCodingException e) {
			throw new RuntimeException("Error while encoding {" + text + "} to ISO-8859-1");
		}
	}

	/**
	 * Decodes the given text using Utf8 and replaces all unmappable characters with the replacement char.
	 * 
	 * @param text
	 * @return
	 * @throws CharacterCodingException
	 */
	public static String ensureUtf8(String text) throws CharacterCodingException {
		text = removeUTF8BOM(text);
		ByteBuffer inputBuffer = ByteBuffer.wrap(text.getBytes(UTF8_CHARSET));

		// Decode UTF-8
		CharBuffer data = UTF8_CHARSET.newDecoder()
			.onMalformedInput(REPLACE)
			.onUnmappableCharacter(REPLACE)
			.replaceWith(REPLACEMENT)
			.decode(inputBuffer);
		return data.toString();
	}

	/**
	 * Removed the BOM character from the string if it starts with one.
	 * 
	 * @param s
	 * @return
	 */
	public static String removeUTF8BOM(String s) {
		if (s.startsWith(UTF8_BOM)) {
			s = s.substring(1);
		}
		return s;
	}

	/**
	 * Encode the given text to conform to RFC 5597.
	 * 
	 * @param text
	 * @return
	 */
	public static String encodeForRFC5597(String text) {
		try {
			return "utf-8''" + URLEncoder.encode(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
