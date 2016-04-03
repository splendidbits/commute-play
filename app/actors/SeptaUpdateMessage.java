package actors;

import models.app.AgencyUpdateType;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 3/28/16 Splendid Bits.
 */
public class SeptaUpdateMessage implements AgencyUpdateProtocol {

    @Override
    public AgencyUpdateType getAgencyType() {
        return AgencyUpdateType.TYPE_SEPTA;
    }
}
