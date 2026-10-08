package com.example.data.model

object CategoryConstants {
    val defaultCategories = listOf(
        "Salary & Compensation",
        "Food & Dining",
        "Groceries",
        "Utilities & Bills",
        "Housing & Rent",
        "Transportation & Fuel",
        "Shopping & Supplies",
        "Health & Medical",
        "Entertainment & Leisure",
        "Freelance & Business",
        "Investment & Savings",
        "Education & Courses",
        "Travel & Holidays",
        "Personal Care",
        "Gifts & Donations",
        "General / Other"
    )

    val defaultExpenseCategories: List<String> = defaultCategories
    val defaultIncomeCategories: List<String> = defaultCategories

    fun getCategoryIcon(category: String): String {
        return when (category.lowercase()) {
            "salary & compensation", "salary" -> "attach_money"
            "freelance & business", "freelance", "business" -> "laptop"
            "investment & savings", "investment" -> "trending_up"
            "gifts & donations", "bonus", "gift" -> "card_giftcard"
            "food & dining", "food" -> "restaurant"
            "groceries" -> "shopping_cart"
            "utilities & bills", "utilities" -> "bolt"
            "housing & rent", "housing" -> "home"
            "transportation & fuel", "transportation", "travel", "travel & holidays" -> "flight"
            "shopping & supplies", "shopping" -> "shopping_bag"
            "health & medical", "health" -> "local_hospital"
            "entertainment & leisure", "entertainment" -> "movie"
            "education & courses", "education" -> "school"
            else -> "receipt_long"
        }
    }
}
