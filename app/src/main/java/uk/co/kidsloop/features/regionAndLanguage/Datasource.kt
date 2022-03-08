package uk.co.kidsloop.features.regionAndLanguage

import android.content.Context

class Datasource(val context: Context) {
    fun getLanguageList(): List<String> {
        return flowerList()
    }
}
