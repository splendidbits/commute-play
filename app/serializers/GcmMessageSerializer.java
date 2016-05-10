package serializers;

import com.google.gson.*;
import pushservices.models.database.Message;
import pushservices.models.database.PayloadElement;
import pushservices.models.database.Recipient;

import java.lang.reflect.Type;

public class GcmMessageSerializer implements JsonSerializer<Message> {

    @Override
    public JsonElement serialize(Message message, Type typeOfSrc, JsonSerializationContext context) {

        // Serialise the app data block into a Json Element.
        JsonObject jsonPayloadData = new JsonObject();
        if (message.payloadData != null) {
            for (PayloadElement payloadElement : message.payloadData) {
                jsonPayloadData.add(payloadElement.name, new JsonPrimitive(payloadElement.value));
            }
        }

        JsonArray registrationIdArray = new JsonArray();
        if (message.recipients != null) {
            for (Recipient recipient : message.recipients) {
                registrationIdArray.add(new JsonPrimitive(recipient.token));
            }
        }

        // Serialise the main elements.
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.add("collapse_key", new JsonPrimitive(message.collapseKey));
        jsonMessage.add("time_to_live", new JsonPrimitive(message.ttlSeconds));
        jsonMessage.add("dry_run", new JsonPrimitive(message.isDryRun));
        jsonMessage.add("registration_ids", registrationIdArray);
        jsonMessage.add("data", jsonPayloadData);
        if (message.credentials != null && message.credentials.packageUri != null) {
            jsonMessage.add("restricted_package_name", new JsonPrimitive(message.credentials.packageUri));
        }

        return jsonMessage;
    }
}