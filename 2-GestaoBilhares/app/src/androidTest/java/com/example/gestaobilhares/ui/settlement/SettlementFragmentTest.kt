package com.example.gestaobilhares.ui.settlement

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.gestaobilhares.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettlementFragmentTest {
    @get:Rule
    val activityRule = ActivityTestRule(com.example.gestaobilhares.MainActivity::class.java)

    @Test
    fun testMesaCardDisplaysCorrectly() {
        // Verifica se o RecyclerView exibe o número da mesa e o valor de fichas inicial
        onView(withId(R.id.rvMesasAcerto))
            .check(matches(hasDescendant(withText("Mesa 10")))) // Exemplo: número da mesa
        onView(withId(R.id.etRelogioInicial))
            .check(matches(withText("1500"))) // Exemplo: relógio inicial cadastrado
    }
} 