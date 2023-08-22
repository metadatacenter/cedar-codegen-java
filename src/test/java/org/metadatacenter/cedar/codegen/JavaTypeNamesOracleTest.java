package org.metadatacenter.cedar.codegen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class JavaTypeNamesOracleTest {

    protected static final JavaTypeNameFormat SUFFIX_TYPE = JavaTypeNameFormat.SUFFIX_WITH_ARTIFACT_TYPE;

    private JavaTypeNamesOracle oracle;

    @BeforeEach
    void setUp() {
        oracle = new JavaTypeNamesOracle(SUFFIX_TYPE);
    }

    @Test
    public void getJavaTypeName_DefaultTemplateArtifact_ReturnsDefaultTemplateName() {
        var node = mock(CodeGenerationNode.class);
        when(node.name()).thenReturn("");
        when(node.artifactType()).thenReturn(CodeGenerationNode.ArtifactType.TEMPLATE);

        var result = oracle.getJavaTypeName(node, new HashSet<>());
        assertEquals("MetadataInstance", result);
    }

    @Test
    public void getJavaTypeName_FieldArtifactType_ReturnsCamelCaseWithFieldSuffix() {
        var node = mock(CodeGenerationNode.class);
        when(node.name()).thenReturn("name");
        when(node.artifactType()).thenReturn(CodeGenerationNode.ArtifactType.LITERAL_FIELD);

        var result = oracle.getJavaTypeName(node, new HashSet<>());
        assertEquals("NameField", result);
    }

    @Test
    public void getJavaTypeName_ElementArtifactType_ReturnsCamelCaseWithElementSuffix() {

        var node = mock(CodeGenerationNode.class);
        when(node.name()).thenReturn("Person");
        when(node.artifactType()).thenReturn(CodeGenerationNode.ArtifactType.ELEMENT);

        var result = oracle.getJavaTypeName(node, new HashSet<>());
        assertEquals("PersonElement", result);
    }

    @Test
    public void getJavaTypeName_ContextWithSameName_ReturnsNameWithSuffix() {

        var node = mock(CodeGenerationNode.class);
        when(node.name()).thenReturn("Person");
        when(node.artifactType()).thenReturn(CodeGenerationNode.ArtifactType.ELEMENT);

        var context = new HashSet<CodeGenerationNode>();
        context.add(node);

        var result = oracle.getJavaTypeName(node, context);
        assertEquals("PersonElement2", result);
    }
}
