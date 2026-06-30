package com.keepgoing.keepgoing.worker.application.port.out;

public class QuarantineObjectNotFoundException extends ImageStorageException {
	public QuarantineObjectNotFoundException(String storageKey, Throwable cause) {
		super("quarantine object not found: " + storageKey, cause);
	}
}
