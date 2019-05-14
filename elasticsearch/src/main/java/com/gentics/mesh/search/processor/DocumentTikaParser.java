package com.gentics.mesh.search.processor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;

class DocumentTikaParser {

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
	 * Suppored parsers.
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
	};

	private static final AutoDetectParser PARSER_INSTANCE = new AutoDetectParser(PARSERS);

	private static final Tika TIKA_INSTANCE = new Tika(PARSER_INSTANCE.getDetector(), PARSER_INSTANCE);

	public static String parse(final byte content[], final Metadata metadata, final int limit) throws TikaException, IOException {
		try {
			return TIKA_INSTANCE.parseToString(new ByteArrayInputStream(content), metadata, limit);
		} catch (Exception e) {
			Throwable cause = e.getCause();
			if (cause instanceof TikaException) {
				throw (TikaException) cause;
			} else if (cause instanceof IOException) {
				throw (IOException) cause;
			} else {
				throw new AssertionError(cause);
			}
		}
	}

}