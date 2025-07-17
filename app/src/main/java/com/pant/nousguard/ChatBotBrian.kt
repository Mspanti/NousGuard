package pant.com.nousguard

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.random.Random

// Data class to hold tokenizer information
data class TokenizerData(
    val wordIndex: Map<String, Int>,
    val oovToken: String? = null,
    val maxSequenceLength: Int
)

// Main ChatbotBrain class
class ChatbotBrain(private val context: Context) {

    // TFLite Interpreter for the Intent Model only
    private var tfliteIntentInterpreter: Interpreter? = null

    // Tokenizer and Label Map for Intent Model
    private var intentTokenizerData: TokenizerData? = null
    private var intentLabelMap: Map<Int, String>? = null

    // Map to store responses for each intent tag
    private var responsesMap: Map<String, List<String>>? = null

    // File names for assets
    private val INTENT_MODEL_FILE = "intent_model.tflite"
    private val TOKENIZER_FILE = "tokenizer.json"
    private val INTENT_LABEL_MAP_FILE = "intent_label_map.json"
    private val RESPONSES_MAP_FILE = "responses_map.json" // New file for responses

    init {
        try {
            // Initialize TFLite Interpreter for Intent Model
            tfliteIntentInterpreter = Interpreter(loadTFLiteModelFile(context, INTENT_MODEL_FILE))
            Log.d("ChatbotBrain", "TensorFlow Lite Intent Model loaded successfully: $INTENT_MODEL_FILE")

            // Load tokenizer, label map, and responses map for Intent Model
            loadIntentComponents(
                TOKENIZER_FILE,
                INTENT_LABEL_MAP_FILE,
                RESPONSES_MAP_FILE
            )

        } catch (e: Exception) {
            Log.e("ChatbotBrain", "Failed to initialize ChatbotBrain: ${e.message}", e)
            tfliteIntentInterpreter = null
            intentTokenizerData = null
            intentLabelMap = null
            responsesMap = null
        }
    }

    // Helper function to load the .tflite model from assets
    private fun loadTFLiteModelFile(context: Context, modelFileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Function to load all components related to the Intent Model
    private fun loadIntentComponents(
        tokenizerFileName: String,
        labelMapFileName: String,
        responsesMapFileName: String
    ) {
        try {
            // Load Tokenizer
            context.assets.open(tokenizerFileName).bufferedReader().use { reader ->
                val jsonString = reader.readText()
                val jsonObject = JSONObject(jsonString)

                val wordIndex: MutableMap<String, Int> = mutableMapOf()
                val oovToken: String?
                val maxSequenceLength: Int

                // Expect word_index, oov_token, and max_sequence_length at top-level
                if (jsonObject.has("word_index")) {
                    val wordIndexJson = jsonObject.getJSONObject("word_index")
                    wordIndexJson.keys().forEach { key ->
                        wordIndex[key] = wordIndexJson.getInt(key)
                    }
                } else {
                    throw JSONException("No 'word_index' found in tokenizer.json at top level.")
                }

                oovToken = if (jsonObject.has("oov_token")) jsonObject.getString("oov_token") else null
                maxSequenceLength = if (jsonObject.has("max_sequence_length")) jsonObject.getInt("max_sequence_length") else 25 // Default to 25 if not found

                intentTokenizerData = TokenizerData(wordIndex, oovToken, maxSequenceLength)
                Log.d("ChatbotBrain", "Intent Tokenizer loaded. Vocab size: ${wordIndex.size}, Max Length: ${intentTokenizerData?.maxSequenceLength}")
            }

            // Load Intent Label Map
            context.assets.open(labelMapFileName).bufferedReader().use { reader ->
                val jsonString = reader.readText()
                val gson = Gson()
                val mapType = object : TypeToken<Map<String, String>>() {}.type
                val mapRaw: Map<String, String> = gson.fromJson(jsonString, mapType)
                intentLabelMap = mapRaw.mapKeys { it.key.toInt() }
                Log.d("ChatbotBrain", "Intent Label map loaded. Classes: ${intentLabelMap?.values?.joinToString()}")
            }

            // Load Responses Map
            context.assets.open(responsesMapFileName).bufferedReader().use { reader ->
                val jsonString = reader.readText()
                val gson = Gson()
                val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
                responsesMap = gson.fromJson(jsonString, mapType)
                Log.d("ChatbotBrain", "Responses map loaded. Number of tags: ${responsesMap?.size}")
            }

        } catch (e: Exception) {
            Log.e("ChatbotBrain", "Failed to load intent components: ${e.message}", e)
        }
    }

    // Function to tokenize input text
    private fun tokenizeInput(text: String, tokenizer: TokenizerData): IntArray? {
        if (tokenizer.wordIndex.isEmpty()) {
            Log.e("ChatbotBrain", "Tokenizer word index is empty.")
            return null
        }
        Log.d("ChatbotBrain", "tokenizeInput: Using maxSequenceLength = ${tokenizer.maxSequenceLength} for tokenizer.")

        val sanitizedText = text
            .replace("’", "'").replace("“", "\"").replace("”", "\"").replace("…", "...")
            .replace(Regex("[^a-zA-Z0-9\\s']"), "").lowercase(java.util.Locale.ROOT)

        val words = sanitizedText.split(Regex("\\s+"))
        val sequence = IntArray(tokenizer.maxSequenceLength) { 0 }

        var currentLength = 0
        for (word in words) {
            if (currentLength >= tokenizer.maxSequenceLength) break
            val index = tokenizer.wordIndex[word] ?: tokenizer.wordIndex[tokenizer.oovToken]
            if (index != null) {
                sequence[currentLength] = index
                currentLength++
            }
        }
        Log.d("ChatbotBrain", "Sanitized Input: '$sanitizedText'")
        Log.d("ChatbotBrain", "Tokenized input sequence: ${sequence.joinToString()}")
        return sequence
    }

    // Main function to get bot response
    fun getBotResponse(userInput: String): String {
        if (tfliteIntentInterpreter == null || intentTokenizerData == null || intentLabelMap == null || responsesMap == null) {
            Log.e("ChatbotBrain", "Chatbot components not initialized. Cannot get response.")
            return "I'm sorry, I'm not able to process messages right now. My brain is offline."
        }

        // --- 1. Predict Intent using TFLite Model ---
        val intentInputSequence = tokenizeInput(userInput, intentTokenizerData!!)
        val predictedIntentTag: String = if (intentInputSequence != null) {
            try {
                val inputBuffer = ByteBuffer.allocateDirect(intentTokenizerData!!.maxSequenceLength * 4)
                    .order(ByteOrder.nativeOrder())
                inputBuffer.asIntBuffer().put(intentInputSequence)
                inputBuffer.rewind()

                val outputBuffer = ByteBuffer.allocateDirect(intentLabelMap!!.size * 4)
                    .order(ByteOrder.nativeOrder())

                tfliteIntentInterpreter?.run(inputBuffer, outputBuffer)

                outputBuffer.rewind()
                val logits = FloatArray(intentLabelMap!!.size)
                outputBuffer.asFloatBuffer().get(logits)

                Log.d("ChatbotBrain", "Intent Raw Logits: ${logits.joinToString()}")
                val probabilities = softmax(logits)
                Log.d("ChatbotBrain", "Intent Probabilities: ${probabilities.joinToString()}")

                val predictedIndex = probabilities.indices.maxByOrNull { i -> probabilities[i] } ?: -1
                val tag = intentLabelMap?.get(predictedIndex) ?: "no-response" // Fallback to 'no-response' tag
                Log.d("ChatbotBrain", "User Input: '$userInput' -> Predicted Intent Tag: '$tag'")
                tag
            } catch (e: Exception) {
                Log.e("ChatbotBrain", "Error during TFLite Intent inference: ${e.message}", e)
                "no-response" // Fallback tag in case of inference error
            }
        } else {
            "no-response" // Fallback tag if tokenization fails
        }

        // --- 2. Get Response based on Predicted Intent ---
        val possibleResponses = responsesMap?.get(predictedIntentTag)
        return if (!possibleResponses.isNullOrEmpty()) {
            // Pick a random response from the list
            possibleResponses[Random.nextInt(possibleResponses.size)]
        } else {
            // Fallback if no responses found for the predicted tag
            Log.w("ChatbotBrain", "No responses found for tag: $predictedIntentTag. Falling back to generic.")
            "I'm here to listen. Could you tell me more about what's on your mind?"
        }
    }

    // Softmax activation function
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expValues = logits.map { exp(it - maxLogit) }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toFloatArray()
    }

    // Close interpreters
    fun close() {
        tfliteIntentInterpreter?.close()
        Log.d("ChatbotBrain", "TensorFlow Lite Intent interpreter closed.")
    }
}