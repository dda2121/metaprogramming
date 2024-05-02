package sk.tuke.meta.persistence.processor.model;

import java.util.List;

public record TableData(String tableName, List<TableField> fields) {
}
