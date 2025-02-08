/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cloudbeaver.service.security;

import io.cloudbeaver.auth.NoAuthCredentialsProvider;
import io.cloudbeaver.license.core.CustomLicenseManager;
import io.cloudbeaver.license.core.LicenseInstall;
import io.cloudbeaver.model.app.ServletAuthApplication;
import io.cloudbeaver.model.config.SMControllerConfiguration;
import io.cloudbeaver.model.config.WebDatabaseConfig;
import io.cloudbeaver.service.security.db.CBDatabase;
import io.cloudbeaver.service.security.internal.ClearAuthAttemptInfoJob;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.auth.SMCredentialsProvider;

/**
 * Embedded Security Controller Factory
 */
public class EmbeddedSecurityControllerFactory<T extends ServletAuthApplication> {
    private static volatile CBDatabase DB_INSTANCE;
    private static volatile CBDatabase USER_INSTANCE;
    private static volatile CBDatabase DB_DB_INSTANCE;
    private static volatile CBDatabase EDITOR_DB_INSTANCE;
    public static CBDatabase getDbInstance() {
        return DB_INSTANCE;
    }
    //新增
    public static CBDatabase getUserInstance() {
        return USER_INSTANCE;
    }
    //新增
    public static CBDatabase getDbDbInstance() {
        return DB_DB_INSTANCE;
    }

    public static CBDatabase getEditorDbInstance() {
        return EDITOR_DB_INSTANCE;
    }
    private static final Log log = Log.getLog(EmbeddedSecurityControllerFactory.class);
    /**
     * Create new security controller instance with custom configuration
     */
    public CBEmbeddedSecurityController<T> createSecurityService(
        T application,
        WebDatabaseConfig databaseConfig,
        SMCredentialsProvider credentialsProvider,
        SMControllerConfiguration smConfig
    ) throws DBException {
        if (DB_INSTANCE == null) {
            synchronized (EmbeddedSecurityControllerFactory.class) {
                if (DB_INSTANCE == null) {
                    DB_INSTANCE = createAndInitDatabaseInstance(
                        application,
                        databaseConfig,
                        smConfig
                    );
                }
            }

            if (application.isLicenseRequired()) {
                // delete expired auth info job in enterprise products
                new ClearAuthAttemptInfoJob(createEmbeddedSecurityController(
                    application, DB_INSTANCE, new NoAuthCredentialsProvider(), smConfig
                )).schedule();
            }
        }
        return createEmbeddedSecurityController(
            application, DB_INSTANCE, credentialsProvider, smConfig
        );
    }
    public CBEmbeddedSecurityController<T> createSecurityServiceNew(
            T application,
            WebDatabaseConfig databaseConfig,
            WebDatabaseConfig userdatabaseConfig,
            WebDatabaseConfig dbdatabaseConfig,
            WebDatabaseConfig editordatabaseConfig,
            SMCredentialsProvider credentialsProvider,
            SMControllerConfiguration smConfig
    ) throws DBException {

        LicenseInstall licenseInstall = new LicenseInstall();
        try {
            licenseInstall.init();
        } catch (Exception e){
            e.printStackTrace();
        }

        if (DB_INSTANCE == null) {
            log.info("系统数据库初始化完成");
            synchronized (EmbeddedSecurityControllerFactory.class) {
                if (DB_INSTANCE == null) {
                    DB_INSTANCE = createAndInitDatabaseInstance(
                            application,
                            databaseConfig,
                            smConfig
                    );
                }
            }

            if (application.isLicenseRequired()) {
                // delete expired auth info job in enterprise products
                new ClearAuthAttemptInfoJob(createEmbeddedSecurityController(
                        application, DB_INSTANCE, new NoAuthCredentialsProvider(), smConfig
                )).schedule();
            }
        }

        // 用户数据源
        if (USER_INSTANCE == null) {
            log.info("用户数据库初始化完成");
            synchronized (EmbeddedSecurityControllerFactory.class) {
                if (USER_INSTANCE == null) {
                    USER_INSTANCE = createAndInitDatabaseInstanceNew(
                            application,
                            userdatabaseConfig,
                            smConfig
                    );
                }
                try {
                    log.info("USER_INSTANCE.openConnection().getMetaData().getDatabaseProductName():"+USER_INSTANCE.openConnection().getMetaData().getDatabaseProductName());
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }

        // 数据库数据源
        if (DB_DB_INSTANCE == null) {
            log.info("datasource数据库初始化完成");
            synchronized (EmbeddedSecurityControllerFactory.class) {
                if (DB_DB_INSTANCE == null) {
                    DB_DB_INSTANCE = createAndInitDatabaseInstanceNew(
                            application,
                            dbdatabaseConfig,
                            smConfig
                    );
                }
            }
        }

        // EDITOR数据库数据源
        if (EDITOR_DB_INSTANCE == null) {
            log.info("开发中心数据库初始化完成");
            synchronized (EmbeddedSecurityControllerFactory.class) {
                if (EDITOR_DB_INSTANCE == null) {
                    EDITOR_DB_INSTANCE = createAndInitDatabaseInstanceNew(
                            application,
                            editordatabaseConfig,
                            smConfig
                    );
                }
            }
        }

        return createEmbeddedSecurityController(
                application, DB_INSTANCE, credentialsProvider, smConfig
        );
    }
    protected @NotNull CBDatabase createAndInitDatabaseInstance(
        @NotNull T application,
        @NotNull WebDatabaseConfig databaseConfig,
        @NotNull SMControllerConfiguration smConfig
    ) throws DBException {
        var database = new CBDatabase(application, databaseConfig);
        var securityController = createEmbeddedSecurityController(
            application, database, new NoAuthCredentialsProvider(), smConfig
        );
        //FIXME circular dependency
        database.setAdminSecurityController(securityController);
        try {
            database.initialize();
        } catch (DBException e) {
            database.shutdown();
            throw e;
        }

        return database;
    }

    protected @NotNull CBDatabase createAndInitDatabaseInstanceNew(
            @NotNull T application,
            @NotNull WebDatabaseConfig databaseConfig,
            @NotNull SMControllerConfiguration smConfig
    ) throws DBException {
        log.info(":" + databaseConfig.getUrl());
        var database = new CBDatabase(application, databaseConfig);
//        var securityController = createEmbeddedSecurityController(
//                application, database, new NoAuthCredentialsProvider(), smConfig
//        );
        //FIXME circular dependency
//        database.setAdminSecurityController(securityController);
        try {
            database.initializeDB();
        } catch (DBException e) {
            database.shutdown();
            throw e;
        }

        return database;
    }

    protected CBEmbeddedSecurityController<T> createEmbeddedSecurityController(
        T application,
        CBDatabase database,
        SMCredentialsProvider credentialsProvider,
        SMControllerConfiguration smConfig
    ) {
        return new CBEmbeddedSecurityController<T>(application, database, credentialsProvider, smConfig);
    }
}
