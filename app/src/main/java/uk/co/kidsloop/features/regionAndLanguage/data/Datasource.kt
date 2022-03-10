package uk.co.kidsloop.features.regionAndLanguage.data

import android.content.Context

class Datasource(val context: Context) {
    fun getLanguageList(): List<Language> {
        return languagesList()
    }

    fun getRegionsList(): List<String> {
        return regionsList()
    }
}
