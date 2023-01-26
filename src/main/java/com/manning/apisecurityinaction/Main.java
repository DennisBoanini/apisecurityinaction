package com.manning.apisecurityinaction;

import com.google.common.util.concurrent.RateLimiter;
import com.manning.apisecurityinaction.controller.AuditController;
import com.manning.apisecurityinaction.controller.SpaceController;
import com.manning.apisecurityinaction.controller.UserController;
import org.dalesbred.Database;
import org.dalesbred.result.EmptyResultException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.json.JSONException;
import org.json.JSONObject;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.afterAfter;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.post;
import static spark.Spark.secure;

public class Main {
    public static void main( String[] args ) throws URISyntaxException, IOException {
        secure("localhost.p12", "changeit", null, null);
        var datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter", "password");
        var database = Database.forDataSource(datasource);
        createTables(database);

        datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter_api_user", "password");
        database = Database.forDataSource(datasource);

        var rateLimiter = RateLimiter.create(2.0d);

        before(((request, response) -> {
            if (!rateLimiter.tryAcquire()) {
                response.header("Retry-After", "2");
                halt(429);
            }
        }));

        before(((request, response) -> {
            if (request.requestMethod().equals("POST") && !"application/json".equals(request.contentType())) {
                halt(415, new JSONObject()
                        .put("error", "Only application/json supported").toString());
            }
        }));

        afterAfter(((request, response) -> {
            response.type("application/json;charset=utf-8");
            response.header("X-Content-Type-Options", "nosniff");
            response.header("X-Frame-Options", "DENY");
            response.header("X-XSS-Protection", "0");
            response.header("Cache-Control", "no-store");
            response.header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; sandbox");
            response.header("Server", "");
        }));


        var userController = new UserController(database);
        before(userController::authenticate);
        post("/users", userController::registerUser);

        var auditController = new AuditController(database);
        before(auditController::auditRequestStart);
        afterAfter(auditController::auditRequestEnd);
        get("/logs", auditController::readAuditLog);

        var spaceController = new SpaceController(database);
        before("/spaces", userController::requireAuthentication);
        post("/spaces", spaceController::createSpace);
        get("/spaces", spaceController::getSpaces);
        before("/spaces/:spaceId/messages", userController.requirePermissions("POST", "w"));
        post("/spaces/:spaceId/messages", spaceController::postMessage);
        before("/spaces/:spaceId/messages/:msgId", userController.requirePermissions("GET", "r"));
        get("/spaces/:spaceId/messages/:msgId", spaceController::readMessage);
        before("/spaces/:spaceId/messages", userController.requirePermissions("GET", "r"));
        get("/spaces/:spaceId/messages", spaceController::findMessages);

        internalServerError(new JSONObject().put("error", "internal server error").toString());
        notFound(new JSONObject().put("error", "not found").toString());
        exception(IllegalArgumentException.class, Main::badRequest);
        exception(JSONException.class, Main::badRequest);
        exception(EmptyResultException.class, (e, request, response) -> response.status(404));


    }

    private static <T extends Exception> void badRequest(final T t, final Request request, final Response response) {
        response.status(400);
        response.body(new JSONObject()
                .put("error", t.getMessage()).toString());
    }

    private static void createTables(final Database database) throws URISyntaxException, IOException {
        var path = Paths.get(Main.class.getResource("/schema.sql").toURI());
        database.update(Files.readString(path));
    }
}
