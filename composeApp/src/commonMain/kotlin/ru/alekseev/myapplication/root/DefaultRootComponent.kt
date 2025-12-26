package ru.alekseev.myapplication.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import ru.alekseev.myapplication.feature.main.presentation.DefaultMainComponent
import ru.alekseev.myapplication.feature.main.presentation.MainComponent
import ru.alekseev.myapplication.feature.settings.presentation.DefaultSettingsComponent
import ru.alekseev.myapplication.feature.settings.presentation.SettingsComponent
import ru.alekseev.myapplication.feature.settings.presentation.SettingsStore
import ru.alekseev.myapplication.feature.welcome.presentation.DefaultWelcomeComponent
import ru.alekseev.myapplication.feature.welcome.presentation.WelcomeComponent
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext, KoinComponent {

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
            is Config.Settings -> RootComponent.Child.Settings(settingsComponent(childComponentContext))
        }

    private fun mainComponent(componentContext: ComponentContext): MainComponent =
        DefaultMainComponent(
            componentContext = componentContext,
            onOpenSettings = { navigation.pushNew(Config.Settings) },
        )

    private fun welcomeComponent(componentContext: ComponentContext): WelcomeComponent =
        DefaultWelcomeComponent(
            componentContext = componentContext,
            onFinished = navigation::pop,
        )

    private fun settingsComponent(componentContext: ComponentContext): SettingsComponent {
        val settingsStore: SettingsStore by inject()
        return DefaultSettingsComponent(
            componentContext = componentContext,
            settingsStore = settingsStore,
            onFinished = navigation::pop,
        )
    }

    override fun onBackClicked(toIndex: Int) {
        navigation.popTo(index = toIndex)
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Main : Config

        @Serializable
        data object Welcome : Config

        @Serializable
        data object Settings : Config
    }
}
