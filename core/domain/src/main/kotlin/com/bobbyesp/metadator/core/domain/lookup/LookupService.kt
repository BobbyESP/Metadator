package com.bobbyesp.metadator.core.domain.lookup

import com.bobbyesp.metadator.core.domain.settings.SettingsRepository
import com.bobbyesp.metadator.lookup.api.LookupCandidate
import com.bobbyesp.metadator.lookup.api.LookupQuery
import com.bobbyesp.metadator.lookup.api.LookupResult
import com.bobbyesp.metadator.lookup.api.MetadataProvider
import com.bobbyesp.metadator.lookup.api.rank
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Asks every enabled provider at once and merges their answers into one ranked list. One provider
 * failing does not hide the others' results; the outcome is only an error when all of them fail.
 */
class LookupService(
    private val providers: List<MetadataProvider>,
    private val settings: SettingsRepository,
) {
    val availableProviders: List<MetadataProvider>
        get() = providers

    suspend fun search(query: LookupQuery): LookupOutcome {
        val enabled = settings.settings.first().enabledProviders
        val active = providers.filter { it.id in enabled }
        if (active.isEmpty()) return LookupOutcome.NoProviders
        if (query.isBlank) return LookupOutcome.Results(emptyList(), failedProviders = emptyList())

        val results = coroutineScope {
            active.map { provider -> async { provider to provider.search(query) } }.awaitAll()
        }
        val candidates = results.flatMap { (_, result) ->
            (result as? LookupResult.Success)?.candidates.orEmpty()
        }
        val failed = results.filter { (_, result) -> result !is LookupResult.Success }

        return when {
            failed.size == results.size && results.all { it.second == LookupResult.Offline } ->
                LookupOutcome.Offline
            failed.size == results.size && results.any { it.second == LookupResult.RateLimited } ->
                LookupOutcome.RateLimited
            failed.size == results.size -> LookupOutcome.Failed
            else ->
                LookupOutcome.Results(
                    candidates = rank(query, candidates),
                    failedProviders = failed.map { it.first.displayName },
                )
        }
    }
}

sealed interface LookupOutcome {
    data class Results(val candidates: List<LookupCandidate>, val failedProviders: List<String>) :
        LookupOutcome

    data object NoProviders : LookupOutcome

    data object Offline : LookupOutcome

    data object RateLimited : LookupOutcome

    data object Failed : LookupOutcome
}
