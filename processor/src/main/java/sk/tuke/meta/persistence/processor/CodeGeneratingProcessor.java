package sk.tuke.meta.persistence.processor;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import sk.tuke.meta.persistence.annotations.Table;
import sk.tuke.meta.persistence.processor.model.TableClass;
import sk.tuke.meta.persistence.processor.service.DaoGenerationService;
import sk.tuke.meta.persistence.processor.service.PersistenceManagerGenerationService;
import sk.tuke.meta.persistence.processor.service.TableScriptGenerationService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_19)
@SupportedAnnotationTypes("sk.tuke.meta.persistence.annotations.*")
public class CodeGeneratingProcessor extends AbstractProcessor {
    private VelocityEngine velocity;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        velocity = new VelocityEngine();
        velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        velocity.init();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> tableClasses = roundEnv.getElementsAnnotatedWith(Table.class);
        List<TableClass> classes = new ArrayList<>();
        for (Element tableClass : tableClasses) {
            try {
                DaoGenerationService daoGenerationService = new DaoGenerationService(processingEnv, velocity);
                daoGenerationService.generateDao(tableClass);
                classes.add(new TableClass(tableClass.getSimpleName().toString(),
                        tableClass.getEnclosingElement().toString()));
            } catch (IOException e) {
                System.err.println("Error: " + e);
            }
        }

        if (!classes.isEmpty()) {
            try {
                PersistenceManagerGenerationService persistenceManagerGenerationService =
                        new PersistenceManagerGenerationService(processingEnv, velocity);
                persistenceManagerGenerationService.generatePersistenceManager(classes);

                TableScriptGenerationService tableScriptGenerationService =
                        new TableScriptGenerationService(processingEnv, velocity);
                tableScriptGenerationService.generateScripts(tableClasses);

            } catch (IOException e) {
                System.err.println("Error: " + e);
            }
        }

        return false;
    }
}
