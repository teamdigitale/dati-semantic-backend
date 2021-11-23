package it.teamdigitale.ndc.harvester.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

class SemanticAssetMetadataTest {

    @Test
    void shouldDefineConstraints() throws NoSuchFieldException {
        assertThat(getAnnotations("identifier", NotBlank.class)).isNotNull();
        assertThat(getAnnotations("uri", NotBlank.class)).isNotNull();
        assertThat(getAnnotations("type", NotNull.class)).isNotNull();
        assertThat(getAnnotations("title", NotBlank.class)).isNotNull();
        assertThat(getAnnotations("description", NotBlank.class)).isNotNull();
        assertThat(getAnnotations("modified", NotNull.class)).isNotNull();
        assertThat(getAnnotations("theme", NotBlank.class)).isNotNull();
        assertThat(getAnnotations("rightsHolder", NotBlank.class)).isNotNull();
        assertThat(getAnnotations("accrualPeriodicity", NotBlank.class)).isNotNull();
        assertThat(getAnnotations("distribution", NotEmpty.class)).isNotNull();
    }

    private Annotation getAnnotations(String fieldName, Class annotationClass)
        throws NoSuchFieldException {
        return SemanticAssetMetadata.class.getDeclaredField(fieldName)
            .getDeclaredAnnotation(annotationClass);
    }
}