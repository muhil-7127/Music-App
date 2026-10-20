package com.muhil.music.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muhil.music.ui.screens.auth.*
import com.muhil.music.ui.screens.home.HomeScreen
import com.muhil.music.ui.screens.library.*
import com.muhil.music.ui.screens.player.NowPlayingScreen
import com.muhil.music.ui.screens.lyrics.LyricsScreen
import com.muhil.music.ui.screens.search.SearchScreen
import com.muhil.music.ui.screens.settings.SettingsScreen
import com.muhil.music.ui.screens.playlists.*

sealed class Screen(val route: String) {
    object Splash      : Screen("splash")
    object Login       : Screen("login")
    object Register    : Screen("register")
    object ForgotPw    : Screen("forgot_password")
    object PhoneLogin  : Screen("phone_login")
    object OtpVerify   : Screen("otp_verify/{verificationId}") {
        fun route(verificationId: String) = "otp_verify/$verificationId"
    }
    object Home        : Screen("home")
    object Search      : Screen("search")
    object AllSongs    : Screen("all_songs")
    object Albums      : Screen("albums")
    object Artists     : Screen("artists")
    object Playlists   : Screen("playlists")
    object NowPlaying  : Screen("now_playing")
    object Lyrics      : Screen("lyrics")
    object Favorites   : Screen("favorites")
    object RecentPlay  : Screen("recent")
    object Settings    : Screen("settings")
    object CreatePlaylist : Screen("create_playlist")
}

@Composable
fun MuhilNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                onNavigateToHome  = { navController.navigate(Screen.Home.route)  { popUpTo(Screen.Splash.route) { inclusive = true } } }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome     = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgot   = { navController.navigate(Screen.ForgotPw.route) },
                onNavigateToPhone    = { navController.navigate(Screen.PhoneLogin.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegistered = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } }
            )
        }
        composable(Screen.ForgotPw.route) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PhoneLogin.route) {
            PhoneLoginScreen(
                onOtpSent = { verificationId ->
                    navController.navigate(Screen.OtpVerify.route(verificationId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.OtpVerify.route) { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            OtpVerifyScreen(
                verificationId = verificationId,
                onVerified = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        composable(Screen.AllSongs.route) {
            AllSongsScreen(navController = navController)
        }
        composable(Screen.Albums.route) {
            AlbumsScreen(navController = navController)
        }
        composable(Screen.Artists.route) {
            ArtistsScreen(navController = navController)
        }
        composable(Screen.Playlists.route) {
            PlaylistsScreen(navController = navController)
        }
        composable(Screen.NowPlaying.route) {
            NowPlayingScreen(navController = navController)
        }
        composable(Screen.Lyrics.route) {
            LyricsScreen(navController = navController)
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(navController = navController)
        }
        composable(Screen.RecentPlay.route) {
            RecentlyPlayedScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.CreatePlaylist.route) {
            CreatePlaylistScreen(navController = navController)
        }
    }
}
