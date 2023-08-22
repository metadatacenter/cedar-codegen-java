package org.metadatacenter.cedar.codegen;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2023-08-08
 */
public class IriFieldRecordTemplate {

    private static final String IRI_FIELD_TYPE_DECL = """
                
                /**
                 *  ${javaDoc}
                 */
                @JsonInclude(JsonInclude.Include.NON_EMPTY)
                public static record ${typeName}(String id,
                                                 String label) implements IriField {
                    
                    /**
                     *  Creates an empty ${typeName} instance, with null values for the id and label.
                     */
                    public static ${typeName} of() {
                        return new ${typeName}(null, null);
                    }
                    
                    /**
                     * Create an instance of ${typeName} with the specified id and label.
                     * @param id The id.  This is an IRI.
                     * @param label The rdfs:label for the specified id.
                     */
                    @JsonCreator
                    public static ${typeName} of(@JsonProperty("@id") String id, @JsonProperty("rdfs:label") String label) {
                        return new ${typeName}(id, label);
                    }
                }
            """;

    private static final String IRI_FIELD_WITHOUT_LABEL_DECL = """
                
                /**
                 *  ${javaDoc}
                 */
                @JsonInclude(JsonInclude.Include.NON_EMPTY)
                public static record ${typeName}(String id) implements IriField {
                    
                    /**
                     *  Creates an empty ${typeName} instance, with null values for the id and label.
                     */
                    public static ${typeName} of() {
                        return new ${typeName}(null);
                    }
                    
                    /**
                     * Create an instance of ${typeName} with the specified id and label.
                     * @param id The id.  This is an IRI.
                     */
                    @JsonCreator
                    public static ${typeName} of(@JsonProperty("@id") String id) {
                        return new ${typeName}(id);
                    }
                    
                    @Override
                    public String label() {
                        return "";
                    }
                }
            """;


    public String fillTemplate(String typeName, String javaDoc) {
        return IRI_FIELD_TYPE_DECL.replace("${typeName}", typeName)
                .replace("${javaDoc}", javaDoc);
    }

    public String fillTemplateWithoutLabel(String typeName, String javaDoc) {
        return IRI_FIELD_WITHOUT_LABEL_DECL.replace("${typeName}", typeName)
                                  .replace("${javaDoc}", javaDoc);
    }
}
