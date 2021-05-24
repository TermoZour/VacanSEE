package com.punchy.pmt.vacansee.httpRequests

class FinanceData (
    val date: String,
    val sharePrice: Double
) {
    override fun toString(): String {
        return "{date: ${this.date}, sharePrice: ${this.sharePrice}}"
    }
}