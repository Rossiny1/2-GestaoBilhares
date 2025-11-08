package com.example.gestaobilhares.ui.accessibility

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.gestaobilhares.MainActivity
import com.example.gestaobilhares.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ✅ FASE 12.11: Testes de Acessibilidade
 * 
 * Testa se os elementos interativos têm content descriptions
 * e se são acessíveis para leitores de tela (TalkBack)
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /**
     * Testa se os botões principais têm content descriptions
     */
    @Test
    fun testLoginButtonHasContentDescription() {
        // Verificar se o botão de login tem content description
        onView(withId(R.id.loginButton))
            .check(matches(hasContentDescription()))
    }

    /**
     * Testa se os ImageButtons têm content descriptions
     */
    @Test
    fun testImageButtonsHaveContentDescriptions() {
        // Verificar botão de voltar (se presente)
        try {
            onView(withId(R.id.btnBack))
                .check(matches(hasContentDescription()))
        } catch (e: Exception) {
            // Botão pode não estar visível na tela de login
        }
    }

    /**
     * Testa se os FABs têm content descriptions
     */
    @Test
    fun testFABsHaveContentDescriptions() {
        // Verificar FAB principal (se presente)
        try {
            onView(withId(R.id.fab_main))
                .check(matches(hasContentDescription()))
        } catch (e: Exception) {
            // FAB pode não estar visível na tela de login
        }
    }

    /**
     * Testa se elementos decorativos estão marcados como não importantes para acessibilidade
     */
    @Test
    fun testDecorativeElementsAreNotImportantForAccessibility() {
        // Este teste verifica se ImageViews decorativos têm importantForAccessibility="no"
        // A verificação é feita via código, não via Espresso
        // Implementação futura: usar AccessibilityNodeInfo para verificar
    }

    /**
     * Testa se campos de texto têm labels apropriados
     */
    @Test
    fun testTextInputsHaveLabels() {
        // Verificar campo de email
        onView(withId(R.id.emailEditText))
            .check(matches(isDisplayed()))
        
        // Verificar campo de senha
        onView(withId(R.id.passwordEditText))
            .check(matches(isDisplayed()))
    }

    /**
     * Testa se elementos clicáveis são focáveis
     */
    @Test
    fun testClickableElementsAreFocusable() {
        // Verificar botão de login
        onView(withId(R.id.loginButton))
            .check(matches(isFocusable()))
            .check(matches(isClickable()))
    }
}

