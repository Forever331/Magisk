package com.topjohnwu.magisk.adapters;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.topjohnwu.magisk.R;
import com.topjohnwu.magisk.ViewBinder;
import com.topjohnwu.magisk.components.ExpandableView;
import com.topjohnwu.magisk.container.SuLogEntry;
import com.topjohnwu.magisk.database.MagiskDatabaseHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.recyclerview.widget.RecyclerView;

public class SuLogAdapter extends SectionedAdapter<SuLogAdapter.SectionHolder, SuLogAdapter.LogViewHolder> {

    private List<List<Integer>> logEntryList;
    private Set<Integer> itemExpanded, sectionExpanded;
    private MagiskDatabaseHelper suDB;
    private Cursor suLogCursor = null;

    public SuLogAdapter(MagiskDatabaseHelper db) {
        suDB = db;
        logEntryList = Collections.emptyList();
        sectionExpanded = new HashSet<>();
        itemExpanded = new HashSet<>();
    }

    @Override
    public int getSectionCount() {
        return logEntryList.size();
    }

    @Override
    public int getItemCount(int section) {
        return sectionExpanded.contains(section) ? logEntryList.get(section).size() : 0;
    }

    @Override
    public SectionHolder onCreateSectionViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_sulog_group, parent, false);
        return new SectionHolder(v);
    }

    @Override
    public LogViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_sulog, parent, false);
        return new LogViewHolder(v);
    }

    @Override
    public void onBindSectionViewHolder(SectionHolder holder, int section) {
        suLogCursor.moveToPosition(logEntryList.get(section).get(0));
        SuLogEntry entry = new SuLogEntry(suLogCursor);
        holder.arrow.setRotation(sectionExpanded.contains(section) ? 180 : 0);
        holder.itemView.setOnClickListener(v -> {
            RotateAnimation rotate;
            if (sectionExpanded.contains(section)) {
                holder.arrow.setRotation(0);
                rotate = new RotateAnimation(180, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                sectionExpanded.remove(section);
                notifyItemRangeRemoved(getItemPosition(section, 0), logEntryList.get(section).size());
            } else {
                rotate = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                sectionExpanded.add(section);
                notifyItemRangeInserted(getItemPosition(section, 0), logEntryList.get(section).size());
            }
            rotate.setDuration(300);
            rotate.setFillAfter(true);
            holder.arrow.setAnimation(rotate);
        });
        holder.date.setText(entry.getDateString());
    }

    @Override
    public void onBindItemViewHolder(LogViewHolder holder, int section, int position) {
        int sqlPosition = logEntryList.get(section).get(position);
        suLogCursor.moveToPosition(sqlPosition);
        SuLogEntry entry = new SuLogEntry(suLogCursor);
        holder.setExpanded(itemExpanded.contains(sqlPosition));
        holder.itemView.setOnClickListener(view -> {
            if (holder.isExpanded()) {
                holder.collapse();
                itemExpanded.remove(sqlPosition);
            } else {
                holder.expand();
                itemExpanded.add(sqlPosition);
            }
        });
        holder.appName.setText(entry.appName);
        holder.action.setText(entry.action ? R.string.grant : R.string.deny);
        holder.command.setText(entry.command);
        holder.fromPid.setText(String.valueOf(entry.fromPid));
        holder.toUid.setText(String.valueOf(entry.toUid));
        holder.time.setText(entry.getTimeString());
    }

    public void notifyDBChanged() {
        if (suLogCursor != null)
            suLogCursor.close();
        suLogCursor = suDB.getLogCursor();
        logEntryList = suDB.getLogStructure();
        itemExpanded.clear();
        sectionExpanded.clear();
        sectionExpanded.add(0);
        notifyDataSetChanged();
    }

    public static class SectionHolder extends RecyclerView.ViewHolder {

        public TextView date;
        public ImageView arrow;

        SectionHolder(View itemView) {
            super(itemView);
            ViewBinder.bind(this, itemView);
        }
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder implements ExpandableView {

        public TextView appName;
        public TextView action;
        public TextView time;
        public TextView fromPid;
        public TextView toUid;
        public TextView command;
        public ViewGroup expandLayout;

        private Container container = new Container();

        LogViewHolder(View itemView) {
            super(itemView);
            ViewBinder.bind(this, itemView);
            container.expandLayout = expandLayout;
            setupExpandable();
        }

        @Override
        public Container getContainer() {
            return container;
        }
    }
}
