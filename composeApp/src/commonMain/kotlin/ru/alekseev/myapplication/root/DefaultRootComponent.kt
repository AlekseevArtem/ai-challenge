package ru.alekseev.myapplication.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.value.Value
import com.example.myapplication.feature_main.di.featureMainModule
import com.example.myapplication.feature_main.presentation.DefaultMainComponent
import com.example.myapplication.feature_main.presentation.MainComponent
import com.example.myapplication.feature_welcome.presentation.DefaultWelcomeComponent
import com.example.myapplication.feature_welcome.presentation.WelcomeComponent
import kotlinx.serialization.Serializable
import org.koin.core.context.startKoin

class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    init {
        // Initialize Koin
        startKoin {
            modules(featureMainModule)
        }
    }

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Main,
            handleBackButton = true,
            childFactory = ::child,
        )

    private fun child(config: Config, childComponentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Main -> RootComponent.Child.Main(mainComponent(childComponentContext))
            is Config.Welcome -> RootComponent.Child.Welcome(welcomeComponent(childComponentContext))
        }

    private fun mainComponent(componentContext: ComponentContext): MainComponent =
        DefaultMainComponent(
            componentContext = componentContext,
        )

    private fun welcomeComponent(componentContext: ComponentContext): WelcomeComponent =
        DefaultWelcomeComponent(
            componentContext = componentContext,
            onFinished = navigation::pop,
        )

    override fun onBackClicked(toIndex: Int) {
        navigation.popTo(index = toIndex)
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Main : Config

        @Serializable
        data object Welcome : Config
    }
}
