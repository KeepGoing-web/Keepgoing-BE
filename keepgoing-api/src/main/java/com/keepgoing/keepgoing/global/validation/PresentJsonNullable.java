package com.keepgoing.keepgoing.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PresentJsonNullableValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PresentJsonNullable {

	String message() default "필드는 반드시 포함되어야 합니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
