package com.hxz.vertx;

import com.hxz.vertx.verticle.ServerVerticle;
import com.hxz.vertx.verticle.SyncServerVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;


public class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String [] agr){
       // Vertx vertx = Vertx.vertx();

       // vertx.deployVerticle(new SyncServerVerticle());


        Consumer<Vertx> runner = vertx -> {
            try {
                //vertx.deployVerticle(SyncServerVerticle.class.getName());

                vertx.deployVerticle(ServerVerticle.class.getName());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        };
        Vertx vertx = Vertx.vertx();
        runner.accept(vertx);
    }


}
