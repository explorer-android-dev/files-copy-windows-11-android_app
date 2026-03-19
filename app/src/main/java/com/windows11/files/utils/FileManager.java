package com.windows11.files.utils;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.windows11.files.R;
import com.windows11.files.models.FileItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileManager {
    
    private static final String TAG = "FileManager";
    
    public void openFile(Context context, File file) {
        try {
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            } else {
                uri = Uri.fromFile(file);
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = getMimeType(file);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No application found to open file: " + file.getName());
        }
    }
    
    public void showFileOptions(Context context, FileItem fileItem) {
        String[] options = {"Открыть", "Копировать", "Переместить", "Переименовать", "Удалить", "Свойства"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Опции файла: " + fileItem.getName());
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Открыть
                    openFile(context, new File(fileItem.getPath()));
                    break;
                case 1: // Копировать
                    // TODO: Реализовать копирование
                    break;
                case 2: // Переместить
                    // TODO: Реализовать перемещение
                    break;
                case 3: // Переименовать
                    // TODO: Реализовать переименование
                    break;
                case 4: // Удалить
                    deleteFile(context, fileItem);
                    break;
                case 5: // Свойства
                    showFileProperties(context, fileItem);
                    break;
            }
        });
        builder.show();
    }
    
    private void deleteFile(Context context, FileItem fileItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Подтверждение удаления");
        builder.setMessage("Вы уверены, что хотите удалить " + 
                         (fileItem.isDirectory() ? "папку" : "файл") + " \"" + 
                         fileItem.getName() + "\"?");
        builder.setPositiveButton("Удалить", (dialog, which) -> {
            File file = new File(fileItem.getPath());
            boolean deleted = deleteRecursively(file);
            if (deleted) {
                // TODO: Обновить список файлов
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
    
    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }
    
    private void showFileProperties(Context context, FileItem fileItem) {
        File file = new File(fileItem.getPath());
        StringBuilder properties = new StringBuilder();
        properties.append("Имя: ").append(fileItem.getName()).append("\n");
        properties.append("Путь: ").append(fileItem.getPath()).append("\n");
        properties.append("Тип: ").append(fileItem.isDirectory() ? "Папка" : "Файл").append("\n");
        properties.append("Размер: ").append(fileItem.getFormattedSize()).append("\n");
        
        if (!fileItem.isDirectory()) {
            String extension = getFileExtension(fileItem.getName());
            if (!extension.isEmpty()) {
                properties.append("Расширение: .").append(extension).append("\n");
            }
        }
        
        properties.append("Изменен: ").append(new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                .format(new java.util.Date(fileItem.getLastModified()))).append("\n");
        
        if (fileItem.isDirectory() && file.exists()) {
            File[] children = file.listFiles();
            int fileCount = 0;
            int folderCount = 0;
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        folderCount++;
                    } else {
                        fileCount++;
                    }
                }
            }
            properties.append("Содержимое: ").append(folderCount).append(" папок, ")
                      .append(fileCount).append(" файлов");
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Свойства");
        builder.setMessage(properties.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    public void copyFile(File source, File destination) {
        new CopyTask().execute(new File[]{source, destination});
    }
    
    private static class CopyTask extends AsyncTask<File, Void, Boolean> {
        @Override
        protected Boolean doInBackground(File... files) {
            File source = files[0];
            File destination = files[1];
            
            try {
                if (source.isDirectory()) {
                    copyDirectory(source, destination);
                } else {
                    copySingleFile(source, destination);
                }
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error copying file", e);
                return false;
            }
        }
        
        private void copyDirectory(File source, File destination) throws IOException {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    File destFile = new File(destination, file.getName());
                    if (file.isDirectory()) {
                        copyDirectory(file, destFile);
                    } else {
                        copySingleFile(file, destFile);
                    }
                }
            }
        }
        
        private void copySingleFile(File source, File destination) throws IOException {
            try (FileChannel inChannel = new FileInputStream(source).getChannel();
                 FileChannel outChannel = new FileOutputStream(destination).getChannel()) {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            }
        }
    }
    
    private String getMimeType(File file) {
        String extension = getFileExtension(file.getName());
        if (extension.isEmpty()) {
            return "application/octet-stream";
        }
        
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType != null ? mimeType : "application/octet-stream";
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}
