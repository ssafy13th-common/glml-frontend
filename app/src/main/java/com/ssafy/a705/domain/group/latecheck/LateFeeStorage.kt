package com.ssafy.a705.domain.group.latecheck

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 지각비 정보 저장 및 조회 유틸리티
 * - SharedPreferences를 사용하여 지각비 정보를 로컬에 저장
 * - MemberScreen에서 지각비 정보를 조회할 수 있도록 제공
 */
@Singleton
class LateFeeStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("late_fees", Context.MODE_PRIVATE)

    /**
     * 그룹의 지각비 정보 저장
     * @param groupId 그룹 ID
     * @param lateFees 멤버별 지각비 정보 (email to fee)
     */
    fun saveLateFees(groupId: Long, lateFees: Map<String, Int>) {
        lateFees.forEach { (email, fee) ->
            prefs.edit().putInt("${groupId}_${email}", fee).apply()
        }

        // 그룹의 지각비 확정 상태 저장
        prefs.edit().putBoolean("${groupId}_finalized", true).apply()
    }

    /**
     * 특정 멤버의 지각비 조회
     * @param groupId 그룹 ID
     * @param email 멤버 이메일
     * @return 지각비 (확정되지 않은 경우 0)
     */
    fun getLateFee(groupId: Long, email: String): Int {
        return if (isLateFeeFinalized(groupId)) {
            prefs.getInt("${groupId}_${email}", 0)
        } else {
            0
        }
    }

    /**
     * 그룹의 모든 지각비 정보 조회
     * @param groupId 그룹 ID
     * @return 멤버별 지각비 정보 (확정되지 않은 경우 빈 맵)
     */
    fun getAllLateFees(groupId: Long): Map<String, Int> {
        if (!isLateFeeFinalized(groupId)) {
            return emptyMap()
        }

        val lateFees = mutableMapOf<String, Int>()
        val allPrefs = prefs.all

        allPrefs.forEach { (key, value) ->
            if (key.startsWith("${groupId}_") && key != "${groupId}_finalized") {
                val email = key.substring("${groupId}_".length)
                if (value is Int) {
                    lateFees[email] = value
                }
            }
        }

        return lateFees
    }

    /**
     * 지각비 확정 여부 확인
     * @param groupId 그룹 ID
     * @return 확정 여부
     */
    fun isLateFeeFinalized(groupId: Long): Boolean {
        return prefs.getBoolean("${groupId}_finalized", false)
    }

    /**
     * 그룹의 지각비 정보 삭제
     * @param groupId 그룹 ID
     */
    fun clearLateFees(groupId: Long) {
        val allPrefs = prefs.all
        val editor = prefs.edit()

        allPrefs.forEach { (key, _) ->
            if (key.startsWith("${groupId}_")) {
                editor.remove(key)
            }
        }

        editor.apply()
    }
}
