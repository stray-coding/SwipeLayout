package com.coding.swipelayout

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Scroller
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * @author: Coding.He
 * @date: 2020/9/16
 * @emil: 229101253@qq.com
 * @des:LinearLayout，增加HeaderView、RecyclerView、FooterView,并且实现上拉加载、下拉刷新的逻辑
 * @see startLoadMore() 主动执行上拉加载
 * @see startRefresh() 主动执行下拉刷新
 * @see smoothResetScroll() 隐藏上拉加载 or 下拉刷新 的视图
 * @see contentRv 获取到内部的RecyclerView对象
 * @see headerView 获取/设置 headerView对象
 * @see footerView 获取/设置 footerView对象
 * @see isHeaderViewEnable 是否启用下拉刷新
 * @see isFooterViewEnable 是否启用上拉加载
 */
class SwipeLayout(ctx: Context, attr: AttributeSet) : LinearLayout(ctx, attr) {
    companion object {
        private const val TAG = "SwipeLayout"

        /*自动回弹的时间*/
        private const val ANIM_DURATION = 300

        private const val SCROLL_DISTANCE = 80f
    }

    /**
     * 作为数据加载的RecyclerView
     * */
    var contentRv = RecyclerView(ctx)

    /**
     * 处理平滑移动的scroller
     * */
    private val scroller = Scroller(context)

    var headerView: View = View.inflate(context, getId(context, "layout", "swipe_header_view"), null)
        set(value) {
            removeView(field)
            field = value
            addView(field, 0, LayoutParams(LayoutParams.MATCH_PARENT, HEADER_HEIGHT))
        }

    var footerView: View = View.inflate(context, getId(context, "layout", "swipe_footer_view"), null)
        set(value) {
            removeView(field)
            field = value
            addView(field, -1, LayoutParams(LayoutParams.MATCH_PARENT, FOOTER_HEIGHT))
        }

    /**
     * 是否启用下拉刷新
     * */
    var isHeaderViewEnable = true

    /**
     * 是否启用上拉加载
     * */
    var isFooterViewEnable = true

    /*状态监听*/
    var actionListener: ActionListener? = null

    private val HEADER_HEIGHT = dp2px(ctx, SCROLL_DISTANCE)

    private val FOOTER_HEIGHT = dp2px(ctx, SCROLL_DISTANCE)

    /*最大滑动距离*/
    private val MAX_SCROLL_DISTANCE = dp2px(ctx, SCROLL_DISTANCE)


    /*整个SwipeLayout的宽高*/
    private var layoutHeight = 0
    private var layoutWidth = 0


    /*是否正在进行上拉加载/下拉刷新操作*/
    private var isLoadData = false


    init {
        gravity = Gravity.CENTER
        orientation = VERTICAL
        contentRv.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        /**
         * 延迟初始化View,是为了获取整个ViewGroup的高度，从而式RecyclerView的高度与ViewGroup保持一致
         * */
        post {
            layoutHeight = height
            layoutWidth = width
            Log.d(TAG, "width:$layoutWidth height:$layoutHeight")
            addView(headerView, LayoutParams(LayoutParams.MATCH_PARENT, HEADER_HEIGHT))
            addView(contentRv, 1, LayoutParams(LayoutParams.MATCH_PARENT, layoutHeight))
            addView(footerView, LayoutParams(LayoutParams.MATCH_PARENT, FOOTER_HEIGHT))
        }
    }

    /*手指按下时的Y坐标*/
    private var downY = 0.0f

    /*手指最后停留的Y坐标*/
    private var lastY = 0.0f

    /*是否到达了RecyclerView的顶部*/
    private var rvIsArriveTop = false

    /*是否到达了RecyclerView的底部*/
    private var rvIsArriveDown = false

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        Log.d(TAG, "dispatchTouchEvent: y:${ev!!.y.toInt()}")
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = ev.y
                lastY = ev.y
                Log.d(TAG, "downY:$downY")
            }
            MotionEvent.ACTION_MOVE -> {
                rvIsArriveTop = isArriveTop(contentRv)
                rvIsArriveDown = isArriveBottom(contentRv)
                if (!isLoadData) {
                    scrollLayout(ev.y)
                }
                lastY = ev.y
            }
            MotionEvent.ACTION_UP -> {
                /**
                 * 超过一半且未在加载数据，则视为进行下拉刷新和上拉加载操作
                 * */
                if ((abs(scrollY) >= MAX_SCROLL_DISTANCE / 2) and !isLoadData) {
                    isLoadData = true
                    if (rvIsArriveDown) {
                        startLoadMore()
                    } else if (rvIsArriveTop) {
                        startRefresh()
                    }
                } else {
                    smoothResetScroll()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun scrollLayout(moveY: Float) {
        val nextScrollY = scrollY + lastY - moveY
        Log.d(TAG, "nextScrollY:${nextScrollY} currScrollX:$scrollX")
        if (rvIsArriveTop) {
            pullDownScroll(nextScrollY)
        } else if (rvIsArriveDown) {
            pullUpScroll(nextScrollY)
        }
    }

    /**
     * 下拉刷新
     * 正常的scrollY的值应该在 -MAX_SCROLL_DISTANCE ~ 0之间
     * 超过0则按0处理
     * 低于-MAX_SCROLL_DISTANCE 按 -MAX_SCROLL_DISTANCE处理
     * */
    private fun pullDownScroll(nextScrollY: Float) {
        if (!isHeaderViewEnable) return
        when {
            nextScrollY >= 0 -> {
                scrollTo(0, 0)
            }
            nextScrollY < -MAX_SCROLL_DISTANCE -> {
                scrollTo(0, -MAX_SCROLL_DISTANCE)
            }
            else -> {
                scrollTo(0, nextScrollY.toInt())
            }
        }
    }

    /**
     * 上拉加载
     * 正常的scrollY的值应该在 0 ~ MAX_SCROLL_DISTANCE之间
     * 低于0则按0处理
     * 超过MAX_SCROLL_DISTANCE 按MAX_SCROLL_DISTANCE处理
     * */
    private fun pullUpScroll(nextScrollY: Float) {
        if (!isFooterViewEnable) return
        when {
            nextScrollY < 0 -> {
                scrollTo(0, 0)
            }
            nextScrollY > MAX_SCROLL_DISTANCE -> {
                scrollTo(0, MAX_SCROLL_DISTANCE)
            }
            else -> {
                scrollTo(0, nextScrollY.toInt())
            }
        }
    }

    /**
     * 展现下拉刷新的视图
     * */
    fun startRefresh() {
        if (!isHeaderViewEnable) return
        isLoadData = true
        contentRv.smoothScrollToPosition(0)
        scroller.startScroll(0, scrollY, 0, -MAX_SCROLL_DISTANCE - scrollY)
        actionListener?.onRefresh()
        invalidate()
    }

    /**
     * 展现上拉加载的视图
     * */
    fun startLoadMore() {
        if (!isFooterViewEnable) return
        isLoadData = true
        if (contentRv.adapter != null) {
            val itemCount = contentRv.adapter!!.itemCount
            contentRv.smoothScrollToPosition(if (itemCount > 0) itemCount - 1 else 0)
        }
        scroller.startScroll(0, scrollY, 0, MAX_SCROLL_DISTANCE - scrollY)
        actionListener?.onLoadMore()
        invalidate()
    }

    /**
     * 隐藏上拉加载/下拉刷新的视图
     * */
    fun smoothResetScroll() {
        Log.d(TAG, "scrollY:$scrollY")
        scroller.startScroll(0, scrollY, 0, -scrollY, ANIM_DURATION)
        isLoadData = false
        invalidate()
    }

    /**
     * RecyclerView.canScrollVertically(-1)的值表示是否能向下滚动，false表示已经滚动到顶部
     * */
    private fun isArriveTop(rv: RecyclerView): Boolean {
        return !rv.canScrollVertically(-1)
    }

    /**
     * * RecyclerView.canScrollVertically(1)的值表示是否能向上滚动，false表示已经滚动到底部
     * */
    private fun isArriveBottom(rv: RecyclerView): Boolean {
        return !rv.canScrollVertically(1)
    }

    private fun dp2px(ctx: Context, dpValue: Float): Int {
        val scale = ctx.applicationContext.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private fun getId(paramContext: Context, paramI: String, paramII: String): Int {
        try {
            return paramContext.resources.getIdentifier(paramII, paramI, paramContext.packageName)
        } catch (localException: Exception) {
            Log.w("ResourceUtil", "getId error in ResourceUtil" + localException.message)
        }
        return 0
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(0, scroller.currY)
            postInvalidate()
        }
    }

    interface ActionListener {
        fun onRefresh()
        fun onLoadMore()
    }
}