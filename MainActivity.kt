package com.apiteste.chatbot

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val bg = Color.rgb(13, 17, 23)
    private val panel = Color.rgb(22, 27, 34)
    private val green = Color.rgb(85, 220, 150)
    private lateinit var root: LinearLayout
    private lateinit var endpoint: EditText
    private lateinit var apiKey: EditText
    private lateinit var prompt: EditText
    private lateinit var method: Spinner
    private lateinit var body: EditText
    private lateinit var mode: Spinner
    private lateinit var output: TextView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(18, 22, 18, 18)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(label("API CHATBOT TESTER", 24, green))
        content.addView(label("Chat OpenAI + testador HTTP", 14, Color.LTGRAY))
        content.addView(label("Endpoint HTTPS", 14, green))
        endpoint = field("https://api.openai.com/v1/chat/completions")
        content.addView(endpoint)
        content.addView(label("Chave API (não é salva)", 14, green))
        apiKey = field("")
        apiKey.hint = "Cole sua chave apenas para este teste"
        apiKey.inputType = 129
        content.addView(apiKey)
        content.addView(label("Modo", 14, green))
        mode = Spinner(this)
        mode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Chat OpenAI", "Teste HTTP genérico"))
        content.addView(mode)
        content.addView(label("Mensagem", 14, green))
        prompt = field("Olá! Responda em português.")
        content.addView(prompt)
        content.addView(label("Método HTTP (modo genérico)", 14, green))
        method = Spinner(this)
        method.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("GET", "POST", "PUT", "DELETE"))
        content.addView(method)
        content.addView(label("Corpo JSON (modo genérico)", 14, green))
        body = field("{\\"message\\":\\"teste\\"}")
        body.minLines = 3
        content.addView(body)
        content.addView(Button(this).apply {
            text = "ENVIAR REQUISIÇÃO"
            setTextColor(Color.BLACK)
            setBackgroundColor(green)
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(root.windowToken, 0)
                if (mode.selectedItemPosition == 0) sendChat() else sendGeneric()
            }
        })
        status = label("Pronto para testar.", 14, Color.YELLOW)
        content.addView(status)
        content.addView(label("Resposta", 16, green))
        output = label("A resposta da API aparecerá aqui.", 14, Color.WHITE).apply {
            setPadding(12, 12, 12, 12)
            setBackgroundColor(panel)
            minHeight = 150
        }
        content.addView(output)
        content.addView(label(
            "Aviso: chaves usadas em apps clientes podem ser expostas. Para produção, use um backend seguro.",
            12, Color.LTGRAY))
        setContentView(root)
    }

    private fun label(text: String, size: Int, color: Int) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color)
        setPadding(0, 10, 0, 8)
    }

    private fun field(value: String) = EditText(this).apply {
        setText(value)
        setTextColor(Color.WHITE)
        setHintTextColor(Color.GRAY)
        setBackgroundColor(panel)
        setPadding(12, 6, 12, 6)
        textSize = 14f
    }

    private fun sendChat() {
        val key = apiKey.text.toString().trim()
        if (key.isEmpty()) {
            status.text = "Informe uma chave API para testar o chat."
            return
        }
        val json = JSONObject()
            .put("model", "gpt-4o-mini")
            .put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt.text.toString())))
            .toString()
        request(endpoint.text.toString().trim(), "POST", json, key)
    }

    private fun sendGeneric() {
        val target = endpoint.text.toString().trim()
        if (!target.startsWith("https://")) {
            status.text = "Por segurança, use uma URL HTTPS."
            return
        }
        val verb = method.selectedItem.toString()
        request(target, verb, if (verb == "GET") null else body.text.toString(), "")
    }

    private fun request(target: String, verb: String, payload: String?, key: String) {
        if (!target.startsWith("https://")) {
            status.text = "Por segurança, use uma URL HTTPS."
            return
        }
        status.text = "Enviando..."
        output.text = ""
        thread {
            var conn: HttpURLConnection? = null
            try {
                val started = System.currentTimeMillis()
                conn = (URL(target).openConnection() as HttpURLConnection).apply {
                    requestMethod = verb
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("Accept", "application/json")
                    if (key.isNotEmpty() && target.startsWith("https://api.openai.com/"))
                        setRequestProperty("Authorization", "Bearer $key")
                    if (payload != null && verb != "GET") {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                }
                if (payload != null && verb != "GET")
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() } ?: "(resposta vazia)"
                val elapsed = System.currentTimeMillis() - started
                runOnUiThread {
                    status.text = "HTTP $code • ${elapsed} ms"
                    output.text = pretty(response)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Falha na requisição"
                    output.text = e.message ?: e.javaClass.simpleName
                }
            } finally { conn?.disconnect() }
        }
    }

    private fun pretty(value: String): String = try {
        JSONObject(value).toString(2)
    } catch (_: Exception) {
        try { JSONArray(value).toString(2) } catch (_: Exception) { value }
    }
}
