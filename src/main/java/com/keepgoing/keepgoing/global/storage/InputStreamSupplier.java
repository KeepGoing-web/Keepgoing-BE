package com.keepgoing.keepgoing.global.storage;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface InputStreamSupplier {
	InputStream get() throws IOException;
}
