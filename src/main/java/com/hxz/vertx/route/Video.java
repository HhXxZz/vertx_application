package com.hxz.vertx.route;

import co.paralleluniverse.fibers.Suspendable;
import com.hxz.vertx.annotation.Blocking;
import com.hxz.vertx.base.BaseDatabate;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static io.vertx.ext.sync.Sync.awaitResult;


@Path("/video")
public class Video {

    private static final Logger logger = LoggerFactory.getLogger(Video.class);

    static int count = 0;


    @Suspendable
    @GET
    @Path("/videos2")
    @Produces({MediaType.APPLICATION_JSON})
    public JsonObject list2(){
        long mill1 = System.currentTimeMillis();
        AsyncSQLClient client = BaseDatabate.getJDBCClient();
        int random = new Random().nextInt(1680)+1;
        int start = random;
        long mill2 = System.currentTimeMillis();
        logger.info("step1:"+(mill2-mill1));

        SQLConnection conn = awaitResult(client::getConnection);
        String sql = "select * from video_info WHERE 1=1 LIMIT "+start+","+50;
        ResultSet res = awaitResult(h -> conn.query(sql, h));
        long mill3 = System.currentTimeMillis();
        logger.info("step2:"+(mill3-mill1));

        conn.close();
        count ++ ;

        logger.info("返回成功");
        JsonObject resInfo = new JsonObject();
        resInfo.put("errcode",0);
        resInfo.put("start",start);
        //resInfo.put("end",end);
        resInfo.put("size",res.getNumRows());
        resInfo.put("data",res.toJson().getJsonArray("results"));
        long mill4 = System.currentTimeMillis();
        logger.info("step3:"+(mill4-mill1));
        return resInfo;
    }


    @GET
    @Path("/videos")
    @Produces({MediaType.APPLICATION_JSON})
    @Blocking
    public void list( @Context HttpServerResponse response,@QueryParam("name")String name, @FormParam("id")int id){
//        if(routingContext == null){
//            logger.info("routingContext  null===========");
//        }
        //HttpServerResponse response = routingContext.response();
        if(response == null){
           // response = routingContext.response();
            logger.info("HttpServerResponse  null===========");
        }
        AsyncSQLClient client = BaseDatabate.getJDBCClient();


        int random = new Random().nextInt(1680)+1;

        int start = random;
        //int end = random + 20;

//        client.rxGetConnection().flatMap(conn->{
//            String sql = "select * from video_info WHERE 1=1 LIMIT "+start+","+50;
//            logger.info("sql:"+sql);
//            Single<ResultSet>resultSet = conn.rxQuery(sql);
//            return resultSet.doAfterTerminate(conn :: close);
//        }).subscribe(resultSetSingle -> {
//            JsonObject resInfo = new JsonObject();
//            resInfo.put("code",0);
//            resInfo.put("start",start);
//            //resInfo.put("end",end);
//            resInfo.put("size",resultSetSingle.getNumRows());
//            resInfo.put("data",resultSetSingle.toJson().getJsonArray("results"));
//            response.end(resInfo.encode());
//        });



        client.getConnection(res -> {
            if(res.succeeded()){
                logger.info("连接成功");

                SQLConnection connection = res.result();
                String sql = "select * from video_info WHERE 1=1 LIMIT "+start+","+50;
                connection.query(sql,r->{
                    if(r.succeeded()){
                        ResultSet reset = r.result();
                        logger.info("result:"+reset.getNumRows());
                        //logger.info(r.result());

                        JsonObject resInfo = new JsonObject();
                        resInfo.put("errcode",0);
                        resInfo.put("start",start);
                        //resInfo.put("end",end);
                        resInfo.put("size",reset.getNumRows());
                        resInfo.put("data",reset.toJson().getJsonArray("results"));
                        response.end(resInfo.encode());
                        connection.close();
                        count ++ ;
                        logger.debug("request:"+count+",thread:"+Thread.currentThread().getName());
                    }
                });

            }else{
                logger.info("连接失败");
            }
        });
        logger.info("返回成功");

    }



}
