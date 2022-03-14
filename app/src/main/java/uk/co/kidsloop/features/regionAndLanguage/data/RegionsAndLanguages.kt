package uk.co.kidsloop.features.regionAndLanguage.data

object RegionsAndLanguages {

    fun languagesList(): List<Language> {
        return listOf(
            Language("English", "en"),
            Language("Spanish", "es"),
            Language("Korean", "ko"),
            Language("Chinese", "zh"),
            Language("Vietnamese", "vi"),
            Language("Bahasa Indonesia", "id"),
            Language("Thai", "th")
        )
    }

    fun regionsList(): List<Region> {
        return listOf(
            Region("United Kingdom"),
            Region("Sri Lanka"),
            Region("South Korea"),
            Region("India"),
            Region("Vietnam"),
            Region("Indonesia"),
            Region("Thailand"),
            Region("United States"),
            Region("Pakistan"),
        )
    }
}
