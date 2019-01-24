import com.hxz.vertx.route.Video;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public class AnnotationTest {

    public static void main(String [] a){
        Class clazz = Video.class;

        Path path = (Path) clazz.getAnnotation(Path.class);

        System.out.println("class.path:"+path.value());

        for (Method method : clazz.getMethods()) {
            Class mt = method.getDeclaringClass();
            if ( mt ==  Object.class){
                continue;
            }

            Path pathMethod = method.getAnnotation(Path.class);
            System.out.println("method.path:"+pathMethod.value());
            Produces produces = method.getAnnotation(Produces.class);
            System.out.println("method.product:"+produces.value()[0]);

            Class httpMethod = getHttpMethod(method);
            System.out.println("method.http.method:"+httpMethod.getSimpleName());

            Parameter[] parameters = method.getParameters();
            Class<?>[] parameterTypes = method.getParameterTypes();
            Annotation[][] annotations = method.getParameterAnnotations();

            for (int i=0;i<annotations.length;i++) {
                Annotation [] an = annotations[i];

                System.out.println("method.params:"+parameters[i].getName());

                System.out.println("method.param.type:"+parameterTypes[i].getSimpleName());

                for (Annotation anParam : an){
                    String value;
                    if (anParam instanceof Context){
                        //argInfo.setContext(true);
                        System.out.println("method.param.context:");
                    }else
                    if (anParam instanceof DefaultValue){
                        value = ((DefaultValue) anParam).value();
                        System.out.println("method.param.default:"+value);
                    }else if (anParam instanceof PathParam){
                        //argInfo.setPathParam(true);
                        value = ((PathParam) anParam).value();
                        System.out.println("method.param.path:"+value);
                    }else if (anParam instanceof QueryParam){
                        //argInfo.setQueryParam(true);
                        value = ((QueryParam) anParam).value();
                        System.out.println("method.param.query:"+value);
                    }else if (anParam instanceof FormParam){
                        //argInfo.setFormParam(true);
                        value = ((FormParam) anParam).value();
                        System.out.println("method.param.form:"+value);
                    }
                }
            }
        }
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

}
