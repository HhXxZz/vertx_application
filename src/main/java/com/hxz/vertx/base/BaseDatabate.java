package com.hxz.vertx.base;

import io.vertx.core.json.JsonObject;
import io.vertx.core.Vertx;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;

public class BaseDatabate {

    private static AsyncSQLClient mySQLClient;

    public static void initClient(Vertx vertx, JsonObject config){

        mySQLClient = MySQLClient.createShared(vertx, config);
    }

    public static AsyncSQLClient getJDBCClient(){
        return mySQLClient;
    }

    public static void close(){
        if(mySQLClient != null){
            mySQLClient.close();
        }
    }


}
