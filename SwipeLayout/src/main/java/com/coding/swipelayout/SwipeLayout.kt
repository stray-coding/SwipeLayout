package com.coding.swipelayout

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.Scroller
import androidx.recyclerview.widget.RecyclerView
import com.coding.swipelayout.util.ResourceUtil
import com.coding.swipelayout.util.SizeUtils

/**
 * @author: Coding.He
 * @date: 2020/9/16
 * @emil: 229101253@qq.com
 * @des:在SwipeRefreshLayout基础上，增加RecyclerView,并且实现上拉加载的功能
 */
class SwipeLayout(ctx: Context, attr: AttributeSet) : LinearLayout(ctx, attr) {
    companion object {
        private const val TAG = "SwipeLayout"

        /*手指向上*/
        private const val PULL_UP = 1

        /*手指向下*/
        private const val PULL_DOWN = 2

    }

    /**
     * 作为数据加载的RecyclerView
     * */
    private var contentRv = RecyclerView(ctx)

    /**
     * 处理平滑移动的scroller
     * */
    private val scroller = Scroller(context)

    private var headerView: View? = null
    private var footerView: View? = null

    private val HEADER_HEIGHT = SizeUtils.dp2px(ctx, 60f)

    private val FOOTER_HEIGHT = SizeUtils.dp2px(ctx, 60f)

    /*最大滑动距离*/
    private val MAX_SCROLL_DISTANCE = SizeUtils.dp2px(ctx, 60f)

    /*手势方向最小滑动距离*/
    private val MIN_SCROLL_SLOP = ViewConfiguration.get(context).scaledTouchSlop

    private var HEADER_VIEW_ENABLE = false
    private var FOOTER_VIEW_ENABLE = false


    private var layoutHeight = 0
    private var layoutWidth = 0


    init {
        gravity = Gravity.CENTER
        orientation = VERTICAL
        post {
            layoutHeight = height
            layoutWidth = width
            initView()
        }
    }

    private fun initView() {
        if (headerView == null) {
            Log.d(TAG, "not set the header view , init default header view")
            headerView =
                View.inflate(context, ResourceUtil.getLayoutId(context, "swipe_header_view"), null)
        }

        if (footerView == null) {
            Log.d(TAG, "not set the footer view , init default footer view")
            footerView =
                View.inflate(context, ResourceUtil.getLayoutId(context, "swipe_footer_view"), null)
        }

        val headerLp = LayoutParams(LayoutParams.MATCH_PARENT, HEADER_HEIGHT)
        Log.d(TAG, "width:$layoutWidth height:$layoutHeight")
        /**
         * 延迟初始化View,是为了获取整个ViewGroup的高度，从而式RecyclerView的高度与ViewGroup保持一致
         * */
        val contentRvLp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            layoutHeight
        )
        val footerLp = LayoutParams(LayoutParams.MATCH_PARENT, FOOTER_HEIGHT)
        contentRv.setBackgroundColor(Color.GRAY)
        addView(headerView, headerLp)
        addView(contentRv, contentRvLp)
        addView(footerView, footerLp)
    }


    private fun smoothResetScroll() {
        scrollTo(0, 0)
/*        Log.d(TAG, "scrollX:$scrollX scrollY:$scrollY")
        scroller.startScroll(scrollX, scrollY, 0, 0)
        invalidate()*/
    }

    override fun computeScroll() {
        // 第三步，重写computeScroll()方法，并在其内部完成平滑滚动的逻辑
        Log.d(TAG, "currScrollX:${scroller.currX} currScrollY:${scroller.currY}")
        if (scroller.computeScrollOffset()) {
            Log.d(TAG, "currScrollX:${scroller.currX} currScrollY:${scroller.currY}")
            this.scrollTo(0, 0)
            invalidate()
        }
    }

    private var downY = 0.0f
    private var lastY = 0.0f

    /*移动方向判定完毕*/
    private var isDireJudgeComplete = true

    /*手指移动方向*/
    private var pullDirection = 0
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        Log.d(TAG, "onTouchEvent")
        Log.d(TAG, "onTouchEvent: y:${ev!!.y.toInt()}")
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isDireJudgeComplete = false
                downY = ev.y
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                judgeMoveDirection(ev.y)
                scrollLayout(ev.y)
                lastY = ev.y
            }
            MotionEvent.ACTION_UP -> {
                smoothResetScroll()
            }
        }
        return true
    }

    /*判断移动方向*/
    private fun judgeMoveDirection(moveY: Float) {
        if (!isDireJudgeComplete) {
            if (moveY - downY > MIN_SCROLL_SLOP) {
                pullDirection = PULL_DOWN
                isDireJudgeComplete = true
            } else if (downY - moveY > MIN_SCROLL_SLOP) {
                pullDirection = PULL_UP
                isDireJudgeComplete = true
            }
        }
    }

    private fun scrollLayout(moveY: Float) {
        if (isDireJudgeComplete) {
            if (pullDirection == PULL_DOWN) {
                /**
                 * 下拉刷新
                 * 正常的scrollY的值应该在 -MAX_SCROLL_DISTANCE ~ 0之间
                 * 超过0则按0处理
                 * 低于-MAX_SCROLL_DISTANCE 按 -MAX_SCROLL_DISTANCE处理
                 * */
                val nextScrollY = scrollY + lastY - moveY
                Log.d(TAG, "nextScrollY:${nextScrollY}")
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
            } else if (pullDirection == PULL_UP) {
                /**
                 * 上拉加载
                 * 正常的scrollY的值应该在 0 ~ MAX_SCROLL_DISTANCE之间
                 * 低于0则按0处理
                 * 超过MAX_SCROLL_DISTANCE 按MAX_SCROLL_DISTANCE处理
                 * */
                val nextScrollY = scrollY + lastY - moveY
                Log.d(TAG, "nextScrollY:${nextScrollY}")
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
        }

    }

    fun setHeaderView(view: View) {
        headerView = view
    }

    fun setFooterView(view: View) {
        footerView = view
    }
}