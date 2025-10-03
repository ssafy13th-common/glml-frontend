package com.ssafy.a705.tracking

object TrackingNavRoutes {
    const val Graph = "tracking_graph"  // 하위 그래프 식별용 ID
    const val Tracking = "tracking_real_time"
    const val Update = "tracking_update"
    const val UpdateWithArg = "$Update/{trackingId}"
    const val History = "tracking_history"
}
