package dao;

import annotations.CommuteEbeanServer;
import io.ebean.EbeanServer;
import io.ebean.config.ServerConfig;
import io.ebeaninternal.api.SpiEbeanServer;
import main.Constants;
import play.Logger;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 6/15/16 Splendid Bits.
 */
class BaseDao {
    EbeanServer mEbeanServer;

    BaseDao(@CommuteEbeanServer EbeanServer ebeanServer) {
        mEbeanServer = ebeanServer;
    }

    /**
     * Attempt to create the database using the ebean ddl generated schemas.
     */
    void createDatabase() {
        if (Constants.GENERATE_RUN_DLL_DATABASE) {
            Logger.warn("Error performing action on EbeanServer. Creating database with generated schemas.");

            ServerConfig serverConfig = ((SpiEbeanServer) mEbeanServer).getServerConfig();
            serverConfig.setDdlGenerate(true);
            serverConfig.setDdlRun(true);
        }
    }
}
