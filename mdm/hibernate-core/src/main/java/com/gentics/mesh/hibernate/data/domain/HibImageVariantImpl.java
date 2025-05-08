package com.gentics.mesh.hibernate.data.domain;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.binary.HibImageVariantSetter;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;

@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "imagevariant")
@NamedQueries({
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_binary",
			query = "select v from imagevariant v where "
					+ " ((:width is null and v.imageWidth is null) or v.imageWidth = :width) and "
					+ " ((:height is null and v.imageHeight is null) or v.imageHeight = :height) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and v.auto = :auto and "
					+ " v.binary = :binary"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_binary_auto_width",
			query = "select v from imagevariant v where "
					+ " ((:height is null and v.imageHeight is null) or v.imageHeight = :height) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and v.auto = :auto and "
					+ " v.binary = :binary"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_binary_auto_height",
			query = "select v from imagevariant v where "
					+ " ((:width is null and v.imageWidth is null) or v.imageWidth = :width) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and v.auto = :auto and "
					+ " v.binary = :binary"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_binary_no_auto",
			query = "select v from imagevariant v where "
					+ " ((:width is null and v.imageWidth is null) or v.imageWidth = :width) and "
					+ " ((:height is null and v.imageHeight is null) or v.imageHeight = :height) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and v.binary = :binary"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_binary_no_auto_width",
			query = "select v from imagevariant v where "
					+ " ((:height is null and v.imageHeight is null) or v.imageHeight = :height) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and v.binary = :binary"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_binary_no_auto_height",
			query = "select v from imagevariant v where "
					+ " ((:width is null and v.imageWidth is null) or v.imageWidth = :width) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and v.binary = :binary"
		),
	@NamedQuery(
			name = "imagevariant_find_all_by_binary",
			query = "select v from imagevariant v where v.binary = :binary"
		),
	@NamedQuery(
			name = "imagevariant_find_all_by_field",
			query = "select v from imagevariant v join v.fields f where f = :field"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_field",
			query = "select v from imagevariant v join v.fields f where "
					+ " ((:width is null and v.imageWidth is null) or v.imageWidth = :width) and "
					+ " ((:height is null and v.imageHeight is null) or v.imageHeight = :height) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and v.auto = :auto and "
					+ " f = :field"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_field_no_auto",
			query = "select v from imagevariant v join v.fields f where "
					+ " ((:width is null and v.imageWidth is null) or v.imageWidth = :width) and "
					+ " ((:height is null and v.imageHeight is null) or v.imageHeight = :height) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and f = :field"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_field_auto_height",
			query = "select v from imagevariant v join v.fields f where "
					+ " ((:width is null and v.imageWidth is null) or v.imageWidth = :width) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and v.auto = :auto and "
					+ " f = :field"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_field_auto_width",
			query = "select v from imagevariant v join v.fields f where "
					+ " ((:height is null and v.imageHeight is null) or v.imageHeight = :height) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and v.auto = :auto and "
					+ " f = :field"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_field_no_auto_height",
			query = "select v from imagevariant v join v.fields f where "
					+ " ((:width is null and v.imageWidth is null) or v.imageWidth = :width) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and f = :field"
		),
	@NamedQuery(
			name = "imagevariant_find_by_manipulation_field_no_auto_width",
			query = "select v from imagevariant v join v.fields f where "
					+ " ((:height is null and v.imageHeight is null) or v.imageHeight = :height) and "
					+ HibImageVariantImpl.COMMON_FETCH_FILTER
					+ " and f = :field"
		)
})
@Table(
	indexes = {
		@Index(name = "idx_manipulation", columnList = "imageWidth,imageHeight,fpx,fpy,fpz,cropx,cropy,auto")
	}
)
public class HibImageVariantImpl extends AbstractImageDataImpl implements HibImageVariantSetter, Serializable {

	static final String COMMON_FETCH_FILTER = " ((:fpx is null and v.fpx is null) or abs(v.fpx - :fpx) < 0.0000001) and "
			+ " ((:fpy is null and v.fpy is null) or abs(v.fpy - :fpy) < 0.0000001) and "
			+ " ((:fpz is null and v.fpz is null) or abs(v.fpz - :fpz) < 0.0000001) and "
			+ " ((:cropX is null and v.cropX is null) or v.cropX = :cropX) and "
			+ " ((:cropY is null and v.cropY is null) or v.cropY = :cropY) and "
			+ " ((:cropWidth is null and v.cropWidth is null) or v.cropWidth = :cropWidth) and "
			+ " ((:cropHeight is null and v.cropHeight is null) or v.cropHeight = :cropHeight) and "
			+ " ((:cropMode is null and v.cropMode is null) or v.cropMode = :cropMode) and "
			+ " ((:resizeMode is null and v.resizeMode is null) or v.resizeMode = :resizeMode) ";

	private static final long serialVersionUID = 1L;

	protected Float fpx;
	protected Float fpy;
	protected Float fpz;
	protected Integer cropX;
	protected Integer cropY;
	protected Integer cropWidth;
	protected Integer cropHeight;
	protected boolean auto;

	@Enumerated(EnumType.STRING)
	@Column
	protected CropMode cropMode;

	@Enumerated(EnumType.STRING)
	@Column
	protected ResizeMode resizeMode;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = HibBinaryImpl.class)
	protected HibBinary binary;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "binary_field_variant", inverseJoinColumns = {@JoinColumn(name = "fields_dbUuid")}, joinColumns = {@JoinColumn(name = "variants_dbUuid")})
	protected Set<HibBinaryFieldEdgeImpl> fields = new HashSet<>();

	@Override
	public Object getBinaryDataId() {
		return getId();
	}

	@Override
	public HibBinary getBinary() {
		return binary;
	}

	@Override
	public Result<? extends HibBinaryField> findFields() {
		return new TraversalResult<>(fields);
	}

	@Override
	public Integer getWidth() {
		return imageWidth;
	}

	@Override
	public Integer getHeight() {
		return imageHeight;
	}

	@Override
	public Float getFocalPointX() {
		return fpx;
	}

	@Override
	public Float getFocalPointY() {
		return fpy;
	}

	@Override
	public Float getFocalPointZoom() {
		return fpz;
	}

	@Override
	public Integer getCropStartX() {
		return cropX;
	}

	@Override
	public Integer getCropStartY() {
		return cropY;
	}

	@Override
	public CropMode getCropMode() {
		return cropMode;
	}

	@Override
	public ResizeMode getResizeMode() {
		return resizeMode;
	}

	@Override
	public boolean isAuto() {
		return auto;
	}

	@Override
	public Integer getCropWidth() {
		return cropWidth;
	}

	@Override
	public Integer getCropHeight() {
		return cropHeight;
	}

	@Override
	public HibImageVariant setCropWidth(Integer cropWidth) {
		this.cropWidth = cropWidth;
		return this;
	}

	@Override
	public HibImageVariant setCropHeight(Integer cropHeight) {
		this.cropHeight = cropHeight;
		return this;
	}

	@Override
	public HibImageVariant setAuto(boolean auto) {
		this.auto = auto;
		return this;
	}

	@Override
	public HibImageVariant setResizeMode(ResizeMode resize) {
		this.resizeMode = resize;
		return this;
	}

	@Override
	public HibImageVariant setCropMode(CropMode crop) {
		this.cropMode = crop;
		return this;
	}

	@Override
	public HibImageVariant setCropStartY(Integer cropY) {
		this.cropY = cropY;
		return this;
	}

	@Override
	public HibImageVariant setCropStartX(Integer cropX) {
		this.cropX = cropX;
		return this;
	}

	@Override
	public HibImageVariant setFocalPointZoom(Float fpz) {
		this.fpz = fpz;
		return this;
	}

	@Override
	public HibImageVariant setFocalPointY(Float fpy) {
		this.fpy = fpy;
		return this;
	}

	@Override
	public HibImageVariant setFocalPointX(Float fpx) {
		this.fpx = fpx;
		return this;
	}

	@Override
	public HibImageVariantImpl setHeight(Integer height) {
		this.imageHeight = height;
		return this;
	}

	@Override
	public HibImageVariantImpl setWidth(Integer width) {
		this.imageWidth = width;
		return this;
	}

	public void setBinary(HibBinary binary) {
		this.binary = binary;
	}

	public void addField(HibBinaryFieldEdgeImpl field, boolean throwOnExisting) {
		fields.stream().filter(f -> f.getId().equals(field.getId())).findAny().ifPresentOrElse(u -> {
			if (throwOnExisting) {
				throw error(BAD_REQUEST, "Requested variant already exists"/*, getFieldKey()*/);
			}
		}, () -> {
			fields.add(field);
			HibernateTx.get().entityManager().merge(this);
			HibernateTx.get().entityManager().flush();
			HibernateTx.get().refresh(field);
		});
	}

	public void removeField(HibBinaryFieldEdgeImpl field, boolean throwOnAbsent) {
		fields.stream().filter(f -> f.getId().equals(field.getId())).findAny().ifPresentOrElse(u -> {
			HibernateUtil.dropGroupUserConnection(HibernateTx.get().entityManager(), this, field);
		}, () -> {
			if (throwOnAbsent) {
				throw error(BAD_REQUEST, "Requested variant not found"/*, getFieldKey()*/);
			}
		});
	}
}
