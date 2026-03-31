package com.xiaoxiao0301.amberplay.core.common.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI instrumentation tests for [TvFocusCard].
 *
 * Verifies:
 *  1. The card is rendered and has a click action.
 *  2. performClick() fires the onClick callback.
 *  3. The card can receive focus via requestFocus() and reports isFocused.
 *
 * Run on a device / emulator: ./gradlew :core:common:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class TvFocusCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tvFocusCard_isDisplayedAndHasClickAction() {
        composeTestRule.setContent {
            TvFocusCard(
                onClick   = {},
                modifier  = Modifier.size(120.dp).testTag("card"),
            ) {
                Text("Hello")
            }
        }

        composeTestRule.onNodeWithTag("card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card").assertHasClickAction()
    }

    @Test
    fun tvFocusCard_performClick_invokesCallback() {
        var clicked = false

        composeTestRule.setContent {
            TvFocusCard(
                onClick  = { clicked = true },
                modifier = Modifier.size(120.dp).testTag("card"),
            ) {
                Text("Click me")
            }
        }

        composeTestRule.onNodeWithTag("card").performClick()
        assertTrue("onClick not invoked after performClick()", clicked)
    }

    @Test
    fun tvFocusCard_requestFocus_gainsFocus() {
        composeTestRule.setContent {
            TvFocusCard(
                onClick  = {},
                modifier = Modifier.size(120.dp).testTag("card"),
            ) {
                Text("Focused card")
            }
        }

        // Request focus programmatically and verify the node reports isFocused = true.
        composeTestRule.onNodeWithTag("card").requestFocus()
        composeTestRule.onNodeWithTag("card").assertIsFocused()
    }

    @Test
    fun tvFocusCard_contentIsVisible() {
        composeTestRule.setContent {
            TvFocusCard(onClick = {}, modifier = Modifier.size(120.dp)) {
                Text("Album Cover")
            }
        }

        composeTestRule.onNodeWithText("Album Cover").assertIsDisplayed()
    }
}
