package com.windows11.files.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClipboardManager {
    private static final String PREF_NAME = "w11_clipboard";
    private static final String KEY_OPERATION = "operation";
    private static final String KEY_FILES = "files";
    private static final String OPERATION_NONE = "none";
    private static final String OPERATION_COPY = "copy";
    private static final String OPERATION_CUT = "cut";
    
    private SharedPreferences preferences;
    private List<String> currentFiles;
    private String currentOperation;
    private static ClipboardManager instance;
    
    public static synchronized ClipboardManager getInstance(Context context) {
        if (instance == null) {
            instance = new ClipboardManager(context);
        }
        return instance;
    }
    
    private ClipboardManager(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.currentFiles = new ArrayList<>();
        this.currentOperation = OPERATION_NONE;
    }
    
    public void copy(List<String> files) {
        this.currentFiles = new ArrayList<>(files);
        this.currentOperation = OPERATION_COPY;
    }
    
    public void cut(List<String> files) {
        this.currentFiles = new ArrayList<>(files);
        this.currentOperation = OPERATION_CUT;
    }
    
    public void paste(String targetDirectory) throws IOException {
        if (currentFiles.isEmpty() || currentOperation.equals(OPERATION_NONE)) {
            return;
        }
        
        File targetDir = new File(targetDirectory);
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            throw new IOException("Target directory does not exist");
        }
        
        for (String filePath : currentFiles) {
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                continue;
            }
            
            File destFile = new File(targetDir, sourceFile.getName());
            
            if (currentOperation.equals(OPERATION_COPY)) {
                if (sourceFile.isDirectory()) {
                    copyDirectory(sourceFile, destFile);
                } else {
                    copyFile(sourceFile, destFile);
                }
            } else if (currentOperation.equals(OPERATION_CUT)) {
                if (sourceFile.isDirectory()) {
                    copyDirectory(sourceFile, destFile);
                    deleteDirectory(sourceFile);
                } else {
                    copyFile(sourceFile, destFile);
                    sourceFile.delete();
                }
            }
        }
        
        if (currentOperation.equals(OPERATION_CUT)) {
            clear();
        }
    }
    
    private void copyFile(File source, File dest) throws IOException {
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
    
    private void copyDirectory(File sourceDir, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File newFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, newFile);
                } else {
                    copyFile(file, newFile);
                }
            }
        }
    }
    
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    
    public List<String> getFiles() {
        return new ArrayList<>(currentFiles);
    }
    
    public String getOperation() {
        return currentOperation;
    }
    
    public boolean canPaste() {
        return !currentFiles.isEmpty() && !currentOperation.equals(OPERATION_NONE);
    }
    
    public void clear() {
        currentFiles.clear();
        currentOperation = OPERATION_NONE;
    }
}
