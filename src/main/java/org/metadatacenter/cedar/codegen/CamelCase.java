package org.metadatacenter.cedar.codegen;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2023-08-08
 */
public class CamelCase {

    @Nonnull
    public static String toCamelCase(@Nonnull String s,
                                     @Nonnull CamelCaseOption caseOption) {
        if (s.isBlank()) {
            return s;
        }
        s = stripName(s);
        var words = s.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])|\\W+|_");
        var joined = joinWords(words);
        if (caseOption.equals(CamelCaseOption.START_WITH_LOWERCASE)) {
            return lowercaseFirstLetter(joined);
        }
        else {
            return joined;
        }
    }

    @Nonnull
    private static String lowercaseFirstLetter(@Nonnull String joined) {
        return Character.toLowerCase(joined.charAt(0)) + joined.substring(1);
    }

    @Nonnull
    private static String joinWords(@Nonnull String[] words) {
        if(words.length == 1) {
            var singleWord = words[0];
            return Character.toUpperCase(singleWord.charAt(0)) + singleWord.substring(1);
        }
        return Arrays.stream(words)
                     .map(String::toLowerCase)
                     .filter(word -> !word.isBlank())
                     .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                     .collect(Collectors.joining());
    }

    @Nonnull
    private static String stripName(@Nonnull String name) {
        if (name.startsWith(">")) {
            name = name.substring(1);
        }
        return name;
    }

    public enum CamelCaseOption {
        START_WITH_LOWERCASE,
        START_WITH_UPPERCASE
    }

}
