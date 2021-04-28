package eu.epitech.workshopapp

import io.uniflow.android.test.createTestObserver
import io.uniflow.test.rule.UniflowTestDispatchersRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CookieGameViewModelTest : KoinTest {

    @Before
    fun before() {
        startKoin {
            modules(myModule)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @get:Rule
    val testDispatcherRule = UniflowTestDispatchersRule()

    @Test
    fun `it should not display not hello world`() {

        val viewModel = CookieGameViewModel()
        val dataFlow = viewModel.createTestObserver()

        viewModel.clickWithLogic(true)

        dataFlow.verifySequence(
            Idle,
            Loading,
            Result("hello world")
        )
    }

    @Test
    fun `it should display not hello world`() {

        val viewModel = CookieGameViewModel()
        val dataFlow = viewModel.createTestObserver()

        viewModel.clickWithLogic(false)

        dataFlow.verifySequence(
            Idle,
            Loading,
            Result("not hello world")
        )
    }
}