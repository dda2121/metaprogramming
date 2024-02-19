package sk.tuke.meta.persistence.util;

public class SQLUtil {

    public static String typeToSQL(Class c) {
        if (c.equals(long.class) || c.equals(int.class)) {
            return "INT";
        } else if (c.equals(double.class) || c.equals(float.class)) {
            return "DOUBLE";
        } else if (c.equals(char.class)) {
            return "CHAR";
        } else if (c.equals(String.class)) {
            return "VARCHAR(255)";
        } else if (c.equals(boolean.class)) {
            return "BOOLEAN";
        } else {
            return "INT";
        }
    }
}
