package org.metadatacenter.cedar.codegen;

import org.metadatacenter.artifacts.model.core.FieldInputType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2023-05-09
 *
 * Represents the information about an artifact that is required for code generation.  Each code generation node
 * corresponds to a node in the artifact tree of a CEDAR template.
 */
public record CodeGenerationNodeRecord(@Nullable String id,
                                       boolean root,
                                       @Nonnull String name,
                                       @Nonnull List<CodeGenerationNode> childNodes,
                                       @Nonnull ArtifactType artifactType,
                                       @Nullable String description,
                                       @Nullable String xsdDatatype,
                                       @Nonnull Required required,
                                       @Nonnull Cardinality cardinality,
                                       @Nullable String propertyIri,
                                       @Nullable FieldInputType fieldInputType) implements CodeGenerationNode {

    public CodeGenerationNodeRecord(@Nullable String id,
                                    boolean root,
                                    @Nonnull String name,
                                    List<CodeGenerationNode> childNodes,
                                    ArtifactType artifactType,
                                    @Nullable String description,
                                    @Nullable String xsdDatatype,
                                    Required required,
                                    Cardinality cardinality,
                                    @Nullable String propertyIri,
                                    FieldInputType fieldInputType) {
        this.id = id;
        this.root = root;
        this.name = Objects.requireNonNull(name);
        this.childNodes = Objects.requireNonNull(childNodes);
        this.artifactType = Objects.requireNonNull(artifactType);
        this.description = Objects.requireNonNull(description);
        this.xsdDatatype = xsdDatatype;
        this.required = Objects.requireNonNull(required);
        this.cardinality = Objects.requireNonNullElse(cardinality, Cardinality.getZeroOrMore());
        this.propertyIri = propertyIri;
        this.fieldInputType = fieldInputType;
    }

    @Override
    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    @Override
    public Optional<String> getXsdDatatype() {
        return Optional.ofNullable(xsdDatatype);
    }

    @Override
    public Optional<String> getPropertyIri() {
        return Optional.ofNullable(propertyIri);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public boolean isAttributeValueField() {
        return FieldInputType.ATTRIBUTE_VALUE.equals(fieldInputType);
    }

    @Override
    public boolean isListType() {
        return this.cardinality != null && this.cardinality().isMultiple();
    }


}
