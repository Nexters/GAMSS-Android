package com.gamss.android.data

import ai.onnxruntime.OrtEnvironment
import android.os.Build
import android.system.Os
import android.system.OsConstants
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.TensorFlowLite

/**
 * 4KB 정렬된 .so 는 16KB 페이지 기기에서 dlopen 자체가 실패한다. APK 정적 검사의 최종 확인용.
 * 16KB 환경은 `google_apis_playstore_ps16k` 시스템 이미지 AVD 로 만든다.
 */
@RunWith(AndroidJUnit4::class)
class NativeLibraryLoadTest {

    @Test
    fun nativeLibrariesLoadOn16kPageDevice() {
        val pageSize = Os.sysconf(OsConstants._SC_PAGESIZE)
        Log.i(TAG, "pageSize=$pageSize model=${Build.MODEL} api=${Build.VERSION.SDK_INT}")
        assumeTrue("16KB 페이지 기기가 아니라 건너뜁니다(pageSize=$pageSize)", pageSize == PAGE_SIZE_16KB)

        assertNotNull(OrtEnvironment.getEnvironment())
        assertTrue("LiteRT 런타임 버전이 비어 있습니다", TensorFlowLite.runtimeVersion().isNotEmpty())
    }

    private companion object {
        const val TAG = "NativeLibraryLoadTest"
        const val PAGE_SIZE_16KB = 16384L
    }
}
