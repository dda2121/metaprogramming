package sk.tuke.meta.persistence.util;

import sk.tuke.meta.persistence.exception.MissedIdException;

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

    public static String castToString(Object obj) throws MissedIdException, NoSuchFieldException, IllegalAccessException {
        if (obj == null) {
            return null;
        }
        Class cls = obj.getClass();
        if (cls.equals(Integer.class) || cls.equals(Long.class) || cls.equals(Boolean.class)
                || cls.equals(String.class) || cls.equals(Double.class) || cls.equals(Float.class)
                || cls.equals(Character.class)) {
            return obj.toString();
        }
        String id = SQLUtil.getObjectIdValue(obj);
        // TODO add better validation
        if (id == null || id.equals("0")) {
            throw new MissedIdException("Object with type " + getClassNameWithoutPackage(cls) +
                    " has empty id field.");
        }
        return id;
    }
}
