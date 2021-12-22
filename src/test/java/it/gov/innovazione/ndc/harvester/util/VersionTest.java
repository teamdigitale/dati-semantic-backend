package it.gov.innovazione.ndc.harvester.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionTest {

    @ParameterizedTest
    @ValueSource(strings = {"latest", "1", "v1", "1.2.3", "v1.2.4"})
    void canCreateValidVersion(String version) {
        assertThat(Version.of(version)).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"later", "old", "v1.", "1.", "1.alpha"})
    void createsEmptyVersionsForInvalidVersion(String version) {
        assertThat(Version.of(version)).isEmpty();
    }

    @Test
    void cannotCreateFromNull() {
        assertThatThrownBy(() -> Version.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canCreateFromLatest() {
        Version.of("latest");
    }

    @Test
    void compareLatestBefore() {
        assertFirstVersionIsGreaterThanSecond("latest", "0.1");
    }

    @Test
    void compareLatestAfter() {
        assertFirstVersionIsLessThanSecond("0.2", "latest");
    }

    @Test
    void compareSingleNumbers() {
        assertFirstVersionIsGreaterThanSecond("2", "1");
    }

    @Test
    void compareSingleNumbersWithLeadingV() {
        assertFirstVersionIsGreaterThanSecond("v2", "v1");
    }

    @Test
    void compareNestedNumbers() {
        assertFirstVersionIsLessThanSecond("1.2.3", "1.3.4");
    }

    @Test
    void compareVersionWithItsSubVersion() {
        assertFirstVersionIsLessThanSecond("1.2", "1.2.4");
    }

    @Test
    void compareSubVersionWithItsSupVersion() {
        assertFirstVersionIsGreaterThanSecond("1.2.4", "1.2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "1", "v1", "1.2.3", "v1.2.4"})
    void compareVersionWithItself(String version) {
        assertValidVersionsAndCompare(version, version, (v1, v2) -> assertThat(v1).isEqualByComparingTo(v2));
    }

    private void assertFirstVersionIsGreaterThanSecond(String a, String b) {
        assertValidVersionsAndCompare(a, b, (v1, v2) -> assertThat(v1).isGreaterThan(v2));
    }

    private void assertFirstVersionIsLessThanSecond(String s1, String s2) {
        assertValidVersionsAndCompare(s1, s2, (v1, v2) -> assertThat(v1).isLessThan(v2));
    }

    private void assertValidVersionsAndCompare(String firstVersionSource, String secondVersionSource, BiConsumer<Version, Version> assertionOnVersion) {
        Optional<Version> firstVersion = Version.of(firstVersionSource);
        Optional<Version> secondVersion = Version.of(secondVersionSource);
        assertThat(firstVersion).isNotEmpty();
        assertThat(secondVersion).isNotEmpty();
        assertionOnVersion.accept(firstVersion.get(), secondVersion.get());
    }
}