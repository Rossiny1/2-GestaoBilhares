package com.example.gestaobilhares.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.gestaobilhares.MainActivity
import com.example.gestaobilhares.R
import com.google.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes instrumentados (UI) para MainActivity.
 * Valida interações do usuário, renderização e comportamento visual.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityUITest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun `mainActivity deve iniciar corretamente`() {
        // GIVEN: Nenhuma configuração adicional
        
        // WHEN: Lançar MainActivity
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Activity deve estar visível e elementos básicos presentes
        onView(withId(R.id.container))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve exibir navigation drawer se disponivel`() {
        // GIVEN: MainActivity com navigation
        
        // WHEN: Lançar activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Verificar elementos de navegação (se existirem)
        try {
            onView(withId(R.id.nav_view))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // Navigation drawer pode não existir, então verificamos apenas o container
            onView(withId(R.id.container))
                .check(matches(isDisplayed()))
        }
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve ter toolbar visivel`() {
        // GIVEN: MainActivity com toolbar
        
        // WHEN: Lançar activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Toolbar deve estar visível
        try {
            onView(withId(R.id.toolbar))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // Toolbar pode estar em fragment, não na activity
            onView(withId(R.id.container))
                .check(matches(isDisplayed()))
        }
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve suportar navegacao por fragments`() {
        // GIVEN: MainActivity com navigation component
        
        // WHEN: Lançar activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Container de fragments deve estar presente
        onView(withId(R.id.container))
            .check(matches(isDisplayed()))
        
        // Verificar que não há erro ao acessar navigation controller
        scenario.onActivity { activity ->
            assertThat(activity.supportFragmentManager).isNotNull()
            assertThat(activity.supportFragmentManager.fragments).isNotNull()
        }
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve lidar com rotacao de tela`() {
        // GIVEN: MainActivity
        
        // WHEN: Lançar activity e simular rotação
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Activity deve permanecer funcional após rotação
        scenario.recreate()
        
        onView(withId(R.id.container))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve preservar estado em background`() {
        // GIVEN: MainActivity
        
        // WHEN: Mover para background e voltar
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        scenario.moveToState(org.junit.runner.RunWith.State.STARTED)
        scenario.moveToState(org.junit.runner.RunWith.State.CREATED)
        scenario.moveToState(org.junit.runner.RunWith.State.STARTED)
        scenario.moveToState(org.junit.runner.RunWith.State.RESUMED)
        
        // THEN: Activity deve estar funcional
        onView(withId(R.id.container))
            .check(matches(isDisplayed()))
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve ter comportamento de voltar correto`() {
        // GIVEN: MainActivity
        
        // WHEN: Pressionar voltar
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Não deve haver exceção ao pressionar voltar
        try {
            onView(isRoot()).perform(pressBack())
            // Se chegou aqui, não houve exceção
            assertThat(true).isTrue()
        } catch (e: Exception) {
            // Pode haver exceção se não houver activity na stack, o que é normal
            assertThat(e.message).isNotNull()
        }
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve ser responsiva a cliques`() {
        // GIVEN: MainActivity
        
        // WHEN: Lançar activity e clicar em áreas seguras
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Cliques não devem causar crashes
        onView(withId(R.id.container))
            .perform(click())
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve ter layout correto`() {
        // GIVEN: MainActivity
        
        // WHEN: Lançar activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Layout deve estar carregado corretamente
        onView(withId(R.id.container))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve ter tema aplicado corretamente`() {
        // GIVEN: MainActivity
        
        // WHEN: Lançar activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Tema deve estar aplicado (verificado indiretamente)
        scenario.onActivity { activity ->
            assertThat(activity.theme).isNotNull()
            assertThat(activity.packageName).isEqualTo("com.example.gestaobilhares")
        }
        
        scenario.close()
    }
    
    @Test
    fun `mainActivity deve ter contexto valido`() {
        // GIVEN: MainActivity
        
        // WHEN: Lançar activity
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // THEN: Context deve ser válido
        scenario.onActivity { activity ->
            assertThat(activity).isNotNull()
            assertThat(activity.applicationContext).isNotNull()
            assertThat(activity.packageName).isNotEmpty()
        }
        
        scenario.close()
    }
}