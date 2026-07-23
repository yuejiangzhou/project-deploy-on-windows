package com.company.deploy.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PackageProgressDTO {

    private String taskId;
    private String status;
    private Integer progress;
    private String currentStep;
    private List<LogEntry> logs = new ArrayList<>();

    @Data
    public static class LogEntry {
        private String time;
        private String message;
        private String level;

        public LogEntry() {}

        public LogEntry(String time, String message, String level) {
            this.time = time;
            this.message = message;
            this.level = level;
        }
    }
}
