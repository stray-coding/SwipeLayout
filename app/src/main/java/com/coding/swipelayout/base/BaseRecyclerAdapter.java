package com.coding.swipelayout.base;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Coding.He
 * @date: 2020/6/16
 * @emil: 229101253@qq.com
 * @des:
 */

public abstract class BaseRecyclerAdapter<T> extends RecyclerView.Adapter<BaseRecyclerVH> {
    public List<T> mData;
    public final Context mContext;
    private LayoutInflater mInflater;
    private OnItemClickListener mClickListener;
    private OnItemLongClickListener mLongClickListener;

    public BaseRecyclerAdapter(Context ctx, List<T> list) {
        mData = (list != null) ? list : new ArrayList<>();
        mContext = ctx;
        mInflater = LayoutInflater.from(ctx);
    }

    @Override
    public BaseRecyclerVH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        final BaseRecyclerVH holder = new BaseRecyclerVH(mInflater.inflate(getItemLayoutId(viewType), parent, false));
        if (mClickListener != null) {
            holder.itemView.setOnClickListener(v -> mClickListener.onItemClick(holder.itemView, holder.getLayoutPosition()));
        }
        if (mLongClickListener != null) {
            holder.itemView.setOnLongClickListener(v -> {
                mLongClickListener.onItemLongClick(holder.itemView, holder.getLayoutPosition());
                return true;
            });
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseRecyclerVH holder, int position) {
        bindData(holder, position, mData.get(position));
    }

    public T getItemData(int pos) {
        return mData.get(pos);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void add(int pos, T item) {
        mData.add(pos, item);
        notifyItemInserted(pos);
    }

    public void delete(int pos) {
        mData.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, mData.size() - pos);
    }

    public void setData(List<T> list) {
        mData = (list != null) ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mLongClickListener = listener;
    }

    abstract public int getItemLayoutId(int viewType);

    abstract public void bindData(BaseRecyclerVH holder, int position, T item);

    public interface OnItemClickListener {
        void onItemClick(View itemView, int pos);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View itemView, int pos);
    }
}