package com.aikyn.calculator

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ActivityHistory : AppCompatActivity() {

    var history_array = ArrayList<ListItem>()
    var pref: SharedPreferences? = null
    var count = 1
    var recycler: RecyclerView? = null
    var buttonClear: AppCompatButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        pref = getSharedPreferences("HISTORY", MODE_PRIVATE)
        count = pref?.getInt("count", 1)!!
        recycler = findViewById(R.id.recyclerView)
        buttonClear = findViewById(R.id.button_clear)

        recycler?.hasFixedSize()
        recycler?.layoutManager = LinearLayoutManager(this)

        setAdapter()

        buttonClear?.setOnClickListener {
            clearHistory()
            setAdapter()
        }
    }

    fun clearHistory() {
        count = 1
        val editor = pref?.edit()
        editor?.clear()?.commit()
        history_array.clear()
    }

    fun setAdapter() {
        for (i in count downTo 1) {
            history_array.add(ListItem(pref?.getString("exp$i", "No History")!!, pref?.getString("equal$i", "")!!))
        }
        recycler?.adapter = Adapter(history_array, this)
    }

}