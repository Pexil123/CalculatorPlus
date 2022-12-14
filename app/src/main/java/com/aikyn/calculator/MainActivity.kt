package com.aikyn.calculator

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.aikyn.calculator.databinding.ActivityMainBinding
import org.mariuszgromada.math.mxparser.Expression
import java.text.DecimalFormat
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var pref: SharedPreferences? = null
    var tablePref: SharedPreferences? = null
    var history_array = ArrayList<ListItem>()
    var isOpened = false
    var canOpen = true
    var errorToast by Delegates.notNull<Toast>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        errorToast = Toast.makeText(this, "Использован недопустимый формат.", Toast.LENGTH_SHORT)

        pref = getSharedPreferences("HISTORY", MODE_PRIVATE)
        tablePref = getSharedPreferences("TABLE", MODE_PRIVATE)

        binding.input.showSoftInputOnFocus = false

        binding.input.requestFocus()

        binding.recyclerView.hasFixedSize()
        binding.recyclerView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL, true)

        updateStateButtonHistory()

        binding.buttonOne.setOnClickListener {setValue("1")}
        binding.buttonTwo.setOnClickListener {setValue("2")}
        binding.buttonThree.setOnClickListener {setValue("3")}
        binding.buttonFour.setOnClickListener {setValue("4")}
        binding.buttonFive.setOnClickListener {setValue("5")}
        binding.buttonSix.setOnClickListener {setValue("6")}
        binding.buttonSeven.setOnClickListener {setValue("7")}
        binding.buttonEight.setOnClickListener {setValue("8")}
        binding.buttonNine.setOnClickListener {setValue("9")}
        binding.buttonZero.setOnClickListener {setValue("0")}
        binding.buttonPlus.setOnClickListener {setValue("+")}
        binding.buttonMinus.setOnClickListener {setValue("-")}
        binding.buttonDivide.setOnClickListener {setValue("÷")}
        binding.buttonMultiply.setOnClickListener {setValue("×")}
        binding.buttonBracketStart.setOnClickListener {setValue("(")}
        binding.buttonBracketEnd.setOnClickListener {setValue(")")}
        binding.buttonDot.setOnClickListener {
            if (binding.input.text.substring(0, binding.input.selectionStart).lastOrNull().toString() in listOf(
                    "0", "1", "2", "3",
                    "4", "5", "6",
                    "7", "8", "9"
                ) && !binding.input.text.substring(0, binding.input.selectionStart).endsWith(",")
            ){
                setValue(",")
            } else if (!binding.input.text.substring(0, binding.input.selectionStart).endsWith("0,") && !binding.input.text.substring(0, binding.input.selectionStart).endsWith(",")) {
                setValue("0,")
            }
        }

        binding.buttonMinusNumber.setOnClickListener {
            val cursorPos = binding.input.selectionStart
            val textBefore = binding.input.text.substring(0, cursorPos)
            val textAfter = binding.input.text.substring(cursorPos)

            if (!textBefore.endsWith("(-")) {
                with(binding.input) {
                    setText("$textBefore(-$textAfter")
                    setSelection(cursorPos+2)
                }
            } else {
                with(binding.input) {
                    setText("${textBefore.dropLast(2)}$textAfter")
                    setSelection(cursorPos-2)
                }
            }

        }

        binding.input.doOnTextChanged { text, start, before, count ->
            if (binding.input.text.isEmpty()) {
                binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_dark_24)
            } else {
                binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_24)
            }
            calculate_expression()
        }

        binding.buttonC.setOnClickListener {
            binding.input.setText("")
            binding.output.text = ""
            binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_dark_24)
        }

        binding.backspace.setOnClickListener {
            val cursorPos = binding.input.selectionStart
            val cursorEndPos = binding.input.selectionEnd
            val textLen = binding.input.text.length

            if (cursorPos != 0 && textLen != 0 && cursorPos == cursorEndPos) {
                val selection = binding.input.getText() as SpannableStringBuilder
                selection.replace(cursorPos - 1, cursorPos, "")
                binding.input.setText(selection)
                binding.input.setSelection(cursorPos - 1)
                calculate_expression()
            }
            if (cursorPos != cursorEndPos) {
                val selection = binding.input.getText() as SpannableStringBuilder
                selection.replace(cursorPos, cursorEndPos, "")
                binding.input.setText(selection)
                binding.input.setSelection(cursorPos)
                calculate_expression()
            }

            updateBackspaceState()
        }


        binding.buttonEquals.setOnClickListener {

            val passsword = tablePref?.getString("password", "123456789")
            if (binding.input.text.toString() == "$passsword+)") {
                val i = Intent(this, SecretActivity::class.java)
                startActivity(i)
            } else if (!binding.output.text.isEmpty()) {
                saveHistory()
                binding.input.setText(binding.output.text)
                binding.input.setSelection(binding.output.text.length)
                binding.output.text = ""
            } else if (!binding.input.text.isEmpty()) {
                errorToast.cancel()
                errorToast.show()
            }
        }

        binding.buttonHistory.setOnClickListener {
            if (!isOpened && canOpen) {
                binding.buttonHistory.setImageResource(R.drawable.ic_close_history)
                setAdapter()
                openHistoryScreen()
            } else if (canOpen) {
                binding.buttonHistory.setImageResource(R.drawable.ic_history)
                closeHistoryScreen()
            }

        }

        binding.buttonClear.setOnClickListener {
            closeHistoryScreen()
            clearHistory()
            updateStateButtonHistory()
        }

    }

    fun updateBackspaceState() {
        if (binding.input.text.isEmpty()) {
            binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_dark_24)
        } else {
            binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_24)
        }
    }

    fun updateStateButtonHistory() {
        if (pref?.getInt("count", -1) == -1) {
            canOpen = false
            binding.buttonHistory.setImageResource(R.drawable.ic_history_dark)
        } else {
            canOpen = true
            binding.buttonHistory.setImageResource(R.drawable.ic_history)
        }
    }

    fun openHistoryScreen() {
        isOpened = true
        binding.historyScreen.visibility = View.VISIBLE
        binding.historyScreen.animate().apply {
            duration = 30
            alpha(1f)
        }.withEndAction {
            binding.recyclerView.animate().apply {
                duration = 30
                alpha(1f)
                translationYBy(-300f)
            }.start()
        }
    }

    fun closeHistoryScreen() {
        isOpened = false
        binding.historyScreen.visibility = View.GONE
        binding.historyScreen.animate().apply {
            duration = 30
            alpha(0f)
        }.withEndAction {
            binding.recyclerView.animate().apply {
                duration = 30
                alpha(0f)
                translationYBy(300f)
            }.start()
        }
    }

    fun clearHistory() {
        canOpen = false
        val editor = pref?.edit()
        editor?.clear()?.apply()
        history_array.clear()
    }

    fun setAdapter() {
        if (count()!=0) {
            history_array.clear()
            for (i in count() downTo 1) {
                history_array.add(ListItem(pref?.getString("exp$i", "")!!, pref?.getString("equal$i", "")!!))
            }
        }
        binding.recyclerView.adapter = Adapter(history_array, this)
    }

    fun setValue(value: String){
        if (binding.input.text.length<13) {
            val oldStr: String = binding.input.text.toString()
            val cursorPos: Int = binding.input.selectionStart
            val leftStr = oldStr.substring(0, cursorPos)
            val rightStr = oldStr.substring(cursorPos)

            binding.input.setText("$leftStr$value$rightStr")
            binding.input.setSelection(cursorPos + value.length)
            calculate_expression()
            updateBackspaceState()
        }
    }

    fun calculate_expression() {
        try {
            val expression = binding.input.text.toString()
                .replace("×", "*")
                .replace("÷", "/")
                .replace(",", ".")
            val result = Expression(expression).calculate()
            if (result.isNaN()) {
                binding.output.text = ""
            } else {
                binding.output.text = DecimalFormat("0.######").format(result).toString()
                    .replace(".", ",")
            }
        } catch (e: Exception) {
            binding.output.text = ""
        }
    }

    fun saveHistory(){
        canOpen = true
        val editor = pref?.edit()
        val count = count()+1
        editor?.putInt("count", count)
        editor?.putString("exp$count", binding.input.text.toString())
        editor?.putString("equal$count", "=${binding.output.text}")
        editor?.apply()
        updateStateButtonHistory()
    }

    fun count(): Int {
        return pref?.getInt("count", 0)!!
    }

}