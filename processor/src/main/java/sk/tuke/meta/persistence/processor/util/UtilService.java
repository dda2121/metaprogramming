package sk.tuke.meta.persistence.processor.util;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.processor.model.EntityFieldProperty;
import sk.tuke.meta.persistence.processor.model.FieldProperty;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

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

    public static String parseIdColumnSetter(Element tableClass) {
        for (Element e : tableClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD &&
                    e.getAnnotation(Id.class) != null) {
                return "set" + capitalizeFirstLetter(e.getSimpleName().toString());
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

    public static List<FieldProperty> getColumnFieldsWithSetters(Element element) {
        List<FieldProperty> fields = new ArrayList<>();
        for (Element e : element.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD) {
                Column columnAnnotation = e.getAnnotation(Column.class);
                if (columnAnnotation == null) {
                    if (e.getAnnotation(Id.class) == null) {
                        continue;
                    }
                    String simpleName = e.asType().toString();
                    simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1);
                    fields.add(new FieldProperty(e.getSimpleName().toString(),
                            capitalizeFirstLetter(e.getSimpleName().toString()),
                            simpleName,
                            false,
                            null,
                            ""));
                } else {
                    String simpleName = e.asType().toString();
                    boolean lazyFetch = false;
                    String target = null;
                    String idFieldName;
                    if (columnAnnotation.lazyFetch()) {
                        lazyFetch = true;
                        target = getTarget(columnAnnotation);
                        idFieldName = getTargetIdFieldName(columnAnnotation);
                    } else {
                        idFieldName = getIdFieldName(e.asType());
                    }
                    fields.add(new FieldProperty(columnAnnotation.name().isEmpty() ? e.getSimpleName().toString() : columnAnnotation.name(),
                            capitalizeFirstLetter(e.getSimpleName().toString()),
                            simpleName,
                            lazyFetch,
                            target,
                            idFieldName));
                }
            }
        }
        return fields;
    }

    public static String getIdFieldName(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType declaredType) {
            Element el = declaredType.asElement();
            for (Element t : el.getEnclosedElements()) {
                if (t.getKind() == ElementKind.FIELD && t.getAnnotation(Id.class) != null) {
                    return capitalizeFirstLetter(t.getSimpleName().toString());
                }
            }
        }
        return "";
    }

    public static String getTargetIdFieldName(Column column) {
        try {
            return column.targetClass().getName();
        } catch (MirroredTypeException e) {
            TypeMirror typeMirror = e.getTypeMirror();
            if (typeMirror instanceof DeclaredType declaredType) {
                Element el = declaredType.asElement();
                for (Element t : el.getEnclosedElements()) {
                    if (t.getKind() == ElementKind.FIELD && t.getAnnotation(Id.class) != null) {
                        return capitalizeFirstLetter(t.getSimpleName().toString());
                    }
                }
            }
            return null;
        }
    }

    public static List<EntityFieldProperty> getEntityFields(Element element) {
        List<EntityFieldProperty> fields = new ArrayList<>();
        for (Element e : element.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD && e.getAnnotation(Column.class) != null) {
                Column columnAnnotation = e.getAnnotation(Column.class);
                String columnName = columnAnnotation.name().isEmpty() ? e.getSimpleName().toString() : columnAnnotation.name();
                String fieldName = capitalizeFirstLetter(e.getSimpleName().toString());
                boolean isPrimaryKey = e.getAnnotation(Id.class) != null;
                String typeName = e.asType().toString();
                fields.add(new EntityFieldProperty(columnName, fieldName, isPrimaryKey, typeName));
            }
        }
        return fields;
    }

    private static String getTarget(Column column) {
        try {
            return column.targetClass().getName();
        } catch (MirroredTypeException e) {
            TypeMirror typeMirror = e.getTypeMirror();
            return typeMirror.toString();
        }
    }
}
