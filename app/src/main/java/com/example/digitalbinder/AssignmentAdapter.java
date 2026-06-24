package com.example.digitalbinder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.ViewHolder> {

    private List<Assignment> assignments = new ArrayList<>();
    private final List<Assignment> selectedItems = new ArrayList<>();
    private boolean isSelectionMode = false;
    private SelectionListener listener;

    public interface SelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    public void setSelectionListener(SelectionListener listener) {
        this.listener = listener;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
        notifyDataSetChanged();
    }

    public List<Assignment> getSelectedItems() {
        return selectedItems;
    }

    public void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Assignment a = assignments.get(position);

        holder.title.setText(a.getTitle());
        holder.subject.setText(a.getSubject());
        holder.date.setText(a.getFormattedDate());

        // Multi-Selection UI Toggling
        if (selectedItems.contains(a)) {
            // If you get an error on "overlay" or "checkIcon" here, make sure your item_assignment.xml is also updated!
            holder.overlay.setVisibility(View.VISIBLE);
            holder.checkIcon.setVisibility(View.VISIBLE);
        } else {
            holder.overlay.setVisibility(View.GONE);
            holder.checkIcon.setVisibility(View.GONE);
        }

        try {
            byte[] decoded = Base64.decode(a.getImageBase64(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            holder.image.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(a, position);
            } else {
                DetailActivity.currentAssignment = a;
                Intent intent = new Intent(holder.itemView.getContext(), DetailActivity.class);
                holder.itemView.getContext().startActivity(intent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(a, position);
                return true;
            }
            return false;
        });
    }

    private void toggleSelection(Assignment item, int position) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        notifyItemChanged(position);

        if (listener != null) {
            listener.onSelectionChanged(selectedItems.size());
        }
    }

    @Override
    public int getItemCount() { return assignments.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subject, date;
        ImageView image, checkIcon;
        View overlay;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTitle);
            subject = itemView.findViewById(R.id.textSubject);
            date = itemView.findViewById(R.id.textDate);
            image = itemView.findViewById(R.id.imageAssignment);

            // These connect to the item_assignment.xml file
            overlay = itemView.findViewById(R.id.selectedOverlay);
            checkIcon = itemView.findViewById(R.id.checkIcon);
        }
    }
}