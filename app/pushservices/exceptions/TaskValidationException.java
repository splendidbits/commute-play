package pushservices.exceptions;

/**
 *
 * Describes fatal problems with a {@link pushservices.models.database.Task}.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/11/16 Splendid Bits.
 */
public class TaskValidationException extends Throwable {

    public TaskValidationException(String message) {
        super(message);
    }
}
