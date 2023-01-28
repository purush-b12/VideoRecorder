package com.android.hopein.dancevideorecorder.ui.videos

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.hopein.dancevideorecorder.common.CommonUtils
import com.android.hopein.dancevideorecorder.databinding.VideoListItemBinding
import com.bumptech.glide.Glide

class VideoRecyclerViewAdapter(var context: Context, var list: MutableList<CommonUtils.Video>, var videoOnclickListener: VideoOnclickListener)
    : RecyclerView.Adapter<VideoRecyclerViewAdapter.ViewHolder> (){

    inner class ViewHolder(val binding: VideoListItemBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VideoRecyclerViewAdapter.ViewHolder {
        val binding = VideoListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: VideoRecyclerViewAdapter.ViewHolder,
        position: Int
    ) {

        with(holder){
            with(list[position]){
                binding.videoName.text = this.name
                binding.videoDate.text = this.date
                binding.videoSize.text = this.size.toString()+"MB"
                binding.videoLength.text = this.duration.toString()
                binding.videoImage.visibility = View.VISIBLE
                binding.playIcon.visibility = View.VISIBLE

                Glide.with(context)
                    .load(this.uri)
                    .into(binding.videoImage)

                binding.deleteButton.setOnClickListener {
                    videoOnclickListener.deleteClickListener(this.uri)
                }

                binding.shareButton.setOnClickListener {
                    videoOnclickListener.shareClickListener(this.uri)
                }

                binding.playIcon.setOnClickListener {
                    videoOnclickListener.videoPlayListener(this.uri)
                }

            }
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun deleteAndUpdateList(vList: MutableList<CommonUtils.Video>){
        try{
            list = vList
            notifyDataSetChanged()
        }catch (e: Exception){
            Log.e("Adapter", e.stackTraceToString())
        }

    }

}