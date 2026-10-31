package com.queuepilot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class JobHandlerRegistry {

    private final Map<String, JobHandler> handlers = new HashMap<>();

    public JobHandlerRegistry(List<JobHandler> jobHandlers) {
        for (JobHandler handler : jobHandlers) {
            String jobType = handler.getJobType();

            if (handlers.putIfAbsent(jobType, handler) != null) {
                throw new IllegalStateException(
                        "Duplicate job handler registered: " + jobType
                );
            }
        }
    }

    public JobHandler getHandler(String jobType) {
        JobHandler handler = handlers.get(jobType);

        if (handler == null) {
            throw new IllegalArgumentException(
                    "Unsupported job type: " + jobType
            );
        }

        return handler;
    }
}
