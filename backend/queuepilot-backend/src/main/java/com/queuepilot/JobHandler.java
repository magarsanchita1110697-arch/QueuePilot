package com.queuepilot;

import java.util.Map;

public interface JobHandler {

    String getJobType();

    Map<String, Object> execute(Map<String, Object> payload);
}