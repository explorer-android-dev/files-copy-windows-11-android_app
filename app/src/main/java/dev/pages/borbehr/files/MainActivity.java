package dev.pages.borbehr.files;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import dev.pages.borbehr.files.adapters.FileAdapter;
import dev.pages.borbehr.files.adapters.QuickAccessAdapter;
import dev.pages.borbehr.files.models.FileItem;
import dev.pages.borbehr.files.models.QuickAccessItem;
import dev.pages.borbehr.files.utils.ClipboardManager;
import dev.pages.borbehr.files.utils.FileManager;
import dev.pages.borbehr.files.utils.SearchUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements 
        FileAdapter.OnFileClickListener, QuickAccessAdapter.OnItemClickListener {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    // UI компоненты
    private RecyclerView fileRecyclerView;
    private RecyclerView quickAccessRecyclerView;
    private TextView currentPathText;
    private TextView itemCountText;
    private TextView statusText;
    private ProgressBar statusProgressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout breadcrumbContainer;
    private ImageButton backButton, forwardButton, searchButton, menuButton;
    
    // Адаптеры
    private FileAdapter fileAdapter;
    private QuickAccessAdapter quickAccessAdapter;
    
    // Данные
    private List<FileItem> fileList;
    private List<QuickAccessItem> quickAccessItems;
    private File currentDirectory;
    private Stack<File> history;
    private Stack<File> forwardHistory;
    
    // Утилиты
    private FileManager fileManager;
    private ClipboardManager clipboardManager;
    
    // Состояние
    private boolean isSearchMode = false;
    private int currentViewType = FileAdapter.VIEW_TYPE_DETAILS;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initData();
        setupClickListeners();
        setupSwipeRefresh();
        initQuickAccess();
        
        if (checkPermissions()) {
            loadDirectory(Environment.getExternalStorageDirectory());
        } else {
            requestPermissions();
        }
    }
    
    private void initViews() {
        fileRecyclerView = findViewById(R.id.fileRecyclerView);
        quickAccessRecyclerView = findViewById(R.id.quickAccessRecyclerView);
        currentPathText = findViewById(R.id.currentPathText);
        itemCountText = findViewById(R.id.itemCountText);
        statusText = findViewById(R.id.statusText);
        statusProgressBar = findViewById(R.id.statusProgressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        breadcrumbContainer = findViewById(R.id.breadcrumbContainer);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        searchButton = findViewById(R.id.searchButton);
        menuButton = findViewById(R.id.menuButton);
        
        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileList, this);
        fileAdapter.setViewType(currentViewType);
        fileRecyclerView.setAdapter(fileAdapter);
        fileRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void initData() {
        fileManager = new FileManager();
        clipboardManager = ClipboardManager.getInstance(this);
        history = new Stack<>();
        forwardHistory = new Stack<>();
    }
    
    private void initQuickAccess() {
        quickAccessItems = new ArrayList<>();
        
        // Скачать корректные пути для быстрого доступа
        quickAccessItems.add(new QuickAccessItem("Этот ПК", "/", R.drawable.ic_menu));
        quickAccessItems.add(new QuickAccessItem("Документы", 
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(),
                R.drawable.ic_folder));
        quickAccessItems.add(new QuickAccessItem("Загрузки",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                R.drawable.ic_folder));
        quickAccessItems.add(new QuickAccessItem("Изображения",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(),
                R.drawable.ic_file_image));
        quickAccessItems.add(new QuickAccessItem("Видео",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath(),
                R.drawable.ic_file_video));
        quickAccessItems.add(new QuickAccessItem("Музыка",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
                R.drawable.ic_file_audio));
        
        quickAccessAdapter = new QuickAccessAdapter(quickAccessItems, this);
        quickAccessRecyclerView.setAdapter(quickAccessAdapter);
        quickAccessRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> navigateBack());
        forwardButton.setOnClickListener(v -> navigateForward());
        searchButton.setOnClickListener(v -> showSearchDialog());
        menuButton.setOnClickListener(v -> showMenu());
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentDirectory != null) {
                loadDirectory(currentDirectory);
            }
            swipeRefreshLayout.setRefreshing(false);
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(0xFF0078D4);
    }
    
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readPermission == PackageManager.PERMISSION_GRANTED && 
                   writePermission == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires explicit MANAGE_EXTERNAL_STORAGE permission via Settings
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    // Fallback: show dialog with instructions
                    showPermissionDialog();
                }
            }
        } else {
            // Android 10 and below
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
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    showPermissionDialog();
                }
            }
        }
    }
    
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Требуется доступ к файлам")
                .setMessage("Приложению требуется доступ ко всем файлам на устройстве.\\n\\nПожалуйста, разрешите это в настройках.")
                .setPositiveButton("Открыть настройки", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        // Fallback to all apps settings
                        startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
    
    private void loadDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            Toast.makeText(this, "Директория не существует", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentDirectory = directory;
        currentPathText.setText(directory.getName().isEmpty() ? "/" : directory.getName());
        updateBreadcrumb();
        
        fileList.clear();
        statusText.setText("Загрузка...");
        
        File[] files = directory.listFiles();
        
        if (files != null) {
            List<FileItem> folders = new ArrayList<>();
            List<FileItem> regularFiles = new ArrayList<>();
            
            for (File file : files) {
                if (file.isHidden()) continue;
                
                FileItem item = new FileItem(
                    file.getName(),
                    file.getAbsolutePath(),
                    file.isDirectory(),
                    file.length(),
                    file.lastModified()
                );
                
                if (file.isDirectory()) {
                    folders.add(item);
                } else {
                    regularFiles.add(item);
                }
            }
            
            fileList.addAll(folders);
            fileList.addAll(regularFiles);
        }
        
        updateItemCount();
        fileAdapter.notifyDataSetChanged();
        statusText.setText("Готово");
    }
    
    private void updateBreadcrumb() {
        breadcrumbContainer.removeAllViews();
        
        File current = currentDirectory;
        List<File> path = new ArrayList<>();
        
        while (current != null) {
            path.add(0, current);
            current = current.getParentFile();
        }
        
        for (int i = 0; i < path.size(); i++) {
            File f = path.get(i);
            String name = f.getName().isEmpty() ? "/" : f.getName();
            
            TextView breadcrumb = new TextView(this);
            breadcrumb.setText(name);
            breadcrumb.setTextColor(0xFF0078D4);
            breadcrumb.setPadding(8, 4, 8, 4);
            File finalF = f;
            breadcrumb.setOnClickListener(v -> loadDirectory(finalF));
            
            breadcrumbContainer.addView(breadcrumb);
            
            if (i < path.size() - 1) {
                TextView separator = new TextView(this);
                separator.setText(">");
                separator.setTextColor(0xFF808080);
                breadcrumbContainer.addView(separator);
            }
        }
    }
    
    private void updateItemCount() {
        itemCountText.setText(fileList.size() + " элементов");
    }
    
    private void navigateBack() {
        if (!history.isEmpty()) {
            forwardHistory.push(currentDirectory);
            loadDirectory(history.pop());
        }
    }
    
    private void navigateForward() {
        if (!forwardHistory.isEmpty()) {
            history.push(currentDirectory);
            loadDirectory(forwardHistory.pop());
        }
    }
    
    @Override
    public void onFileClick(FileItem fileItem) {
        File file = new File(fileItem.getPath());
        if (file.isDirectory()) {
            history.push(currentDirectory);
            forwardHistory.clear();
            loadDirectory(file);
        } else {
            fileManager.openFile(this, file);
        }
    }
    
    @Override
    public void onFileLongClick(FileItem fileItem) {
        showFileContextMenu(fileItem);
    }
    
    @Override
    public void onFileSelectionChanged(int count) {
        if (count > 0) {
            statusText.setText(count + " выбрано");
        } else {
            statusText.setText("Готово");
        }
    }
    
    @Override
    public void onItemClick(QuickAccessItem item) {
        history.push(currentDirectory);
        forwardHistory.clear();
        loadDirectory(new File(item.getPath()));
    }
    
    private void showFileContextMenu(FileItem fileItem) {
        PopupMenu popupMenu = new PopupMenu(this, getCurrentFocus() != null ? getCurrentFocus() : fileRecyclerView);
        popupMenu.getMenuInflater().inflate(R.menu.file_context_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_copy:
                    copyFiles();
                    return true;
                case R.id.action_cut:
                    cutFiles();
                    return true;
                case R.id.action_paste:
                    pasteFiles();
                    return true;
                case R.id.action_delete:
                    deleteFiles();
                    return true;
                case R.id.action_rename:
                    renameFile(fileItem);
                    return true;
                case R.id.action_properties:
                    showProperties(fileItem);
                    return true;
                default:
                    return false;
            }
        });
        
        popupMenu.show();
    }
    
    private void showMenu() {
        PopupMenu popupMenu = new PopupMenu(this, menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_new_folder:
                    createNewFolder();
                    return true;
                case R.id.action_view_details:
                    currentViewType = FileAdapter.VIEW_TYPE_DETAILS;
                    fileAdapter.setViewType(currentViewType);
                    fileRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                    return true;
                case R.id.action_view_icons:
                    currentViewType = FileAdapter.VIEW_TYPE_ICON;
                    fileAdapter.setViewType(currentViewType);
                    fileRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
                    return true;
                case R.id.action_sort_name:
                    sortFiles("name");
                    return true;
                case R.id.action_sort_date:
                    sortFiles("date");
                    return true;
                case R.id.action_sort_size:
                    sortFiles("size");
                    return true;
                case R.id.action_select_all:
                    fileAdapter.selectAll();
                    return true;
                default:
                    return false;
            }
        });
        
        popupMenu.show();
    }
    
    private void copyFiles() {
        List<String> selectedPaths = new ArrayList<>(fileAdapter.getSelectedItems());
        if (!selectedPaths.isEmpty()) {
            clipboardManager.copy(selectedPaths);
            fileAdapter.clearSelection();
            Toast.makeText(this, "Скопировано", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void cutFiles() {
        List<String> selectedPaths = new ArrayList<>(fileAdapter.getSelectedItems());
        if (!selectedPaths.isEmpty()) {
            clipboardManager.cut(selectedPaths);
            fileAdapter.clearSelection();
            Toast.makeText(this, "Вырезано", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void pasteFiles() {
        if (clipboardManager.canPaste() && currentDirectory != null) {
            try {
                clipboardManager.paste(currentDirectory.getAbsolutePath());
                loadDirectory(currentDirectory);
                Toast.makeText(this, "Вставлено", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Ошибка при вставке: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void deleteFiles() {
        if (fileAdapter.getSelectedItems().isEmpty()) {
            Toast.makeText(this, "Выберите файлы", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Удалить?")
            .setMessage("Вы уверены?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                for (String filePath : fileAdapter.getSelectedItems()) {
                    File file = new File(filePath);
                    deleteRecursively(file);
                }
                fileAdapter.clearSelection();
                loadDirectory(currentDirectory);
                Toast.makeText(MainActivity.this, "Удалено", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
    
    private void renameFile(FileItem fileItem) {
        EditText input = new EditText(this);
        input.setText(fileItem.getName());
        input.setSelection(fileItem.getName().length());
        
        new AlertDialog.Builder(this)
            .setTitle("Переименовать")
            .setView(input)
            .setPositiveButton("ОК", (dialog, which) -> {
                String newName = input.getText().toString();
                if (!newName.isEmpty()) {
                    File oldFile = new File(fileItem.getPath());
                    File newFile = new File(oldFile.getParent(), newName);
                    
                    if (oldFile.renameTo(newFile)) {
                        loadDirectory(currentDirectory);
                        Toast.makeText(MainActivity.this, "Переименовано", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Ошибка при переименовании", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void createNewFolder() {
        EditText input = new EditText(this);
        input.setHint("Новая папка");
        
        new AlertDialog.Builder(this)
            .setTitle("Создать папку")
            .setView(input)
            .setPositiveButton("ОК", (dialog, which) -> {
                String folderName = input.getText().toString();
                if (!folderName.isEmpty() && currentDirectory != null) {
                    File newFolder = new File(currentDirectory, folderName);
                    
                    if (newFolder.mkdirs()) {
                        loadDirectory(currentDirectory);
                        Toast.makeText(MainActivity.this, "Папка создана", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Ошибка при создании папки", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void showProperties(FileItem fileItem) {
        File file = new File(fileItem.getPath());
        String properties = "Имя: " + fileItem.getName() + "\n" +
                           "Путь: " + fileItem.getPath() + "\n" +
                           "Размер: " + fileItem.getFormattedSize() + "\n" +
                           "Тип: " + (fileItem.isDirectory() ? "Папка" : "Файл") + "\n" +
                           "Изменён: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(fileItem.getLastModified()));
        
        new AlertDialog.Builder(this)
            .setTitle("Свойства")
            .setMessage(properties)
            .setPositiveButton("ОК", null)
            .show();
    }
    
    private void sortFiles(String sortBy) {
        // Реализовать сортировку
        Toast.makeText(this, "Сортировка: " + sortBy, Toast.LENGTH_SHORT).show();
    }
    
    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Поиск");
        
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        
        builder.setPositiveButton("Поиск", (dialog, which) -> {
            String query = input.getText().toString();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });
        
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
    
    private void performSearch(String query) {
        if (currentDirectory == null) return;
        
        isSearchMode = true;
        statusText.setText("Поиск...");
        
        SearchUtils.searchFiles(currentDirectory, query, new SearchUtils.SearchCallback() {
            @Override
            public void onSearchStart() {
                fileList.clear();
                fileAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onSearchResult(List<FileItem> results) {
                fileList.clear();
                fileList.addAll(results);
                updateItemCount();
                fileAdapter.notifyDataSetChanged();
            }
            
            @Override
            public void onSearchComplete() {
                statusText.setText("Готово");
            }
        });
    }
    
    @Override
    public void onBackPressed() {
        if (isSearchMode) {
            isSearchMode = false;
            if (currentDirectory != null) {
                loadDirectory(currentDirectory);
            }
        } else if (currentDirectory != null && !currentDirectory.getAbsolutePath().equals("/")) {
            navigateBack();
        } else {
            super.onBackPressed();
        }
    }
}
