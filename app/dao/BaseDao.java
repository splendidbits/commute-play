package dao;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.dbmigration.DdlGenerator;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import services.splendidlog.Logger;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 6/15/16 Splendid Bits.
 */
class BaseDao {
    EbeanServer mEbeanServer;

    BaseDao(EbeanServer ebeanServer) {
        mEbeanServer = ebeanServer;
    }

    /**
     * Attempt to create the database using the ebean ddl generated schemas.
     */
    void createDatabase() {
        Logger.warn("Error performing action on EbeanServer. Creating database with generated schemas.");

        ServerConfig serverConfig = ((SpiEbeanServer) mEbeanServer).getServerConfig();
        serverConfig.setDdlGenerate(true);
        serverConfig.setDdlRun(true);

        DdlGenerator ddlGenerator = new DdlGenerator((SpiEbeanServer) mEbeanServer, serverConfig);
        ddlGenerator.execute(true);
    }
}
