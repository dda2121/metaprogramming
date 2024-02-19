package sk.tuke.meta.persistence.util;

public class Util {

    public static String getClassNameWithoutPackage(Class<?> c) {
        String fullClassName = c.getName();
        int lastDotIndex = fullClassName.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return fullClassName;
        } else {
            return fullClassName.substring(lastDotIndex + 1);
        }
    }
}
