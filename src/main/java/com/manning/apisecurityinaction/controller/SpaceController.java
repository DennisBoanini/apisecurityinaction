package com.manning.apisecurityinaction.controller;

import com.manning.apisecurityinaction.model.Message;
import com.manning.apisecurityinaction.model.Space;
import org.dalesbred.Database;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.util.List;

public class SpaceController {

    private final Database database;

    public SpaceController(final Database database) {
        this.database = database;
    }

    public JSONObject createSpace(Request request, Response response) {
        var json = new JSONObject(request.body());
        var spaceName = json.getString("name");
        if (spaceName == null) {
            throw new IllegalArgumentException("Space name is mandatory");
        }

        if (spaceName.length() > 255) {
            throw new IllegalArgumentException("Space name length cannot be greater than 255 characters");
        }

        var owner = json.getString("owner");
        if (!owner.matches("[a-zA-Z]{1,29}")) {
            throw new IllegalArgumentException("Invalid username");
        }

        var subject = request.attribute("subject");
        if (!owner.equals(subject)) {
            throw new IllegalArgumentException("owner must match authenticated user");
        }

        return database.withTransaction(tx -> {
            var spaceId = database.findUniqueLong("SELECT NEXT VALUE FOR space_id_seq;");

            database.updateUnique(
                    "INSERT INTO spaces(space_id, name, owner) " +
                            "VALUES(?, ?, ?);",
                    spaceId, spaceName, owner
            );

            database.updateUnique(
                    "INSERT INTO permissions(space_id, user_id, perms) " +
                            "VALUES(?, ?, ?)",
                    spaceId, owner, "rwd"
            );

            response.status(201);
            response.header("Location", "/spaces/" + spaceId);

            return new JSONObject()
                    .put("name", spaceName)
                    .put("uri", "/spaces/" + spaceId);
        });
    }

    public JSONObject postMessage(final Request request, final Response response) {
        var spaceId = Long.parseLong(request.params(":spaceId"));
        var json = new JSONObject(request.body());
        var user = json.getString("author");
        if (!user.matches("[a-zA-Z0-9]{0,29}")) {
            throw new IllegalArgumentException("invalid username");
        }
        var message = json.getString("message");
        if (message.length() > 1024) {
            throw new IllegalArgumentException("message is too long");
        }

        return database.withTransaction(tx -> {
            var msgId = database.findUniqueLong(
                    "SELECT NEXT VALUE FOR msg_id_seq;");
            database.updateUnique(
                    "INSERT INTO messages(space_id, msg_id, msg_time," +
                            "author, msg_text) " +
                            "VALUES(?, ?, current_timestamp, ?, ?)",
                    spaceId, msgId, user, message);

            response.status(201);
            var uri = "/spaces/" + spaceId + "/messages/" + msgId;
            response.header("Location", uri);
            return new JSONObject().put("uri", uri);
        });
    }

    public Message readMessage(final Request request, final Response response) {
        var spaceId = Long.parseLong(request.params(":spaceId"));
        var messageId = Long.parseLong(request.params(":msgId"));

        final Message message = database.findUnique(Message.class,
                "SELECT space_id as spaceId, msg_id as msgId, author, msg_time, msg_text " +
                        "FROM messages " +
                        "WHERE msg_id = ? AND space_id = ?",
                messageId, spaceId);

        response.status(200);
        return message;
    }

    public List<Message> findMessages(final Request request, final Response response) {
        var spaceId = Long.parseLong(request.params(":spaceId"));

        List<Message> messages = database.findAll(Message.class,
                "SELECT space_id as spaceId, msg_id as msgId, author, msg_time, msg_text " +
                        "FROM messages " +
                        "WHERE space_id = ?",
                spaceId
        );
        return messages;
    }

    public List<Space> getSpaces(final Request request, final Response response) {
        return database.findAll(Space.class,
                "SELECT space_id, owner, name " +
                        "FROM spaces"
        );
    }

    public JSONObject addMember(Request request, Response response) {
        var json = new JSONObject(request.body());
        var spaceId = Long.parseLong(request.params(":spaceId"));
        var userToAdd = json.getString("username");
        var perms = json.getString("permissions");

        if (!perms.matches("r?w?d?")) {
            throw new IllegalArgumentException("invalid permissions");
        }

        database.updateUnique(
                "INSERT INTO permissions(space_id, user_id, perms)" +
                        "VALUES(?, ?, ?)",
                spaceId, userToAdd, perms
        );

        response.status(200);
        return new JSONObject()
                .put("username", userToAdd)
                .put("permissions", perms);
    }
}
