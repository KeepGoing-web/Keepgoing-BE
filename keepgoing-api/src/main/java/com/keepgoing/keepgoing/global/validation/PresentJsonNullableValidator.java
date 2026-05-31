package com.keepgoing.keepgoing.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openapitools.jackson.nullable.JsonNullable;

public class PresentJsonNullableValidator
		implements ConstraintValidator<PresentJsonNullable, JsonNullable<?>> {

	@Override
	public boolean isValid(JsonNullable<?> value, ConstraintValidatorContext context) {
		return value != null && value.isPresent();
	}

}
