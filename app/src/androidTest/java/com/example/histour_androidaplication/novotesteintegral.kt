package com.example.histour_androidaplication

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PoiDetailNavigationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun navegarParaDetalhesPOI() {
        // Supondo que existe um POI com o nome "Museu XYZ"
        onView(withText("Torre dos Clérigos")).perform(click())

        // Verifica se o nome aparece no detalhe
        onView(withText("Torre dos Clérigos")).check(matches(isDisplayed()))
    }
}
