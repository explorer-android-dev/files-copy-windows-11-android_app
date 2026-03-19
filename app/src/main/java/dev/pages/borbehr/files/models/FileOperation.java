package dev.pages.borbehr.files.models;

import java.io.File;

public class FileOperation {
    public enum Type {
        NONE, COPY, CUT, DELETE, RENAME, NEW_FOLDER
    }
    
    private Type type;
    private File sourceFile;
    private String newName;
    
    public FileOperation(Type type, File sourceFile) {
        this.type = type;
        this.sourceFile = sourceFile;
    }
    
    public FileOperation(Type type, File sourceFile, String newName) {
        this.type = type;
        this.sourceFile = sourceFile;
        this.newName = newName;
    }
    
    public Type getType() {
        return type;
    }
    
    public File getSourceFile() {
        return sourceFile;
    }
    
    public String getNewName() {
        return newName;
    }
}
