package com.punchy.pmt.vacansee.httpRequests

class DetailedJob(
    val jobDetails: Job,
    val financeData: MutableList<FinanceData>,
    val reviewData: MutableList<ReviewData>
) {
    //constructor() : this(Job(), mutableListOf(), mutableListOf())
}