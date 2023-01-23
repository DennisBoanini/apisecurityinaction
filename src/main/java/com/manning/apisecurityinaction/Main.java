package com.manning.apisecurityinaction;

import com.manning.apisecurityinaction.controller.SpaceController;
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

import static spark.Spark.after;
import static spark.Spark.afterAfter;
import static spark.Spark.exception;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.post;

/**
 * Hello world!
 *
 */
public class Main
{
    public static void main( String[] args ) throws URISyntaxException, IOException {
        var datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter", "password");
        var database = Database.forDataSource(datasource);
        createTables(database);

        datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter_api_user", "password");
        database = Database.forDataSource(datasource);

        var spaceController = new SpaceController(database);
        post("/spaces", spaceController::createSpace);

        after(((request, response) -> response.type("application/json")));
        internalServerError(new JSONObject().put("error", "internal server error").toString());
        notFound(new JSONObject().put("error", "not found").toString());
        exception(IllegalArgumentException.class, Main::badRequest);
        exception(JSONException.class, Main::badRequest);
        exception(EmptyResultException.class, (e, request, response) -> response.status(404));

        afterAfter(((request, response) -> response.header("Server", "")));
    }

    private static <T extends Exception> void badRequest(final T t, final Request request, final Response response) {
        response.status(400);
        response.body("{\"error:\": \"" + t +  "\"}");
    }

    private static void createTables(final Database database) throws URISyntaxException, IOException {
        var path = Paths.get(Main.class.getResource("/schema.sql").toURI());
        database.update(Files.readString(path));
    }
}
