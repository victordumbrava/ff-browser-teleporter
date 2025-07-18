/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import android.graphics.Bitmap
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.concept.awesomebar.AwesomeBar
import java.lang.Integer.MAX_VALUE
import java.util.UUID

/**
 * [AwesomeBar.SuggestionProvider] implementation that provides suggestions based on the search engine list.
 *
 * @property searchEnginesList a search engine list used to search
 * @property selectShortcutEngine the use case invoked to temporarily change engine used for search
 * @property description the description to be shown for the suggestion(s), same description for all
 * @property searchIcon the icon to be shown for the suggestion(s), same icon for all
 * @property maxSuggestions the maximum number of suggestions to be provided
 * @property charactersThreshold the minimum typed characters used to match to a search engine name
 * @property titleFormatter a function to format the title of the suggestion, typically used to apply
 */
class SearchEngineSuggestionProvider(
    private val searchEnginesList: List<SearchEngine>,
    private val selectShortcutEngine: (engine: SearchEngine) -> Unit,
    private val description: String?,
    private val searchIcon: Bitmap?,
    internal val maxSuggestions: Int = DEFAULT_MAX_SUGGESTIONS,
    internal val charactersThreshold: Int = DEFAULT_CHARACTERS_THRESHOLD,
    private val titleFormatter: (String) -> String,
) : AwesomeBar.SuggestionProvider {
    override val id: String = UUID.randomUUID().toString()

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        if (text.isEmpty() || text.length < charactersThreshold) {
            return emptyList()
        }

        val suggestions = searchEnginesList
            .filter { it.name.startsWith(text, true) }.take(maxSuggestions)

        return if (suggestions.isNotEmpty()) {
            suggestions.into()
        } else {
            return emptyList()
        }
    }

    /**
     *  Generates a list of [AwesomeBar.Suggestion] from a [SearchEngine] list
     */
    private fun List<SearchEngine>.into(): List<AwesomeBar.Suggestion> {
        return this.map {
            AwesomeBar.Suggestion(
                provider = this@SearchEngineSuggestionProvider,
                id = it.id,
                icon = searchIcon,
                flags = setOf(AwesomeBar.Suggestion.Flag.BOOKMARK),
                title = titleFormatter(it.name),
                description = description,
                onSuggestionClicked = { selectShortcutEngine(it) },
                score = MAX_VALUE,
            )
        }
    }

    companion object {
        internal const val DEFAULT_MAX_SUGGESTIONS = 1
        internal const val DEFAULT_CHARACTERS_THRESHOLD = 2
    }
}
