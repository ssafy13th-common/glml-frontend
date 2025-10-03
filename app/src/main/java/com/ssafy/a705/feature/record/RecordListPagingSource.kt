package com.ssafy.a705.feature.record

import androidx.paging.PagingSource
import androidx.paging.PagingState

class RecordPagingSource(
    private val api: RecordApi,
    private val pageSize: Int,
    private val locationCode: Int?
) : PagingSource<Long, RecordListItem>() {

    override fun getRefreshKey(state: PagingState<Long, RecordListItem>): Long? {
        // 첫 로딩으로 새로고침
        return null
    }

    override suspend fun load(
        params: LoadParams<Long>
    ): LoadResult<Long, RecordListItem> = try {
        val cursor = params.key
        val size = pageSize

        val resp = api.getDiaries(cursorId = cursor, size = size, locationCode = locationCode)
        resp.message?.let { throw Exception(it) }

        val itemsDto = resp.data?.diaries.orEmpty()

        val nextKey = itemsDto.lastOrNull()?.id
        LoadResult.Page(data = itemsDto, prevKey = null, nextKey = nextKey)
    } catch (e: Exception) {
        LoadResult.Error(e)
    }
}