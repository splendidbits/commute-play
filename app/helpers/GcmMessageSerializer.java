package helpers;

import com.google.gson.*;
import models.taskqueue.Message;
import models.taskqueue.Recipient;

import java.lang.reflect.Type;

public class GcmMessageSerializer implements JsonSerializer<Message> {

    @Override
    public JsonElement serialize(Message message, Type typeOfSrc, JsonSerializationContext context) {

        // Serialise the app data block into a Json Element.
        Gson gson = new Gson();
        JsonElement payloadData = gson.toJsonTree(message.payloadData);

        JsonArray registrationIdArray = new JsonArray();
        for (Recipient recipient : message.recipients) {
            registrationIdArray.add(new JsonPrimitive(recipient.recipientId));
        }

        // Serialise the main elements.
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.add("collapse_key", new JsonPrimitive(message.collapseKey));
        jsonMessage.add("time_to_live", new JsonPrimitive(message.ttl));
        jsonMessage.add("dry_run", new JsonPrimitive(message.isDryRun));
        jsonMessage.add("registration_ids", registrationIdArray);
        jsonMessage.add("data", payloadData);
        if (message.restrictedPackageName != null) {
            jsonMessage.add("restricted_package_name", new JsonPrimitive(message.restrictedPackageName));
        }

        return jsonMessage;
    }
}