package com.gentics.diktyo.orientdb3;

import static com.gentics.diktyo.db.DatabaseType.MEMORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.diktyo.Diktyo;
import com.gentics.diktyo.db.Database;
import com.gentics.diktyo.orientdb3.domain.Job;
import com.gentics.diktyo.orientdb3.domain.JobImpl;
import com.gentics.diktyo.orientdb3.domain.Person;
import com.gentics.diktyo.orientdb3.domain.PersonImpl;
import com.gentics.diktyo.tx.Tx;

public class WrapperTest {

	@Test
	public void testWrapperAPI() {
		Diktyo diktyo = Diktyo.diktyo();
		diktyo.db().create("test", MEMORY);
		try (Database db = diktyo.db().open("test", MEMORY)) {
			try (Tx tx = db.tx()) {
				Job job = db.createVertex(JobImpl.class);
				assertNotNull(job);
				job.setName("Software Developer");
				assertEquals("Software Developer", job.getName());

				Person person = db.createVertex(PersonImpl.class);
				person.setName("Johannes");
				assertEquals("Johannes", person.getName());

				person.setJob(job);
				Job foundJob = person.getJob();
				assertNotNull("The previously set job could not be found.", foundJob);
				assertEquals("Software Developer", foundJob.getName());

				// Refresh the job name index.
				// job.index().get("name").refresh();
				// job.index().get("name").traverse();
				tx.rollback();
			}

			try (Tx tx = db.tx()) {
				//WrappedTraversal<Job> jobResult = db.traverse(g -> g.V().hasLabel("Job")).wrap(JobImpl.class);
				//db.index().exists("abc");
			}

		}
	}
}
