package org.metadatacenter.cedar.codegen;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2023-08-08
 *
 * This class represents a template for generating Java record classes that implement the
 * LiteralField interface and include a datatype field.
 */
public class LiteralFieldWithDatatypeRecordTemplate {

    private static final String LITERAL_FIELD_TYPE_WITH_DATATYPE_DECL = """
                /**
                 *  ${javadoc}
                 */
                 @JsonPropertyOrder({"@type", "@value"})
                public static record ${typeName}(String value) implements LiteralField {
                    
                    /**
                     * Creates an instance of the {@code ${typeName}} record with a {@code null} value.
                     * @return An instance of the {@code ${typeName}} record.
                     */
                    public static ${typeName} of() {
                        return new ${typeName}(null);
                    }
                    
                    /**
                     * Creates an instance of the {@code ${typeName}} record with the specified value.
                     * @param value The value to set for the record.
                     * @return An instance of the {@code ${typeName}} record.
                     */
                    @JsonCreator
                    public static ${typeName} of(@JsonProperty("@value") String value) {
                        return new ${typeName}(value);
                    }
                    
                    /**
                     * Gets the datatype associated with the record.
                     * @return The datatype string.
                     */
                    @JsonView(CoreView.class)
                    @JsonProperty("@type")
                    public String getDatatype() {
                        return "${datatype}";
                    }
                }
            """;

    /**
     * Fills the template for the Literal Field record with the specified values returning Java code as a string.
     *
     * @param javaRecordName The name of the Java record.
     * @param datatype       The datatype associated with the record.
     * @param javaDoc        The Javadoc comment for the generated class.
     * @return The filled template as a string.
     * @throws NullPointerException if any of the parameters is null.
     */
    public String fillTemplate(@Nonnull String javaRecordName, @Nonnull String datatype, @Nonnull String javaDoc) {
        Objects.requireNonNull(javaRecordName);
        Objects.requireNonNull(datatype);
        Objects.requireNonNull(javaDoc);
        return LITERAL_FIELD_TYPE_WITH_DATATYPE_DECL.replace("${typeName}", javaRecordName)
                                                    .replace("${datatype}", datatype)
                                                    .replace("${javadoc}", javaDoc);
    }
}
