package com.pierfrancescosoffritti.expandablelayout.sample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pierfrancescosoffritti.expandablelayout.ExpandableLayout;

import java.util.List;

public class StringAdapter extends RecyclerView.Adapter<StringAdapter.StringViewHolder> {

    private final List<String> data;

    public StringAdapter(List<String> data) {
        this.data = data;
    }

    @Override
    public StringAdapter.StringViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        return new StringViewHolder(view);
    }

    @Override
    public void onBindViewHolder(StringAdapter.StringViewHolder holder, int position) {
        holder.nonExpandableView.setText(data.get(position));

        holder.expandableLayout.collapse(false);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class StringViewHolder extends RecyclerView.ViewHolder {

        final ExpandableLayout expandableLayout;
        final TextView nonExpandableView;

        public StringViewHolder(View itemView) {
            super(itemView);

            expandableLayout = (ExpandableLayout) itemView.findViewById(R.id.expandable_layout);
            nonExpandableView = (TextView) itemView.findViewById(R.id.text_view);

            nonExpandableView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    expandableLayout.toggle(true);
                }
            });

        }
    }
}
