package dev.pages.borbehr.files.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import dev.pages.borbehr.files.R;
import dev.pages.borbehr.files.models.QuickAccessItem;

import java.util.List;

public class QuickAccessAdapter extends RecyclerView.Adapter<QuickAccessAdapter.ViewHolder> {
    
    private List<QuickAccessItem> items;
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(QuickAccessItem item);
    }
    
    public QuickAccessAdapter(List<QuickAccessItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_access, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickAccessItem item = items.get(position);
        holder.nameText.setText(item.getName());
        holder.iconImage.setImageResource(item.getIcon());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImage;
        TextView nameText;
        
        ViewHolder(View itemView) {
            super(itemView);
            iconImage = itemView.findViewById(R.id.quickAccessIcon);
            nameText = itemView.findViewById(R.id.quickAccessName);
        }
    }
}
