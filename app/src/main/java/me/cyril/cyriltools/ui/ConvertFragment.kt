package me.cyril.cyriltools.ui

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.SparseArray
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import me.cyril.cyriltools.NO_MENU
import me.cyril.cyriltools.R
import me.cyril.cyriltools.base.BaseFragment

class ConvertFragment : BaseFragment() {

    companion object {
        private const val TYPE_UNICODE: String = "Unicode"
        private const val TYPE_UTF16 = "UTF-16"
        private const val TYPE_UTF8 = "UTF-8"
        private const val TYPE_BASE64: String = "Base64"
        private const val TYPE_MORSE: String = "Morse"
        private const val TYPE_URL: String = "Url"
    }

    override val resLayout: Int
        get() = R.layout.fragment_convert
    override val resMenu: Int
        get() = NO_MENU

    private val morseTree by lazy {
        charArrayOf(
            '\u0000', 'E', 'T', 'I', 'A', 'N', 'M', 'S', 'U', 'R',
            'W', 'D', 'K', 'G', 'O', 'H', 'V', 'F', '\u0000', 'L',
            '\u0000', 'P', 'J', 'B', 'X', 'C', 'Y', 'Z', 'Q'
        )
    }

    private val morseExtra by lazy {
        SparseArray<Char>().apply {
            put(31, '5')
            put(32, '4')
            put(34, '3')
            put(38, '2')
            put(39, '&')
            put(41, '+')
            put(46, '1')
            put(47, '6')
            put(48, '=')
            put(49, '/')
            put(53, '(')
            put(55, '7')
            put(59, '8')
            put(61, '9')
            put(62, '0')
            put(75, '?')
            put(81, '\"')
            put(84, '.')
            put(89, '@')
            put(93, '\'')
            put(96, '-')
            put(106, '!')
            put(108, ')')
            put(114, ',')
            put(119, ':')
        }
    }

    private var mType = TYPE_UNICODE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etNormal = view.findViewById<EditText>(R.id.text_normal)
        val etSpecial = view.findViewById<EditText>(R.id.text_special)
        val btnConvert = view.findViewById<Button>(R.id.convert)
        val btnClear = view.findViewById<Button>(R.id.text_clear)
        val spinner = view.findViewById<Spinner>(R.id.options)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mType = spinner.selectedItem.toString()
            }

        }

        btnConvert.setOnClickListener {
            val normal = etNormal.text.toString()
            val special = etSpecial.text.toString()
            if (normal.isEmpty()) {
                if (special.isEmpty()) {
                    mActivity?.snack("Empty input")
                } else {
                    val origin = when (mType) {
                        TYPE_UTF16 -> utf16ToText(special)
                        TYPE_UTF8 -> utf8ToText(special)
                        TYPE_UNICODE -> unicodeToText(special)
                        TYPE_BASE64 -> base64ToText(special)
                        TYPE_MORSE -> morseToText(special)
                        TYPE_URL -> Uri.decode(special)
                        else -> null
                    }
                    origin?.run {
                        with(etNormal) {
                            setText(origin)
                            setSelection(text.length)
                        }
                    }
                }
            } else {
                val result = when (mType) {
                    TYPE_UTF16 -> textToUtf16(normal)
                    TYPE_UTF8 -> textToUtf8(normal)
                    TYPE_UNICODE -> textToUnicode(normal)
                    TYPE_BASE64 -> textToBase64(normal)
                    TYPE_MORSE -> textToMorse(normal)
                    TYPE_URL -> Uri.encode(normal)
                    else -> null
                }
                result?.run {
                    with(etSpecial) {
                        setText(result)
                        setSelection(text.length)
                    }
                }
            }
        }

        btnClear.setOnClickListener {
            etNormal.text.clear()
            etSpecial.text.clear()
        }
    }

    private fun base64ToText(base64: String): String? {
        return try {
            val result = Base64.decode(base64, Base64.NO_WRAP)
            for (b in result) {
                require(b >= 0)
            }
            String(result)
        } catch (e: IllegalArgumentException) {
            mActivity?.snack("Invalid base64.")
            null
        }
    }

    private fun morseToText(morse: String): String? {
        val text = StringBuilder()
        val words = morse.split("   ")

        for (word in words) {
            val letters = word.split(' ')

            for (letter in letters) {
                if (letter.isEmpty()) {
                    mActivity?.snack("Invalid separator.")
                    return null
                }

                var index = 0
                for (c in letter) {
                    index = when (c) {
                        '.' -> index * 2 + 1
                        '-' -> (index + 1) * 2
                        else -> {
                            mActivity?.snack("Invalid Morse code.")
                            return null
                        }
                    }
                }

                when (index) {
                    in 1 until morseTree.size -> {
                        if (morseTree[index] == '\u0000') {
                            mActivity?.snack("Invalid Morse code.")
                            return null
                        } else {
                            text.append(morseTree[index])
                        }
                    }
                    else -> {
                        val i = morseExtra.indexOfKey(index)
                        if (i < 0) {
                            mActivity?.snack("Invalid Morse code.")
                            return null
                        } else {
                            text.append(morseExtra.valueAt(i))
                        }
                    }
                }
            }
            text.append(' ')
        }
        text.deleteCharAt(text.length - 1)

        return text.toString()
    }

    private fun unicodeToText(unicode: String): String? {
        val text = StringBuilder()
        val hexes = unicode.split("U+")

        if (hexes.size < 2) {
            mActivity?.snack("Invalid unicode.")
            return null
        }

        for (i in 1 until hexes.size) {
            val parse = try {
                hexes[i].toInt(16)
            } catch (e: Exception) {
                mActivity?.snack("Invalid unicode.")
                return null
            }
            when (hexes[i].length) {
                4 -> text.append(parse.toChar())
                5, 6 -> {
                    val index = hexes[i].toInt(16) - 0x10000
                    if (index > 0xfffff) {
                        mActivity?.snack("Invalid unicode.")
                        return null
                    } else {
                        val highBits = index.ushr(10) and 0x3ff
                        val lowBits = index and 0x3ff
                        var code = highBits + (0x36).shl(10)
                        text.append(code.toChar())
                        code = lowBits + (0x37).shl(10)
                        text.append(code.toChar())
                    }
                }
                else -> {
                    mActivity?.snack("Invalid unicode.")
                    return null
                }
            }
        }

        return text.toString()
    }

    private fun utf16ToText(utf16: String): String? {
        val text = StringBuilder()
        val hexes = utf16.split("\\u")

        if (hexes.size < 2) {
            mActivity?.snack("Invalid utf-16.")
            return null
        }

        for (i in 1 until hexes.size) {
            val parse = try {
                require(hexes[i].length == 4)
                hexes[i].toInt(16)
            } catch (e: Exception) {
                mActivity?.snack("Invalid utf-16.")
                return null
            }
            text.append(parse.toChar())
        }

        return text.toString()
    }

    private fun utf8ToText(utf8: String): String? {
        val hexes = utf8.split("\\x")

        if (hexes.size < 2) {
            mActivity?.snack("Invalid utf-8.")
            return null
        }

        val bytes = ByteArray(hexes.size - 1)
        for (i in 1 until hexes.size) {
            try {
                bytes[i - 1] = hexes[i].toInt(16).toByte()
            } catch (e: Exception) {
                mActivity?.snack("Invalid utf-8.")
                return null
            }
        }

        return String(bytes)
    }

    private fun textToBase64(text: String) =
        Base64.encodeToString(text.toByteArray(), Base64.NO_WRAP)

    private fun textToMorse(text: String): String? {
        val morse = StringBuilder()

        for (c in text) {
            when (c.toInt()) {
                in 38..58, 33, 34, 61, 63, 64 -> {
                    val index = morseExtra.indexOfValue(c)
                    morse.append(generateMorse(morseExtra.keyAt(index)))
                }
                in 65..90, in 97..122 -> {
                    for (i in 1 until morseTree.size) {
                        if (morseTree[i] == c.toUpperCase()) {
                            morse.append(generateMorse(i))
                            break
                        }
                    }
                }
                10, 32 -> morse.append(' ')
                else -> {
                    mActivity?.snack("Cannot convert to Morse code.")
                    return null
                }
            }
            morse.append(' ')
        }
        morse.deleteCharAt(morse.length - 1)

        return morse.toString()
    }

    private fun textToUnicode(text: String): String {
        val unicode = StringBuilder()

        var i = 0
        while (i < text.length) {
            unicode.append("U+")
            var code = text[i].toInt()
            if (code.ushr(10) and 0x3f == 0x36) {
                val highBits = code and 0x3ff
                val lowBits = text[++i].toInt() and 0x3ff
                code = highBits.shl(10) + lowBits + 0x10000
            }
            val str = Integer.toHexString(code)
            for (j in 0 until 4 - str.length) {
                unicode.append('0')
            }
            unicode.append(str)
            ++i
        }

        return unicode.toString()
    }

    private fun textToUtf16(text: String): String {
        val utf16 = StringBuilder()

        for (c in text) {
            utf16.append("\\u")
            val str = Integer.toHexString(c.toInt())
            for (j in 0 until 4 - str.length) {
                utf16.append('0')
            }
            utf16.append(str)
        }

        return utf16.toString()
    }

    private fun textToUtf8(text: String): String {
        val utf8 = StringBuilder()

        val bytes = text.toByteArray()
        for (byte in bytes) {
            utf8.append("\\x")
            utf8.append(Integer.toHexString(byte.toInt() and 0xff))
        }

        return utf8.toString()
    }

    private fun generateMorse(index: Int): String {
        val sb = StringBuilder()
        var i = index

        while (i != 0) {
            i = if (i % 2 == 1) {
                sb.append('.')
                (i - 1) / 2
            } else {
                sb.append('-')
                i / 2 - 1
            }
        }

        return sb.reverse().toString()
    }

}
