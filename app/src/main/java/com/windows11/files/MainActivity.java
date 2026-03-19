package com.windows11.files;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windows11.files.adapters.FileAdapter;
import com.windows11.files.models.FileItem;
import com.windows11.files.utils.FileManager;
import com.windows11.files.utils.SearchUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private RecyclerView recyclerView;
    private TextView pathText;
    private ImageButton backButton, homeButton, refreshButton, searchButton, menuButton;
    private FileAdapter fileAdapter;
    private List<FileItem> fileList;
    private File currentDirectory;
    private FileManager fileManager;
    private boolean isSearchMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupClickListeners();
        fileManager = new FileManager();
        
        if (checkPermissions()) {
            loadDirectory(Environment.getExternalStorageDirectory());
        } else {
            requestPermissions();
        }
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        pathText = findViewById(R.id.pathText);
        backButton = findViewById(R.id.backButton);
        homeButton = findViewById(R.id.homeButton);
        refreshButton = findViewById(R.id.refreshButton);
        searchButton = findViewById(R.id.searchButton);
        menuButton = findViewById(R.id.menuButton);
        
        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(fileAdapter);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            if (currentDirectory != null && currentDirectory.getParent() != null) {
                loadDirectory(currentDirectory.getParentFile());
            }
        });
        
        homeButton.setOnClickListener(v -> {
            loadDirectory(Environment.getExternalStorageDirectory());
        });
        
        refreshButton.setOnClickListener(v -> {
            if (currentDirectory != null) {
                loadDirectory(currentDirectory);
            }
        });
        
        searchButton.setOnClickListener(v -> showSearchDialog());
        
        menuButton.setOnClickListener(v -> showOptionsMenu());
    }
    
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return android.os.Environment.isExternalStorageManager();
        } else {
            int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readPermission == PackageManager.PERMISSION_GRANTED && 
                   writePermission == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkPermissions()) {
                loadDirectory(Environment.getExternalStorageDirectory());
            } else {
                Toast.makeText(this, "Требуются разрешения для доступа к файлам", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void loadDirectory(File directory) {
        currentDirectory = directory;
        pathText.setText(directory.getAbsolutePath());
        
        fileList.clear();
        File[] files = directory.listFiles();
        
        if (files != null) {
            List<FileItem> folders = new ArrayList<>();
            List<FileItem> regularFiles = new ArrayList<>();
            
            for (File file : files) {
                FileItem item = new FileItem(file.getName(), file.getAbsolutePath(), file.isDirectory(), file.length(), file.lastModified());
                if (file.isDirectory()) {
                    folders.add(item);
                } else {
                    regularFiles.add(item);
                }
            }
            
            fileList.addAll(folders);
            fileList.addAll(regularFiles);
        }
        
        fileAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onFileClick(FileItem fileItem) {
        File file = new File(fileItem.getPath());
        if (file.isDirectory()) {
            loadDirectory(file);
        } else {
            fileManager.openFile(this, file);
        }
    }
    
    @Override
    public void onFileLongClick(FileItem fileItem) {
        // Показать контекстное меню для операций с файлами
        fileManager.showFileOptions(this, fileItem);
    }
    
    @Override
    public void onBackPressed() {
        if (currentDirectory != null && !currentDirectory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            loadDirectory(currentDirectory.getParentFile());
        } else {
            super.onBackPressed();
        }
    }
    
    private void showSearchDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search, null);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        EditText searchEditText = dialogView.findViewById(R.id.searchEditText);
        Button searchButton = dialogView.findViewById(R.id.searchButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        
        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
                dialog.dismiss();
            }
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void performSearch(String query) {
        if (currentDirectory == null) return;
        
        isSearchMode = true;
        pathText.setText("Результаты поиска: " + query);
        
        SearchUtils.searchFiles(currentDirectory, query, new SearchUtils.SearchCallback() {
            @Override
            public void onSearchStart() {
                // Показать индикатор загрузки
                fileList.clear();
                fileAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onSearchResult(List<FileItem> results) {
                fileList.clear();
                fileList.addAll(results);
                fileAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onSearchComplete() {
                // Скрыть индикатор загрузки
            }
        });
    }
    
    private void showOptionsMenu() {
        String[] options = {"Новая папка", "Сортировать по имени", "Сортировать по дате", "Сортировать по размеру", "Показать скрытые файлы"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Меню");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Новая папка
                    createNewFolder();
                    break;
                case 1: // Сортировать по имени
                    sortFiles("name");
                    break;
                case 2: // Сортировать по дате
                    sortFiles("date");
                    break;
                case 3: // Сортировать по размеру
                    sortFiles("size");
                    break;
                case 4: // Показать скрытые файлы
                    // TODO: Реализовать показ скрытых файлов
                    break;
            }
        });
        builder.show();
    }
    
    private void createNewFolder() {
        // TODO: Реализовать создание новой папки
    }
    
    private void sortFiles(String criteria) {
        // TODO: Реализовать сортировку файлов
    }
}
