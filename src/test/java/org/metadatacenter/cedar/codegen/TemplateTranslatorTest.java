package org.metadatacenter.cedar.codegen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.metadatacenter.artifacts.model.core.*;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TemplateTranslatorTest {

    protected static final String SPECIFIED_TEMPLATE_CLASS_NAME = "SpecifiedTemplateClassName";

    protected static final String THE_FIELD_NAME = "TheFieldName";

    private TemplateTranslator translator;

    @Mock
    protected FieldSchemaArtifact field;

    @Before
    public void setUp() throws Exception {
        translator = new TemplateTranslator(SPECIFIED_TEMPLATE_CLASS_NAME);
        when(field.getName()).thenReturn(THE_FIELD_NAME);
    }

    @Test
    public void shouldGenerateNodeWithNameForField() {
        var node = translator.toCodeGenerationNode(field);
        assertThat(node.name()).isEqualTo(THE_FIELD_NAME);
    }

    @Test
    public void shouldGenerateNodeAsNonRootForField() {
        var node = translator.toCodeGenerationNode(field);
        assertThat(node.root()).isFalse();
    }

    @Test
    public void shouldGenerateNodeWithEmptyId() {
        var node = translator.toCodeGenerationNode(field);
        assertThat(node.id()).isEmpty();
    }

    @Test
    public void shouldGenerateNodeWithNonNullId() {
        final var expectedUri = "https://example.org";
        when(field.getIdentifier()).thenReturn(Optional.of(expectedUri));
        var node = translator.toCodeGenerationNode(field);
        assertThat(node.id()).isEqualTo(expectedUri);
    }


    @Test
    public void shouldGenerateNodeWithArtifactTypeLiteralField() {
        var node = translator.toCodeGenerationNode(field);
        assertThat(node.artifactType()).isEqualTo(CodeGenerationNodeRecord.ArtifactType.LITERAL_FIELD);
    }


    @Test
    public void shouldGenerateNodeWithArtifactTypeIriField() {
        var vc = mock(ValueConstraints.class);
        when(vc.getClasses()).thenReturn(List.of(mock(ClassValueConstraint.class)));
        when(field.getValueConstraints()).thenReturn(Optional.of(vc));
        var node = translator.toCodeGenerationNode(field);
        assertThat(node.artifactType()).isEqualTo(CodeGenerationNodeRecord.ArtifactType.LITERAL_FIELD);
        assertThat(node.artifactType()).isEqualTo(CodeGenerationNodeRecord.ArtifactType.IRI_FIELD);
    }
}