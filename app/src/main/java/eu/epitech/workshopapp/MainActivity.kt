package eu.epitech.workshopapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import eu.epitech.workshopapp.databinding.FragmentCookiegameBinding
import eu.epitech.workshopapp.databinding.FragmentNewpageBinding
import io.uniflow.android.AndroidDataFlow
import io.uniflow.android.livedata.onStates
import io.uniflow.core.flow.data.UIState
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject
import splitties.views.onClick

class NewDirectionFragment : Fragment() {

    private val args by navArgs<NewDirectionFragmentArgs>()
    lateinit var binding: FragmentNewpageBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNewpageBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dataDisplay.text = args.sampleData
    }
}

sealed class CookieGameState : UIState()
object Idle : CookieGameState()
object Loading : CookieGameState()
object Error : CookieGameState()
data class Result(val data: String) : CookieGameState()

class Loader {
    suspend fun loadData(): List<String> {
        delay(1000)
        return listOf("a", "b", "c")
    }
    fun magicNumber() = 42
}

class CookieGameViewModel(val loader: Loader) : AndroidDataFlow() {

    init {
        action {
            setState(Idle)
        }
    }

    fun clickWithLogic(boolean: Boolean) {
        action {
            setState(Loading)
            if (boolean) {
                setState(Result("hello world"))
            } else {
                setState(Result("not hello world"))
            }
        }
    }

    fun click() {
        action {
            setState(Loading)
            delay(3000)
            try {
                setState(Result(loader.loadData().joinToString(",")))
            } catch (e: Exception) {
                setState(Error)
            }
        }
    }

}

class CookieGameFragment : Fragment() {
    private val cookieGameViewModel: CookieGameViewModel by inject()

    lateinit var binding: FragmentCookiegameBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCookiegameBinding.inflate(inflater)

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onStates(cookieGameViewModel) { state ->
            when (state) {
                is Idle -> {
                    binding.counter.isVisible = false
                    binding.loader.isVisible = false
                    binding.clickMe.isVisible = true

                }
                is Loading -> {
                    binding.counter.isVisible = false
                    binding.loader.isVisible = true
                    binding.clickMe.isVisible = false

                }
                is Result -> {
                    binding.counter.isVisible = true
                    binding.loader.isVisible = false
                    binding.clickMe.isVisible = true

                    binding.counter.text = state.data
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.clickMe.onClick {
            cookieGameViewModel.click()
        }

        binding.goToNextPage.onClick {
            findNavController().navigate(CookieGameFragmentDirections.openNewPage("Welcome Epitech"))
        }
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}