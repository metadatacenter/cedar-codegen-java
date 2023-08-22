package org.metadatacenter.cedar.codegen;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CamelCaseTest {

    @Test
    void testToCamelCaseStartWithLowercase() {
        var input = "hello_world";
        var expectedOutput = "helloWorld";
        var result = CamelCase.toCamelCase(input, CamelCase.CamelCaseOption.START_WITH_LOWERCASE);
        assertThat(result).isEqualTo(expectedOutput);
    }

    @Test
    void testToCamelCaseStartWithUppercase() {
        var input = "hello_world";
        var expectedOutput = "HelloWorld";
        var result = CamelCase.toCamelCase(input, CamelCase.CamelCaseOption.START_WITH_UPPERCASE);
        assertThat(result).isEqualTo(expectedOutput);
    }

    @Test
    void testToCamelCaseEmptyString() {
        var input = "";
        var expectedOutput = "";
        var result = CamelCase.toCamelCase(input, CamelCase.CamelCaseOption.START_WITH_UPPERCASE);
        assertThat(result).isEqualTo(expectedOutput);
    }

    @Test
    void testToCamelCaseSingleWordStartWithLowercase() {
        var input = "hello";
        var expectedOutput = "hello";
        var result = CamelCase.toCamelCase(input, CamelCase.CamelCaseOption.START_WITH_LOWERCASE);
        assertThat(result).isEqualTo(expectedOutput);
    }

    @Test
    void testToCamelCaseSingleWordStartWithUppercase() {
        var input = "hello";
        var expectedOutput = "Hello";
        var result = CamelCase.toCamelCase(input, CamelCase.CamelCaseOption.START_WITH_UPPERCASE);
        assertThat(result).isEqualTo(expectedOutput);
    }

    @Test
    void testToCamelCaseWithSpecialCharacters() {
        var input = ">special-_case";
        var expectedOutput = "SpecialCase";
        var result = CamelCase.toCamelCase(input, CamelCase.CamelCaseOption.START_WITH_UPPERCASE);
        assertThat(result).isEqualTo(expectedOutput);
    }

    @Test
    void testToCamelCaseMultipleWordsWithMixedCaseOption() {
        var input = "camelCase_Test";
        var expectedOutput = "CamelCaseTest";
        var result = CamelCase.toCamelCase(input, CamelCase.CamelCaseOption.START_WITH_UPPERCASE);
        assertThat(result).isEqualTo(expectedOutput);
    }
}
