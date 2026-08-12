package com.gamss.android.data.safety

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.inject.Provider

class FirestoreRiskLexiconDataSourceTest {

    @Test
    fun `원격 사전은 서버 소스로만 조회한다`() = runTest {
        val firestore = mockk<FirebaseFirestore>()
        val collection = mockk<CollectionReference>()
        val document = mockk<DocumentReference>()
        val snapshot = mockk<DocumentSnapshot>()
        val provider = mockk<Provider<FirebaseFirestore>>()

        every { provider.get() } returns firestore
        every { firestore.collection("app_config") } returns collection
        every { collection.document("risk_lexicon") } returns document
        every { document.get(Source.SERVER) } returns Tasks.forResult(snapshot)
        every { snapshot.exists() } returns true
        every { snapshot.getLong("version") } returns 3L
        every { snapshot.get("terms") } returns listOf(mapOf("term" to "죽고싶", "level" to "CRITICAL"))
        every { snapshot.get("safePhrases") } returns listOf("죽고싶지않")
        every { snapshot.get("agencies") } returns listOf(
            mapOf(
                "id" to "kr-109",
                "name" to "자살예방 상담전화",
                "description" to "24시간 무료 전문 상담",
                "phoneNumber" to "109",
                "priority" to 1L,
            ),
        )

        val dto = FirestoreRiskLexiconDataSource(provider).fetch()

        assertEquals(3, dto?.version)
        assertEquals(listOf("죽고싶"), dto?.terms?.map { it.term })
        verify(exactly = 1) { document.get(Source.SERVER) }
        verify(exactly = 0) { document.get() }
    }
}
