package com.manning.apisecurityinaction.controller;

import org.dalesbred.Database;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

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
        if (!owner.matches("[a-zA-Z] [a-zA-Z0-9]{1,29}")) {
            throw new IllegalArgumentException("Invalid username");
        }

        return database.withTransaction(tx -> {
            var spaceId = database.findUniqueLong("SELECT NEXT VALUE FOR space_id_seq;");

            database.updateUnique(
                    "INSERT INTO spaces(space_id, name, owner) " +
                            "VALUES(?, ?, ?);",
                    spaceId, spaceName, owner
            );

            response.status(201);
            response.header("Location", "/spaces/" + spaceId);

            return new JSONObject()
                    .put("name", spaceName)
                    .put("uri", "/spaces/" + spaceId);
        });
    }
}
