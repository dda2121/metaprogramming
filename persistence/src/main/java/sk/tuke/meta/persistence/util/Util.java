package sk.tuke.meta.persistence.util;

import sk.tuke.meta.persistence.model.Property;
import sk.tuke.meta.persistence.ReflectivePersistenceManager;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.exception.MissedIdException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public static Object castToObject(Object obj) throws MissedIdException, IllegalAccessException {
        if (obj == null) {
            return null;
        }
        Class cls = obj.getClass();
        if (cls.equals(Integer.class) || cls.equals(Long.class) || cls.equals(Boolean.class)
                || cls.equals(String.class) || cls.equals(Double.class) || cls.equals(Float.class)
                || cls.equals(Character.class)) {
            return obj;
        }
        Long id = getObjectIdValue(obj);
        if (id == 0) {
            throw new MissedIdException("Object with type " + getClassNameWithoutPackage(cls) +
                    " has empty id field.");
        }
        return id;
    }

    public static Object convertDataType(Class<?> type, String value, Connection connection) {
        if (value == null) {
            if (type.isPrimitive()) {
                return Array.get(Array.newInstance(type, 1), 0);
            } else {
                return null;
            }
        } else if (type.equals(String.class)) {
            return value;
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            return Integer.parseInt(value);
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return Double.parseDouble(value);
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return Float.parseFloat(value);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return Long.parseLong(value);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        } else {
            ReflectivePersistenceManager manager = new ReflectivePersistenceManager(connection);
            Optional<?> optional = manager.get(type, Long.parseLong(value));
            return optional.orElse(null);
        }
    }

    public static <T> String getTableName(Class<T> cls) {
        Table tableAnnotation = cls.getAnnotation(Table.class);
        return tableAnnotation.name().isEmpty() ? getClassNameWithoutPackage(cls) :
                tableAnnotation.name();
    }

    public static boolean containsPrimaryKeyField(List<Property> values) {
        for (Property property: values) {
            if (property.isPrimaryKey()) {
                return true;
            }
        }
        return false;
    }

    public static Object getPrimaryKeyValue(List<Property> values) {
        for (Property property: values) {
            if (property.isPrimaryKey()) {
                return property.value();
            }
        }
        return null;
    }

    public static String getPrimaryKeyName(List<Property> values) {
        for (Property property: values) {
            if (property.isPrimaryKey()) {
                return property.name();
            }
        }
        return "";
    }

    public static Field getFieldAnnotatedWith(Object entity, Class<? extends Annotation> annotation) {
        for (Field field: entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(annotation)) {
                return field;
            }
        }
        return null;
    }

    public static Long getObjectIdValue(Object obj) throws IllegalAccessException {
        Field idField = Objects.requireNonNull(getFieldAnnotatedWith(obj, Id.class));
        idField.setAccessible(true);
        return (Long) idField.get(obj);
    }
}
