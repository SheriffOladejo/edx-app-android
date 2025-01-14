package com.grassroot.academy.module.db.impl;

import android.content.Context;
import android.util.SparseArray;

import com.grassroot.academy.base.MainApplication;
import com.grassroot.academy.core.EdxDefaultModule;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.module.db.IDatabase;

import dagger.hilt.android.EntryPointAccessors;

/**
 * This class provides singleton instances of database implemention as {@link com.grassroot.academy.module.db.IDatabase}.
 *
 * @author rohan
 */
public class DatabaseFactory {

    public static final int TYPE_DATABASE_NATIVE = 1;
    /* Keep singleton instances in a map, so that multiple db implementations can be handled */
    private static SparseArray<IDatabase> dbMap = new SparseArray<IDatabase>();
    private static final Logger logger = new Logger(DatabaseFactory.class.getName());

    /**
     * Returns singleton instance of the {@link IDatabase} for the given type.
     * The only supported type is TYPE_DATABASE_NATIVE.
     *
     * @throws IllegalArgumentException if the type is invalid.
     */
    public static IDatabase getInstance(int type) {
        return getInstance(type, MainApplication.instance());
    }

    public static IDatabase getInstance(int type, Context context) {
        IDatabase db;

        if (type == TYPE_DATABASE_NATIVE) {
            // manage singleton object
            if (dbMap.get(type) == null) {
                db = EntryPointAccessors
                        .fromApplication(context, EdxDefaultModule.ProviderEntryPoint.class).getIDatabase();
                dbMap.put(type, db);
                logger.debug("Database object created");
            }
            db = dbMap.get(type);

            return db;
        }

        throw new IllegalArgumentException("Database type " + type +
                " is not supported");
    }

}
