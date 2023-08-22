package org.metadatacenter.cedar.codegen;

import org.metadatacenter.artifacts.model.core.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.metadatacenter.cedar.codegen.CodeGenerationNode.*;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2023-08-11
 */
public class TemplateTranslator {

    private final String templateClassName;

    public TemplateTranslator(String templateClassName) {
        this.templateClassName = templateClassName;
    }

    @Nonnull
    public CodeGenerationNode translateTemplate(TemplateSchemaArtifact template) {
        return toCodeGenerationNode(template);
    }

    @Nonnull
    protected CodeGenerationNode toCodeGenerationNode(SchemaArtifact artifact) {
        if (artifact instanceof TemplateSchemaArtifact template) {
            return toTemplateNode(template);
        }
        else if (artifact instanceof ElementSchemaArtifact element) {
            return toElementNode(element);
        }
        else if (artifact instanceof FieldSchemaArtifact field) {
            return toFieldNode(field);
        }
        else {
            throw new RuntimeException("Unknown artifact type: " + artifact);
        }
    }

    @Nonnull
    private CodeGenerationNode toFieldNode(FieldSchemaArtifact field) {
        return CodeGenerationNode.get(field.getJsonLdId().map(URI::toString).orElse(""),
                                      false,
                                      field.getName(),
                                      List.of(),
                                      field.hasIRIValue() ? ArtifactType.IRI_FIELD : ArtifactType.LITERAL_FIELD,
                                      field.getDescription(),
                                      getXsdDatatype(field),
                                      field.getValueConstraints()
                                           .map(ValueConstraints::isRequiredValue)
                                           .filter(required -> required)
                                           .map(required -> Required.REQUIRED)
                                           .orElse(Required.OPTIONAL),
                                      toCardinality(field),
                                      getPropertyIri(field).orElse(null),
                                      field.getFieldUI().getInputType());
    }

    @Nonnull
    private CodeGenerationNode toElementNode(ElementSchemaArtifact element) {
        var childNodes = element.getChildSchemas()
                                .stream()
                                .map(childSchemaArtifact -> toCodeGenerationNode((SchemaArtifact) childSchemaArtifact))
                                .toList();
        return CodeGenerationNode.get(element.getJsonLdId().map(URI::toString).orElse(""),
                                      false,
                                      element.getName(),
                                      childNodes,
                                      ArtifactType.ELEMENT,
                                      element.getDescription(),
                                      null,
                                      Required.OPTIONAL,
                                      toCardinality(element),
                                      getPropertyIri(element).orElse(null),
                                      null);
    }

    @Nonnull
    private CodeGenerationNode toTemplateNode(TemplateSchemaArtifact template) {
        var childNodes = template.getChildSchemas().stream().map(childSchemaArtifact -> {
            return toCodeGenerationNode((SchemaArtifact) childSchemaArtifact);
        }).toList();
        return CodeGenerationNode.get(template.getJsonLdId().map(URI::toString).orElse(""),
                                      true,
                                      templateClassName,
                                      childNodes,
                                      ArtifactType.TEMPLATE,
                                      template.getDescription(),
                                      null,
                                      Required.OPTIONAL,
                                      null,
                                      getPropertyIri(template).orElse(null),
                                      null);
    }

    @Nullable
    private static String getXsdDatatype(@Nonnull FieldSchemaArtifact field) {
        var valueConstraints = field.getValueConstraints();
        if (valueConstraints.isEmpty()) {
            return null;
        }
        var vc = valueConstraints.get();
        if (vc.getNumberType().isPresent()) {
            return vc.getNumberType().map(NumberType::getText).orElse(null);
        }
        else if (vc.getTemporalType().isPresent()) {
            return vc.getTemporalType().map(TemporalType::getText).orElse(null);
        }
        else {
            return null;
        }
    }

    private Cardinality toCardinality(ChildSchemaArtifact artifact) {
        if (artifact.isMultiple()) {
            return new Cardinality(artifact.getMinItems().orElse(0), artifact.getMaxItems().orElse(Integer.MAX_VALUE));
        }
        else {
            return Cardinality.getZeroOrOne();
        }
    }


    private Optional<String> getPropertyIri(Artifact artifact) {
        if (artifact instanceof ChildSchemaArtifact childSchemaArtifact) {
            return childSchemaArtifact.getPropertyURI().map(URI::toString);
        }
        return Optional.empty();
    }
}
