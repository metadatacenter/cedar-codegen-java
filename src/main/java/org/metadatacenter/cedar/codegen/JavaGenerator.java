package org.metadatacenter.cedar.codegen;

import com.fasterxml.jackson.annotation.*;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaRecordSource;
import org.jboss.forge.roaster.model.source.TypeHolderSource;
import org.metadatacenter.artifacts.model.core.FieldInputType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.metadatacenter.cedar.codegen.CamelCase.toCamelCase;
import static org.metadatacenter.cedar.codegen.CodeGenerationNode.*;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2023-05-03
 */
public class JavaGenerator {


    protected static final String JAVA_FILE_EXTENSION = ".java";

    protected static final String LITERAL_FIELD_IMPL = """

            public static record LiteralFieldImpl(@JsonProperty("@value") String value) implements LiteralField, Map<String, String> {

                    @Override
                    public int size() {
                        return 1;
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public boolean containsKey(Object key) {
                        return "@value".equals(key);
                    }

                    @Override
                    public boolean containsValue(Object value) {
                        return this.value.equals(value);
                    }

                    @Override
                    public String get(Object key) {
                        return value;
                    }

                    @Override
                    public String put(String key, String value) {
                        return value;
                    }

                    @Override
                    public String remove(Object key) {
                        return null;
                    }

                    @Override
                    public void putAll(Map<? extends String, ? extends String> m) {

                    }

                    @Override
                    public void clear() {

                    }

                    @Override
                    public Set<String> keySet() {
                        return Collections.singleton("@value");
                    }

                    @Override
                    public Collection<String> values() {
                        return Collections.singleton(value);
                    }

                    @Override
                    public Set<Entry<String, String>> entrySet() {
                        return Collections.singleton(new Entry<String, String>() {
                            @Override
                            public String getKey() {
                                return "@value";
                            }

                            @Override
                            public String getValue() {
                                return value;
                            }

                            @Override
                            public String setValue(String value) {
                                return null;
                            }
                        });
                    }
                }

            """;

    private final String packageName;

    private final JavaTypeNamesOracle javaTypeNamesOracle;

    private final String rootClassName;


    public JavaGenerator(String packageName, String rootClassName, JavaTypeNamesOracle javaTypeNamesOracle) {
        this.packageName = packageName;
        this.rootClassName = rootClassName;
        this.javaTypeNamesOracle = javaTypeNamesOracle;
    }

    public static JavaGenerator get(String packageName,
                                    String rootClassName,
                                    boolean suffixJavaTypeNames) {
        var typeNameFormat = suffixJavaTypeNames ? JavaTypeNameFormat.SUFFIX_WITH_ARTIFACT_TYPE : JavaTypeNameFormat.DO_NOT_SUFFIX_WITH_ARTIFACT_TYPE;
        System.err.println("Type name format: " + typeNameFormat);
        return new JavaGenerator(packageName, rootClassName, new JavaTypeNamesOracle(typeNameFormat));
    }

    public String generateJava(CodeGenerationNode node) {
        var rootCls = Roaster.create(JavaClassSource.class);
        rootCls.setPackage(packageName);
        rootCls.setName(rootClassName);
        generateImports(rootCls);
        generateConstants(node, rootCls);
        generateBaseInterfaces(rootCls);
        generateViewClassDeclarations(rootCls);
        generateUtils(rootCls);
        generateArtifactRecords(node, rootCls, new HashSet<>());
        return tidyAndPrintCode(rootCls);
    }

    private static String tidyAndPrintCode(JavaClassSource rootCls) {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        pw.println("// Generated code.  Do not edit by hand.");
        pw.println(rootCls);
        pw.flush();
        var code = sw.toString();
        return tidyCode(code);
    }

    public void writeJavaFile(CodeGenerationNode node, Path outputDirectory) throws IOException {
        var javaFileName = rootClassName + JAVA_FILE_EXTENSION;
        var packagePath = Path.of(packageName.replace(".", "/"));
        var packageDirectory = outputDirectory.resolve(packagePath);
        if (!Files.exists(packageDirectory)) {
            Files.createDirectories(packageDirectory);
        }
        var javaFilePath = packageDirectory.resolve(javaFileName);
        var code = generateJava(node);
        Files.write(javaFilePath, code.getBytes());
    }


    private static String tidyCode(String code) {
        // Get rid of spurious semicolons generated by the library that we use
        var tidiedCode = code.replaceAll("(?m)^\\s*;\\s*$", "");
        // Remove unnecessary static qualifier
        tidiedCode = tidiedCode.replace("public static record", "public record");
        return tidiedCode;
    }

    private void generateBaseInterfaces(JavaClassSource parentCls) {

        var instanceNodeInterface = createAndAddInstanceNodeInterface(parentCls);
        var artifactInterface = createAndAddArtifactInterface(parentCls, instanceNodeInterface);
        var fieldInterface = createAndAddFieldInterface(parentCls, artifactInterface);

        createAndAddElementInterface(parentCls, artifactInterface);
        createAndAddArtifactListInterface(parentCls, instanceNodeInterface);

        var compactableInterface = createAndAddCompactableInterface(parentCls);
        createAndAddLiteralFieldInterface(parentCls, fieldInterface, compactableInterface);
        createAndAddIriFieldInterface(parentCls, fieldInterface, compactableInterface);

        parentCls.addNestedType(LITERAL_FIELD_IMPL);
    }

    private static void generateUtils(JavaClassSource parentCls) {
        parentCls.addField()
                 .setPublic()
                 .setStatic(true)
                 .setFinal(true)
                 .setType(String.class)
                 .setName("IRI_PREFIX")
                 .setStringInitializer("https://repo.metadatacenter.org/template-element-instances/");

        parentCls.addMethod()
                 .setPublic()
                 .setStatic(true)
                 .setReturnType(String.class)
                 .setName("generateId")
                 .setBody("return IRI_PREFIX + UUID.randomUUID();");

        parentCls.addMethod("""
                                    private static Stream<Artifact> streamArtifacts(Object ... in) {
                                            return Arrays.stream(in)
                                                    .flatMap(o -> {
                                                        if(o instanceof List l) {
                                                            return l.stream();
                                                        }
                                                        else if(o instanceof Artifact) {
                                                            return Stream.of(o);
                                                        }
                                                        else {
                                                            return Stream.empty();
                                                        }
                                                    })
                                                    .filter(o -> o instanceof Artifact)
                                                    .map(o -> (Artifact) o);
                                        }
                                    """);
    }

    private static void createAndAddIriFieldInterface(JavaClassSource parentCls,
                                  JavaInterfaceSource fieldInterface,
                                  JavaInterfaceSource compactableInterface) {
        var iriFieldInterface = Roaster.create(JavaInterfaceSource.class);
        iriFieldInterface.setName("IriField");
        iriFieldInterface.addInterface(fieldInterface);
        iriFieldInterface.addInterface(compactableInterface);
        iriFieldInterface.addMethod()
                .setReturnType(String.class)
                .setName("id")
                .addAnnotation(JsonProperty.class)
                .setStringValue("@id");
        iriFieldInterface.addMethod()
                .setReturnType(String.class)
                .setName("label")
                .addAnnotation(JsonProperty.class)
                .setStringValue("rdfs:label");
        iriFieldInterface.addMethod()
                .setDefault(true)
                .setReturnType(String.class)
                .setName("compact")
                .setBody("return this.id();");
        iriFieldInterface.addMethod()
                             .setDefault(true)
                             .setReturnType(boolean.class)
                             .setName("isEmpty")
                             .setBody("return id() == null;")
                             .addAnnotation(JsonIgnore.class);

        parentCls.addNestedType(iriFieldInterface);
    }

    private static void createAndAddLiteralFieldInterface(JavaClassSource parentCls,
                                  JavaInterfaceSource fieldInterface,
                                  JavaInterfaceSource compactableInterface) {
        var literalFieldInterface = Roaster.create(JavaInterfaceSource.class);
        literalFieldInterface.setName("LiteralField");
        literalFieldInterface.addInterface(fieldInterface);
        literalFieldInterface.addInterface(compactableInterface);
        literalFieldInterface.addMethod()
                .setReturnType(String.class)
                .setName("value")
                .addAnnotation(JsonProperty.class)
                .setStringValue("@value");
        literalFieldInterface.addMethod()
                .setDefault(true)
                .setReturnType(String.class)
                .setName("compact")
                .setBody("return this.value();");
        literalFieldInterface.addMethod()
                .setPublic()
                .setStatic(true)
                .setReturnType("LiteralField")
                .setName("of")
                .setBody("return new LiteralFieldImpl(value);")
                .addParameter(String.class, "value");
        literalFieldInterface.addMethod()
                .setDefault(true)
                .setReturnType(boolean.class)
                .setName("isEmpty")
                .setBody("return value() == null;")
                             .addAnnotation(JsonIgnore.class);
        literalFieldInterface.addAnnotation(JsonIgnoreProperties.class)
                .setLiteralValue("ignoreUnknown", "true");

        parentCls.addNestedType(literalFieldInterface);
    }

    private static JavaInterfaceSource createAndAddCompactableInterface(JavaClassSource parentCls) {
        var compactableInterface = Roaster.create(JavaInterfaceSource.class);
        compactableInterface.setPublic()
                            .setName("Compactable")
                            .addMethod()
                            .setName("compact")
                            .setReturnType(String.class);
        parentCls.addNestedType(compactableInterface);
        return compactableInterface;
    }

    private static void createAndAddArtifactListInterface(JavaClassSource parentCls, JavaInterfaceSource instanceNodeInterface) {
        var artifactListInterface = Roaster.create(JavaInterfaceSource.class);
        artifactListInterface.setName("ArtifactList")
                .addInterface(instanceNodeInterface);
        artifactListInterface.addMethod()
                .setReturnType("List<Artifact>")
                .setName("getArtifacts")
                .addAnnotation(JsonValue.class);
        artifactListInterface.addMethod()
                .setDefault(true)
                .setReturnType(boolean.class)
                .setName("isEmpty")
                .setBody("return getArtifacts().stream().allMatch(Artifact::isEmpty);");
        parentCls.addNestedType(artifactListInterface);
    }

    private static void createAndAddElementInterface(JavaClassSource parentCls, JavaInterfaceSource artifactInterface) {
        var elementInterface = Roaster.create(JavaInterfaceSource.class);
        elementInterface.setPublic()
                .addInterface(artifactInterface)
                        .setName("Element")
                .addMethod()
                .setDefault(true)
                .setReturnType("boolean")
                .setName("isEmpty")
                .setBody("return getArtifacts().allMatch(Artifact::isEmpty);")
                .addAnnotation(Override.class);
        elementInterface.addMethod()
                .setName("id")
                .setReturnType(String.class)
                .addAnnotation(JsonProperty.class)
                .setStringValue("@id");
        elementInterface.addMethod()
                .setName("getArtifacts")
                .setReturnType("Stream<Artifact>")
                .addAnnotation(JsonIgnore.class);
        parentCls.addNestedType(elementInterface);
    }

    private static JavaInterfaceSource createAndAddFieldInterface(JavaClassSource parentCls,
                                                              JavaInterfaceSource artifactInterface) {
        var fieldInterface = Roaster.create(JavaInterfaceSource.class);
        fieldInterface.setName("Field")
                .addInterface(artifactInterface);
        parentCls.addNestedType(fieldInterface);
        return fieldInterface;
    }

    private static JavaInterfaceSource createAndAddArtifactInterface(JavaClassSource parentCls,
                                                              JavaInterfaceSource instanceNodeInterface) {
        var artifactInterface = Roaster.create(JavaInterfaceSource.class);
        artifactInterface.setName("Artifact")
                .addInterface(instanceNodeInterface);
        parentCls.addNestedType(artifactInterface);
        return artifactInterface;
    }

    private static JavaInterfaceSource createAndAddInstanceNodeInterface(JavaClassSource parentCls) {
        var instanceNodeInterface = Roaster.create(JavaInterfaceSource.class);
        instanceNodeInterface.setName("InstanceNode")
                .addMethod()
                .setReturnType(boolean.class)
                .setName("isEmpty")
                .addAnnotation(JsonIgnore.class);
        parentCls.addNestedType(instanceNodeInterface);
        return instanceNodeInterface;
    }

    private static void generateConstants(CodeGenerationNode rootNode, JavaClassSource parentCls) {

        var src = Roaster.create(JavaInterfaceSource.class);
        src.setName("FieldNames");

        var elements = new LinkedHashSet<CodeGenerationNode>();
        collectElements(rootNode, elements);
        var processed = new HashSet<String>();
        elements.forEach(element -> {
            var stripName = stripName(element.name());
            if(!processed.contains(stripName)) {
                processed.add(stripName);
                var field = src.addField();
                field.setType(String.class)
                     .setName(toConstantSymbol(element))
                        .setStringInitializer(stripName);
            }
        });
        parentCls.addNestedType(src);
    }

    private static String toConstantSymbol(CodeGenerationNode node) {
        var stripped = stripName(node.name());
        return stripped.trim().replaceAll(" +|-", "_");
    }

    private static void collectElements(CodeGenerationNode node, Collection<CodeGenerationNode> elements) {
        if (!node.root()) {
            elements.add(node);
        }
        node.childNodes()
                .forEach(childNode -> collectElements(childNode, elements));
    }

    private void generateArtifactRecords(CodeGenerationNode node, TypeHolderSource<?> parentCls, Set<CodeGenerationNode> context) {
//        if(generateNames.add(node.name())) {
            if (node.artifactType().isField()) {
                generateFieldDeclaration(node, parentCls, context);
            }
            else {
                context.add(node);
                var elementJavaCls = generateElementDeclaration(node, parentCls, context);
                node.childNodes().forEach(cn -> generateArtifactRecords(cn, elementJavaCls, context));
                context.remove(node);
            }
//        }
    }

    private static void generateImports(JavaClassSource parentClass) {
        parentClass.addImport(JsonInclude.class);
        parentClass.addImport(JsonProperty.class);
        parentClass.addImport(JsonView.class);
        parentClass.addImport(JsonCreator.class);
        parentClass.addImport(JsonValue.class);
        parentClass.addImport(JsonAnySetter.class);
        parentClass.addImport(JsonAnyGetter.class);
        parentClass.addImport(JsonUnwrapped.class);
        parentClass.addImport(JsonIgnore.class);
        parentClass.addImport(JsonIgnoreProperties.class);
        parentClass.addImport(JsonPropertyOrder.class);
        parentClass.addImport(Instant.class);
        parentClass.addImport(Nonnull.class);
        parentClass.addImport(Nullable.class);
        parentClass.addImport("java.util.*");
        parentClass.addImport(Stream.class);
    }

    private static void generateViewClassDeclarations(JavaClassSource parentCls) {
        var viewInterface = Roaster.create(JavaInterfaceSource.class);
        viewInterface.setName("CoreView");
        parentCls.addNestedType(viewInterface);
    }

    private void generateFieldDeclaration(CodeGenerationNode node, TypeHolderSource<?> parentCls, Set<CodeGenerationNode> context) {
        var recordName = javaTypeNamesOracle.getJavaTypeName(node, context);
        if(node.isAttributeValueField()) {
            // Nothing to do, because attribute value fields are phantom fields in a sense... they really mutate the
            // parent element and therefore modify the class representing that element, not this field
            return;
        }
        if (node.artifactType().equals(ArtifactType.LITERAL_FIELD)) {
            generateLiteralFieldDeclaration(node, parentCls, recordName);
        }
        else {
            generateIriFieldDeclaration(parentCls, recordName, node.getDescription().orElse(""),
                                        !FieldInputType.LINK.equals(node.fieldInputType()));
        }
        if(node.cardinality().isMultiple()) {
            generateArtifactListDeclaration(node, parentCls, context);
        }
    }

    private static void generateIriFieldDeclaration(TypeHolderSource<?> parentCls,
                                                    String recordName,
                                                    String description,
                                                    boolean withLabel) {
        var template = new IriFieldRecordTemplate();
        final String decl;
        if(withLabel) {
            decl = template.fillTemplate(recordName, description);
        }
        else {
            decl = template.fillTemplateWithoutLabel(recordName, description);
        }
        parentCls.addNestedType(decl);
    }

    private static void generateLiteralFieldDeclaration(CodeGenerationNode node,
                                                        TypeHolderSource<?> parentCls,
                                                        String recordName) {
        var datatype = node.getXsdDatatype();
        if (datatype.isPresent()) {
            var literalFieldTemplate = new LiteralFieldWithDatatypeRecordTemplate();
            var decl = literalFieldTemplate.fillTemplate(recordName, node.getXsdDatatype().orElse(""), node.getDescription().orElse(""));
            parentCls.addNestedType(decl);
        }
        else {
            var literalFieldTemplate = new LiteralFieldRecordTemplate();
            var decl = literalFieldTemplate.fillTemplate(recordName, node.getDescription().orElse(""));
            parentCls.addNestedType(decl);
        }
    }

    private static final String ATTRIBUTE_VALUE_ELEMENT_EXTENSION = """
                @JsonCreator
                public static ${typeName} fromJson(${paramDeclarationsList}) {
                        return new ${typeName}(${argsList});
                }
                
                @JsonAnySetter
                public void setAttributeValue(String key, LiteralFieldImpl value) {
                    this.attributeValues.put(key, value);
                }
            """;

    private void generateArtifactListDeclaration(CodeGenerationNode node, TypeHolderSource<?> parentCls, Set<CodeGenerationNode> context) {

        var javaTypeName = javaTypeNamesOracle.getJavaTypeName(node, context);
        var listJavaTypeName = javaTypeName + "List";
        var paramName = getParameterName(node);
        var listParamName = paramName + "List";
        var listCls = Roaster.create(JavaRecordSource.class);

        // Record that has a Java list of the node's Java type
        listCls.setPublic()
               .setName(listJavaTypeName)
               .addRecordComponent("List<" + javaTypeName + ">", listParamName);

        listCls.addInterface("ArtifactList");
        // ArtifactList.of()
        var ofMethod = listCls.addMethod()
               .setPublic()
               .setStatic(true)
               .setReturnType(listJavaTypeName)
               .setName("of");
        if (node.required().equals(Required.REQUIRED)) {
            ofMethod
                   .setBody("return of(" + javaTypeName + ".of());");
        }
        else {
            ofMethod.setBody("return new ${listJavaTypeName}(List.of());".replace("${listJavaTypeName}", listJavaTypeName));
        }

        listCls.addMethod()
                .setPublic()
                .setReturnType(int.class)
                .setName("minItems")
               .setBody("return " + node.cardinality().minCardinality() + ";");

        var maxCardinalityValue = node.cardinality().hasUpperCardinality() ? String.valueOf(node.cardinality().maxCardinality()) : "Integer.MAX_VALUE";

        listCls.addMethod()
                .setReturnType(int.class)
                .setPublic()
                .setName("maxItems")
               .setBody("return " + maxCardinalityValue + ";");

        listCls.addMethod()
                .setReturnType(boolean.class)
                .setPublic()
                .setName("isCardinalitySatisfied")
               .setBody("return " + listParamName + ".size() >= minItems() && " + listParamName + ".size() <= maxItems();");

        // Non empty list
        // ArtifactList.of(List<ArtifactJavaType>)
        var creatorMethod = listCls.addMethod();
        creatorMethod
               .setPublic()
               .setStatic(true)
               .setReturnType(listJavaTypeName)
               .setName("of")
               .setBody("""
                         return new ${listJavaTypeName}(${listParamName});
                        """.replace("${listJavaTypeName}", listJavaTypeName)
                                .replace("${listParamName}", listParamName))
               .addParameter("List<" + javaTypeName + ">", listParamName);
        creatorMethod.addAnnotation(JsonCreator.class);

        // Singleton list
        // ArtifactList.of(ArtifactJavaType)
        listCls.addMethod()
               .setPublic()
               .setStatic(true)
               .setReturnType(listJavaTypeName)
               .setName("of")
               .setBody("return new " + listJavaTypeName + "(List.of(" + paramName +"));")
               .addParameter(javaTypeName, paramName);

        listCls.addMethod()
               .setPublic()
               .setReturnType("List<Artifact>")
               .setName("getArtifacts")
               .setBody("return new ArrayList<>(" + listParamName + ");")
               .addAnnotation(Override.class);
        parentCls.addNestedType(listCls);
    }

    private TypeHolderSource<?> generateElementDeclaration(CodeGenerationNode node, TypeHolderSource<?> parentClass, Set<CodeGenerationNode> context) {

        var idParam = "@JsonProperty(\"@id\") String id,";

        var childParamDecls = getChildArtifactsParameterList(node, context);

        if(containsAttributeValueField(node)) {
            childParamDecls += ",\n@JsonAnyGetter Map<String, LiteralField> attributeValues";
        }

        var rootNodeExtras = getRootNodeExtras(node);

        var paramDeclarationsList = idParam + rootNodeExtras.stream()
                                                            .map(s -> s + ",\n")
                                                            .collect(Collectors.joining()) + childParamDecls;

        var emptyArgumentsList = "generateId(),\n" + rootNodeExtras.stream()
                                                           .map(s -> "null,\n")
                                                           .collect(Collectors.joining()) + node.childNodes()
                                                                                                .stream()
                                                                                                .map(n -> getEmptyInstance(n, context))
                                                                                                .collect(Collectors.joining(
                                                                                                        ",\n"));

        if(containsAttributeValueField(node)) {
            emptyArgumentsList += ",\nnew LinkedHashMap<>()";
        }


        var childNodeArgsList = getChildArtifactsArgsList(node);
        var argsList = "id,\n" + rootNodeExtras.stream()
                                                                   .map(s -> "null,\n")
                                                                   .collect(Collectors.joining())
                + childNodeArgsList;

        if(containsAttributeValueField(node)) {
            argsList += ",\nnew LinkedHashMap<>()";
        }

        var typeName = javaTypeNamesOracle.getJavaTypeName(node, context);

        var contextBlock = new StringBuilder();

        if (!node.artifactType().isField()) {
            contextBlock.append("var contextMap = new LinkedHashMap<String, Object>();\n");
            node.childNodes().forEach(childNode -> {
                childNode.getPropertyIri().ifPresent(propertyIri -> {
                    contextBlock.append("contextMap.put(FieldNames.");
                    contextBlock.append(toConstantSymbol(childNode));
                    contextBlock.append(", \"");
                    contextBlock.append(propertyIri);
                    contextBlock.append("\");\n");
                });
            });
        }
        else {
            contextBlock.append("var contextMap = Map.of();");
        }


        if (node.root()) {
            contextBlock.append(String.format("contextMap.put(\"%s\", \"%s\");\n", "schema", "http://schema.org/"));
            contextBlock.append(String.format("contextMap.put(\"%s\", \"%s\");\n",
                                              "xsd",
                                              "http://www.w3.org/2001/XMLSchema#"));
            contextBlock.append(String.format("contextMap.put(\"%s\", \"%s\");\n",
                                              "skos",
                                              "http://www.w3.org/2004/02/skos/core#"));
            contextBlock.append(String.format("contextMap.put(\"%s\", \"%s\");\n",
                                              "rdfs",
                                              "http://www.w3.org/2000/01/rdf-schema#"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "pav:createdOn",
                                              "Map.of(\"@type\", \"xsd:dateTime\")"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "pav:createdBy",
                                              "Map.of(\"@type\", \"@id\")"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "rdfs:label",
                                              "Map.of(\"@type\", \"xsd:string\")"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "oslc:modifiedBy",
                                              "Map.of(\"@type\", \"@id\")"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "pav:derivedFrom",
                                              "Map.of(\"@type\", \"@id\")"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "skos:notation",
                                              "Map.of(\"@type\", \"xsd:string\")"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "schema:isBasedOn",
                                              "Map.of(\"@type\", \"@id\")"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "schema:description",
                                              "Map.of(\"@type\", \"xsd:string\")"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "pav:lastUpdatedOn",
                                              "Map.of(\"@type\", \"xsd:dateTime\")"));
            contextBlock.append(String.format("contextMap.put(\"%s\", %s);\n",
                                              "schema:name",
                                              "Map.of(\"@type\", \"xsd:string\")"));
        }
        contextBlock.append("return contextMap;");

        String attributeValueElementExtension;
        if (containsAttributeValueField(node)) {
            // Slight hack here.  We remove the map for attribute values because this is set through the @AnySetter annotated method.
             var truncatedParamDeclarationsList = paramDeclarationsList.substring(0, paramDeclarationsList.lastIndexOf(",\n@JsonAnyGetter"));
             attributeValueElementExtension = ATTRIBUTE_VALUE_ELEMENT_EXTENSION.replace("${typeName}", typeName)
                    .replace("${argsList}", argsList)
                                                                                  .replace("${paramDeclarationsList}", truncatedParamDeclarationsList)
                                                                                  .replace("${emptyArgumentsList}", emptyArgumentsList);

        }
        else {
            attributeValueElementExtension = "";
        }


        var elementTemplate = new ElementRecordTemplate();
        var elementRecordDecl = elementTemplate.fillTemplate(typeName,
                                     attributeValueElementExtension,
                                     paramDeclarationsList,
                                     emptyArgumentsList,
                                     childNodeArgsList,
                                     contextBlock.toString());
        var javaCls = parentClass.addNestedType(elementRecordDecl);

        if(node.cardinality().isMultiple()) {
            generateArtifactListDeclaration(node, parentClass, context);
        }
        return (TypeHolderSource<?>) javaCls;
    }

    /**
     * Get root node extra parameters
     * @param node The root node
     * @return A list of parameter declarations for the root node
     */
    private static ArrayList<String> getRootNodeExtras(CodeGenerationNode node) {
        var rootNodeExtras = new ArrayList<String>();
        if (node.root()) {
            rootNodeExtras.add("@JsonView(CoreView.class) @JsonProperty(\"schema:name\") String schemaName");
            rootNodeExtras.add("@JsonView(CoreView.class) @JsonProperty(\"schema:description\") String schemaDescription");
            rootNodeExtras.add("@JsonView(CoreView.class) @JsonProperty(\"schema:isBasedOn\") String isBasedOn");
            rootNodeExtras.add("@JsonView(CoreView.class) @JsonProperty(\"pav:createdOn\") Instant pavCreatedOn");
            rootNodeExtras.add("@JsonView(CoreView.class) @JsonProperty(\"pav:createdBy\") String pavCreatedBy");
            rootNodeExtras.add("@JsonView(CoreView.class) @JsonProperty(\"pav:lastUpdatedOn\") Instant pavLastUpdatedOn");
            rootNodeExtras.add("@JsonView(CoreView.class) @JsonProperty(\"oslc:modifiedBy\") String oslcModifiedBy");
            rootNodeExtras.add("@JsonView(CoreView.class) @JsonProperty(\"pav:derivedFrom\") String pavDerivedFrom");
        }
        return rootNodeExtras;
    }

    private static String getChildArtifactsArgsList(CodeGenerationNode node) {
        return node.childNodes()
                   .stream()
                   .map(JavaGenerator::getParameterName)
                   .collect(Collectors.joining(",\n"));
    }

    private String getChildArtifactsParameterList(CodeGenerationNode node, Set<CodeGenerationNode> context) {
        return node.childNodes()
                   .stream()
                   .map(n -> getParameterDeclaration(n, context))
                   .collect(Collectors.joining(",\n"));
    }

    private static boolean containsAttributeValueField(CodeGenerationNode node) {
        return node.childNodes().stream().anyMatch(CodeGenerationNode::isAttributeValueField);
    }

    private static String stripName(String name) {
        if (name.startsWith(">")) {
            name = name.substring(1);
        }
        return name;
    }

    private String getEmptyInstance(CodeGenerationNode node, Set<CodeGenerationNode> context) {
        if(node.isAttributeValueField()) {
            return "List.of()";
        }
        var typeName = javaTypeNamesOracle.getJavaTypeName(node, context);
        if (node.isListType()) {
            return typeName + "List.of()";
        }
        else {
            return typeName + ".of()";
        }
    }

    private String getParameterDeclaration(CodeGenerationNode node, Set<CodeGenerationNode> context) {
        String paramType;
        var paramName = getParameterName(node);
        var constantName = toConstantSymbol(node);
        if(node.isAttributeValueField()) {
            paramType = "List<String>";
        }
        else {
            var typeName = javaTypeNamesOracle.getJavaTypeName(node, context);
            // Accounts for multivalued
            paramType = getParameterType(node, typeName);
        }

        var required = isRequired(node);
        var requiredAnnotation = "";
        if(required) {
            requiredAnnotation = "@Nonnull";
        }
        else {
            requiredAnnotation = "@Nullable";
        }
        return requiredAnnotation + " @JsonView(CoreView.class) @JsonProperty(FieldNames." + constantName + ") " + paramType + " " + paramName;
    }

    private static String getParameterName(CodeGenerationNode node) {
        var name = stripName(node.name());
        return toCamelCase(name, CamelCase.CamelCaseOption.START_WITH_LOWERCASE);
    }

    private static boolean isRequired(CodeGenerationNode node) {
        var required = node.required();
        if(required.equals(Required.REQUIRED)) {
            return true;
        }
        else {
            return node.childNodes()
                    .stream()
                    .anyMatch(JavaGenerator::isRequired);

        }
    }

    private static String getParameterType(CodeGenerationNode node, String typeName) {
        var listType = node.isListType();
        return listType ? typeName + "List" : typeName;
    }


}
