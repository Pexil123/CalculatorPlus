package com.aikyn.calculator

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Adapter (listArray: ArrayList<ListItem>, context: Context): RecyclerView.Adapter<Adapter.ViewHolder>() {

    var array = listArray
    var cont = context

    //Создаем то что будем привязывать
    class ViewHolder (view: View): RecyclerView.ViewHolder(view) {
        val expression = view.findViewById<TextView>(R.id.expression)!!
        val equals = view.findViewById<TextView>(R.id.equals)!!

        fun bind(listItem: ListItem, cont: Context){
            expression.text = listItem.expression
            equals.text = listItem.equals
        }
    }

    //Заполняем шаблоны
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(cont)
        return ViewHolder(inflater.inflate(R.layout.item_layout, parent, false))
    }

    //Привязываем все и что то делаем
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(array.get(position), cont)
    }

    //Сколько элементов
    override fun getItemCount(): Int {
        return array.size
    }
}