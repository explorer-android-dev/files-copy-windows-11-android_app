package com.windows11.files.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.windows11.files.models.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchUtils {
    
    private static final String TAG = "SearchUtils";
    
    public interface SearchCallback {
        void onSearchStart();
        void onSearchResult(List<FileItem> results);
        void onSearchComplete();
    }
    
    public static void searchFiles(File directory, String query, SearchCallback callback) {
        new SearchTask(directory, query, callback).execute();
    }
    
    private static class SearchTask extends AsyncTask<Void, List<FileItem>, List<FileItem>> {
        private File searchDirectory;
        private String searchQuery;
        private SearchCallback callback;
        
        public SearchTask(File directory, String query, SearchCallback callback) {
            this.searchDirectory = directory;
            this.searchQuery = query.toLowerCase();
            this.callback = callback;
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (callback != null) {
                callback.onSearchStart();
            }
        }
        
        @Override
        protected List<FileItem> doInBackground(Void... voids) {
            List<FileItem> results = new ArrayList<>();
            searchRecursive(searchDirectory, searchQuery, results);
            return results;
        }
        
        @Override
        protected void onPostExecute(List<FileItem> fileItems) {
            super.onPostExecute(fileItems);
            if (callback != null) {
                callback.onSearchResult(fileItems);
                callback.onSearchComplete();
            }
        }
        
        private void searchRecursive(File directory, String query, List<FileItem> results) {
            if (isCancelled()) {
                return;
            }
            
            File[] files = directory.listFiles();
            if (files == null) {
                return;
            }
            
            for (File file : files) {
                if (isCancelled()) {
                    return;
                }
                
                // Проверяем, соответствует ли имя файла поисковому запросу
                if (file.getName().toLowerCase().contains(query)) {
                    FileItem item = new FileItem(
                        file.getName(),
                        file.getAbsolutePath(),
                        file.isDirectory(),
                        file.length(),
                        file.lastModified()
                    );
                    results.add(item);
                    
                    // Отправляем промежуточные результаты
                    publishResults(new ArrayList<>(results));
                }
                
                // Рекурсивно ищем в подпапках
                if (file.isDirectory() && !file.isHidden()) {
                    searchRecursive(file, query, results);
                }
            }
        }
        
        private void publishResults(List<FileItem> results) {
            publishProgress(results);
        }
    }
    
    public static List<FileItem> quickSearch(File directory, String query) {
        List<FileItem> results = new ArrayList<>();
        if (directory == null || query == null || query.trim().isEmpty()) {
            return results;
        }
        
        String searchQuery = query.toLowerCase();
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase().contains(searchQuery)) {
                    FileItem item = new FileItem(
                        file.getName(),
                        file.getAbsolutePath(),
                        file.isDirectory(),
                        file.length(),
                        file.lastModified()
                    );
                    results.add(item);
                }
            }
        }
        
        return results;
    }
}
