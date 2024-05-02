package sk.tuke.meta.persistence.processor.service;

import org.apache.velocity.app.VelocityEngine;

import javax.annotation.processing.ProcessingEnvironment;

public abstract class GenerationService {
    public static final String TEMPLATE_PATH = "sk/tuke/meta/persistence/";
    protected final ProcessingEnvironment processingEnv;
    protected final VelocityEngine velocity;

    protected GenerationService(ProcessingEnvironment processingEnv,
                                VelocityEngine velocity) {
        this.processingEnv = processingEnv;
        this.velocity = velocity;
    }
}
