package com.constructora_backend.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * La URL debe empezar por https:// y no llevar espacios. Se permite vacío o null para los campos opcionales (los
 * obligatorios se marcan además con @NotBlank). Así no se guardan javascript:, data: (Base64) ni http sin cifrar: estas
 * direcciones las usa el sitio público como src de imágenes, iframes y enlaces.
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = "^$|^https://\\S+$")
@ReportAsSingleViolation
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlHttps {

    String message() default "La URL debe empezar por https:// y no puede contener espacios";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
