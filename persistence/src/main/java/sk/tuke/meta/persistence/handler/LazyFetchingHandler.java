package sk.tuke.meta.persistence.handler;

import sk.tuke.meta.persistence.ReflectivePersistenceManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Optional;

public class LazyFetchingHandler implements InvocationHandler {
    private long targetId;
    private Connection connection;
    private Class<?> target;
    private Object targetObj;

    public LazyFetchingHandler(long targetId, Connection connection, Class<?> target) {
        this.targetId = targetId;
        this.connection = connection;
        this.target = target;
    }

    public static Object perform(Connection connection, Class<?> target, long targetId){
        return Proxy.newProxyInstance(
                target.getClassLoader(),
                target.getInterfaces(),
                new LazyFetchingHandler(targetId, connection, target));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("toString")
                || method.getName().equals("equals")
                || method.getName().equals("hashcode")) return null;
        System.out.println(method.getName());
        if (targetObj == null) {
            ReflectivePersistenceManager manager = new ReflectivePersistenceManager(connection);
            Optional<?> optional = manager.get(target, targetId);
            targetObj = optional.orElse(null);
        }
        return method.invoke(targetObj, args);
    }

    public Class<?> getTarget() {
        return target;
    }

    public Object getTargetObj() {
        return targetObj;
    }
}
