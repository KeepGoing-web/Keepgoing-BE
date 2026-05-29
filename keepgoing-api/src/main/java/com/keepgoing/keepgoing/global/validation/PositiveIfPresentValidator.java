package com.keepgoing.keepgoing.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openapitools.jackson.nullable.JsonNullable;

public class PositiveIfPresentValidator
		implements ConstraintValidator<PositiveIfPresent, JsonNullable<Long>> {

	@Override
	public boolean isValid(JsonNullable<Long> value, ConstraintValidatorContext context) {
		if (value == null || !value.isPresent()) {
			return true; // presence는 다른 validator가 담당
		}

		Long unwrapped = value.orElse(null);
		if (unwrapped == null) {
			return true;
		}

		return unwrapped > 0L;
	}
}
