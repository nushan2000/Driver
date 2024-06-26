package com.example.driver

data class Passenger(
    val name: String ,
    val position: String,
    val pickup: String ,
    val drop: String
) {
    // No-argument constructor
    constructor() : this("", "", "", "")
}

data class Request(
    val id: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val reason: String,
    val section: String,
    val vehicle: String,
    val departureLocation: String,
    val destination: String,
    val comeBack: Boolean,
    val distance: Double,  // Ensure this matches the type in Firestore
    val applier: String,
    val applyDate: String,
    val passengers: List<Passenger>
) {
    // No-argument constructor
    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        false,
        0.0,
        "",
        "",
        listOf()
    )
}
