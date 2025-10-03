package com.ssafy.a705.domain.group.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDeleteViewModel @Inject constructor(
    private val repository: GroupRepository
) : ViewModel() {

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    fun deleteGroup(
        groupId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        _isDeleting.value = true
        runCatching { 
            repository.deleteGroup(groupId) 
        }
        .onSuccess {
            _isDeleting.value = false
            onSuccess()
        }
        .onFailure {
            _isDeleting.value = false
            onError(it.message ?: "삭제 실패")
        }
    }
}
