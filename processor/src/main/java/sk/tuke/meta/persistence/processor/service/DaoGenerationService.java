package sk.tuke.meta.persistence.processor.service;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.io.IOException;

import static sk.tuke.meta.persistence.processor.util.UtilService.*;

public class DaoGenerationService extends GenerationService {

    public DaoGenerationService(ProcessingEnvironment processingEnv, VelocityEngine velocity) {
        super(processingEnv, velocity);
    }

    public void generateDao(Element tableClass) throws IOException {
        var javaFile = processingEnv.getFiler().createSourceFile(
                "sk.tuke.meta.persistence.model.dao." + tableClass.getSimpleName() + "DAO" );
        try (var out = javaFile.openWriter()) {
            var template = velocity.getTemplate(TEMPLATE_PATH + "dao.java.vm" );
            var context = new VelocityContext();
            context.put("package", tableClass.getEnclosingElement().toString());
            context.put("tableFields", getColumnFieldsWithSetters(tableClass));
            context.put("entityFields", getEntityFields(tableClass));
            context.put("simplifiedClassName", tableClass.getSimpleName().toString());
            context.put("className", tableClass.toString());
            context.put("tableName", parseTableName(tableClass));
            context.put("idColumnName", parseIdColumnName(tableClass));
            // ask about this
            context.put("idColumnGetter", parseIdColumnGetter(tableClass));
            context.put("idColumnSetter", parseIdColumnSetter(tableClass));
            template.merge(context, out);
        }
    }
}
