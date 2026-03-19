package com.windows11.files.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.windows11.files.R;
import com.windows11.files.models.FileItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    
    private List<FileItem> fileList;
    private OnFileClickListener listener;
    private Context context;
    
    public interface OnFileClickListener {
        void onFileClick(FileItem fileItem);
        void onFileLongClick(FileItem fileItem);
    }
    
    public FileAdapter(List<FileItem> fileList, OnFileClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = fileList.get(position);
        
        holder.nameText.setText(fileItem.getName());
        holder.sizeText.setText(fileItem.getFormattedSize());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        holder.dateText.setText(sdf.format(new Date(fileItem.getLastModified())));
        
        // Установка иконки в зависимости от типа файла
        if (fileItem.isDirectory()) {
            holder.iconImage.setImageResource(R.drawable.ic_folder);
        } else {
            holder.iconImage.setImageResource(getFileIcon(fileItem.getName()));
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(fileItem);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onFileLongClick(fileItem);
            }
            return true;
        });
    }
    
    private int getFileIcon(String fileName) {
        String extension = fileName.toLowerCase();
        if (extension.endsWith(".txt") || extension.endsWith(".log")) {
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
    
    @Override
    public int getItemCount() {
        return fileList.size();
    }
    
    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImage;
        TextView nameText;
        TextView sizeText;
        TextView dateText;
        
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImage = itemView.findViewById(R.id.iconImage);
            nameText = itemView.findViewById(R.id.nameText);
            sizeText = itemView.findViewById(R.id.sizeText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}
