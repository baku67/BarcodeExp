package com.example.barcode.features.bootstrap

class TimelineRepository {
    suspend fun fetchTimelineIntro3Days(): Pair<IntArray, IntArray> {
        // TODO: appeler ton endpoint timeline (3 jours)
        // Retour attendu :
        val expired = intArrayOf(1, 0)
        val soon = intArrayOf(2, 3)
        return expired to soon
    }
}
