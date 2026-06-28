package com.keepgoing.keepgoing.note.controller.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidImageFileValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImageFile {

	String message() default "올바른 이미지 파일이 아닙니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	long maxSize() default 5 * 1024 * 1024L;
}
