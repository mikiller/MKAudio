package com.smg.audioeditor.adapters;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.smg.audioeditor.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mikiller on 2018/5/18.
 */

public class FileRcvAdapter extends RecyclerView.Adapter<FileRcvAdapter.FileHolder> {
    private List<File> fileList = new ArrayList<>();
    private onItemClickListener itemClickListener;

    public void setFileList(List<File> files){
        fileList = new ArrayList<>(files);
        notifyDataSetChanged();
    }

    public void setFileList(File file){
        fileList.add(file);
        notifyDataSetChanged();
    }

    public void setItemClickListener(onItemClickListener listener){
        itemClickListener = listener;
    }

    @Override
    public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wavfile, parent, false);
        return new FileHolder(view);
    }

    @Override
    public void onBindViewHolder(FileHolder holder, int position) {
        final File file = fileList.get(position);
        holder.tv_file.setText(file.getName());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(itemClickListener != null)
                    itemClickListener.onItemClick(file);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList == null ? 0 : fileList.size();
    }

    protected static class FileHolder extends RecyclerView.ViewHolder{
        private TextView tv_file;
        public FileHolder(View itemView) {
            super(itemView);
            tv_file = itemView.findViewById(R.id.tv_fileName);

        }
    }

    public interface onItemClickListener{
        void onItemClick(File file);
    }
}
