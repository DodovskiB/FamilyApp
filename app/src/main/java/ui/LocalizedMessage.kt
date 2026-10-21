package com.example.familyapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.familyapp.R

/** Translate only app-owned status/error copy, never names or backend/user content. */
@Composable
fun localizedMessage(message: String): String = when (message) {
    "Could not open this link. Please try again." -> stringResource(R.string.message_1)
    "Could not open the camera or gallery. Please try again." -> stringResource(R.string.message_2)
    "Could not save the change. Please try again." -> stringResource(R.string.message_3)
    "Please sign in again." -> stringResource(R.string.message_4)
    "Could not open account data. Please try again." -> stringResource(R.string.message_5)
    "Could not restore your session. Connect to the internet and sign in again." -> stringResource(R.string.message_6)
    "Could not start Google sign-in. Please check your connection and try again." -> stringResource(R.string.message_7)
    "Sign-in could not be completed. Please try again." -> stringResource(R.string.message_8)
    "Signed out on this device. The server could not be reached." -> stringResource(R.string.message_9)
    "Connecting…" -> stringResource(R.string.message_10)
    "This phone and your account have matching list names. Open Account to choose how to connect them." -> stringResource(R.string.message_11)
    "Up to date" -> stringResource(R.string.message_12)
    "Waiting to sync" -> stringResource(R.string.message_13)
    "Waiting for connection — changes stay on this phone" -> stringResource(R.string.message_14)
    "Access to a shared list ended. Its offline changes were kept privately and were not uploaded." -> stringResource(R.string.message_15)
    "Invitation added. They can accept it in Invitations using this Google email. No email was sent." -> stringResource(R.string.message_16)
    "Could not complete this action. Check your connection, email and list access, then try again." -> stringResource(R.string.message_17)
    else -> message
}
