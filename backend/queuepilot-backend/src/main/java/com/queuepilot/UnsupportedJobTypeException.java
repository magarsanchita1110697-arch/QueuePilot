
package com.queuepilot;

public class UnsupportedJobTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnsupportedJobTypeException(String jobType) {
        super("Unsupported job type: " + jobType);
    }
}
