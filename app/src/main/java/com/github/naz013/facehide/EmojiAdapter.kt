package com.github.naz013.facehide

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.naz013.facehide.databinding.ListItemEmojiBinding

class EmojiAdapter : RecyclerView.Adapter<EmojiAdapter.Holder>() {

    private val data: MutableList<Int> = mutableListOf()

    var clickListener: ((Int) -> Unit)? = null

    fun setData(list: List<Int>) {
        this.data.clear()
        this.data.addAll(list)
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(parent)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    inner class Holder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_emoji, parent, false)
    ) {
        private val binding: ListItemEmojiBinding = DataBindingUtil.bind(itemView)!!

        init {
            binding.iv.setOnClickListener {
                clickListener?.invoke(data[adapterPosition])
            }
        }

        fun bind(id: Int) {
            binding.iv.setImageResource(id)
        }
    }
}