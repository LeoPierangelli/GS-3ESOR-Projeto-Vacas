package com.ordenha.monitorada

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Rotas {
    const val LOGIN = "login"
    const val REBANHO = "rebanho"
    const val FICHA = "ficha/{vacaId}"
    const val CADASTRO = "cadastro"
    fun ficha(id: Int) = "ficha/$id"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Rotas.LOGIN) {

        composable(Rotas.LOGIN) {
            LoginScreen(
                onLoginOk = {
                    nav.navigate(Rotas.REBANHO) { popUpTo(Rotas.LOGIN) { inclusive = true } }
                }
            )
        }

        composable(Rotas.REBANHO) {
            RebanhoScreen(
                onFicha = { id -> nav.navigate(Rotas.ficha(id)) },
                onCadastro = { nav.navigate(Rotas.CADASTRO) },
                onSair = {
                    nav.navigate(Rotas.LOGIN) { popUpTo(Rotas.REBANHO) { inclusive = true } }
                }
            )
        }

        composable(
            route = Rotas.FICHA,
            arguments = listOf(navArgument("vacaId") { type = NavType.IntType })
        ) { entry ->
            FichaVacaScreen(
                vacaId = entry.arguments?.getInt("vacaId") ?: 0,
                onVoltar = { nav.popBackStack() }
            )
        }

        composable(Rotas.CADASTRO) {
            CadastroVacaScreen(
                onVoltar = { nav.popBackStack() },
                onSalvo = { nav.popBackStack() }
            )
        }
    }
}
