package sk.tuke.meta.persistence.processor.model;

import java.util.List;

public record TableField(String name, String type, List<String> additionalInfo){}
