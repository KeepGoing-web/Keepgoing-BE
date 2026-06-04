package com.keepgoing.keepgoing.note.controller.validation;

import com.keepgoing.keepgoing.common.image.domain.ImageContentTypePolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class ValidImageFileValidator implements ConstraintValidator<ValidImageFile, MultipartFile> {

	private long maxSize;

	@Override
	public void initialize(ValidImageFile constraintAnnotation) {
		this.maxSize = constraintAnnotation.maxSize();
	}

	@Override
	public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
		if (file == null) {
			return true;
		}
		if (file.isEmpty()) {
			addViolation(context, "이미지 파일은 비어 있을 수 없습니다.");
			return false;
		}

		String originalFilename = StringUtils.getFilename(file.getOriginalFilename());
		if (originalFilename == null || originalFilename.isBlank()) {
			addViolation(context, "이미지 파일명은 비어 있을 수 없습니다.");
			return false;
		}
		if (originalFilename.length() > 255) {
			addViolation(context, "이미지 파일명은 255자를 초과할 수 없습니다.");
			return false;
		}

		String contentType = file.getContentType();
		if (contentType == null || contentType.isBlank()) {
			addViolation(context, "이미지 Content-Type은 비어 있을 수 없습니다.");
			return false;
		}
		if (!ImageContentTypePolicy.isAllowed(contentType)) {
			addViolation(context, "지원하지 않는 이미지 형식입니다.");
			return false;
		}
		if (file.getSize() > maxSize) {
			addViolation(context, "이미지 파일 크기가 허용 범위를 초과했습니다.");
			return false;
		}
		return true;
	}

	private void addViolation(ConstraintValidatorContext context, String message) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message)
				.addConstraintViolation();
	}
}
