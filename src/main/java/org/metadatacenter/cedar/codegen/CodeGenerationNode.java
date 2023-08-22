package org.metadatacenter.cedar.codegen;

import org.metadatacenter.artifacts.model.core.FieldInputType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2023-08-16
 */
public interface CodeGenerationNode {

    static CodeGenerationNode get(@Nullable String id,
                                  boolean root,
                                  @Nonnull String name,
                                  @Nonnull List<CodeGenerationNode> childNodes,
                                  @Nonnull ArtifactType artifactType,
                                  @Nullable String description,
                                  @Nullable String xsdDatatype,
                                  @Nonnull Required required,
                                  @Nonnull Cardinality cardinality,
                                  @Nullable String propertyIri,
                                  @Nullable FieldInputType fieldInputType) {
        return new CodeGenerationNodeRecord(id, root, name, childNodes, artifactType, description, xsdDatatype, required, cardinality, propertyIri, fieldInputType);
    }

    Optional<String> getId();

    Optional<String> getXsdDatatype();

    Optional<String> getPropertyIri();

    Optional<String> getDescription();

    boolean isAttributeValueField();

    boolean isListType();

    String id();

    boolean root();

    String name();

    List<CodeGenerationNode> childNodes();

    ArtifactType artifactType();

    String description();

    String xsdDatatype();

    Required required();

    Cardinality cardinality();

    String propertyIri();

    FieldInputType fieldInputType();

    enum ArtifactType {
        ELEMENT, LITERAL_FIELD, IRI_FIELD, TEMPLATE;

        public boolean isField() {
            return this.equals(CodeGenerationNode.ArtifactType.IRI_FIELD) || this.equals(CodeGenerationNode.ArtifactType.LITERAL_FIELD);
        }
    }

    enum Required {
        REQUIRED,

        OPTIONAL
    }

    record Cardinality(int minCardinality,
                              int maxCardinality) {

        public Cardinality(int minCardinality, int maxCardinality) {
            if (minCardinality < 0) {
                throw new IllegalArgumentException("minCardinality < 0 (" + minCardinality + ")");
            }
            if (maxCardinality < 0) {
                throw new IllegalArgumentException("maxCardinality < 0 (" + maxCardinality + ")");
            }
            if (minCardinality > maxCardinality) {
                throw new IllegalArgumentException("minCardinality (" + minCardinality + ") > maxCardinality (" + maxCardinality + ")");
            }
            this.minCardinality = minCardinality;
            this.maxCardinality = maxCardinality;
        }

        public static Cardinality getZeroOrMore() {
            return new Cardinality(0, Integer.MAX_VALUE);
        }

        public static Cardinality getZeroOrOne() {
            return new Cardinality(0, 1);
        }

        public static Cardinality getExactlyOne() {
            return new Cardinality(1, 1);
        }

        public boolean hasUpperCardinality() {
            return maxCardinality < Integer.MAX_VALUE;
        }

        public boolean satisfiesMinCardinality(int cardinality) {
            return cardinality >= minCardinality;
        }

        public boolean satisfiesMaxCardinality(int cardinality) {
            return cardinality <= maxCardinality;
        }

        public boolean isMultiple() {
            return maxCardinality > 1;
        }

        public boolean isSingle() {
            return maxCardinality == 1;
        }
    }
}
