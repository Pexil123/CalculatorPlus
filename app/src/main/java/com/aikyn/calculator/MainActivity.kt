package com.aikyn.calculator

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AppCompatActivity
import com.aikyn.calculator.databinding.ActivityMainBinding
import org.mariuszgromada.math.mxparser.Expression
import java.text.DecimalFormat


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var pref: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pref = getSharedPreferences("HISTORY", MODE_PRIVATE)

        binding.input.setShowSoftInputOnFocus(false)

        binding.input.requestFocus()

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

        binding.buttonC.setOnClickListener {
            binding.input.setText("")
            binding.output.text = ""
            binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_dark_24)
        }

        binding.backspace.setOnClickListener {
            val cursorPos = binding.input.selectionStart
            val textLen = binding.input.text.length

            if (cursorPos != 0 && textLen != 0) {
                val selection = binding.input.getText() as SpannableStringBuilder
                selection.replace(cursorPos - 1, cursorPos, "")
                binding.input.setText(selection)
                binding.input.setSelection(cursorPos - 1)
                calculate_expression()
            }

            if (binding.input.text.isEmpty()) {
                binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_dark_24)
            } else {
                binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_24)
            }
        }

        binding.buttonEquals.setOnClickListener {

            val passsword = getSharedPreferences("TABLE", MODE_PRIVATE).getString("password", "123456789")
            if (binding.input.text.toString() == "$passsword+)") {
                val i = Intent(this, SecretActivity::class.java)
                startActivity(i)
            }
            if (!binding.output.text.isEmpty()) {
                saveHistory()
                binding.input.setText(binding.output.text)
                binding.input.setSelection(binding.output.text.length)
                binding.output.text = ""
            }
        }

        binding.buttonHistory.setOnClickListener {
            val i = Intent(this, ActivityHistory::class.java)
            startActivity(i)
        }

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
            if (binding.input.text.isEmpty()) {
                binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_dark_24)
            } else {
                binding.backspace.setImageResource(R.drawable.ic_baseline_backspace_24)
            }
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
        val editor = pref?.edit()
        val count = count()+1
        editor?.putInt("count", count)
        editor?.putString("exp$count", binding.input.text.toString())
        editor?.putString("equal$count", "=${binding.output.text}")
        editor?.apply()
    }

    fun count(): Int {
        return pref?.getInt("count", 0)!!
    }

}