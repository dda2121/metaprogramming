package sk.tuke.meta.persistence.processor.model;

public record EntityFieldProperty(String columnName, String fieldName, boolean isPrimaryKey, String typeName) {}
