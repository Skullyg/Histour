package com.example.histour_androidaplication

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CreatePoiIntegrationTest {

    @Test
    fun testAbrirCreatePoiEInserirDados() {
        // Abre a Activity de criação de POI
        ActivityScenario.launch(CreatePoiActivity::class.java)

        // Preenche o nome e descrição
        onView(withId(R.id.editTextNome)).perform(replaceText("POI Teste"), closeSoftKeyboard())
        onView(withId(R.id.editTextDescricao)).perform(replaceText("Descrição de teste"), closeSoftKeyboard())


        // Seleciona um item no spinner
        onView(withId(R.id.spinner_tipo_poi)).perform(click())
        onView(withText("Museu")).perform(click())

        // Clica no botão "Salvar POI"
        onView(withId(R.id.btnSalvar)).perform(click())

        // Aqui poderias verificar um Toast ou mudança de tela,
        // mas como não usamos `intended()` por agora, só testamos fluxo de UI
    }
}
