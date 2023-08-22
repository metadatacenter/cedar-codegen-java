package org.metadatacenter.cedar.codegen;

import junit.framework.TestCase;

import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.codegen.LiteralFieldWithDatatypeRecordTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LiteralFieldWithDatatypeRecordTemplateTest {

    protected static final String JAVA_RECORD_NAME = "MyRecord";

    protected static final String DATATYPE = "xsd:string";

    protected static final String JAVA_DOC = "This is a test record.";

    private final LiteralFieldWithDatatypeRecordTemplate template = new LiteralFieldWithDatatypeRecordTemplate();

    @Test
    public void fillTemplate_ValidParameters_ReturnsFilledTemplate() {

        var filledTemplate = template.fillTemplate(JAVA_RECORD_NAME, DATATYPE, JAVA_DOC);
        assertThat(filledTemplate).contains(JAVA_RECORD_NAME);
        assertThat(filledTemplate).contains(DATATYPE);
        assertThat(filledTemplate).contains(JAVA_DOC);
    }

    @Test
    public void fillTemplate_NullJavaRecordName_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            template.fillTemplate(null, DATATYPE, JAVA_DOC);
        });
    }

    @Test
    public void fillTemplate_NullDatatype_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            template.fillTemplate(JAVA_RECORD_NAME, null, JAVA_DOC);
        });
    }

    @Test
    public void fillTemplate_NullJavaDoc_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            template.fillTemplate(JAVA_RECORD_NAME, DATATYPE, null);
        });
    }
}
