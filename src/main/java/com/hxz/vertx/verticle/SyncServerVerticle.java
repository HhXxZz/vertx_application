package com.hxz.vertx.verticle;

import co.paralleluniverse.fibers.Suspendable;
import com.hxz.vertx.base.BaseDatabate;
import com.hxz.vertx.base.BaseRouter;
import com.hxz.vertx.route.Video;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sync.Sync;
import io.vertx.ext.sync.SyncVerticle;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.vertx.ext.sync.Sync.awaitResult;
import static io.vertx.ext.sync.Sync.fiberHandler;

public class SyncServerVerticle extends SyncVerticle {

    private static final Logger logger = LoggerFactory.getLogger(SyncServerVerticle.class);

    private JsonObject config = null;

    @Suspendable
    @Override
    public void start() {
        Router router = Router.router(vertx);
        BaseRouter baseRouter = new BaseRouter(router,vertx);
        baseRouter.register(Video.class);

        //Sync.

//        vertx.fileSystem().readFile("config.json",Sync.awaitEvent(b->{
//
//        }));


//        System.out.println(config.encode());
//        AsyncSQLClient mySQLClient = MySQLClient.createShared(vertx, config);
//        SQLConnection conn = awaitResult(mySQLClient::getConnection);
//        String sql = "select * from video_info WHERE 1=1 LIMIT 100";
//        ResultSet res = awaitResult(h -> conn.query(sql, h));
//        conn.close();
//        logger.info(res.toJson().getJsonArray("results").encode());

        AsyncResult configStr = awaitResult(bufferAsyncResult -> vertx.fileSystem().readFile("config.json",result->{
            //System.out.println("aaaa2:start"+Thread.currentThread().getName());

            if(result.succeeded()){
                config = new JsonObject(result.result().toString());

                System.out.println(config.toString());
                int port = config.getJsonObject("server").getInteger("port");
                System.out.println(port);

                System.out.println(config.getJsonObject("db"));
                BaseDatabate.initClient(vertx,config.getJsonObject("db"));

                vertx.createHttpServer()
                        .requestHandler(router)
                        .listen(port);
            }

        }));
//
//        System.out.println("==============="+configStr.toString());

//        vertx.fileSystem().readFile("config.json",bufferAsyncResult -> {
//            if (bufferAsyncResult.succeeded()) {
//
//                config = new JsonObject(bufferAsyncResult.result().toString());
//
//                System.out.println(config.toString());
//                int port = config.getJsonObject("server").getInteger("port");
//                System.out.println(port);
//
//
//                System.out.println(config.getJsonObject("db"));
//                BaseDatabate.initClient(vertx,config.getJsonObject("db"));
//
//                vertx.createHttpServer()
//                        .requestHandler(router)
//                        .listen(port);
//            }
//        });



    }


    @Override
    public void stop() throws Exception {
        super.stop();

        //BaseDatabate.close();

    }
}
