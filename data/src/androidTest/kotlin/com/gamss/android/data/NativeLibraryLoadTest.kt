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
 * 16KB 페이지 기기에서 네이티브 라이브러리가 실제로 dlopen 되는지 확인한다.
 *
 * 4KB 정렬된 .so 는 16KB 페이지로 구성된 기기에서 로드 자체가 실패한다. APK 정적 검사(ELF p_align)로도
 * 잡히지만, 실제 로더를 태워보는 것이 최종 확인이다. 4KB 기기에서는 검증 의미가 없어 건너뛴다.
 *
 * 16KB 환경은 `google_apis_playstore_ps16k` 시스템 이미지 AVD 로 만들 수 있다.
 */
@RunWith(AndroidJUnit4::class)
class NativeLibraryLoadTest {

    @Test
    fun nativeLibrariesLoadOn16kPageDevice() {
        val pageSize = Os.sysconf(OsConstants._SC_PAGESIZE)
        Log.i(TAG, "pageSize=$pageSize model=${Build.MODEL} api=${Build.VERSION.SDK_INT}")
        assumeTrue("16KB 페이지 기기가 아니라 건너뜁니다(pageSize=$pageSize)", pageSize == PAGE_SIZE_16KB)

        // libonnxruntime.so + libonnxruntime4j_jni.so
        assertNotNull(OrtEnvironment.getEnvironment())
        // libLiteRt.so
        assertTrue("LiteRT 런타임 버전이 비어 있습니다", TensorFlowLite.runtimeVersion().isNotEmpty())
    }

    private companion object {
        const val TAG = "NativeLibraryLoadTest"
        const val PAGE_SIZE_16KB = 16384L
    }
}
