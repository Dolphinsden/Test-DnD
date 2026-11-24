package com.dndcoop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntry {

    public enum EntryType {
        DM,
        PLAYER,
        DICE,
        COMBAT,
        SYSTEM
    }

    private EntryType type;
    private String speaker;
    private String text;
    private String detail;
    private String timestamp;

    public LogEntry() {
    }

    public LogEntry(EntryType type, String speaker, String text, String detail) {
        this.type = type;
        this.speaker = speaker;
        this.text = text;
        this.detail = detail;
        this.timestamp = Instant.now().toString();
    }

    public static LogEntry dm(String sourceLabel, String narrative) {
        return new LogEntry(EntryType.DM, sourceLabel, narrative, null);
    }

    public static LogEntry player(String characterName, String actionText) {
        return new LogEntry(EntryType.PLAYER, characterName, actionText, null);
    }

    public static LogEntry dice(String actorName, String summary, String formulaDetail) {
        return new LogEntry(EntryType.DICE, actorName, summary, formulaDetail);
    }

    public static LogEntry combat(String title, String text) {
        return new LogEntry(EntryType.COMBAT, title, text, null);
    }

    public static LogEntry system(String text) {
        return new LogEntry(EntryType.SYSTEM, "System", text, null);
    }

    public EntryType getType() { return type; }
    public void setType(EntryType type) { this.type = type; }
    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
