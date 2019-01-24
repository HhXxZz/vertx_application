package com.hxz.vertx.verticle;

import com.hxz.vertx.base.BaseDatabate;
import com.hxz.vertx.base.BaseRouter;
import com.hxz.vertx.route.Video;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;

public class ServerVerticle extends AbstractVerticle {

    private JsonObject config = null;


    @Override
    public void start() throws Exception {

        Router router = Router.router(vertx);
        BaseRouter baseRouter = new BaseRouter(router,vertx);
        baseRouter.register(Video.class);



        vertx.fileSystem().readFile("config.json",bufferAsyncResult -> {
            if (bufferAsyncResult.succeeded()) {

                config = new JsonObject(bufferAsyncResult.result().toString());

                System.out.println(config.toString());
                int port = config.getJsonObject("server").getInteger("port");
                System.out.println(port);


                System.out.println(config.getJsonObject("db"));
                BaseDatabate.initClient(vertx,config.getJsonObject("db"));

                vertx.createHttpServer()
                        .requestHandler(router)
                        .listen(port);
            }
        });



    }


    @Override
    public void stop() throws Exception {
        super.stop();

        BaseDatabate.close();

    }
}
