package org.ligi.passandroid.ui.edit

import android.content.Intent
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import org.ligi.kaxt.doAfterEdit
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.model.pass.PassBarCodeFormat.*
import org.ligi.passandroid.ui.BarcodeUIController
import org.ligi.passandroid.ui.PassViewHelper
import java.util.*

class BarcodeEditController(private val rootView: View, internal val context: AppCompatActivity, barCode: BarCode) {
    private var alternativeMessageInput: AppCompatEditText
    private var messageInput: AppCompatEditText
    private var barcodeFormat: PassBarCodeFormat?
    private val intentFragment: Fragment
    private val passFormatRadioButtons: MutableMap<PassBarCodeFormat, RadioButton> = EnumMap(PassBarCodeFormat::class.java)

    class IntentFragment : Fragment() {
        var scanCallback: (format: String, result: String) -> Unit = { _, _ -> }

        private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult
            val format = data.getStringExtra("SCAN_RESULT_FORMAT") ?: return@registerForActivityResult
            val message = data.getStringExtra("SCAN_RESULT") ?: return@registerForActivityResult
            scanCallback(format, message)
        }

        fun scan(formats: List<String>) {
            val intent = Intent("com.google.zxing.client.android.SCAN").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra("SCAN_FORMATS", formats.joinToString(","))
            }
            runCatching { scanLauncher.launch(intent) }
                .onFailure {
                    Toast.makeText(requireContext(), "No barcode scanner is available", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun bindRadio(formats: Array<PassBarCodeFormat>) {
        formats.forEach {
            val radioButton = RadioButton(context)
            rootView.findViewById<RadioGroup>(R.id.barcodeRadioGroup).addView(radioButton)
            passFormatRadioButtons[it] = radioButton

            radioButton.text = it.name
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    barcodeFormat = it
                    refresh()
                }
            }

            radioButton.isChecked = barcodeFormat == it
        }

    }

    init {
        intentFragment = IntentFragment()
        barcodeFormat = barCode.format


        messageInput =rootView.findViewById(R.id.messageInput)
        alternativeMessageInput =rootView.findViewById(R.id.alternativeMessageInput)

        rootView.findViewById<AppCompatImageButton>(R.id.randomButton).setOnClickListener {
            messageInput.setText(when (barcodeFormat) {
                EAN_8 -> getRandomEAN8()
                EAN_13 -> getRandomEAN13()
                ITF -> getRandomITF()
                else -> UUID.randomUUID().toString().uppercase(Locale.ROOT)
            })
            refresh()
        }

        rootView.findViewById<View>(R.id.scanButton).setOnClickListener {
            intentFragment.scan(PassBarCodeFormat.values().map { it.name })
        }

        intentFragment.scanCallback = { newFormat, newMessage ->
            messageInput.setText(newMessage)
            rootView.findViewById<RadioGroup>(R.id.barcodeRadioGroup).check(passFormatRadioButtons[PassBarCodeFormat.valueOf(newFormat)]!!.id)
            refresh()
        }
        context.supportFragmentManager.commit { add(intentFragment, "intent_fragment") }

        bindRadio(PassBarCodeFormat.values())

        messageInput.setText(barCode.message)
        messageInput.doAfterEdit {
            refresh()
        }

        alternativeMessageInput.setText(barCode.alternativeText)

        refresh()
    }

    fun refresh() {
        val barcodeUIController = BarcodeUIController(rootView, getBarCode(), context, PassViewHelper(context))
        val isBarcodeShown = barcodeUIController.getBarcodeView().visibility == View.VISIBLE

        if (!isBarcodeShown) {
            messageInput.error = "Invalid message"
        } else {
            messageInput.error = null
        }
    }

    fun getBarCode() = BarCode(barcodeFormat, messageInput.text.toString()).apply {
        val newAlternativeText = alternativeMessageInput.text.toString()
        if (newAlternativeText.isNotEmpty()) {
            alternativeText = newAlternativeText
        }
    }
}
