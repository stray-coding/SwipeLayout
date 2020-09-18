package com.coding.swipelayout

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    /*获取Girl图片URL的相关参数*/
    private val mCategory = "Girl"
    private val mType = "Girl"
    private val mCount = 10
    private var mPage = 1
    private val mDataList: ArrayList<PageGirlBean.DataBean> = arrayListOf()
    private lateinit var mAdapter: GirlAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cb_refresh.isChecked = true
        cb_load_more.isChecked = true
        cb_refresh.setOnCheckedChangeListener { buttonView, isChecked ->
            sl_pic.isHeaderViewEnable = isChecked
        }

        cb_load_more.setOnCheckedChangeListener { buttonView, isChecked ->
            sl_pic.isFooterViewEnable = isChecked
        }

        btn_load_more.setOnClickListener {
            sl_pic.startLoadMore()
        }

        btn_refresh.setOnClickListener {
            sl_pic.startRefresh()
        }

        btn_change_refresh.setOnClickListener {
            sl_pic.headerView = View.inflate(this, R.layout.new_header_view, null)
        }

        btn_change_load_more.setOnClickListener {
            sl_pic.footerView = View.inflate(this, R.layout.new_footer_view, null)
        }

        sl_pic.contentRv.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        mAdapter = GirlAdapter(this, mDataList)
        sl_pic.contentRv.adapter = mAdapter
        sl_pic.actionListener = object : SwipeLayout.ActionListener {
            override fun onRefresh() {
                loadPic(true)

            }

            override fun onLoadMore() {
                loadPic(false)
            }
        }
        sl_pic.startRefresh()
        loadPic(true)
    }

    fun showTip(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        Log.d(TAG, msg)
    }

    @Synchronized
    private fun loadPic(isRefresh: Boolean) {
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://gank.io/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val request = retrofit.create(GetOnePic::class.java)
            if (isRefresh) mPage = 1 else mPage++
            val url = getRequestUrl(mCategory, mType, mPage, mCount)
            val call = request.getCall(url)
            call.enqueue(object : Callback<PageGirlBean> {
                override fun onResponse(
                    call: Call<PageGirlBean>,
                    response: Response<PageGirlBean>
                ) {
                    Log.i(TAG, "response: {$response}")
                    Log.i(TAG, "response.body(): {${response.body()}}")
                    val body = response.body() as PageGirlBean
                    if (isRefresh) {
                        mDataList.clear()
                    }
                    for (item in body.data) {
                        if (!mDataList.contains(item)) {
                            Log.i(TAG, "图片添加至list:{${item._id}}")
                            mDataList.add(item)
                        } else {
                            Log.i(TAG, "数据中已含有该图片:{${item._id}}")
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                    sl_pic.smoothResetScroll()
                    val msg = if (isRefresh) "数据刷新成功" else "数据加载成功"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(call: Call<PageGirlBean>, t: Throwable) {
                    Log.e(TAG, "onFailure$t")
                    showTip("数据加载失败")
                    sl_pic.smoothResetScroll()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "e:$e")
            showTip("数据加载失败")
            sl_pic.smoothResetScroll()
        }
    }

    interface GetOnePic {
        @GET
        fun getCall(@Url url: String): Call<PageGirlBean>
    }

    private fun getRequestUrl(category: String, type: String, page: Int, count: Int): String {
        return "v2/data/category/$category/type/$type/page/$page/count/$count"
    }
}