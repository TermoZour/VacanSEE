package com.punchy.pmt.vacansee.searchJobs.httpRequests

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.punchy.pmt.vacansee.searchJobs.Job
import com.punchy.pmt.vacansee.searchJobs.JobDetails
import java.net.URL
import javax.net.ssl.HttpsURLConnection


var route = "https://3f15331b7bf8.ngrok.io/api"

fun getJobs(): MutableList<Job> {
    var jobsList = mutableListOf<Job>()
    val gson = Gson() // JSON converter

    val url = URL("$route/jobs?search=developer")

    with(url.openConnection() as HttpsURLConnection) {
        requestMethod = "GET"  // optional default is GET
        println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

        val parseTemplate = object :
            TypeToken<MutableList<Job>>() {}.type //https://bezkoder.com/kotlin-parse-json-gson/
        inputStream.bufferedReader().use {
            it.lines().forEach { line ->
                println(line)
                jobsList = gson.fromJson(line, parseTemplate)
                jobsList.forEachIndexed { idx, tut -> println("> Item ${idx}:\n${tut}") }
            }
        }

        return jobsList
    }
}

fun getJobDetails(employerName: String, employerId: Int, jobId: Int): JobDetails {
    val gson = Gson()

    val url = URL("$route/moreDetails?empName=$employerName&empID=$employerId&jobID=$jobId")

    with(url.openConnection() as HttpsURLConnection) {
        requestMethod = "GET"  // optional default is GET
        println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

        val parseTemplate = object :
            TypeToken<MutableList<Job>>() {}.type //https://bezkoder.com/kotlin-parse-json-gson/
        inputStream.bufferedReader().use {
            it.lines().forEach { line ->
                println(line)
//                jobsList = gson.fromJson(line, parseTemplate)
//                jobsList.forEachIndexed { idx, tut -> println("> Item ${idx}:\n${tut}") }
            }
        }
    }

    // TODO - add proper details
    return JobDetails(0,0,0.0f)
}