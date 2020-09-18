RecyclerView出现之前，大家基本都是使用的ListView，通过ListView自带的api能轻松的实现头布局和尾布局的添加，但RecyclerView并不具备这样的API，所以coding今天决定简单实现一个带上拉加载、下拉刷新的布局。
如果你不想听我瞎逼逼，可点击该链接[https://github.com/stray-coding/SwipeLayout](https://github.com/stray-coding/SwipeLayout).直接查看源码即可!

## 第一步：自定义SwipeLayout作为容器
首先我们先剖析一个需要实现的布局，不出意外应该是下面这个样子：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200918165540514.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM2Mzc4ODM2,size_16,color_FFFFFF,t_70#pic_center)
根据上图可知：上拉加载和下拉刷新，正常情况下用户是看不到的，只有用户在滑动手机屏幕的过程中，到达了RecyclerView的顶部/底部时才会显示相应的视图。所以我们先自定义SwipeLayout类，继承LinearLayout布局，然后添加headerView、recyclerView、footerView三个布局，并且其中子布局的RecyclerView的高度应该与ViewGroup保持一致。
```
class SwipeLayout(ctx: Context, attr: AttributeSet) : LinearLayout(ctx, attr) {
    /**
     * 作为数据加载的RecyclerView
     * */
    var contentRv = RecyclerView(ctx)		
    var headerView: View = View.inflate(context, getId(context, "layout", "swipe_header_view"), null)
    var footerView: View = View.inflate(context, getId(context, "layout", "swipe_footer_view"), null)
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
            addView(contentRv, LayoutParams(LayoutParams.MATCH_PARENT, layoutHeight))
            addView(footerView, LayoutParams(LayoutParams.MATCH_PARENT, FOOTER_HEIGHT))
        }
}
```
## 第二步：让SwipeLayout动起来
在添加完布局后，总所周知，LinearLayout是不具备ScrollView的属性的（即：内容会根据手指的移动而滑动)，所以我们就需要让SwipeLayout动起来，这就涉及到对SwipeLayout内部的Event事件进行处理。
我们先来捋一捋SwipeLayout什么时候需要滑动，是不是当其内部的RecyclerView的内容到达顶部或者底部的时候，用户如果继续滑动，我们就时候就需要给用户呈现上拉加载/下拉刷新的视图。然后当滑动继续回归到RecylerView视图中，则又需要将事件交由RecyclerView处理。
经过上面的分析，我们实际上只需要在SwipeLayout的dispatchTouchEvent方法中进行处理即可，在Move事件中判断RecyclerView是否已经到达了顶部或者底部，如果达到则SwipeLayout自己处理，做上拉下载/下拉刷新的视图操作，反之则留给RecyclerView自己处理。
```
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
```
## 第三步：让下拉刷新/上拉加载能自动回弹到顶部/顶部
当用户松开手指时，我们会判断用户的滑动距离，是否足够触发我们的上拉加载/下拉刷新，若不足以，则需要回弹到正常的视图界面，反之则应该完整的显示上拉加载/下拉刷新视图。这里我们需要用到scrollTo的方法，但是scrollTo时瞬时完成的，这样跟用户的UI交互很不好，所以需要使用到Scroller来进行一个回调的动画效果。
Scroller的使用方法大体分为三步：
* 初始化scroller对象 ```private val scroller = Scroller(context)```
* 设置滑动的起点，以及需要滑动的x,y轴的距离 ```scroller.startScroll(x轴起点坐标, y轴起点坐标, x轴滑动距离, y轴滑动距离)```
* 重写父类的computeScroll()方法，内部实现缓慢滑动逻辑 
```
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(0, scroller.currY)
            postInvalidate()
        }
    }
```
判断是否触发数据加载条件，以及实现上拉加载/下拉刷新视图显示与回弹
```
	override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        Log.d(TAG, "dispatchTouchEvent: y:${ev!!.y.toInt()}")
        when (ev.action) {
            MotionEvent.ACTION_UP -> {
                /**
                 * 超过一半且未在加载数据，则视为进行下拉刷新和上拉加载操作
                 * */
                if ((abs(scrollY) >= MAX_SCROLL_DISTANCE / 2) and !isLoadData) {
                    isLoadData = true
                    if (rvIsArriveTop) {
                        startRefresh()
                    } else if (rvIsArriveDown) {
                        startLoadMore()
                    }
                } else {
                    smoothResetScroll()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
```
## 第四步：定义接口，从而监听下拉刷新/上拉加载
定义回调接口，在合适的地方回调，从而告知开发者，目前正在进行下拉刷新/上拉加载操作，需要执行具体的业务逻辑
```
    interface ActionListener {
        fun onRefresh()
        fun onLoadMore()
    }
```
## 最终效果：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200918173055647.gif#pic_center)

详细代码地址：[https://github.com/stray-coding/SwipeLayout](https://github.com/stray-coding/SwipeLayout).
 * startLoadMore() 主动执行上拉加载
 * startRefresh() 主动执行下拉刷新
 * smoothResetScroll() 隐藏上拉加载 or 下拉刷新 的视图
 * contentRv 获取到内部的RecyclerView对象
 * headerView 获取/设置 headerView对象
 * footerView 获取/设置 footerView对象
 * isHeaderViewEnable 是否启用下拉刷新
 * isFooterViewEnable 是否启用上拉加载
 
好啦，上面就是如何自定义内嵌上拉刷新/下拉加载和RecyclerView布局的全部流程，如果你觉得对你有帮助，不要忘记在github帮我点个star哦！

