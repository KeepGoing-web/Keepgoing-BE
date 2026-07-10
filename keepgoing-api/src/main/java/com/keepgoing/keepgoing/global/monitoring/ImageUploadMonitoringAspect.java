package com.keepgoing.keepgoing.global.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ImageUploadMonitoringAspect {

	private final MeterRegistry meterRegistry;

	// 전체 uploadImage() latency
	@Around("execution(* com.keepgoing..NoteImageService.uploadImage(..))")
	public Object monitorTotalUpload(ProceedingJoinPoint pjp) throws Throwable {
		Timer.Sample sample = Timer.start(meterRegistry);
		String exception = "none";
		try {
			return pjp.proceed();
		} catch (Throwable throwable) {
			exception = throwable.getClass().getSimpleName();
			throw throwable;
		} finally {
			sample.stop(Timer.builder("note.image.upload.total")
					.tag("success", Boolean.toString("none".equals(exception)))
					.tag("exception", exception)
					.register(meterRegistry)
			);
		}
	}

	// S3 PUT Storage latency (upload만, delete/presign 제외)
	@Around("execution(* com.keepgoing..ObjectStorageClient.upload(..))")
	public Object monitorStorageUpload(ProceedingJoinPoint pjp) throws Throwable {
		Timer.Sample sample = Timer.start(meterRegistry);
		String exception = "none";
		try {
			return pjp.proceed();
		} catch (Throwable throwable) {
			exception = throwable.getClass().getSimpleName();
			throw throwable;
		} finally {
			sample.stop(Timer.builder("note.image.upload.storage")
					.tag("success", Boolean.toString("none".equals(exception)))
					.tag("exception", exception)
					.register(meterRegistry)
			);
		}
	}
}
