package com.gamss.android.app.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/** debug 전용 화면의 release 대응물. 같은 이름으로 비어 있어야 main 소스셋이 양쪽에서 컴파일된다. */
val debugTopLevelDestinations = emptyList<TopLevelDestination>()

@Suppress("UnusedParameter")
fun EntryProviderScope<NavKey>.addDebugEntries(navigator: Navigator) = Unit
