package uk.co.kidsloop.features.regionAndLanguage.data

object RegionsAndLanguages {

    fun languagesList(): List<Language> {
        return listOf(
            Language("English", "en"),
            Language("Spanish", "es"),
            Language("Korean", "ko"),
            Language("Chinese", "zh"),
            Language("Vietnamese", "vi"),
            Language("Bahasa Indonesia", "id")
        )
    }

    fun regionsList(): List<String> {
        return listOf(
            "United Kingdom",
            "Sri Lanka",
            "South Korea",
            "India",
            "Vietnam",
            "Indonesia",
            "Thailand",
            "United States",
            "Pakistan",
        )
    }
}
