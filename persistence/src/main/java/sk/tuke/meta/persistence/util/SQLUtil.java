package sk.tuke.meta.persistence.util;

import java.lang.reflect.Field;

public class SQLUtil {

    public static String typeToSQL(Class c) {
        if (c.equals(long.class) || c.equals(int.class)) {
            return "INTEGER";
        } else if (c.equals(double.class) || c.equals(float.class)) {
            return "REAL";
        } else if (c.equals(char.class)) {
            return "CHAR";
        } else if (c.equals(String.class)) {
            return "TEXT";
        } else if (c.equals(boolean.class)) {
            return "BOOLEAN";
        } else {
            return "INTEGER";
        }
    }

    public static Long getObjectIdValue(Object obj) throws NoSuchFieldException, IllegalAccessException {
        Class cls = obj.getClass();
        Field idField = cls.getDeclaredField("id");
        idField.setAccessible(true);
        return (Long) idField.get(obj);
    }
}
