package sk.tuke.meta.persistence.processor;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_19)
@SupportedAnnotationTypes("sk.tuke.meta.persistence.annotations.*")
public class ValidationProcessor extends AbstractProcessor {

    private ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Table.class);

            List<String> scripts = new ArrayList<>();
            for (Element element : elements) {
                String script = getScript(element);
                scripts.add(script);
            }
            System.out.println(scripts);
            writeTableScriptsToFile(scripts);
            return true;
        }
        return false;
    }

    private String getScript(Element element) {
        String query = "";
        if (element instanceof TypeElement typeElement) {
            String tableAnnotationName = element.getAnnotation(Table.class).name();
            String elementName = tableAnnotationName.isEmpty() ? element.getSimpleName().toString() : tableAnnotationName;
            System.out.println(elementName);

            query = "CREATE TABLE IF NOT EXISTS [" + elementName + "]" + getTableScript(element.getEnclosedElements());
        }
        return query;
    }

    private String getTableScript(List<? extends Element> enclosedElements) {
        StringBuilder columns = new StringBuilder("(");
        for (Element e : enclosedElements) {
            if (e.getKind() == ElementKind.FIELD &&
                    e.getAnnotation(Column.class) != null) {
                Column columnAnnotation = e.getAnnotation(Column.class);
                String columnAnnotationName = columnAnnotation.name();
                String columnName = columnAnnotationName.isEmpty() ? e.toString() : columnAnnotationName;
                columns.append("[").append(columnName).append("] ");
                columns.append(typeToSQL(e.asType()));
                if (e.getAnnotation(Id.class) != null) {
                    columns.append(" AUTO_INCREMENT PRIMARY KEY");
                }
                if (!columnAnnotation.nullable()) {
                    columns.append(" NOT NULL");
                }
                if (columnAnnotation.unique()) {
                    columns.append(" UNIQUE");
                }
                columns.append(",");
            }
        }
        if (columns.toString().endsWith(",")) {
            columns = new StringBuilder(columns.substring(0, columns.length() - 1));
        }
        columns.append(");");
        return columns.toString();
    }

    private static String typeToSQL(TypeMirror typeMirror) {
        return switch (typeMirror.toString()) {
            case "double", "float" -> "REAL";
            case "char" -> "CHAR";
            case "java.lang.String" -> "TEXT";
            case "boolean" -> "BOOLEAN";
            default -> "INTEGER";
        };
    }

    private void writeTableScriptsToFile(List<String> tableScripts) {
        try {
            FileObject fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "script.sql");

            // Write data to the file
            try (OutputStream outputStream = fileObject.openOutputStream();
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                for (String script: tableScripts) {
                    writer.write(script);
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("Error occurred while writing to the file: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Error occurred while writing to the file: " + e.getMessage());
        }
    }
}
