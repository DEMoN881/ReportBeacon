package demonck.paper.dto;

public class Report {
    private final String reporterName;
    private final String reportedPlayerName;
    private final String reportedMessages;

    public Report(String reporterName, String reportedPlayerName, String reportedMessages) {
        this.reporterName = reporterName;
        this.reportedPlayerName = reportedPlayerName;
        this.reportedMessages = reportedMessages;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getReportedPlayerName() {
        return reportedPlayerName;
    }

    public String getReportedMessages() {
        return reportedMessages;
    }
}
