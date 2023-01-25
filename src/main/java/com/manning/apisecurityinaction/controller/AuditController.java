package com.manning.apisecurityinaction.controller;

import org.dalesbred.Database;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AuditController {

    private final Database database;

    public AuditController(final Database database) {
        this.database = database;
    }

    public void auditRequestStart(Request request, Response response) {
        database.withVoidTransaction(tx -> {
            var auditId = database.findUniqueLong("SELECT NEXT VALUE FOR audit_id_seq");
            request.attribute("audit_id", auditId);
            database.updateUnique(
                    "INSERT INTO audit_log(audit_id, method, path, user_id, audit_time) " +
                            "VALUES (?, ?, ?, ?, current_timestamp)",
                    auditId, request.requestMethod(), request.pathInfo(), request.attribute("subject")
            );
        });
    }

    public void auditRequestEnd(Request request, Response response) {
        database.updateUnique(
                "INSERT INTO audit_log(audit_id, method, path, status, user_id, audit_time) " +
                        "VALUES (?, ?, ?, ?, ?, current_timestamp)",
                request.attribute("audit_id"), request.requestMethod(), request.pathInfo(), response.status(), request.attribute("subject")
        );
    }

    public JSONArray readAuditLog(Request request, Response response) {
        var since = Instant.now().minus(1, ChronoUnit.HOURS);
        var logs = database.findAll(AuditController::recordToJson,
                "SELECT * FROM audit_log WHERE audit_time >= ? LIMIT 20",
                since
        );

        return new JSONArray(logs);
    }

    private static JSONObject recordToJson(final ResultSet resultSet) throws SQLException {
        return new JSONObject()
                .put("id", resultSet.getLong("audit_id"))
                .put("method", resultSet.getString("method"))
                .put("path", resultSet.getString("path"))
                .put("status", resultSet.getInt("status"))
                .put("user", resultSet.getString("user_id"))
                .put("time", resultSet.getTimestamp("audit_time").toInstant());
    }
}
