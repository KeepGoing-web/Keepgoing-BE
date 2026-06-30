package com.keepgoing.keepgoing.worker.infrastructure.adapter.out;

import com.keepgoing.keepgoing.worker.application.port.out.ImageMediaTypeDetectorPort;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class TikaImageMediaTypeDetectorAdapter implements ImageMediaTypeDetectorPort {

	private final Tika tika = new Tika();

	@Override
	public String detect(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		return tika.detect(bytes);
	}
}
