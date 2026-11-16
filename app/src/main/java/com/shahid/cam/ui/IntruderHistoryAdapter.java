package com.shahid.cam.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.shahid.cam.R;
import com.shahid.cam.security.IntruderRecord;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter displaying locally saved Intruder Detection selfies.
 */
public class IntruderHistoryAdapter extends RecyclerView.Adapter<IntruderHistoryAdapter.ViewHolder> {

    public interface Listener {
        void onViewRecord(@NonNull IntruderRecord record);
        void onDeleteRecord(@NonNull IntruderRecord record);
    }

    private final List<IntruderRecord> items = new ArrayList<>();
    private final Listener listener;

    public IntruderHistoryAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<IntruderRecord> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_intruder_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IntruderRecord record = items.get(position);
        holder.tvReason.setText(record.reason);
        holder.tvTimestamp.setText(record.getFormattedDateTime());

        Glide.with(holder.ivThumb.getContext())
                .load(record.getFile())
                .centerCrop()
                .into(holder.ivThumb);

        holder.itemView.setOnClickListener(v -> listener.onViewRecord(record));
        holder.btnView.setOnClickListener(v -> listener.onViewRecord(record));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteRecord(record));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivThumb;
        final TextView tvReason;
        final TextView tvTimestamp;
        final View btnView;
        final View btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_intruder_thumb);
            tvReason = itemView.findViewById(R.id.tv_intruder_reason);
            tvTimestamp = itemView.findViewById(R.id.tv_intruder_timestamp);
            btnView = itemView.findViewById(R.id.btn_view_intruder);
            btnDelete = itemView.findViewById(R.id.btn_delete_intruder);
        }
    }
}
