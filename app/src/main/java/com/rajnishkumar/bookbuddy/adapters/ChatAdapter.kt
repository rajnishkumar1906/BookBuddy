package com.rajnishkumar.bookbuddy.adapters

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.rajnishkumar.bookbuddy.R
import com.rajnishkumar.bookbuddy.models.ChatMessage
import com.rajnishkumar.bookbuddy.models.Quiz

class ChatAdapter(
    private val onQuizChoice: (Boolean) -> Unit,           // true = full quiz, false = single question
    private val onQuizSubmitted: (ChatMessage, Quiz, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()
    private val gson = Gson()

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_CHOICE = 1
        private const val VIEW_TYPE_QUIZ = 2
    }

    fun setMessages(newList: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newList)
        notifyDataSetChanged()
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    /**
     * Updates the text of the last message in the list.
     * Useful for word-by-word streaming effect.
     */
    fun updateLastMessage(text: String) {
        if (messages.isNotEmpty()) {
            messages[messages.size - 1].message = text
            notifyItemChanged(messages.size - 1)
        }
    }

    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].role) {
            "choice" -> VIEW_TYPE_CHOICE
            "quiz" -> VIEW_TYPE_QUIZ
            else -> VIEW_TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CHOICE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_choice, parent, false)
                ChoiceViewHolder(view, onQuizChoice)
            }
            VIEW_TYPE_QUIZ -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_quiz, parent, false)
                QuizViewHolder(view, onQuizSubmitted)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_text, parent, false)
                TextMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is TextMessageViewHolder -> holder.bind(msg)
            is ChoiceViewHolder -> holder.bind()
            is QuizViewHolder -> holder.bind(msg)
        }
    }

    override fun getItemCount() = messages.size

    private fun clean(text: String): String = text.replace("*", "").replace("#", "").trim()

    // ====================== Normal Text Message ======================
    inner class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvChatMessage)

        fun bind(message: ChatMessage) {
            tvMessage.text = clean(message.message)
            if (message.role == "user") {
                tvMessage.setBackgroundResource(R.drawable.chat_bubble_user)
                tvMessage.setTextColor(Color.WHITE)
            } else {
                tvMessage.setBackgroundResource(R.drawable.chat_bubble_ai)
                tvMessage.setTextColor(Color.BLACK)
            }
        }
    }

    // ====================== Choice (Single vs Full Quiz) ======================
    inner class ChoiceViewHolder(itemView: View, private val onChoice: (Boolean) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val btnSingle: MaterialButton = itemView.findViewById(R.id.btnSingleQuestion)
        private val btnFull: MaterialButton = itemView.findViewById(R.id.btnFullQuiz)

        fun bind() {
            btnSingle.setOnClickListener { onChoice(false) }
            btnFull.setOnClickListener { onChoice(true) }
        }
    }

    // ====================== Quiz ViewHolder ======================
    inner class QuizViewHolder(itemView: View, private val onSubmitted: (ChatMessage, Quiz, Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvQuizTitle: TextView = itemView.findViewById(R.id.tvQuizTitle)
        private val container: LinearLayout = itemView.findViewById(R.id.containerQuestions)
        private val btnSubmit: Button = itemView.findViewById(R.id.btnSubmitQuiz)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)

        @SuppressLint("SetTextI18n")
        fun bind(chatMessage: ChatMessage) {
            val quiz = try {
                gson.fromJson(chatMessage.quizJson, Quiz::class.java)
            } catch (e: Exception) {
                null
            } ?: return

            tvQuizTitle.text = clean(chatMessage.message)
            container.removeAllViews()

            val isSubmitted = quiz.questions.all { it.isSubmitted }

            btnSubmit.visibility = if (isSubmitted) View.GONE else View.VISIBLE
            tvScore.visibility = if (isSubmitted) View.VISIBLE else View.GONE

            var totalScore = 0

            quiz.questions.forEachIndexed { qIndex, question ->
                val qView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_quiz_question, container, false)

                val tvQ: TextView = qView.findViewById(R.id.tvQuestion)
                tvQ.text = "${qIndex + 1}. ${clean(question.question)}"

                val buttons = listOf<MaterialButton>(
                    qView.findViewById(R.id.btnOption1),
                    qView.findViewById(R.id.btnOption2),
                    qView.findViewById(R.id.btnOption3),
                    qView.findViewById(R.id.btnOption4)
                )

                buttons.forEachIndexed { optIndex, btn ->
                    val optionText = clean(question.options[optIndex])
                    btn.text = optionText
                    btn.isEnabled = !isSubmitted

                    if (isSubmitted) {
                        when {
                            optionText == clean(question.correctAnswer) -> {
                                setButtonCorrect(btn)
                                if (question.selectedAnswer == question.correctAnswer) totalScore++
                            }
                            optionText == clean(question.selectedAnswer ?: "") -> {
                                setButtonWrong(btn)
                            }
                            else -> {
                                btn.alpha = 0.5f
                                setButtonNeutral(btn)
                            }
                        }
                    } else {
                        if (optionText == clean(question.selectedAnswer ?: "")) {
                            setButtonSelected(btn)
                        } else {
                            setButtonDefault(btn)
                        }

                        btn.setOnClickListener {
                            if (isSubmitted) return@setOnClickListener
                            question.selectedAnswer = optionText
                            buttons.forEach { b ->
                                if (clean(b.text.toString()) == optionText) setButtonSelected(b)
                                else setButtonDefault(b)
                            }
                        }
                    }
                }
                container.addView(qView)
            }

            if (isSubmitted) {
                tvScore.text = "Score: $totalScore / ${quiz.questions.size}"
            }

            btnSubmit.setOnClickListener {
                if (quiz.questions.any { it.selectedAnswer == null }) {
                    Toast.makeText(itemView.context, "Please answer all questions!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                quiz.questions.forEach { it.isSubmitted = true }
                chatMessage.quizJson = gson.toJson(quiz)
                onSubmitted(chatMessage, quiz, totalScore)
                bind(chatMessage) 
            }
        }

        private fun setButtonDefault(btn: MaterialButton) {
            btn.backgroundTintList = null
            // Fixed: Changed from R.id.primary to R.color.primary
            btn.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.primary))
            btn.setTextColor(ContextCompat.getColor(btn.context, R.color.primary))
        }

        private fun setButtonSelected(btn: MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.primary))
            btn.setTextColor(Color.WHITE)
            btn.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.primary_dark))
        }

        private fun setButtonCorrect(btn: MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.correct_green))
            btn.setTextColor(Color.WHITE)
        }

        private fun setButtonWrong(btn: MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.wrong_red))
            btn.setTextColor(Color.WHITE)
        }

        private fun setButtonNeutral(btn: MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.gray_200))
            btn.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(btn.context, R.color.gray_300))
            btn.setTextColor(ContextCompat.getColor(btn.context, R.color.gray_600))
        }
    }
}
