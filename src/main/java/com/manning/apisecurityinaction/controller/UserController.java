package com.manning.apisecurityinaction.controller;

import com.lambdaworks.crypto.SCryptUtil;
import org.dalesbred.Database;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

public class UserController {
    private final Database database;

    public UserController(final Database database) {
        this.database = database;
    }

    public JSONObject registerUser(Request request, Response response) {
        var json = new JSONObject(request.body());
        var username = json.getString("username");
        var password = json.getString("password");

        if (password.length() > 8) {
            throw new IllegalArgumentException("password must be at least at 8 characters");
        }

        var hash = SCryptUtil.scrypt(password, 32768, 8, 1);
        database.updateUnique("INSERT INTO users(user_id, pw_hash) VALUES(?, ?)", username, hash);

        response.status(201);
        response.header("Location", "/users/" + username);

        return new JSONObject().put("username", username);
    }
}
