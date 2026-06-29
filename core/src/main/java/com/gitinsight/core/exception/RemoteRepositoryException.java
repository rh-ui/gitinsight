package com.gitinsight.core.exception;

import java.io.IOException;

public class RemoteRepositoryException extends IOException {
    public RemoteRepositoryException(String message) {
        super(message);
    }

    public RemoteRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

}
