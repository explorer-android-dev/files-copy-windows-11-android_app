package dev.pages.borbehr.files.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import dev.pages.borbehr.files.R;
import dev.pages.borbehr.files.models.FileItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    
    public static final int VIEW_TYPE_DETAILS = 0;
    public static final int VIEW_TYPE_ICON = 1;
    
    private List<FileItem> fileList;
    private OnFileClickListener listener;
    private Context context;
    private int viewType = VIEW_TYPE_DETAILS;
    private Set<String> selectedItems = new HashSet<>();
    private boolean isMultiSelectMode = false;
    
    public interface OnFileClickListener {
        void onFileClick(FileItem fileItem);
        void onFileLongClick(FileItem fileItem);
        void onFileSelectionChanged(int count);
    }
    
    public FileAdapter(List<FileItem> fileList, OnFileClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }
    
    public void setViewType(int viewType) {
        this.viewType = viewType;
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemViewType(int position) {
        return viewType;
    }
    
    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        
        View view;
        if (viewType == VIEW_TYPE_ICON) {
            view = inflater.inflate(R.layout.item_file_icon, parent, false);
        } else {
            view = inflater.inflate(R.layout.item_file_details, parent, false);
        }
        
        return new FileViewHolder(view, viewType);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = fileList.get(position);
        
        // Установка иконки
        if (fileItem.isDirectory()) {
            holder.iconImage.setImageResource(R.drawable.ic_folder);
        } else {
            holder.iconImage.setImageResource(getFileIcon(fileItem.getName()));
        }
        
        // Установка имени
        holder.nameText.setText(fileItem.getName());
        
        // Установка выделения
        boolean isSelected = selectedItems.contains(fileItem.getPath());
        if (holder.checkbox != null) {
            holder.checkbox.setChecked(isSelected);
            holder.checkbox.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        }
        
        // Для вида деталей
        if (viewType == VIEW_TYPE_DETAILS) {
            if (fileItem.isDirectory()) {
                holder.typeText.setText(context.getString(R.string.folder));
                holder.sizeText.setText("-");
            } else {
                String extension = getFileExtension(fileItem.getName()).isEmpty() 
                        ? "Файл" 
                        : getFileExtension(fileItem.getName()).toUpperCase();
                holder.typeText.setText(extension);
                holder.sizeText.setText(fileItem.getFormattedSize());
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            holder.dateText.setText(sdf.format(new Date(fileItem.getLastModified())));
        }
        
        // Обработчики кликов
        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                toggleSelection(fileItem, position);
            } else {
                if (listener != null) {
                    listener.onFileClick(fileItem);
                }
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                isMultiSelectMode = true;
                toggleSelection(fileItem, position);
                notifyDataSetChanged();
            }
            if (listener != null) {
                listener.onFileLongClick(fileItem);
            }
            return true;
        });
        
        if (holder.checkbox != null) {
            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedItems.add(fileItem.getPath());
                } else {
                    selectedItems.remove(fileItem.getPath());
                }
                if (listener != null) {
                    listener.onFileSelectionChanged(selectedItems.size());
                }
            });
        }
    }
    
    private void toggleSelection(FileItem fileItem, int position) {
        if (selectedItems.contains(fileItem.getPath())) {
            selectedItems.remove(fileItem.getPath());
        } else {
            selectedItems.add(fileItem.getPath());
        }
        
        if (selectedItems.isEmpty()) {
            isMultiSelectMode = false;
        }
        
        notifyItemChanged(position);
        if (listener != null) {
            listener.onFileSelectionChanged(selectedItems.size());
        }
    }
    
    public Set<String> getSelectedItems() {
        return new HashSet<>(selectedItems);
    }
    
    public void clearSelection() {
        selectedItems.clear();
        isMultiSelectMode = false;
        notifyDataSetChanged();
    }
    
    public void selectAll() {
        selectedItems.clear();
        for (FileItem item : fileList) {
            selectedItems.add(item.getPath());
        }
        if (listener != null) {
            listener.onFileSelectionChanged(selectedItems.size());
        }
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemCount() {
        return fileList.size();
    }
    
    private int getFileIcon(String fileName) {
        String extension = fileName.toLowerCase();
        if (extension.endsWith(".txt") || extension.endsWith(".log") || extension.endsWith(".md")) {
            return R.drawable.ic_file_text;
        } else if (extension.endsWith(".jpg") || extension.endsWith(".jpeg") || 
                   extension.endsWith(".png") || extension.endsWith(".gif") || 
                   extension.endsWith(".bmp") || extension.endsWith(".webp")) {
            return R.drawable.ic_file_image;
        } else if (extension.endsWith(".mp4") || extension.endsWith(".avi") || 
                   extension.endsWith(".mkv") || extension.endsWith(".mov") || 
                   extension.endsWith(".wmv")) {
            return R.drawable.ic_file_video;
        } else if (extension.endsWith(".mp3") || extension.endsWith(".wav") || 
                   extension.endsWith(".flac") || extension.endsWith(".aac") || 
                   extension.endsWith(".ogg")) {
            return R.drawable.ic_file_audio;
        } else if (extension.endsWith(".pdf")) {
            return R.drawable.ic_file_pdf;
        } else if (extension.endsWith(".doc") || extension.endsWith(".docx")) {
            return R.drawable.ic_file_word;
        } else if (extension.endsWith(".xls") || extension.endsWith(".xlsx")) {
            return R.drawable.ic_file_excel;
        } else if (extension.endsWith(".ppt") || extension.endsWith(".pptx")) {
            return R.drawable.ic_file_powerpoint;
        } else if (extension.endsWith(".zip") || extension.endsWith(".rar") || 
                   extension.endsWith(".7z") || extension.endsWith(".tar") || 
                   extension.endsWith(".gz")) {
            return R.drawable.ic_file_archive;
        } else if (extension.endsWith(".apk")) {
            return R.drawable.ic_file_android;
        } else {
            return R.drawable.ic_file_default;
        }
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }
    
    class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImage;
        TextView nameText;
        TextView typeText;
        TextView sizeText;
        TextView dateText;
        CheckBox checkbox;
        
        FileViewHolder(View itemView, int viewType) {
            super(itemView);
            iconImage = itemView.findViewById(R.id.fileIcon);
            nameText = itemView.findViewById(R.id.fileName);
            checkbox = itemView.findViewById(R.id.itemCheckbox);
            
            if (viewType == VIEW_TYPE_DETAILS) {
                typeText = itemView.findViewById(R.id.fileType);
                sizeText = itemView.findViewById(R.id.fileSize);
                dateText = itemView.findViewById(R.id.fileDate);
            }
        }
    }
}

