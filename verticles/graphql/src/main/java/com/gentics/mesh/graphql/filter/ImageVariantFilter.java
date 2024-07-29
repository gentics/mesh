package com.gentics.mesh.graphql.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.EnumFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;

/**
 * Image variant fields filter.
 * 
 * @author plyhun
 *
 */
public class ImageVariantFilter extends ImageDataFilter<HibImageVariant> {

	private static final Map<String, ImageVariantFilter> instances = new HashMap<>();

	/**
	 * Create an image variant filter.
	 * 
	 * @param context
	 * @return
	 */
	public static ImageVariantFilter filter(String owner) {
		return instances.computeIfAbsent(owner, o -> new ImageVariantFilter(o));
	}

	private ImageVariantFilter(String owner) {
		super("ImageVariantFilter", "Filters over image variant data", owner);
	}

	@Override
	public List<FilterField<HibImageVariant, ?>> getFilters() {
		List<FilterField<HibImageVariant, ?>> filters = super.getFilters();
		filters.add(new MappedFilter<>(owner, "cropX", "Filters by crop X coordinate", NumberFilter.filter(),
				content -> (content == null || content.getCropStartX() == null) ? null : content.getCropStartX()));
		filters.add(new MappedFilter<>(owner, "cropY", "Filters by crop Y coordinate", NumberFilter.filter(),
				content -> (content == null || content.getCropStartY() == null) ? null : content.getCropStartY()));
		filters.add(new MappedFilter<>(owner, "focalX", "Filters by focal point X ratio coordinate", NumberFilter.filter(),
				content -> (content == null || content.getFocalPointX() == null) ? null : content.getFocalPointX()));
		filters.add(new MappedFilter<>(owner, "focalY", "Filters by focal point Y ratio coordinate", NumberFilter.filter(),
				content -> (content == null || content.getFocalPointY() == null) ? null : content.getFocalPointY()));
		filters.add(new MappedFilter<>(owner, "focalZ", "Filters by focal point zoom ratio", NumberFilter.filter(),
				content -> (content == null || content.getFocalPointZoom() == null) ? null : content.getFocalPointZoom()));
		filters.add(new MappedFilter<>(owner, "isAuto", "Filters by auto parameter", BooleanFilter.filter(),
				content -> content == null ? null : content.isAuto()));
		filters.add(new MappedFilter<>(owner, "resizeMode", "Filters by resize mode", EnumFilter.filter(ResizeMode.class),
				content -> content == null ? null : content.getResizeMode()));
		filters.add(new MappedFilter<>(owner, "cropMode", "Filters by crop mode", EnumFilter.filter(CropMode.class),
				content -> content == null ? null : content.getCropMode()));
		return filters;
	}
}
