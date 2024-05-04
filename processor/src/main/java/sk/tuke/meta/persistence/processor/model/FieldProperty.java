package sk.tuke.meta.persistence.processor.model;

public record FieldProperty(String fieldName, String setterName,
                            String typeName, boolean lazyFetch, String target, String idFieldName) {}