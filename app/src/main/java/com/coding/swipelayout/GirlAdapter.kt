package com.coding.swipelayout

import android.content.Context
import com.bumptech.glide.Glide
import com.coding.swipelayout.base.BaseRecyclerAdapter
import com.coding.swipelayout.base.BaseRecyclerVH


/**
 * @author: Coding.He
 * @date: 2020/6/19
 * @emil: 229101253@qq.com
 * @des:RecycleView的图片适配器
 */
class GirlAdapter(context: Context, dataList: List<PageGirlBean.DataBean>) :
    BaseRecyclerAdapter<PageGirlBean.DataBean>(context, dataList) {

    override fun getItemLayoutId(viewType: Int): Int {
        return R.layout.item_girl
    }

    override fun bindData(
        holder: BaseRecyclerVH,
        position: Int,
        item: PageGirlBean.DataBean
    ) {
        val img = holder.getImageView(R.id.img_girl)
        Glide.with(mContext)
            .load(item.url)
            .centerCrop()
            .into(img)
        holder.setText(R.id.tv_msg, item.desc)

}

override fun onBindViewHolder(
    holder: BaseRecyclerVH,
        position: Int,
        payloads: MutableList<Any>
    ) {
        bindData(holder, position, mData[position])
    }

    override fun getItemCount(): Int {
        return mData.size
    }
}
