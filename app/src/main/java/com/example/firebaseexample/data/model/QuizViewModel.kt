package com.example.firebaseexample.data.model

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.firebaseexample.data.repository.QuizRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QuizViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // 뷰모델에 결과페이지에서 전송하는 정보를 업데이트한다.
    private val _savedResults = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val savedResults: StateFlow<List<Map<String, Any>>> = _savedResults

    fun setSavedResults(results: List<Map<String, Any>>) {
        _savedResults.value = results
        println("Saved results to ViewModel: $results")
    }

    // 뷰모델에 저장해둔 값을 기준으로 FireStore에서 문제 정보를 조회한다.
    private val repository = QuizRepository()
    private val _problemQuestion = MutableStateFlow<String?>(null)
    val problemQuestion: StateFlow<String?> = _problemQuestion

    fun loadProblemQuestion(categoryName: String, quizId: String) {
        repository.fetchProblemQuestion(
            categoryName = categoryName,
            quizId = quizId,
            onSuccess = { question ->
                _problemQuestion.value = question.toString()
                println("Problem question loaded: $question")
            },
            onError = { exception ->
                println("Error loading problem question: ${exception.message}")
            }
        )
    }
    // 여기까지

    var quizzes = mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set

    var score = mutableStateOf(0)
        private set

    var solvedQuizzesNum = mutableStateOf(0)
        private set

    var totalQuestions = mutableStateOf(0)
        private set

    private val quizResults = mutableListOf<Map<String, Any>>()

    fun updateDB(categoryName: String, quizId: String, isCorrect: Boolean){
        val result = mapOf(
            "categoryName" to categoryName,
            "quizId" to quizId,
            "isCorrect" to isCorrect
        )
        quizResults.add(result)
    }

    fun getAllResults() : List<Map<String, Any>>{
        return quizResults.toList()
    }
    // 데이터 소스 설정 (오늘의 퀴즈 또는 카테고리별 퀴즈)
    fun fetchQuizzes(source: QuizSource, categoryId: String? = null) {
        when (source) {
            QuizSource.TODAY -> fetchTodayQuizzes()
            QuizSource.CATEGORY -> fetchCategoryQuizzes(categoryId ?: "")
        }
    }

    private fun fetchTodayQuizzes() {
        val today = getCurrentDate()
        println(today)
        db.collection("todayQuiz").document(today).get()
            .addOnSuccessListener { document ->
                val fetchedQuizzes = document.get("problems") as? List<Map<String, Any>> ?: emptyList()
                println(fetchedQuizzes)
                quizzes.value = fetchedQuizzes
                totalQuestions.value = fetchedQuizzes.size
            }
    }

    private fun fetchCategoryQuizzes(categoryId: String) {
        println("Category ID: $categoryId")
        db.collection("quizzes").document(categoryId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // `problems` 배열 필드 가져오기
                    val fetchedQuizzes = document.get("problems") as? List<Map<String, Any>> ?: emptyList()
                    println("Fetched Quizzes: $fetchedQuizzes")

                    // 상태 업데이트
                    quizzes.value = fetchedQuizzes
                    totalQuestions.value = fetchedQuizzes.size
                } else {
                    println("Document does not exist.")
                }
            }
            .addOnFailureListener { exception ->
                println("Error fetching quizzes: ${exception.message}")
            }
    }


    fun updateSolvedQuzzesNum(){
        solvedQuizzesNum.value++
    }

    fun updateScore(isCorrect: Boolean) {
        if (isCorrect) {
            score.value++
        }
    }

    fun resetQuizState() {
        quizzes.value = emptyList()
        score.value = 0
        totalQuestions.value = 0
        solvedQuizzesNum.value = 0
    }

    private fun getCurrentDate(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return "$year-$month-$day" // YYYY-MM-DD 형식
    }
}

// 퀴즈 소스 타입 정의
enum class QuizSource {
    TODAY,
    CATEGORY
}
