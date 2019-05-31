package com.gentics.mesh.core.binary;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;

public class DocumentTikaParser {

	/**
	 * Exclusions
	 */
	private static final Set<MediaType> EXCLUDES = new HashSet<>(Arrays.asList(
		MediaType.application("vnd.ms-visio.drawing"),
		MediaType.application("vnd.ms-visio.drawing.macroenabled.12"),
		MediaType.application("vnd.ms-visio.stencil"),
		MediaType.application("vnd.ms-visio.stencil.macroenabled.12"),
		MediaType.application("vnd.ms-visio.template"),
		MediaType.application("vnd.ms-visio.template.macroenabled.12"),
		MediaType.application("vnd.ms-visio.drawing")));

	/**
	 * Supported parsers.
	 */
	private static final Parser PARSERS[] = new Parser[] {
		// documents
		new org.apache.tika.parser.html.HtmlParser(),
		new org.apache.tika.parser.rtf.RTFParser(),
		new org.apache.tika.parser.pdf.PDFParser(),
		new org.apache.tika.parser.txt.TXTParser(),
		new org.apache.tika.parser.microsoft.OfficeParser(),
		new org.apache.tika.parser.microsoft.OldExcelParser(),
		ParserDecorator.withoutTypes(new org.apache.tika.parser.microsoft.ooxml.OOXMLParser(), EXCLUDES),
		new org.apache.tika.parser.odf.OpenDocumentParser(),
		new org.apache.tika.parser.iwork.IWorkPackageParser(),
		new org.apache.tika.parser.xml.DcXMLParser(),
		new org.apache.tika.parser.epub.EpubParser(),

		// audio
		new org.apache.tika.parser.audio.AudioParser(),
		new org.apache.tika.parser.mp3.Mp3Parser(),

		// video
		new org.apache.tika.parser.video.FLVParser(),
		new org.apache.tika.parser.mp4.MP4Parser(),

		// images
		new org.apache.tika.parser.image.ImageParser(),
		new org.apache.tika.parser.jpeg.JpegParser(),
		new org.apache.tika.parser.image.WebPParser(),

		// ogg (audio/video)
		new org.gagravarr.tika.OggParser()
	};

	private static final AutoDetectParser PARSER_INSTANCE = new AutoDetectParser(PARSERS);

	private static final Tika TIKA_INSTANCE = new Tika(PARSER_INSTANCE.getDetector(), PARSER_INSTANCE);

	/**
	 * Parse the input and set the metadata.
	 * 
	 * @param input
	 * @param metadata
	 * @param limit
	 * @return Optional with the extracted content
	 * @throws TikaException
	 * @throws IOException
	 */
	public static Optional<String> parse(final InputStream input, final Metadata metadata, final int limit) throws TikaException, IOException {
		try {
			String content = TIKA_INSTANCE.parseToString(input, metadata, limit);
			if (isEmpty(content)) {
				return Optional.empty();
			}
			content = StringUtils.trim(content);
			return Optional.of(content);
		} catch (Exception e) {
			Throwable cause = e.getCause();
			if (cause instanceof TikaException) {
				throw (TikaException) cause;
			} else if (cause instanceof IOException) {
				throw (IOException) cause;
			} else {
				throw e;
			}
		}
	}

}