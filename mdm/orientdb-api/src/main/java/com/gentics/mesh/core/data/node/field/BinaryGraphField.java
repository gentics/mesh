package com.gentics.mesh.core.data.node.field;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Map;
import java.util.Objects;

import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.util.UniquenessUtil;

/**
 * The BinaryField Domain Model interface. The field is an edge between the field container and the {@link Binary}
 */
public interface BinaryGraphField extends BasicGraphField<BinaryField>, MeshEdge, DisplayField, HibBinaryField, GraphDeletableField {

	String BINARY_FILENAME_PROPERTY_KEY = "binaryFilename";

	String BINARY_SHA512SUM_PROPERTY_KEY = "binarySha512Sum";

	String BINARY_CONTENT_TYPE_PROPERTY_KEY = "binaryContentType";

	String BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY = "binaryImageDominantColor";

	String BINARY_IMAGE_FOCAL_POINT_X = "binaryImageFocalPointX";

	String BINARY_IMAGE_FOCAL_POINT_Y = "binaryImageFocalPointY";

	String META_DATA_PROPERTY_PREFIX = "metadata_";

	String BINARY_LAT_KEY = "metadata-lat";

	String BINARY_LON_KEY = "metadata-lon";

	String BINARY_ALT_KEY = "metadata-alt";

	String PLAIN_TEXT_KEY = "plainText";

	/**
	 * Set the plain text content.
	 * @param text
	 */
	void setPlainText(String text);

	/**
	 * Return the extracted plain text content of the binary.
	 * @return
	 */
	String getPlainText();

	/**
	 * Return the binary filename.
	 *
	 * @return
	 */
	default String getFileName() {
		return property(BINARY_FILENAME_PROPERTY_KEY);
	}

	/**
	 * Set the binary filename.
	 *
	 * @param fileName
	 * @return Fluent API
	 */
	default HibBinaryField setFileName(String fileName) {
		property(BINARY_FILENAME_PROPERTY_KEY, fileName);
		return this;
	}

	/**
	 * Increment any found postfix number in the filename.
	 *
	 * e.g:
	 * <ul>
	 * <li>test.txt -> test_1.txt</li>
	 * <li>test -> test_1</li>
	 * <li>test.blub.txt -> test.blub_1.txt</li>
	 * <ul>
	 *
	 */
	default void postfixFileName() {
		String oldName = getFileName();
		if (oldName != null && !oldName.isEmpty()) {
			setFileName(UniquenessUtil.suggestNewName(oldName));
		}

	}

	/**
	 * Return the binary mime type of the node.
	 *
	 * @return
	 */
	default String getMimeType() {
		return property(BINARY_CONTENT_TYPE_PROPERTY_KEY);
	}

	/**
	 * Set the binary mime type of the node.
	 *
	 * @param mimeType
	 * @return Fluent API
	 */
	default HibBinaryField setMimeType(String mimeType) {
		property(BINARY_CONTENT_TYPE_PROPERTY_KEY, mimeType);
		return this;
	}

	/**
	 * Set the binary image dominant color.
	 *
	 * @param dominantColor
	 * @return Fluent API
	 */
	default HibBinaryField setImageDominantColor(String dominantColor) {
		property(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY, dominantColor);
		return this;
	}

	/**
	 * Return the binary image dominant color.
	 *
	 * @return
	 */
	default String getImageDominantColor() {
		return property(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY);
	}

	/**
	 * Return the stored focal point of the image.
	 *
	 * @return Focal point or null if no focal point has been set
	 */
	default FocalPoint getImageFocalPoint() {
		Float x = property(BINARY_IMAGE_FOCAL_POINT_X);
		Float y = property(BINARY_IMAGE_FOCAL_POINT_Y);
		if (x == null || y == null) {
			return null;
		}
		return new FocalPoint(x, y);
	}

	/**
	 * Set the image focal point.
	 *
	 * @param point
	 */
	default BinaryGraphField setImageFocalPoint(FocalPoint point) {
		property(BINARY_IMAGE_FOCAL_POINT_X, point.getX());
		property(BINARY_IMAGE_FOCAL_POINT_Y, point.getY());
		return this;
	}

	/**
	 * Return the uuid of the binary field.
	 *
	 * @return
	 */
	String getUuid();

	/**
	 * Set the uuid of the binary field.
	 *
	 * @param uuid
	 */
	void setUuid(String uuid);

	/**
	 * Set the metadata property.
	 *
	 * @param key
	 * @param value
	 */
	void setMetadata(String key, String value);

	/**
	 * Return the metadata properties.
	 *
	 * @return
	 */
	Map<String, String> getMetadataProperties();

	@Override
	Binary getBinary();

	@Override
	NodeGraphFieldContainer getParentContainer();

	@Override
	Result<? extends ImageVariant> getImageVariants();

	/**
	 * Set the location information.
	 *
	 * @param loc
	 */
	default void setLocation(Location loc) {
		Objects.requireNonNull(loc, "A valid location object needs to be supplied. Got null.");
		setLocationLatitude(loc.getLat());
		setLocationLongitude(loc.getLon());
		Integer alt = loc.getAlt();
		if (alt != null) {
			setLocationAltitude(alt);
		}
	}

	/**
	 * Return the location latitude.
	 *
	 * @return
	 */
	default Double getLocationLatitude() {
		return property(BINARY_LAT_KEY);
	}

	/**
	 * Set the location latitude.
	 *
	 * @param lat
	 */
	default void setLocationLatitude(Double lat) {
		property(BINARY_LAT_KEY, lat);
	}

	/**
	 * Return the location longitude.
	 *
	 * @return
	 */
	default Double getLocationLongitude() {
		return property(BINARY_LON_KEY);
	}

	/**
	 * Set the location longitude.
	 *
	 * @param lon
	 */
	default void setLocationLongitude(Double lon) {
		property(BINARY_LON_KEY, lon);
	}

	/**
	 * Return the location altitude.
	 *
	 * @return
	 */
	default Integer getLocationAltitude() {
		return property(BINARY_ALT_KEY);
	}

	/**
	 * Set the location altitude.
	 *
	 * @param alt
	 */
	default void setLocationAltitude(Integer alt) {
		property(BINARY_ALT_KEY, alt);
	}

	/**
	 * Clear the metadata properties.
	 */
	default void clearMetadata() {
		setLocationAltitude(null);
		setLocationLongitude(null);
		setLocationLatitude(null);

		// Remove all other metadata properties
		getPropertyKeys().stream()
			.filter(e -> e.startsWith(META_DATA_PROPERTY_PREFIX))
			.forEach(e -> {
				setMetadata(e.substring(META_DATA_PROPERTY_PREFIX.length()), null);
			});
	}

	/**
	 * Attach the image variant to this binary field.
	 * 
	 * @param variant
	 */
	default void attachImageVariant(ImageVariant variant, boolean throwOnExisting) {
		NodeGraphFieldContainer container = getParentContainer();
		if (container.findImageVariant(getFieldKey(), variant) == null) {
			container.attachImageVariant(getFieldKey(), variant);
		} else {
			if (throwOnExisting) {
				throw error(BAD_REQUEST, "image_error_variant_exists", variant.getKey(), getFieldKey());
			}
		}
	}

	/**
	 * Detach the image variant from this binary field.
	 * 
	 * @param variant
	 */
	default void detachImageVariant(ImageVariant variant, boolean throwOnAbsent) {
		NodeGraphFieldContainer container = getParentContainer();
		if (container.findImageVariant(getFieldKey(), variant) != null) {
			container.detachImageVariant(getFieldKey(), variant);
		} else {
			if (throwOnAbsent) {
				throw error(BAD_REQUEST, "image_error_no_variant", variant.getKey(), getFieldKey());
			}
		}
	}
}
