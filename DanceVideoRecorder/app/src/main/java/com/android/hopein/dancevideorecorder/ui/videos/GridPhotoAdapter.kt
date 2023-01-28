package com.android.hopein.dancevideorecorder.ui.videos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.android.hopein.dancevideorecorder.R
import com.android.hopein.dancevideorecorder.common.CommonUtils
import com.bumptech.glide.Glide

class GridPhotoAdapter(var context: Context, var list: MutableList<CommonUtils.Photo>): BaseAdapter() {

    val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(p0: Int): Any {
        return list[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup?): View {
        var view = view
        if(view == null){
            view = layoutInflater.inflate(R.layout.item_image_content, viewGroup, false)
        }

        val image = view?.findViewById<ImageView>(R.id.mediaImage)
        val size = view?.findViewById<TextView>(R.id.mediaSize)

        Glide.with(context)
            .load(list[position].uri)
            .into(image!!)

        size?.text = list[position].size.toString()+"MB"

        return view!!
    }
}