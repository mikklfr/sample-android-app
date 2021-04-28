package eu.epitech.workshopapp

import androidx.annotation.VisibleForTesting
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.uniflow.android.test.createTestObserver
import io.uniflow.core.dispatcher.ApplicationDispatchers
import io.uniflow.core.dispatcher.UniFlowDispatcher
import io.uniflow.test.dispatcher.TestDispatchers
import io.uniflow.test.rule.UniflowTestDispatchersRule
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import splitties.init.injectAsAppCtx
import java.lang.RuntimeException
import kotlin.coroutines.ContinuationInterceptor

@ExperimentalCoroutinesApi
class TestCoroutineRule : TestRule, TestCoroutineScope by TestCoroutineScope() {

    val dispatcher = coroutineContext[ContinuationInterceptor] as TestCoroutineDispatcher
    private val testDispatchers: TestDispatchers = TestDispatchers(dispatcher)

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {

                Dispatchers.setMain(dispatcher)
                UniFlowDispatcher.dispatcher = testDispatchers

                // everything above this happens before the test
                base.evaluate()
                // everything below this happens after the test

                cleanupTestCoroutines()
                Dispatchers.resetMain()
                UniFlowDispatcher.dispatcher = ApplicationDispatchers()
            }
        }
    }
}

class KoinTestRule private constructor(private val appDeclaration: KoinAppDeclaration) : TestWatcher() {

    private var _koin: Koin? = null
    val koin: Koin
        get() = _koin ?: error("No Koin application found")

    override fun starting(description: Description?) {
        _koin = startKoin(appDeclaration = appDeclaration).koin
    }

    override fun finished(description: Description?) {
        stopKoin()
        _koin = null
    }

    companion object {
        fun create(appDeclaration: KoinAppDeclaration = {}): KoinTestRule {
            return KoinTestRule(appDeclaration)
        }
    }
}

open class CoroutineDispatcherProvider {
    open val Main: MainCoroutineDispatcher by lazy { Dispatchers.Main.immediate }
    open val IO: CoroutineDispatcher by lazy { Dispatchers.IO }
    open val Default: CoroutineDispatcher by lazy { Dispatchers.Default }
}

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
class TestCoroutineDispatcherProvider : CoroutineDispatcherProvider() {
    override val Main: MainCoroutineDispatcher by lazy { Dispatchers.Main }
    override val IO: CoroutineDispatcher by lazy { Dispatchers.Main }
    override val Default: CoroutineDispatcher by lazy { Dispatchers.Main }
}



@RunWith(AndroidJUnit4::class)
@Config(sdk = [24])
class CookieGameViewModelTest : KoinTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @OptIn(ExperimentalStdlibApi::class)
    @get:Rule
    val koinTestRule = KoinTestRule.create {

        modules(buildList {
            val androidContext = ApplicationProvider.getApplicationContext<MyApplication>()
            androidContext(androidContext)
            androidContext.injectAsAppCtx()
            add(myModule)
            add(module {
                single<CoroutineDispatcherProvider>(override = true) { TestCoroutineDispatcherProvider() }
            })
        })
    }

    @Test
    fun `it should handle network errors`() = coroutineRule.runBlockingTest {
        val loader = mockk<Loader>()

        coEvery { loader.loadData() } throws RuntimeException("oh oh")

        val viewModel = CookieGameViewModel(loader)
        val dataFlow = viewModel.createTestObserver()

        viewModel.click()

        dataFlow.verifySequence(
            Idle,
            Loading,
            Error
        )
    }

    @Test
    fun `it should behave ok`() = coroutineRule.runBlockingTest {
        val loader = mockk<Loader>()

        coEvery { loader.loadData() } returns listOf("1", "2")

        val viewModel = CookieGameViewModel(loader)
        val dataFlow = viewModel.createTestObserver()

        viewModel.click()

        dataFlow.verifySequence(
            Idle,
            Loading,
            Result("1,2")
        )
    }

    @Test
    fun `it should not display not hello world`() = coroutineRule.runBlockingTest {

        val viewModel = CookieGameViewModel(mockk())
        val dataFlow = viewModel.createTestObserver()

        viewModel.clickWithLogic(true)

        dataFlow.verifySequence(
            Idle,
            Loading,
            Result("hello world")
        )
    }

    @Test
    fun `it should display not hello world`() = coroutineRule.runBlockingTest {

        val viewModel = CookieGameViewModel(mockk())
        val dataFlow = viewModel.createTestObserver()

        viewModel.clickWithLogic(false)

        dataFlow.verifySequence(
            Idle,
            Loading,
            Result("not hello world")
        )
    }
}