package it.gov.innovazione.ndc.harvester.util;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class Version implements Comparable<Version> {
    private static final Pattern FULL_PATTERN = Pattern.compile("v?(\\d+)((?:\\.\\d+)*)");
    private static final Pattern SUB_VERSIONS_PATTERN = Pattern.compile("\\.(\\d+)");

    public String getSourceString() {
        return sourceString;
    }

    private final String sourceString;
    private final boolean isLatest;
    private final List<Integer> tokens;

    @Override
    public String toString() {
        return sourceString;
    }

    @Override
    public int compareTo(Version o) {
        if (isLatest) {
            if (o.isLatest) {
                return 0;
            }
            return 1;
        }
        if (o.isLatest) {
            return -1;
        }

        int otherSize = o.tokens.size();
        int mySize = tokens.size();
        for (int i = 0; i < mySize && i < otherSize; i++) {
            int comparison = Integer.compare(tokens.get(i), o.tokens.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }

        // if the common parts of token list are the same, the version with more tokens is subsequent (e.g. 1.2.4 > 1.2)
        return Integer.compare(mySize, otherSize);
    }

    public static Optional<Version> of(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Please provide a valid version string");
        }

        if ("latest".equals(s)) {
            return Optional.of(new Version(s, true, Collections.emptyList()));
        }

        Matcher matcher = FULL_PATTERN.matcher(s);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        List<Integer> tokens = new ArrayList<>();
        tokens.add(Integer.valueOf(matcher.group(1)));

        String subVersions = matcher.group(2);
        if (subVersions != null) {
            matcher = SUB_VERSIONS_PATTERN.matcher(subVersions);
            while (matcher.find()) {
                tokens.add(Integer.valueOf(matcher.group(1)));
            }
        }

        return Optional.of(new Version(s, false, tokens));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Version version = (Version) o;
        return sourceString.equals(version.sourceString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceString);
    }
}
