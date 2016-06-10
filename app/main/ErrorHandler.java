package main;

import play.http.HttpErrorHandler;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import services.splendidlog.Logger;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Catches all global exceptions.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 3/23/16 Splendid Bits.
 */
public class ErrorHandler implements HttpErrorHandler{

    @Inject
    public ErrorHandler() {
    }

    public CompletionStage<Result> onClientError(Http.RequestHeader request, int statusCode, String message) {
        String logMessage = message != null ? message : "Exception was triggered";
        Logger.error(logMessage);

        return CompletableFuture.completedFuture(
                Results.status(statusCode, "A client error occurred: " + message)
        );
    }

    public CompletionStage<Result> onServerError(Http.RequestHeader request, Throwable exception) {
        String logMessage = "Exception was triggered";
        Logger.error(logMessage, exception);

        return CompletableFuture.completedFuture(
                Results.internalServerError("A server error occurred")
        );
    }
}
