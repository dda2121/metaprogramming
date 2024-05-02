package sk.tuke.meta.persistence.processor.util;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;

public class UtilService {

    public static String parseIdColumnName(Element tableClass) {
        for (Element e : tableClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD &&
                    e.getAnnotation(Id.class) != null) {
                Column columnAnnotation = e.getAnnotation(Column.class);
                if (columnAnnotation == null) {
                    return e.getSimpleName().toString();
                } else {
                    return columnAnnotation.name().isEmpty() ? e.getSimpleName().toString() : columnAnnotation.name();
                }
            }
        }
        return "";
    }

    public static String parseIdColumnGetter(Element tableClass) {
        for (Element e : tableClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD &&
                    e.getAnnotation(Id.class) != null) {
                return "get" + capitalizeFirstLetter(e.getSimpleName().toString());
            }
        }
        return "";
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        } else {
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
    }

    public static String parseTableName(Element tableClass) {
        Table tableAnnotation = tableClass.getAnnotation(Table.class);
        return tableAnnotation.name().isEmpty() ? tableClass.getSimpleName().toString() : tableAnnotation.name();
    }

    public static String typeToSQL(TypeMirror typeMirror) {
        return switch (typeMirror.toString()) {
            case "double", "float" -> "REAL";
            case "char" -> "CHAR";
            case "java.lang.String" -> "TEXT";
            case "boolean" -> "BOOLEAN";
            default -> "INTEGER";
        };
    }
}
