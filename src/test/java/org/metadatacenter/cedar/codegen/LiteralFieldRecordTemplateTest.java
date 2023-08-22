package org.metadatacenter.cedar.codegen;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LiteralFieldRecordTemplateTest {

    protected static final String JAVA_RECORD_NAME = "MyLiteralField";

    protected static final String JAVA_DOC = "This is a test Javadoc.";

    @Test
    void testFillTemplate() {

        var template = new LiteralFieldRecordTemplate();
        var filledTemplate = template.fillTemplate(JAVA_RECORD_NAME, JAVA_DOC);

        var expectedTemplate = """
                /**
                 * This is a test Javadoc.
                 */
                public static record MyLiteralField(String value) implements LiteralField {
                    
                    /**
                     * Creates an instance of the {@code MyLiteralField} record with a {@code null} value.
                     * @return An instance of the {@code MyLiteralField} record.
                     */
                    public static MyLiteralField of() {
                        return new MyLiteralField(null);
                    }
                    
                    /**
                     * Creates an instance of the {@code MyLiteralField} record with the specified value.
                     * @param value The value to set for the record.
                     * @return An instance of the {@code MyLiteralField} record.
                     */
                    @JsonCreator
                    public static MyLiteralField of(@JsonProperty("@value") String value) {
                        return new MyLiteralField(value);
                    }
                }
            """;

        assertThat(filledTemplate).isEqualTo(expectedTemplate);
    }

    @Test
    void testFillTemplateWithNullName() {
        var template = new LiteralFieldRecordTemplate();
        assertThrows(NullPointerException.class, () -> template.fillTemplate(null, JAVA_DOC));
    }

    @Test
    void testFillTemplateWithNullJavadoc() {
        var template = new LiteralFieldRecordTemplate();
        assertThrows(NullPointerException.class, () -> template.fillTemplate(JAVA_RECORD_NAME, null));
    }
}
