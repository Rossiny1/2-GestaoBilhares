package com.example.gestaobilhares.ui.auth

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
 * ✅ FASE 12.2: Testes de UI com Espresso para LoginFragment
 * 
 * Testa fluxos críticos de autenticação:
 * - Exibição dos campos de login
 * - Validação de campos obrigatórios
 * - Interação com botões
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginFragmentUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun `tela de login deve exibir campos de email e senha`() {
        // Verificar se os campos estão visíveis
        onView(withId(com.example.gestaobilhares.R.id.emailEditText))
            .check(matches(isDisplayed()))
        
        onView(withId(com.example.gestaobilhares.R.id.passwordEditText))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `tela de login deve exibir botão de login`() {
        // Verificar se o botão de login está visível
        onView(withId(com.example.gestaobilhares.R.id.loginButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `deve ser possível digitar email e senha`() {
        val email = "teste@example.com"
        val senha = "senha123"

        // Digitar email
        onView(withId(com.example.gestaobilhares.R.id.emailEditText))
            .perform(typeText(email), closeSoftKeyboard())
        
        // Digitar senha
        onView(withId(com.example.gestaobilhares.R.id.passwordEditText))
            .perform(typeText(senha), closeSoftKeyboard())
        
        // Verificar se os textos foram inseridos
        onView(withId(com.example.gestaobilhares.R.id.emailEditText))
            .check(matches(withText(email)))
        
        onView(withId(com.example.gestaobilhares.R.id.passwordEditText))
            .check(matches(withText(senha)))
    }

    @Test
    fun `botão de login deve estar habilitado quando campos estão preenchidos`() {
        val email = "teste@example.com"
        val senha = "senha123"

        // Preencher campos
        onView(withId(com.example.gestaobilhares.R.id.emailEditText))
            .perform(typeText(email), closeSoftKeyboard())
        
        onView(withId(com.example.gestaobilhares.R.id.passwordEditText))
            .perform(typeText(senha), closeSoftKeyboard())
        
        // Verificar se o botão está habilitado
        onView(withId(com.example.gestaobilhares.R.id.loginButton))
            .check(matches(isEnabled()))
    }
}

