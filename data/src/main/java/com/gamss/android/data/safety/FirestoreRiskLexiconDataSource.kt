package com.gamss.android.data.safety

import com.gamss.android.data.safety.model.RiskLexiconDto
import com.gamss.android.data.safety.model.RiskTermDto
import com.gamss.android.data.safety.model.SupportAgencyDto
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사전 전체를 문서 하나에 담아 읽기 1회로 끝낸다. 쓰기는 콘솔에서만 하고 클라이언트는 읽기만 한다.
 */
@Singleton
internal class FirestoreRiskLexiconDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun fetch(): RiskLexiconDto? =
        firestore.collection(COLLECTION)
            .document(DOCUMENT)
            .get()
            .await()
            .takeIf { it.exists() }
            ?.toDto()

    private fun DocumentSnapshot.toDto(): RiskLexiconDto = RiskLexiconDto(
        version = getLong(FIELD_VERSION)?.toInt() ?: 0,
        terms = objectList(FIELD_TERMS).map {
            RiskTermDto(term = it.string(FIELD_TERM), level = it.string(FIELD_LEVEL))
        },
        safePhrases = (get(FIELD_SAFE_PHRASES) as? List<*>).orEmpty().filterIsInstance<String>(),
        agencies = objectList(FIELD_AGENCIES).map {
            SupportAgencyDto(
                id = it.string(FIELD_ID),
                name = it.string(FIELD_NAME),
                description = it.string(FIELD_DESCRIPTION),
                phoneNumber = it[FIELD_PHONE_NUMBER] as? String,
                url = it[FIELD_URL] as? String,
                priority = (it[FIELD_PRIORITY] as? Number)?.toInt() ?: Int.MAX_VALUE,
            )
        },
    )

    private fun DocumentSnapshot.objectList(field: String): List<Map<*, *>> =
        (get(field) as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()

    private fun Map<*, *>.string(key: String): String = (this[key] as? String).orEmpty()

    private companion object {
        const val COLLECTION = "app_config"
        const val DOCUMENT = "risk_lexicon"

        const val FIELD_VERSION = "version"
        const val FIELD_TERMS = "terms"
        const val FIELD_SAFE_PHRASES = "safePhrases"
        const val FIELD_AGENCIES = "agencies"
        const val FIELD_TERM = "term"
        const val FIELD_LEVEL = "level"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_PHONE_NUMBER = "phoneNumber"
        const val FIELD_URL = "url"
        const val FIELD_PRIORITY = "priority"
    }
}
