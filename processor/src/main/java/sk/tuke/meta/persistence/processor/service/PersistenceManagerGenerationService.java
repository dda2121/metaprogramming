package sk.tuke.meta.persistence.processor.service;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import sk.tuke.meta.persistence.processor.model.TableClass;

import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;
import java.util.List;

public class PersistenceManagerGenerationService extends GenerationService {

    public PersistenceManagerGenerationService(ProcessingEnvironment processingEnv, VelocityEngine velocity) {
        super(processingEnv, velocity);
    }

    public void generatePersistenceManager(List<TableClass> classes) throws IOException {
        var javaFile = processingEnv.getFiler().createSourceFile(
                "sk.tuke.meta.persistence.GeneratedPersistenceManager");
        try (var out = javaFile.openWriter()) {
            var template = velocity.getTemplate(TEMPLATE_PATH + "manager.java.vm");
            var context = new VelocityContext();
            context.put("classes", classes);
            template.merge(context, out);
        }
    }
}
