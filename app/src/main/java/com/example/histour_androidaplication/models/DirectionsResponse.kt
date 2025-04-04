package com.example.histour_androidaplication.models

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<Route>
)

data class Route(
    @SerializedName("legs") val legs: List<Leg>,
    @SerializedName("overview_polyline") val overviewPolyline: OverviewPolyline
)

data class Leg(
    @SerializedName("steps") val steps: List<Step>,
    @SerializedName("duration") val duration: Duration
)

data class Step(
    @SerializedName("start_location") val startLocation: LocationData,
    @SerializedName("end_location") val endLocation: LocationData,
    @SerializedName("polyline") val polyline: OverviewPolyline
)

data class LocationData(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class OverviewPolyline(
    @SerializedName("points") val points: String
)

data class Duration(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int
)