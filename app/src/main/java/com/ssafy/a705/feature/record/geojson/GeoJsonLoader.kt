package com.ssafy.a705.feature.record.geojson

import android.content.Context
import java.io.InputStreamReader

object GeoJsonLoader {
    fun loadFromAssets(context: Context, fileName: String): String =
        // /app/src/main/assets/'fileName'을 InputStream으로 열어서
        context.assets.open(fileName).use { stream ->
            // UTF-8 인코딩으로 파일 바이트를 문자로 변환해 String으로 반환
            InputStreamReader(stream, Charsets.UTF_8).readText()
        }
}