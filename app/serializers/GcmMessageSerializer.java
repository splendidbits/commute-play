package serializers;

import com.google.gson.*;
import pushservices.models.database.Message;
import pushservices.models.database.PayloadElement;
import pushservices.models.database.Recipient;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.List;

public class GcmMessageSerializer implements JsonSerializer<Message> {
    private JsonArray mJsonRegistrationIds = new JsonArray();

    public GcmMessageSerializer(@Nonnull List<Recipient> recipients) {
        for (Recipient recipient : recipients) {
            mJsonRegistrationIds.add(new JsonPrimitive(recipient.token));
        }
    }

    @Override
    public JsonElement serialize(Message message, Type typeOfSrc, JsonSerializationContext context) {

        // Serialise the payload data into Json Elements.
        JsonObject jsonPayloadData = new JsonObject();
        if (message.payloadData != null) {
            for (PayloadElement payloadElement : message.payloadData) {
                jsonPayloadData.add(payloadElement.name, new JsonPrimitive(payloadElement.value));
            }
        }

        // Serialise the main elements.
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.add("collapse_key", new JsonPrimitive(message.collapseKey));
        jsonMessage.add("time_to_live", new JsonPrimitive(message.ttlSeconds));
        jsonMessage.add("dry_run", new JsonPrimitive(message.isDryRun));
        jsonMessage.add("registration_ids", mJsonRegistrationIds);
        jsonMessage.add("data", jsonPayloadData);

        // Add a restricted package attribute.
        if (message.credentials != null && message.credentials.packageUri != null) {
            jsonMessage.add("restricted_package_name", new JsonPrimitive(message.credentials.packageUri));
        }

        return jsonMessage;
    }
}