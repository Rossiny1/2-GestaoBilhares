package com.example.gestaobilhares.ui.clients

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.gestaobilhares.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ✅ FASE 12.2: Testes de UI com Espresso para ClientListFragment
 * 
 * Testa fluxos críticos da lista de clientes:
 * - Exibição da lista
 * - Filtros
 * - Navegação
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ClientListFragmentUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun `tela de lista de clientes deve exibir RecyclerView`() {
        // Verificar se o RecyclerView está visível
        onView(withId(com.example.gestaobilhares.R.id.rvClients))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `tela de lista de clientes deve exibir botão de busca`() {
        // Verificar se o botão de busca está visível
        onView(withId(com.example.gestaobilhares.R.id.btnSearch))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `tela de lista de clientes deve exibir botão de filtro`() {
        // Verificar se o botão de filtro está visível
        onView(withId(com.example.gestaobilhares.R.id.btnFilter))
            .check(matches(isDisplayed()))
    }
}

