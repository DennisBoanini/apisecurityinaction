package com.manning.apisecurityinaction.model;

import org.json.JSONObject;

import java.time.Instant;

public class Message {
    private final long spaceId;
    private final long msgId;
    private final String author;
    private final Instant time;
    private final String message;

    public Message(long spaceId, long msgId, String author, Instant time, String message) {
        this.spaceId = spaceId;
        this.msgId = msgId;
        this.author = author;
        this.time = time;
        this.message = message;
    }
    @Override
    public String toString() {
        JSONObject msg = new JSONObject();
        msg.put("uri",
                "/spaces/" + spaceId + "/messages/" + msgId);
        msg.put("author", author);
        msg.put("time", time.toString());
        msg.put("message", message);
        return msg.toString();
    }
}