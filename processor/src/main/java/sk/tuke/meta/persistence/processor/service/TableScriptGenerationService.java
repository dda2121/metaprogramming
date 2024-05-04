package sk.tuke.meta.persistence.processor.service;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.processor.model.TableData;
import sk.tuke.meta.persistence.processor.model.TableField;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static sk.tuke.meta.persistence.processor.util.UtilService.*;

public class TableScriptGenerationService extends GenerationService {
    public TableScriptGenerationService(ProcessingEnvironment processingEnv, VelocityEngine velocity) {
        super(processingEnv, velocity);
    }

    public void generateScripts(Set<? extends Element> classes) throws IOException {
        for (Element table: classes) {
            FileObject fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", table.getSimpleName().toString().toLowerCase() + ".sql");
            try (var out = fileObject.openWriter()) {
                var template = velocity.getTemplate(TEMPLATE_PATH + "create-table-script.sql.vm");
                var context = new VelocityContext();
                String tableName = parseTableName(table);
                List<TableField> tableFields = parseTableField(table);
                TableData tableData = new TableData(tableName, tableFields);
                System.out.println(tableData);
                context.put("tableData", tableData);
                template.merge(context, out);
            }
        }
    }

    private List<TableField> parseTableField(Element table) {
        List<TableField> tableFields = new ArrayList<>();
        for (Element e: table.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD &&
                    e.getAnnotation(Column.class) != null) {
                Column columnAnnotation = e.getAnnotation(Column.class);
                String columnAnnotationName = columnAnnotation.name();
                String columnName = columnAnnotationName.isEmpty() ? e.toString() : columnAnnotationName;
                String columnType = typeToSQL(e.asType());
                List<String> additionalInfo = new ArrayList<>();
                if (e.getAnnotation(Id.class) != null) {
                    additionalInfo.add("PRIMARY KEY AUTOINCREMENT");
                }
                if (columnAnnotation.unique()) {
                    additionalInfo.add("UNIQUE");
                }
                if (!columnAnnotation.nullable()) {
                    additionalInfo.add("NOT NULL");
                }
                TableField tableField = new TableField(columnName, columnType, additionalInfo);
                tableFields.add(tableField);
            }
        }
        return tableFields;
    }
}
