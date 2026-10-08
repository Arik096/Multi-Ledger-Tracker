package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.LedgerBook
import com.example.ui.components.NetBalanceHeaderCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val dummyBook = LedgerBook(
      id = 1L,
      name = "Personal Finances",
      currencySymbol = "$",
      currencyCode = "USD",
      colorHex = 0xFF0D9488L
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        NetBalanceHeaderCard(
          book = dummyBook,
          netBalance = 4250.75,
          totalIncome = 5800.00,
          totalExpense = 1549.25,
          onSwitchBookClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
