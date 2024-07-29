package com.gentics.mesh.hibernate.util;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import com.gentics.mesh.hibernate.data.PermissionType;
import com.gentics.mesh.hibernate.data.domain.HibPermissionRootImpl;

public class TypeInfoUtilTest {

	@Test
	public void testClassWithAnnotation() {
		ElementType type = TypeInfoUtil.getType(TestClassWithAnnotation.class);
		Assertions.assertThat(type).isEqualTo(ElementType.JOB);
	}

	@Test
	public void testClassWithoutAnnotation() {
		ElementType type = TypeInfoUtil.getType(TestClassWithoutAnnotation.class);
		Assertions.assertThat(type).isNull();
	}

	@Test
	public void testElementWithAnnotation() {
		ElementType type = TypeInfoUtil.getType(new TestClassWithAnnotation());
		Assertions.assertThat(type).isEqualTo(ElementType.JOB);
	}

	@Test
	public void testElementWithoutAnnotation() {
		ElementType type = TypeInfoUtil.getType(new TestClassWithoutAnnotation());
		Assertions.assertThat(type).isNull();
	}

	@Test
	public void testSupportedRootElement() {
		ElementType type = TypeInfoUtil.getType(new HibPermissionRootImpl().setType(PermissionType.USER));
		Assertions.assertThat(type).isEqualTo(ElementType.USER);
	}

	@Test
	public void testUnsupportedRootElement() {
		ElementType type = TypeInfoUtil.getType(new HibPermissionRootImpl().setType(PermissionType.MESH));
		Assertions.assertThat(type).isNull();
	}

	@Test
	public void testNullElement() {
		ElementType type = TypeInfoUtil.getType((HibPermissionRootImpl) null);
		Assertions.assertThat(type).isNull();
	}

	@ElementTypeKey(ElementType.JOB)
	private static class TestClassWithAnnotation implements HibBaseElement {

		@Override
		public void setUuid(String uuid) {

		}

		@Override
		public String getUuid() {
			return null;
		}

		@Override
		public Object getId() {
			return null;
		}

		@Override
		public String getElementVersion() {
			return null;
		}
	}

	private static class TestClassWithoutAnnotation implements HibBaseElement {

		@Override
		public void setUuid(String uuid) {

		}

		@Override
		public String getUuid() {
			return null;
		}

		@Override
		public Object getId() {
			return null;
		}

		@Override
		public String getElementVersion() {
			return null;
		}
	}
}