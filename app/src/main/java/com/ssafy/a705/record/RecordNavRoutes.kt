package com.ssafy.a705.record

// object : 싱글톤 객체
object RecordNavRoutes {
    const val List = "record_list"
    const val ListWithArg = "$List/{locationId}"
    const val Map = "record_map"
    const val Detail = "record_detail"
    const val DetailWithArg = "$Detail/{recordId}"
    const val Create = "record_create"
    const val Update = "record_update"
    const val UpdateWithArg = "$Update/{recordId}"
}