package dao;

import io.ebean.EbeanServer;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 6/15/16 Splendid Bits.
 */
class BaseDao {
    EbeanServer mEbeanServer;

    BaseDao(EbeanServer ebeanServer) {
        this.mEbeanServer = ebeanServer;
    }
}
