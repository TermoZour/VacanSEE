package com.punchy.pmt.vacansee.searchJobs.httpRequests

class ReviewData(
    val employer_id: Int,
    val user_id: Int,
    val rating: Float,
    val title: String,
    val description: String
) {
    override fun toString(): String {
        return "{title: ${this.title}, rating: ${this.rating}}"
    }
}