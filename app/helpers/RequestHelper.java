package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 7/11/16 Splendid Bits.
 */
public class RequestHelper {

    /**
     * Remove the device > subscription > route > alerts attributes.
     *
     * @param arrayNode the json to traverse.
     */
    public static JsonNode removeSubscriptionRouteAlerts(@Nonnull JsonNode arrayNode) {
        List<JsonNode> routeValues = arrayNode.findValues("route");
        for (JsonNode node : routeValues) {
            ObjectNode routeNode = (ObjectNode) node;
            routeNode.remove("alerts");
        }
        return arrayNode;
    }
}
