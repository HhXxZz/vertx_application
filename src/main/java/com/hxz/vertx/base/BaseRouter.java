package com.hxz.vertx.base;

import com.hxz.vertx.annotation.Blocking;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vertx.ext.sync.Sync.fiberHandler;

public class BaseRouter {

    private static final Logger logger = LoggerFactory.getLogger(BaseRouter.class);

    private Router router;
    private Vertx vertx;
    private StringBuffer requestParamSB;

    private static final String DEFAULT_CONTENT_TYPE = MediaType.APPLICATION_JSON+";charset=utf-8";
    private enum ArgInfo{ Context, DefaultValue, PathParam, QueryParam, FormParam, ParamKey}

    public BaseRouter(Router router,Vertx vertx){
        this.router = router;
        this.vertx = vertx;
        init();
    }

    private void init(){
        router.route().handler(BodyHandler.create());
        router.route().handler(CookieHandler.create());
        SessionHandler handler = SessionHandler.create(LocalSessionStore.create(vertx));
        handler.setNagHttps(true);
        router.route().handler(handler);
    }

    /**
     * 注册一个route类
     * @param clazz
     */
    public void register(Class clazz){
        try {
            //获取接口类的path
            String routePath;
            Path classPath = (Path) clazz.getAnnotation(Path.class);
            List<Map<ArgInfo, Object>> paramList;
            Map<ArgInfo, Object> paramMap;

            //实例化一个route
            Route route;
            for (Method method : clazz.getMethods()) {
                route = null;
                Class mt = method.getDeclaringClass();
                if (mt == Object.class) {
                    continue;
                }
                Path pathMethod = method.getAnnotation(Path.class);
                routePath = classPath.value() + pathMethod.value();
                // /{id}/ -> /:id/
                routePath = converter(routePath);

                //把注解的routePath加载到route
                Class httpMethod = getHttpMethod(method);

                if (httpMethod == GET.class) {
                    route = router.get(routePath);
                } else if (httpMethod == POST.class) {
                    route = router.post(routePath);
                } else if (httpMethod == PUT.class) {
                    route = router.put(routePath);
                } else if (httpMethod == DELETE.class) {
                    route = router.delete(routePath);
                } else if (httpMethod == OPTIONS.class) {
                    route = router.options(routePath);
                } else if (httpMethod == HEAD.class) {
                    route = router.head(routePath);
                } else {
                    route = router.route(routePath);
                }

                Produces produces = method.getAnnotation(Produces.class);

                Parameter[] parameters = method.getParameters();
                Class<?>[] parameterTypes = method.getParameterTypes();
                Annotation[][] paramAn = method.getParameterAnnotations();

                Blocking blocking = method.getAnnotation(Blocking.class);

                int paramSize = paramAn.length;
                paramList = new ArrayList<>();

                for (int i = 0; i < paramSize; i++) {
                    paramMap = new HashMap<>();
                    Annotation[] an = paramAn[i];
                    Class paramClazz = parameterTypes[i];

                    for (Annotation paramKeyAn : an) {
                        String paramKey;
                        if (paramKeyAn instanceof Context) {
                            paramMap.put(ArgInfo.Context, paramClazz);
                        } else if (paramKeyAn instanceof DefaultValue) {
                            paramKey = ((DefaultValue) paramKeyAn).value();
                            paramMap.put(ArgInfo.DefaultValue, paramClazz);
                            paramMap.put(ArgInfo.ParamKey, paramKey);
                        } else if (paramKeyAn instanceof PathParam) {
                            paramKey = ((PathParam) paramKeyAn).value();
                            paramMap.put(ArgInfo.PathParam, paramClazz);
                            paramMap.put(ArgInfo.ParamKey, paramKey);

                        } else if (paramKeyAn instanceof QueryParam) {
                            paramKey = ((QueryParam) paramKeyAn).value();
                            paramMap.put(ArgInfo.QueryParam, paramClazz);
                            paramMap.put(ArgInfo.ParamKey, paramKey);
                        } else if (paramKeyAn instanceof FormParam) {
                            paramKey = ((FormParam) paramKeyAn).value();
                            paramMap.put(ArgInfo.FormParam, paramClazz);
                            paramMap.put(ArgInfo.ParamKey, paramKey);
                        }
                    }
                    paramList.add(paramMap);
                }

                if(blocking == null) {
                    route.handler(fiberHandler(getHandler(produces.value()[0], method, paramList, clazz)));
                }else{
                    route.blockingHandler(getHandler(produces.value()[0], method, paramList, clazz));
                }
            }
        }catch (Exception e){
            logger.error("ROUTER.REGISTER.ERROR",e);
        }
    }

    /**
     *
     * 注解参数解析到vertx request参数里
     * @param routingContext
     * @param paramList
     * @return
     */
    private Object[] getArgs(RoutingContext routingContext,List<Map<ArgInfo,Object>> paramList){
        Object[]args = new Object[paramList.size()];

        requestParamSB = new StringBuffer();

        Map<ArgInfo,Object>map;
        Class clz;
        String paramKey;
        for(int i=0;i<paramList.size();i++){
            map = paramList.get(i);
            clz = null;
            paramKey = null;
            for(ArgInfo argInfo:map.keySet()){
                switch (argInfo){
                    case Context:
                        clz = (Class) map.get(argInfo);
                        if (clz == RoutingContext.class){
                            args[i] = routingContext;
                        }else if (clz == HttpServerRequest.class){
                            args[i] =  routingContext.request();
                        }else if (clz == HttpServerResponse.class){
                            args[i] =  routingContext.response();
                        }else if (clz == Session.class){
                            args[i] =  routingContext.session();
                        }else if (clz == Vertx.class){
                            args[i] =  vertx;
                        }
                        break;
                    case FormParam:
                    case QueryParam:
                    case PathParam:
                    case DefaultValue:
                        clz = (Class) map.get(argInfo);
                        break;
                    case ParamKey:
                        paramKey = (String) map.get(argInfo);
                        break;
                        default:
                            break;
                }
            }
            if(clz != null && paramKey != null){
                String paramValue = routingContext.request().getParam(paramKey);
                try {
                    requestParamSB.append(paramKey).append("=").append(paramValue);
                    if(i != paramList.size() - 1){
                        requestParamSB.append(",");
                    }
                    args[i] = covertType(clz,paramValue);
                } catch (Exception e) {
                    logger.error("REQUEST_PARAM_ERROR",e);
                    args[i] = null;
                }
            }
        }

        return args;
    }



    /**
     * 处理vertx http 结果
     * @param productType 返回头
     * @param method      方法
     * @param paramList   方法参数集合
     * @param clazz       接口类
     * @return
     */
    private Handler<RoutingContext>getHandler(final String productType, Method method, List<Map<ArgInfo,Object>>paramList, Class clazz){
        return routingContext -> {

            try{
                String contentType = productType == null ? DEFAULT_CONTENT_TYPE : productType;
                routingContext.response().putHeader("Content-Type",contentType)
                        .setStatusCode(200);

                logger.info("request.routePath:" + routingContext.request().path());
                logger.info("request.headers:"+routingContext.request().headers());

                Object result = method.invoke(newClass(clazz),getArgs(routingContext,paramList));

                logger.info("request.params:"+requestParamSB.toString());
                if(null != result && Void.class != result.getClass()){
                    //
                    if(!routingContext.response().ended()){
                        //TODO after

                        if(result instanceof String){
                            routingContext.response().end((String) result);
                        }else{
                            routingContext.response()
                                    .putHeader("Content-Type", MediaType.APPLICATION_JSON+";charset=utf-8")
                                    .end(responseData(JsonObject.mapFrom(result).encode()));
                        }
                    }
                }else{
                    //logger.error("REQUEST_RESULT_NULL_ERROR");
                    //Void方法实现
                }
            }catch (Exception e){
                logger.error("REQUEST_ERROR",e);
                JsonObject jsonObject = new JsonObject();
                jsonObject.put("code","1");
                jsonObject.put("msg","REQUEST_ERROR");
                jsonObject.put("desc","请求错误");
                jsonObject.put("data","{}");
                routingContext.response().setStatusCode(500).putHeader("Content-Type", MediaType.APPLICATION_JSON+";charset=utf-8")
                        .end(responseData(jsonObject.encode()));
            }finally {

            }
        };
    }

    private String responseData(String responseStr){
        logger.info("request.response:"+responseStr);
        return responseStr;
    }



    private static Class getHttpMethod(Method method) {
        List<Class<? extends Annotation>> search = Arrays.asList(
                GET.class,
                POST.class,
                PUT.class,
                DELETE.class,
                OPTIONS.class,
                HEAD.class);
        for (Class<? extends Annotation> item: search) {
            if (method.getAnnotation(item) != null) {
                return item;
            }
        }
        return null;
    }


    /**
     * @param type
     * @param v
     * @return
     * @throws Exception
     */
    private Object covertType(Class type,String v) throws Exception{
        String typeName = type.getTypeName();
        if (type == String.class){
            return v;
        }
        if (type == Integer.class || typeName.equals("int")){
            if(v == null){
                return 0;
            }
            return Integer.parseInt(v);
        }
        if (type == Long.class || typeName.equals("long")){
            if(v == null){
                return 0L;
            }
            return Long.parseLong(v);
        }
        if (type == Float.class || typeName.equals("float")){
            if(v == null){
                return 0F;
            }
            return Float.parseFloat(v);
        }
        if (type == Double.class || typeName.equals("double")){
            if(v == null){
                return 0D;
            }
            return Double.parseDouble(v);
        }
        return null;
    }


    /**
     * /{id}/ -> /:id/
     * @param path
     * @return
     */
    public static String converter(String path){
        if (path==null||path.length()==0){
            return path;
        }
        Matcher matcher = Pattern.compile("\\{(.*?)\\}").matcher(path);
        while (matcher.find()){
            String p = matcher.group(0);
            if (p.length()>0){
                p = p.replace("{","").replace("}","");
                path=path.replace(matcher.group(0),":"+p);
            }
        }
        return path;
    }

    /**
     * 把route类实例化成对象
     * @param clazz
     * @return
     */
    private static Object newClass(Class clazz){
        try {
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                c.setAccessible(true);
                if (c.getParameterCount() == 0) {
                    return c.newInstance();
                }
            }
        }catch (Exception e){
            logger.error("newClass.ERROR",e);
        }
        return null;
    }

}
