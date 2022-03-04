package com.gentics.mesh.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PojoUtilTest {

	@Test
	public void testGetProperties() {
		assertThat(
			PojoUtil.getProperties(Person.class)
				.map(PojoUtil.Property::getName)
		).containsExactlyInAnyOrder("firstName", "lastName", "age", "active");
	}

	@Test
	public void testAssignIgnoringNull() {
		Person person1 = new Person().setFirstName("John");
		Person person2 = new Person().setLastName("Doe");
		Person person3 = new Person().setAge(23);

		assertPersonValues("John", "Doe", 23, person1, person2, person3);
		assertPersonValues("John", null, 23, person1, person3);
		assertPersonValues("John", "Doe", null, person1, person2);
		assertPersonValues(null, null, null);
	}

	private void assertPersonValues(String firstName, String lastName, Integer age, Person... persons) {
		Person result = PojoUtil.assignIgnoringNull(new Person(), persons);
		assertThat(result.getFirstName()).isEqualTo(firstName);
		assertThat(result.getLastName()).isEqualTo(lastName);
		assertThat(result.getAge()).isEqualTo(age);
	}


	public static class Person {
		private String firstName;
		private String lastName;
		private Integer age;
		private Boolean active;

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public Integer getAge() {
			return age;
		}

		public Person setFirstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		public Person setLastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public Person setAge(Integer age) {
			this.age = age;
			return this;
		}

		public Boolean getActive() {
			return active;
		}

		public Person setActive(Boolean active) {
			this.active = active;
			return this;
		}
	}
}
