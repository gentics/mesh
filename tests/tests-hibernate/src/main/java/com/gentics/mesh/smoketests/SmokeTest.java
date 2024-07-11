package com.gentics.mesh.smoketests;

import static com.gentics.mesh.test.TestDataProvider.CONTENT_UUID;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.image.FocalPointModel;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Smoke test that can be executed using the runSmokeTests Jenkins parameters.
 * This should be used as a sanity check test that can be quickly executed across all supported databases.
 */
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class SmokeTest extends AbstractMeshTest {

    @Test
    public void smokeTest() {
        setup();
        List<NodeResponse> nodeResponseList = client().findNodes(PROJECT_NAME).blockingGet().getData();
        assertThat(nodeResponseList).isNotEmpty();
    }

    private void setup() {
        tx(tx -> {
            Node node = folder("2015");
            Node folder = folder("news");
            tx.contentDao().updateWebrootPathInfo(tx.contentDao().getFieldContainer(folder, "de"), initialBranchUuid(), null);
            tx.contentDao().updateWebrootPathInfo(tx.contentDao().getFieldContainer(folder, "de"), initialBranchUuid(), null);

            Node node2 = content();
            tx.contentDao().updateWebrootPathInfo(tx.contentDao().getFieldContainer(node2, "en"), initialBranchUuid(), null);
            tx.contentDao().updateWebrootPathInfo(tx.contentDao().getFieldContainer(node2, "de"), initialBranchUuid(), null);
            Node node3 = folder("2014");

            // Update the folder schema to contain all fields
            Schema schemaContainer = schemaContainer("folder");
            SchemaVersionModel schema = schemaContainer.getLatestVersion().getSchema();
            schema.setUrlFields("niceUrl");
            schema.setAutoPurge(true);
            NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
            nodeFieldSchema.setName("nodeRef");
            nodeFieldSchema.setLabel("Some label");
            nodeFieldSchema.setAllowedSchemas("folder");
            schema.addField(nodeFieldSchema);

            BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
            binaryFieldSchema.setName("binary");
            schema.addField(binaryFieldSchema);

            S3BinaryFieldSchemaImpl s3BinaryFieldSchema = new S3BinaryFieldSchemaImpl();
            s3BinaryFieldSchema.setName("s3Binary");
            schema.addField(s3BinaryFieldSchema);

            NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
            numberFieldSchema.setName("number");
            schema.addField(numberFieldSchema);

            DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
            dateFieldSchema.setName("date");
            schema.addField(dateFieldSchema);

            HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
            htmlFieldSchema.setName("html");
            schema.addField(htmlFieldSchema);

            HtmlFieldSchema htmlLinkFieldSchema = new HtmlFieldSchemaImpl();
            htmlLinkFieldSchema.setName("htmlLink");
            schema.addField(htmlLinkFieldSchema);

            StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
            stringFieldSchema.setName("string");
            schema.addField(stringFieldSchema);

            StringFieldSchema niceUrlFieldSchema = new StringFieldSchemaImpl();
            niceUrlFieldSchema.setName("niceUrl");
            schema.addField(niceUrlFieldSchema);

            StringFieldSchema stringLinkFieldSchema = new StringFieldSchemaImpl();
            stringLinkFieldSchema.setName("stringLink");
            schema.addField(stringLinkFieldSchema);

            BooleanFieldSchema booleanFieldSchema = new BooleanFieldSchemaImpl();
            booleanFieldSchema.setName("boolean");
            schema.addField(booleanFieldSchema);

            ListFieldSchema stringListSchema = new ListFieldSchemaImpl();
            stringListSchema.setListType("string");
            stringListSchema.setName("stringList");
            schema.addField(stringListSchema);

            ListFieldSchema dateListSchema = new ListFieldSchemaImpl();
            dateListSchema.setListType("date");
            dateListSchema.setName("dateList");
            schema.addField(dateListSchema);

            ListFieldSchema nodeListSchema = new ListFieldSchemaImpl();
            nodeListSchema.setListType("node");
            nodeListSchema.setName("nodeList");
            schema.addField(nodeListSchema);

            ListFieldSchema htmlListSchema = new ListFieldSchemaImpl();
            htmlListSchema.setListType("html");
            htmlListSchema.setName("htmlList");
            schema.addField(htmlListSchema);

            ListFieldSchema booleanListSchema = new ListFieldSchemaImpl();
            booleanListSchema.setListType("boolean");
            booleanListSchema.setName("booleanList");
            schema.addField(booleanListSchema);

            ListFieldSchema numberListSchema = new ListFieldSchemaImpl();
            numberListSchema.setListType("number");
            numberListSchema.setName("numberList");
            schema.addField(numberListSchema);

            ListFieldSchema micronodeListSchema = new ListFieldSchemaImpl();
            micronodeListSchema.setListType("micronode");
            micronodeListSchema.setName("micronodeList");
            schema.addField(micronodeListSchema);


            MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
            micronodeFieldSchema.setAllowedMicroSchemas("vcard");
            micronodeFieldSchema.setName("micronode");
            schema.addField(micronodeFieldSchema);

            schemaContainer("folder").getLatestVersion().setSchema(schema);
            actions().updateSchemaVersion(schemaContainer("folder").getLatestVersion());

            // Setup some test data
            NodeFieldContainer container = tx.contentDao().createFieldContainer(node, "en", initialBranch(), user());

            // node
            container.createNode("nodeRef", node2);

            // number
            container.createNumber("number").setNumber(42.1);

            // date
            long milisec = dateToMilis("2012-07-11 10:55:21");
            container.createDate("date").setDate(milisec);

            // html
            container.createHTML("html").setHtml("some html");

            // htmlLink
            container.createHTML("htmlLink").setHtml("Link: {{mesh.link(\"" + CONTENT_UUID + "\", \"en\")}}");

            // string
            container.createString("string").setString("some string");

            // niceUrl
            container.createString("niceUrl").setString("/some/url");

            // stringLink
            container.createString("stringLink").setString("Link: {{mesh.link(\"" + CONTENT_UUID + "\", \"en\")}}");

            // boolean
            container.createBoolean("boolean").setBoolean(true);

            // binary
            Binary binary = tx.binaries().create("hashsumvalue", 1L).runInExistingTx(tx);
            binary.setImageHeight(10).setImageWidth(20).setSize(2048);
            container.createBinary("binary", binary).setImageDominantColor("00FF00")
                    .setImageFocalPoint(new FocalPointModel(0.2f, 0.3f)).setMimeType("image/jpeg");

            // s3binary
            S3Binary s3binary = tx.s3binaries().create(UUIDUtil.randomUUID(), node.getUuid() + "/s3", "test.jpg").runInExistingTx(tx);
            container.createS3Binary("s3Binary", s3binary);

            // stringList
            StringFieldList stringList = container.createStringList("stringList");
            stringList.createString("A");
            stringList.createString("B");
            stringList.createString("C");
            stringList.createString("D Link: {{mesh.link(\"" + CONTENT_UUID + "\", \"en\")}}");

            // htmlList
            HtmlFieldList htmlList = container.createHTMLList("htmlList");
            htmlList.createHTML("A");
            htmlList.createHTML("B");
            htmlList.createHTML("C");
            htmlList.createHTML("D Link: {{mesh.link(\"" + CONTENT_UUID + "\", \"en\")}}");

            // dateList
            DateFieldList dateList = container.createDateList("dateList");
            dateList.createDate(dateToMilis("2012-07-11 10:55:21"));
            dateList.createDate(dateToMilis("2014-07-11 10:55:30"));
            dateList.createDate(dateToMilis("2000-07-11 10:55:00"));

            // numberList
            NumberFieldList numberList = container.createNumberList("numberList");
            numberList.createNumber(42L);
            numberList.createNumber(1337);
            numberList.createNumber(0.314f);

            // booleanList
            BooleanFieldList booleanList = container.createBooleanList("booleanList");
            booleanList.createBoolean(true);
            booleanList.createBoolean(null);
            booleanList.createBoolean(false);

            // nodeList
            NodeFieldList nodeList = container.createNodeList("nodeList");
            nodeList.createNode(0, node2);
            nodeList.createNode(1, node3);
        });
    }

    protected long dateToMilis(String date) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date).getTime();
    }
}
