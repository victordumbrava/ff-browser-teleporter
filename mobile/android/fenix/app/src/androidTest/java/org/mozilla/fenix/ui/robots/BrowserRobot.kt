/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions", "TooGenericExceptionCaught")

package org.mozilla.fenix.ui.robots

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.widget.TimePicker
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.By.text
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.mediasession.MediaSession
import mozilla.components.concept.engine.utils.EngineReleaseChannel
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.AppAndSystemHelper.registerAndCleanupIdlingResources
import org.mozilla.fenix.helpers.Constants.RETRY_COUNT
import org.mozilla.fenix.helpers.Constants.TAG
import org.mozilla.fenix.helpers.DataGenerationHelper.getStringResource
import org.mozilla.fenix.helpers.HomeActivityComposeTestRule
import org.mozilla.fenix.helpers.MatcherHelper.assertItemTextEquals
import org.mozilla.fenix.helpers.MatcherHelper.assertUIObjectExists
import org.mozilla.fenix.helpers.MatcherHelper.assertUIObjectIsGone
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithDescription
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResIdAndText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResIdContainingText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithText
import org.mozilla.fenix.helpers.SessionLoadedIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.TestHelper.waitForAppWindowToBeUpdated
import org.mozilla.fenix.helpers.TestHelper.waitForObjects
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.home.ui.HomepageTestTag.HOMEPAGE
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.webcompat.BrokenSiteReporterTestTags.BROKEN_SITE_REPORTER_CHOOSE_REASON_BUTTON
import org.mozilla.fenix.webcompat.BrokenSiteReporterTestTags.BROKEN_SITE_REPORTER_SEND_BUTTON
import java.time.LocalDate

class BrowserRobot {
    private lateinit var sessionLoadedIdlingResource: SessionLoadedIdlingResource

    fun waitForPageToLoad(pageLoadWaitingTime: Long = waitingTime) = assertUIObjectIsGone(progressBar(), waitingTime = pageLoadWaitingTime)

    fun verifyCurrentPrivateSession(context: Context) {
        val selectedTab = context.components.core.store.state.selectedTab
        Log.i(TAG, "verifyCurrentPrivateSession: Trying to verify that current browsing session is private")
        assertTrue("Current session is private", selectedTab?.content?.private ?: false)
        Log.i(TAG, "verifyCurrentPrivateSession: Verified that current browsing session is private")
    }

    fun verifyUrl(url: String) {
        sessionLoadedIdlingResource = SessionLoadedIdlingResource()

        registerAndCleanupIdlingResources(sessionLoadedIdlingResource) {
            // Check if toolbar is displayed
            // Check if a prompt is being displayed
            if (
                !itemWithResId("$packageName:id/mozac_browser_toolbar_url_view").exists() &&
                itemWithResId("$packageName:id/title").exists()
            ) {
                Log.i(TAG, "verifyUrl: The toolbar is not displayed")
                Log.i(TAG, "verifyUrl: A prompt is being displayed")
                Log.i(TAG, "verifyUrl: Trying to click device back button")
                // Dismiss the prompt by clicking the device back button
                mDevice.pressBack()
                Log.i(TAG, "verifyUrl: Clicked device back button")
                Log.i(TAG, "verifyUrl: Waiting for $waitingTimeShort ms for $packageName window to be updated")
                mDevice.waitForWindowUpdate(packageName, waitingTimeShort)
                Log.i(TAG, "verifyUrl: Waited for $waitingTimeShort ms for $packageName window to be updated")
            }

            assertUIObjectExists(
                itemWithResIdContainingText(
                    "$packageName:id/mozac_browser_toolbar_url_view",
                    url.replace("http://", ""),
                ),
            )
        }
    }

    fun verifyHelpUrl() {
        try {
            verifyUrl("support.mozilla.org/en-US/products/mobile")
        } catch (e: AssertionError) {
            Log.i(TAG, "verifyHelpUrl: AssertionError caught, checking redirect URL")
            verifyUrl(
                SupportUtils.getSumoURLForTopic(
                    appContext,
                    SupportUtils.SumoTopic.HELP,
                ).replace("https://", ""),
            )
        }
    }

    fun verifyWhatsNewURL() {
        try {
            verifyUrl("/releaseNotes")
        } catch (e: AssertionError) {
            Log.i(TAG, "verifyWhatsNewURL: AssertionError caught, checking redirect URL")
            verifyUrl(SupportUtils.WHATS_NEW_URL)
        }
    }

    fun verifyRateOnGooglePlayURL() {
        verifyUrl("play.google.com/store/apps/details?id=org.mozilla.fenix")
    }

    fun verifyCustomSearchEngineLearnMoreURL() {
        try {
            verifyUrl("support.mozilla.org/en-US/kb/manage-my-default-search-engines-firefox-android")
        } catch (e: AssertionError) {
            Log.i(TAG, "verifyCustomSearchEngineLearnMoreLink: AssertionError caught, checking redirect URL")
            verifyUrl(
                SupportUtils.getSumoURLForTopic(
                    appContext,
                    SupportUtils.SumoTopic.CUSTOM_SEARCH_ENGINES,
                ).replace("https://", ""),
            )
        }
    }

    fun verifyETPLearnMoreURL() {
        // Get and log the URL in case there are failures in the future
        Log.i(TAG, "verifyETPLearnMoreURL: ETP learn more URL = ${itemWithResId("$packageName:id/mozac_browser_toolbar_url_view").text}")
        try {
            verifyUrl("support.mozilla.org/en-US/kb/tracking-protection-firefox-android")
        } catch (e: AssertionError) {
            Log.i(TAG, "verifyETPLearnMoreURL: AssertionError caught, checking redirect URL")
            verifyUrl("support.mozilla.org/en-US/kb/enhanced-tracking-protection-firefox-android")
        } catch (e: AssertionError) {
            verifyUrl(
                SupportUtils.getSumoURLForTopic(appContext, SupportUtils.SumoTopic.TOTAL_COOKIE_PROTECTION).replace("https://", ""),
            )
        }
    }

    fun verifySponsoredShortcutsLearnMoreURL() {
        try {
            verifyUrl("support.mozilla.org/en-US/kb/sponsor-privacy")
        } catch (e: AssertionError) {
            Log.i(TAG, "verifySponsoredShortcutsURL: AssertionError caught, checking redirect URL")
            verifyUrl(
                SupportUtils.getSumoURLForTopic(appContext, SupportUtils.SumoTopic.SPONSOR_PRIVACY).replace("https://", ""),
            )
        }
    }

    /* Asserts that the text within DOM element with ID="testContent" has the given text, i.e.
     *  document.querySelector('#testContent').innerText == expectedText
     *
     */
    fun verifyPageContent(expectedText: String) {
        sessionLoadedIdlingResource = SessionLoadedIdlingResource()

        mDevice.waitNotNull(
            Until.findObject(By.res("$packageName:id/engineView")),
            waitingTime,
        )

        registerAndCleanupIdlingResources(sessionLoadedIdlingResource) {
            assertTrue(
                itemWithResId("$packageName:id/engineView")
                    .getChild(UiSelector().textContains(expectedText)).waitForExists(waitingTimeLong),
            )
        }
    }

    fun verifyTextFragmentsPageContent(expectedText: String) {
        for (i in 1..RETRY_COUNT) {
            Log.i(TAG, "verifyTextFragmentsPageContent: Started try #$i")
            try {
                assertUIObjectExists(
                    itemWithResId("$packageName:id/engineView")
                        .getChild(UiSelector().textContains(expectedText)),
                )

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifyTextFragmentsPageContent: AssertionError caught, executing fallback methods")
                navigationToolbar {
                }.openThreeDotMenu {
                }.refreshPage {
                    waitForPageToLoad()
                }
            }
        }
    }

    fun verifyTabCrashReporterView() {
        assertUIObjectExists(itemWithResId("$packageName:id/crash_tab_image"))
        assertUIObjectExists(itemWithText(getStringResource(R.string.tab_crash_title_2)))
        assertUIObjectExists(itemWithText(getStringResource(R.string.tab_crash_send_report)))
        assertUIObjectExists(itemWithResId("$packageName:id/restoreTabButton"))
        assertUIObjectExists(itemWithResId("$packageName:id/closeTabButton"))
    }

    // Verifies the information displayed on the about:cache page
    fun verifyNetworkCacheIsEmpty(storage: String) {
        val memorySection = mDevice.findObject(UiSelector().description(storage))

        val gridView =
            if (storage == "memory") {
                memorySection.getFromParent(
                    UiSelector()
                        .className("android.widget.GridView")
                        .index(2),
                )
            } else {
                memorySection.getFromParent(
                    UiSelector()
                        .className("android.widget.GridView")
                        .index(4),
                )
            }

        val cacheSizeInfo =
            gridView.getChild(
                UiSelector().text("Number of entries:"),
            ).getFromParent(
                UiSelector().text("0"),
            )

        for (i in 1..RETRY_COUNT) {
            try {
                assertUIObjectExists(cacheSizeInfo)
                break
            } catch (e: AssertionError) {
                browserScreen {
                }.openThreeDotMenu {
                }.refreshPage { }
            }
        }
    }

    fun verifyTabCounter(numberOfOpenTabs: String) =
        onView(
            allOf(
                withId(R.id.counter_text),
                withText(numberOfOpenTabs),
                withEffectiveVisibility(Visibility.VISIBLE),
            ),
        ).check(matches(isDisplayed()))

    fun verifyContextMenuForLocalHostLinks(containsURL: Uri) {
        // If the link is directing to another local asset the "Download link" option is not available
        // If the link is not re-directing to an external app the "Open link in external app" option is not available
        assertUIObjectExists(
            contextMenuLinkUrl(containsURL.toString()),
            contextMenuOpenLinkInNewTab(),
            contextMenuOpenLinkInPrivateTab(),
            contextMenuCopyLink(),
            contextMenuShareLink(),
        )
    }

    fun verifyContextMenuForLinksToOtherApps(containsURL: String) {
        // If the link is re-directing to an external app the "Open link in external app" option is available
        // If the link is not directing to another local asset the "Download link" option is not available
        assertUIObjectExists(
            contextMenuLinkUrl(containsURL),
            contextMenuOpenLinkInNewTab(),
            contextMenuOpenLinkInPrivateTab(),
            contextMenuCopyLink(),
            contextMenuDownloadLink(),
            contextMenuShareLink(),
            contextMenuOpenInExternalApp(),
        )
    }

    fun verifyContextMenuForLinksToOtherHosts(containsURL: Uri) {
        // If the link is re-directing to another host the "Download link" option is available
        // If the link is not re-directing to an external app the "Open link in external app" option is not available
        assertUIObjectExists(
            contextMenuLinkUrl(containsURL.toString()),
            contextMenuOpenLinkInNewTab(),
            contextMenuOpenLinkInPrivateTab(),
            contextMenuCopyLink(),
            contextMenuDownloadLink(),
            contextMenuShareLink(),
        )
    }

    fun verifyLinkImageContextMenuItems(containsURL: Uri) {
        mDevice.waitNotNull(Until.findObject(By.textContains(containsURL.toString())))
        mDevice.waitNotNull(
            Until.findObject(text("Open link in new tab")),
            waitingTime,
        )
        mDevice.waitNotNull(
            Until.findObject(text("Open link in private tab")),
            waitingTime,
        )
        mDevice.waitNotNull(Until.findObject(text("Copy link")), waitingTime)
        mDevice.waitNotNull(Until.findObject(text("Share link")), waitingTime)
        mDevice.waitNotNull(
            Until.findObject(text("Open image in new tab")),
            waitingTime,
        )
        mDevice.waitNotNull(Until.findObject(text("Save image")), waitingTime)
        mDevice.waitNotNull(
            Until.findObject(text("Copy image location")),
            waitingTime,
        )
    }

    fun verifyNavURLBarHidden() = assertUIObjectIsGone(navURLBar())

    fun verifyMenuButton() {
        Log.i(TAG, "verifyMenuButton: Trying to verify main menu button is displayed")
        threeDotButton().check(matches(isDisplayed()))
        Log.i(TAG, "verifyMenuButton: Verified main menu button is displayed")
    }

    fun verifyNoLinkImageContextMenuItems(containsURL: Uri) {
        mDevice.waitNotNull(Until.findObject(By.textContains(containsURL.toString())))
        mDevice.waitNotNull(
            Until.findObject(text("Open image in new tab")),
            waitingTime,
        )
        mDevice.waitNotNull(Until.findObject(text("Save image")), waitingTime)
        mDevice.waitNotNull(
            Until.findObject(text("Copy image location")),
            waitingTime,
        )
    }

    fun verifyNotificationDotOnMainMenu() =
        assertUIObjectExists(itemWithResId("$packageName:id/notification_dot"))

    fun dismissContentContextMenu() {
        Log.i(TAG, "dismissContentContextMenu: Trying to click device back button")
        mDevice.pressBack()
        Log.i(TAG, "dismissContentContextMenu: Clicked device back button")
        assertUIObjectExists(itemWithResId("$packageName:id/engineView"))
    }

    fun createBookmark(composeTestRule: ComposeTestRule, url: Uri, folder: String? = null) {
        navigationToolbar {
        }.enterURLAndEnterToBrowser(url) {
            // needs to wait for the right url to load before saving a bookmark
            verifyUrl(url.toString())
        }.openThreeDotMenu {
        }.bookmarkPage {
        }.takeIf { !folder.isNullOrBlank() }?.let {
            it.openThreeDotMenu {
            }.editBookmarkPage(composeTestRule) {
                setParentFolder(folder!!)
                navigateUp()
            }
        }
    }

    fun longClickPDFImage() = longClickPageObject(itemWithResId("pdfjs_internal_id_13R"))

    fun verifyPDFReaderToolbarItems() =
        assertUIObjectExists(
            itemWithResIdContainingText("download", "Download"),
        )

    fun clickSubmitLoginButton() {
        clickPageObject(itemWithResId("submit"))
        assertUIObjectIsGone(itemWithResId("submit"))
        Log.i(TAG, "clickSubmitLoginButton: Waiting for device to be idle for $waitingTimeLong ms")
        mDevice.waitForIdle(waitingTimeLong)
        Log.i(TAG, "clickSubmitLoginButton: Waited for device to be idle for $waitingTimeLong ms")
    }

    fun enterPassword(password: String) {
        clickPageObject(itemWithResId("password"))
        setPageObjectText(itemWithResId("password"), password)

        assertUIObjectIsGone(itemWithText(password))
    }

    /**
     * Get the current playback state of the currently selected tab.
     * The result may be null if there if the currently playing media tab cannot be found in [store]
     *
     * @param store [BrowserStore] from which to get data about the current tab's state.
     * @return nullable [MediaSession.PlaybackState] indicating the media playback state for the current tab.
     */
    private fun getCurrentPlaybackState(store: BrowserStore): MediaSession.PlaybackState? {
        return store.state.selectedTab?.mediaSessionState?.playbackState
    }

    /**
     * Asserts that in [waitingTime] the playback state of the current tab will be [expectedState].
     *
     * @param store [BrowserStore] from which to get data about the current tab's state.
     * @param expectedState [MediaSession.PlaybackState] the playback state that will be asserted
     * @param waitingTime maximum time the test will wait for the playback state to become [expectedState]
     * before failing the assertion.
     */
    fun assertPlaybackState(store: BrowserStore, expectedState: MediaSession.PlaybackState) {
        val startMills = SystemClock.uptimeMillis()
        var currentMills: Long = 0
        while (currentMills <= waitingTime) {
            if (expectedState == getCurrentPlaybackState(store)) return
            currentMills = SystemClock.uptimeMillis() - startMills
        }
        fail("Playback did not moved to state: $expectedState")
    }

    fun swipeNavBarRight(tabUrl: String) {
        // failing to swipe on Firebase sometimes, so it tries again
        try {
            Log.i(TAG, "swipeNavBarRight: Try block")
            Log.i(TAG, "swipeNavBarRight: Trying to perform swipe right action on navigation toolbar")
            navURLBar().swipeRight(2)
            Log.i(TAG, "swipeNavBarRight: Performed swipe right action on navigation toolbar")
            assertUIObjectIsGone(itemWithText(tabUrl))
        } catch (e: AssertionError) {
            Log.i(TAG, "swipeNavBarRight: AssertionError caught, executing fallback methods")
            Log.i(TAG, "swipeNavBarRight: Trying to perform swipe right action on navigation toolbar")
            navURLBar().swipeRight(2)
            Log.i(TAG, "swipeNavBarRight: Performed swipe right action on navigation toolbar")
            assertUIObjectIsGone(itemWithText(tabUrl))
        }
    }

    fun swipeNavBarLeft(tabUrl: String) {
        // failing to swipe on Firebase sometimes, so it tries again
        try {
            Log.i(TAG, "swipeNavBarLeft: Try block")
            Log.i(TAG, "swipeNavBarLeft: Trying to perform swipe left action on navigation toolbar")
            navURLBar().swipeLeft(2)
            Log.i(TAG, "swipeNavBarLeft: Performed swipe left action on navigation toolbar")
            assertUIObjectIsGone(itemWithText(tabUrl))
        } catch (e: AssertionError) {
            Log.i(TAG, "swipeNavBarLeft: AssertionError caught, executing fallback methods")
            Log.i(TAG, "swipeNavBarLeft: Trying to perform swipe left action on navigation toolbar")
            navURLBar().swipeLeft(2)
            Log.i(TAG, "swipeNavBarLeft: Performed swipe left action on navigation toolbar")
            assertUIObjectIsGone(itemWithText(tabUrl))
        }
    }

    fun clickSuggestedLoginsButton() {
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "clickSuggestedLoginsButton: Started try #$i")
                mDevice.waitForObjects(suggestedLogins())
                Log.i(TAG, "clickSuggestedLoginsButton: Trying to click suggested logins button")
                suggestedLogins().click()
                Log.i(TAG, "clickSuggestedLoginsButton: Clicked suggested logins button")
                mDevice.waitForObjects(suggestedLogins())
                break
            } catch (e: UiObjectNotFoundException) {
                Log.i(TAG, "clickSuggestedLoginsButton: UiObjectNotFoundException caught, executing fallback methods")
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    clickPageObject(itemWithResId("username"))
                }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun clickSuggestedLogin(composeTestRule: ComposeTestRule, userName: String) {
        Log.i(TAG, "clickSuggestedLogin: Waiting for the suggested user name: $userName to exist")
        composeTestRule.waitUntilAtLeastOneExists(hasText(userName))
        Log.i(TAG, "clickSuggestedLogin: Waited for the suggested user name: $userName to exist")
        Log.i(TAG, "clickSuggestedLogin: Trying to click $userName login suggestion")
        composeTestRule.onNodeWithText(userName).performClick()
        Log.i(TAG, "clickSuggestedLogin: Clicked $userName login suggestion")
    }

    fun setTextForApartmentTextBox(apartment: String) {
        Log.i(TAG, "setTextForApartmentTextBox: Trying to set the text for the apartment text box to: $apartment")
        itemWithResId("apartment").setText(apartment)
        Log.i(TAG, "setTextForApartmentTextBox: The text for the apartment text box was set to: $apartment")
    }

    fun clearAddressForm() {
        clearTextFieldItem(itemWithResId("streetAddress"))
        clearTextFieldItem(itemWithResId("city"))
        clearTextFieldItem(itemWithResId("country"))
        clearTextFieldItem(itemWithResId("zipCode"))
        clearTextFieldItem(itemWithResId("telephone"))
        clearTextFieldItem(itemWithResId("email"))
    }

    fun clickSelectAddressButton() {
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "clickSelectAddressButton: Started try #$i")
                assertUIObjectExists(selectAddressButton())
                Log.i(TAG, "clickSelectAddressButton: Trying to click the select address button and wait for $waitingTime ms for a new window")
                selectAddressButton().clickAndWaitForNewWindow(waitingTime)
                Log.i(TAG, "clickSelectAddressButton: Clicked the select address button and waited for $waitingTime ms for a new window")

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "clickSelectAddressButton: AssertionError caught, executing fallback methods")
                // Retrying to trigger the prompt, in case we hit https://bugzilla.mozilla.org/show_bug.cgi?id=1816869
                // This should be removed when the bug is fixed.
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    clickPageObject(itemWithResId("city"))
                    clickPageObject(itemWithResId("country"))
                }
            }
        }
    }

    fun verifySelectAddressButtonExists(exists: Boolean) = assertUIObjectExists(selectAddressButton(), exists = exists)

    fun changeCreditCardExpiryDate(expiryDate: String) {
        Log.i(TAG, "changeCreditCardExpiryDate: Trying to set credit card expiry date to: $expiryDate")
        itemWithResId("expiryMonthAndYear").setText(expiryDate)
        Log.i(TAG, "changeCreditCardExpiryDate: Credit card expiry date was set to: $expiryDate")
    }

    fun clickCreditCardNumberTextBox() {
        Log.i(TAG, "clickCreditCardNumberTextBox: Waiting for $waitingTime ms until finding the credit card number text box")
        mDevice.wait(Until.findObject(By.res("cardNumber")), waitingTime)
        Log.i(TAG, "clickCreditCardNumberTextBox: Waited for $waitingTime ms until the credit card number text box was found")
        Log.i(TAG, "clickCreditCardNumberTextBox: Trying to click the credit card number text box")
        mDevice.findObject(By.res("cardNumber")).click()
        Log.i(TAG, "clickCreditCardNumberTextBox: Clicked the credit card number text box")
        Log.i(TAG, "clickCreditCardNumberTextBox: Waiting for $waitingTimeShort ms for $appName window to be updated")
        mDevice.waitForWindowUpdate(appName, waitingTimeShort)
        Log.i(TAG, "clickCreditCardNumberTextBox: Waited for $waitingTimeShort ms for $appName window to be updated")
    }

    fun clickCreditCardFormSubmitButton() {
        Log.i(TAG, "clickCreditCardFormSubmitButton: Trying to click the credit card form submit button and wait for $waitingTime ms for a new window")
        itemWithResId("submit").clickAndWaitForNewWindow(waitingTime)
        Log.i(TAG, "clickCreditCardFormSubmitButton: Clicked the credit card form submit button and waited for $waitingTime ms for a new window")
    }

    fun fillAndSaveCreditCard(cardNumber: String, cardName: String, expiryMonthAndYear: String) {
        Log.i(TAG, "fillAndSaveCreditCard: Tying to set credit card number to: $cardNumber")
        itemWithResId("cardNumber").setText(cardNumber)
        Log.i(TAG, "fillAndSaveCreditCard: Credit card number was set to: $cardNumber")
        waitForAppWindowToBeUpdated()
        Log.i(TAG, "fillAndSaveCreditCard: Trying to set credit card name to: $cardName")
        itemWithResId("nameOnCard").setText(cardName)
        Log.i(TAG, "fillAndSaveCreditCard: Credit card name was set to: $cardName")
        waitForAppWindowToBeUpdated()
        Log.i(TAG, "fillAndSaveCreditCard: Trying to set credit card expiry month and year to: $expiryMonthAndYear")
        itemWithResId("expiryMonthAndYear").setText(expiryMonthAndYear)
        Log.i(TAG, "fillAndSaveCreditCard: Credit card expiry month and year were set to: $expiryMonthAndYear")
        waitForAppWindowToBeUpdated()
        Log.i(TAG, "fillAndSaveCreditCard: Trying to click the credit card form submit button and wait for $waitingTime ms for a new window")
        itemWithResId("submit").clickAndWaitForNewWindow(waitingTime)
        Log.i(TAG, "fillAndSaveCreditCard: Clicked the credit card form submit button and waited for $waitingTime ms for a new window")
        waitForPageToLoad()
        Log.i(TAG, "fillAndSaveCreditCard: Waiting for $waitingTime ms for $packageName window to be updated")
        mDevice.waitForWindowUpdate(packageName, waitingTime)
        Log.i(TAG, "fillAndSaveCreditCard: Waited for $waitingTime ms for $packageName window to be updated")
    }

    fun clickNegativeSaveCreditCardPromptButton() = onView(withId(R.id.save_cancel)).inRoot(isDialog()).click()

    fun verifyUpdateOrSaveCreditCardPromptExists(exists: Boolean) =
        assertUIObjectExists(
            itemWithResId("$packageName:id/save_credit_card_header"),
            exists = exists,
        )

    fun verifySelectCreditCardPromptExists(exists: Boolean) =
        assertUIObjectExists(selectCreditCardButton(), exists = exists)

    fun verifyCreditCardSuggestion(vararg creditCardNumbers: String) {
        for (creditCardNumber in creditCardNumbers) {
            assertUIObjectExists(
                itemWithResIdContainingText(
                    "$packageName:id/credit_card_number",
                    creditCardNumber,
                ),
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun verifySuggestedUserName(composeTestRule: ComposeTestRule, userName: String) {
        Log.i(TAG, "verifySuggestedUserName: Waiting for the suggested user name: $userName to exist")
        composeTestRule.waitUntilAtLeastOneExists(hasText(userName))
        Log.i(TAG, "verifySuggestedUserName: Waited for the suggested user name: $userName to exist")
        Log.i(TAG, "verifySuggestedUserName: Trying to assert that user name: $userName exists")
        composeTestRule.onNodeWithText(userName).assertExists()
        Log.i(TAG, "verifySuggestedUserName: Asserted that user name: $userName exists")
    }

    fun verifyPrefilledLoginCredentials(composeTestRule: ComposeTestRule, userName: String, password: String, credentialsArePrefilled: Boolean) {
        // Sometimes the assertion of the pre-filled logins fails so we are re-trying after refreshing the page
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "verifyPrefilledLoginCredentials: Started try #$i")
                mDevice.waitForObjects(itemWithResId("username"))
                assertItemTextEquals(itemWithResId("username"), expectedText = userName, isEqual = credentialsArePrefilled)
                mDevice.waitForObjects(itemWithResId("password"))
                assertItemTextEquals(itemWithResId("password"), expectedText = password, isEqual = credentialsArePrefilled)

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifyPrefilledLoginCredentials: AssertionError caught, executing fallback methods")
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    browserScreen {
                    }.openThreeDotMenu {
                    }.refreshPage {
                        clearTextFieldItem(itemWithResId("username"))
                        clickSuggestedLoginsButton()
                        verifySuggestedUserName(composeTestRule, userName)
                        clickSuggestedLogin(composeTestRule, userName)
                        clickPageObject(itemWithResId("togglePassword"))
                    }
                }
            }
        }
    }

    fun verifyAutofilledAddress(streetAddress: String) {
        mDevice.waitForObjects(itemWithResIdAndText("streetAddress", streetAddress))
        assertUIObjectExists(itemWithResIdAndText("streetAddress", streetAddress))
    }

    fun verifyManuallyFilledAddress(apartment: String) {
        mDevice.waitForObjects(itemWithResIdAndText("apartment", apartment))
        assertUIObjectExists(itemWithResIdAndText("apartment", apartment))
    }

    fun verifyAutofilledCreditCard(creditCardNumber: String) {
        mDevice.waitForObjects(itemWithResIdAndText("cardNumber", creditCardNumber))
        assertUIObjectExists(itemWithResIdAndText("cardNumber", creditCardNumber))
    }

    fun verifySaveLoginPromptIsDisplayed() =
        assertUIObjectExists(
            itemWithResId("$packageName:id/feature_prompt_login_fragment"),
        )

    fun verifySaveLoginPromptIsNotDisplayed() =
        assertUIObjectExists(
            itemWithResId("$packageName:id/feature_prompt_login_fragment"),
            exists = false,
        )

    fun verifyTrackingProtectionWebContent(state: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "verifyTrackingProtectionWebContent: Started try #$i")
                assertUIObjectExists(itemContainingText(state))

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifyTrackingProtectionWebContent: AssertionError caught, executing fallback methods")
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    Log.e(TAG, "On try $i, trackers are not: $state")

                    navigationToolbar {
                    }.openThreeDotMenu {
                    }.refreshPage {
                    }
                }
            }
        }
    }

    fun selectTime(hour: Int, minute: Int) {
        Log.i(TAG, "selectTime: Trying to select time picker hour: $hour and minute: $minute")
        onView(
            isAssignableFrom(TimePicker::class.java),
        ).inRoot(
            isDialog(),
        ).perform(PickerActions.setTime(hour, minute))
        Log.i(TAG, "selectTime: Selected time picker hour: $hour and minute: $minute")
    }

    fun verifySelectedDate() {
        val currentDate = LocalDate.now()
        val currentDay = currentDate.dayOfMonth
        val currentMonth = currentDate.month
        val currentYear = currentDate.year

        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "verifySelectedDate: Started try #$i")
                assertUIObjectExists(itemContainingText("Selected date is: $currentDate"))

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifySelectedDate: AssertionError caught, executing fallback methods")
                Log.e(TAG, "Selected time isn't displayed ${e.localizedMessage}")

                clickPageObject(itemWithResId("calendar"))
                clickPageObject(itemWithDescription("$currentDay $currentMonth $currentYear"))
                clickPageObject(itemContainingText("OK"))
                clickPageObject(itemWithResId("submitDate"))
            }
        }

        assertUIObjectExists(itemContainingText("Selected date is: $currentDate"))
    }

    fun verifyNoDateIsSelected() {
        val currentDate = LocalDate.now()
        assertTrue(
            itemContainingText("Selected date is: $currentDate").waitUntilGone(waitingTime),
        )
    }

    fun verifySelectedTime(hour: Int, minute: Int) {
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "verifySelectedTime: Started try #$i")
                assertUIObjectExists(itemContainingText("Selected time is: $hour:$minute"))

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifySelectedTime: AssertionError caught, executing fallback methods")
                Log.e(TAG, "Selected time isn't displayed ${e.localizedMessage}")

                clickPageObject(itemWithResId("clock"))
                clickPageObject(itemContainingText("CLEAR"))
                clickPageObject(itemWithResId("clock"))
                selectTime(hour, minute)
                clickPageObject(itemContainingText("OK"))
                clickPageObject(itemWithResId("submitTime"))
            }
        }
        assertUIObjectExists(itemContainingText("Selected time is: $hour:$minute"))
    }

    fun verifySelectedColor(hexValue: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "verifySelectedColor: Started try #$i")
                assertUIObjectExists(itemContainingText("Selected color is: $hexValue"))

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifySelectedColor: AssertionError caught, executing fallback methods")
                Log.e(TAG, "Selected color isn't displayed ${e.localizedMessage}")

                clickPageObject(itemWithResId("colorPicker"))
                clickPageObject(itemWithDescription(hexValue))
                clickPageObject(itemContainingText("SET"))
                clickPageObject(itemWithResId("submitColor"))
            }
        }

        assertUIObjectExists(itemContainingText("Selected color is: $hexValue"))
    }

    fun verifySelectedDropDownOption(optionName: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "verifySelectedDropDownOption: Started try #$i")
                Log.i(TAG, "verifySelectedDropDownOption: Waiting for $waitingTime ms for \"Submit drop down option\" form button to exist")
                mDevice.findObject(
                    UiSelector()
                        .textContains("Submit drop down option")
                        .resourceId("submitOption"),
                ).waitForExists(waitingTime)
                Log.i(TAG, "verifySelectedDropDownOption: Waited for $waitingTime ms for \"Submit drop down option\" form button to exist")
                assertUIObjectExists(itemContainingText("Selected option is: $optionName"))

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifySelectedDropDownOption: AssertionError caught, executing fallback methods")
                Log.e(TAG, "Selected option isn't displayed ${e.localizedMessage}")

                clickPageObject(itemWithResId("dropDown"))
                clickPageObject(itemContainingText(optionName))
                clickPageObject(itemWithResId("submitOption"))
            }
        }

        assertUIObjectExists(itemContainingText("Selected option is: $optionName"))
    }

    fun verifyNoTimeIsSelected(hour: Int, minute: Int) =
        assertUIObjectExists(itemContainingText("Selected date is: $hour:$minute"), exists = false)

    fun verifyColorIsNotSelected(hexValue: String) =
        assertUIObjectExists(itemContainingText("Selected date is: $hexValue"), exists = false)

    fun verifyCookieBannerExists(exists: Boolean) {
        for (i in 1..RETRY_COUNT) {
            Log.i(TAG, "verifyCookieBannerExists: Started try #$i")
            try {
                // Wait for the blocker to kick-in and make the cookie banner disappear
                Log.i(TAG, "verifyCookieBannerExists: Waiting for $waitingTime ms for cookie banner to be gone")
                itemWithResId("cookieConsentBanner").waitUntilGone(waitingTimeLong)
                Log.i(TAG, "verifyCookieBannerExists: Waited for $waitingTime ms for cookie banner to be gone")
                // Assert that the blocker properly dismissed the cookie banner
                assertUIObjectExists(itemWithResId("cookieConsentBanner"), exists = exists)

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifyCookieBannerExists: AssertionError caught, executing fallback methods")
                if (i == RETRY_COUNT) {
                    throw e
                }
            }
        }
    }

    fun verifyCookieBannerBlockerCFRExists(exists: Boolean) =
        assertUIObjectExists(
            itemContainingText(getStringResource(R.string.cookie_banner_cfr_message)),
            exists = exists,
            waitingTime = waitingTimeLong,
        )

    fun verifyOpenLinkInAnotherAppPrompt(appName: String) {
        assertUIObjectExists(
            itemWithResId("$packageName:id/parentPanel"),
            itemContainingText(
                getStringResource(R.string.mozac_feature_applinks_normal_confirm_dialog_title_with_app_name, appName),
            ),
            itemContainingText(
                getStringResource(R.string.mozac_feature_applinks_normal_confirm_dialog_message),
            ),
        )
    }

    fun verifyPrivateBrowsingOpenLinkInAnotherAppPrompt(appName: String, url: String, pageObject: UiObject) {
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "verifyPrivateBrowsingOpenLinkInAnotherAppPrompt: Started try #$i")
                assertUIObjectExists(
                    itemContainingText(
                        getStringResource(R.string.mozac_feature_applinks_confirm_dialog_title_with_app_name, appName),
                    ),
                    itemContainingText(url),
                )

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifyPrivateBrowsingOpenLinkInAnotherAppPrompt: AssertionError caught, executing fallback methods")
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    browserScreen {
                    }.openThreeDotMenu {
                    }.refreshPage {
                        waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
                        clickPageObject(pageObject)
                    }
                }
            }
        }
    }

    fun verifyFindInPageBar(exists: Boolean) =
        assertUIObjectExists(
            itemWithResId("$packageName:id/findInPageView"),
            exists = exists,
        )

    fun verifyConnectionErrorMessage() =
        assertUIObjectExists(
            itemContainingText(getStringResource(R.string.mozac_browser_errorpages_connection_failure_title)),
            itemWithResId("errorTryAgain"),
        )

    fun verifyAddressNotFoundErrorMessage() =
        assertUIObjectExists(
            itemContainingText(getStringResource(R.string.mozac_browser_errorpages_unknown_host_title)),
            itemWithResId("errorTryAgain"),
        )

    fun verifyNoInternetConnectionErrorMessage() =
        assertUIObjectExists(
            itemContainingText(getStringResource(R.string.mozac_browser_errorpages_no_internet_title_2)),
            itemWithResId("errorTryAgain"),
        )

    fun verifyOpenLinksInAppsCFRExists(exists: Boolean) {
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "verifyOpenLinksInAppsCFRExists: Started try #$i")
                assertUIObjectExists(
                    itemWithResId("$packageName:id/banner_container"),
                    itemWithResIdContainingText(
                        "$packageName:id/banner_info_message",
                        getStringResource(R.string.open_in_app_cfr_info_message_2),
                    ),
                    itemWithResIdContainingText(
                        "$packageName:id/dismiss",
                        getStringResource(R.string.open_in_app_cfr_negative_button_text),
                    ),
                    itemWithResIdContainingText(
                        "$packageName:id/action",
                        getStringResource(R.string.open_in_app_cfr_positive_button_text),
                    ),
                    exists = exists,
                )
            } catch (e: AssertionError) {
                Log.i(TAG, "verifyOpenLinksInAppsCFRExists: AssertionError caught, executing fallback methods")
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    browserScreen {
                    }.openThreeDotMenu {
                    }.refreshPage {
                        waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
                    }
                }
            }
        }
    }

    fun verifySurveyButtonDoesNotExist() =
        assertUIObjectIsGone(itemWithText(getStringResource(R.string.preferences_take_survey)))

    fun clickOpenLinksInAppsDismissCFRButton() {
        Log.i(TAG, "clickOpenLinksInAppsDismissCFRButton: Trying to click the open links in apps banner \"Dismiss\" button")
        itemWithResIdContainingText(
            "$packageName:id/dismiss",
            getStringResource(R.string.open_in_app_cfr_negative_button_text),
        ).click()
        Log.i(TAG, "clickOpenLinksInAppsDismissCFRButton: Clicked the open links in apps banner \"Dismiss\" button")
    }

    fun clickTakeSurveyButton() {
        val button = mDevice.findObject(
            UiSelector().text(
                getStringResource(
                    R.string.preferences_take_survey,
                ),
            ),
        )
        button.waitForExists(waitingTime)
        button.click()
    }
    fun clickNoThanksSurveyButton() {
        val button = mDevice.findObject(
            UiSelector().text(
                getStringResource(
                    R.string.preferences_not_take_survey,
                ),
            ),
        )
        button.waitForExists(waitingTime)
        button.click()
    }

    fun longClickToolbar() {
        Log.i(TAG, "longClickToolbar: Trying to long click the toolbar")
        onView(withId(R.id.mozac_browser_toolbar_url_view)).perform(longClick())
        Log.i(TAG, "longClickToolbar: Long clicked the toolbar")
    }

    fun verifyDownloadPromptIsDismissed() =
        assertUIObjectExists(
            itemWithResId("$packageName:id/viewDynamicDownloadDialog"),
            exists = false,
        )

    fun verifyCancelPrivateDownloadsPrompt(numberOfActiveDownloads: String) {
        assertUIObjectExists(
            itemWithResIdContainingText(
                "$packageName:id/title",
                getStringResource(R.string.mozac_feature_downloads_cancel_active_downloads_warning_content_title),
            ),
            itemWithResIdContainingText(
                "$packageName:id/body",
                "If you close all Private tabs now, $numberOfActiveDownloads download will be canceled. Are you sure you want to leave Private Browsing?",
            ),
            itemWithResIdContainingText(
                "$packageName:id/deny_button",
                getStringResource(R.string.mozac_feature_downloads_cancel_active_private_downloads_deny),
            ),
            itemWithResIdContainingText(
                "$packageName:id/accept_button",
                getStringResource(R.string.mozac_feature_downloads_cancel_active_downloads_accept),
            ),
        )
    }

    fun clickStayInPrivateBrowsingPromptButton() {
        Log.i(TAG, "clickStayInPrivateBrowsingPromptButton: Trying to click the \"STAY IN PRIVATE BROWSING\" prompt button")
        itemWithResIdContainingText(
            "$packageName:id/deny_button",
            getStringResource(R.string.mozac_feature_downloads_cancel_active_private_downloads_deny),
        ).click()
        Log.i(TAG, "clickStayInPrivateBrowsingPromptButton: Clicked the \"STAY IN PRIVATE BROWSING\" prompt button")
    }

    fun clickCancelPrivateDownloadsPromptButton() {
        Log.i(TAG, "clickCancelPrivateDownloadsPromptButton: Trying to click the \"CANCEL DOWNLOADS\" prompt button")
        itemWithResIdContainingText(
            "$packageName:id/accept_button",
            getStringResource(R.string.mozac_feature_downloads_cancel_active_downloads_accept),
        ).click()
        Log.i(TAG, "clickCancelPrivateDownloadsPromptButton: Clicked the \"CANCEL DOWNLOADS\" prompt button")
        Log.i(TAG, "clickCancelPrivateDownloadsPromptButton: Waiting for $waitingTime ms for $packageName window to be updated")
        mDevice.waitForWindowUpdate(packageName, waitingTime)
        Log.i(TAG, "clickCancelPrivateDownloadsPromptButton: Waited for $waitingTime ms for $packageName window to be updated")
    }

    fun fillPdfForm(name: String) {
        // Set PDF form text for the text box
        Log.i(TAG, "fillPdfForm: Trying to set the text of the PDF form text box to: $name")
        itemWithResId("pdfjs_internal_id_10R").setText(name)
        Log.i(TAG, "fillPdfForm: PDF form text box text was set to: $name")
        Log.i(TAG, "fillPdfForm: Waiting for $waitingTime ms for $packageName window to be updated")
        mDevice.waitForWindowUpdate(packageName, waitingTime)
        Log.i(TAG, "fillPdfForm: Waited for $waitingTime ms for $packageName window to be updated")

        // Close the keyboard
        Log.i(TAG, "fillPdfForm: Trying to close the keyboard using device back button")
        mDevice.pressBack()
        Log.i(TAG, "fillPdfForm: Closed the keyboard using device back button")
        Log.i(TAG, "fillPdfForm: Waiting for $waitingTime ms for $packageName window to be updated")
        mDevice.waitForWindowUpdate(packageName, waitingTime)
        Log.i(TAG, "fillPdfForm: Waited for $waitingTime ms for $packageName window to be updated")

        // Click PDF form check box
        Log.i(TAG, "fillPdfForm: Trying to click the PDF form check box")
        itemWithResId("pdfjs_internal_id_11R").click()
        Log.i(TAG, "fillPdfForm: Clicked PDF form check box")
    }

    fun refreshPageFromRedesignedToolbar() {
        assertUIObjectExists(itemWithDescription(getStringResource(R.string.browser_menu_refresh)))
        Log.i(TAG, "refreshPageFromRedesignedToolbar: Trying to click the \"Refresh\" button")
        itemWithDescription(getStringResource(R.string.browser_menu_refresh)).click()
        Log.i(TAG, "refreshPageFromRedesignedToolbar: Clicked the \"Refresh\" button")
    }

    fun goToPreviousPageFromRedesignedToolbar() {
        Log.i(TAG, "goToPreviousPageFromRedesignedToolbar: Trying to click the \"Back\" button")
        itemWithDescription(getStringResource(R.string.browser_menu_back)).click()
        Log.i(TAG, "goToPreviousPageFromRedesignedToolbar: Clicked the \"Back\" button")
    }

    fun goForwardFromRedesignedToolbar() {
        Log.i(TAG, "goForwardFromRedesignedToolbar: Trying to click the \"Forward\" button")
        itemWithDescription(getStringResource(R.string.browser_menu_forward)).click()
        Log.i(TAG, "goForwardFromRedesignedToolbar: Clicked the \"Forward\" button")
    }

    fun getCurrentUrl(): String {
        val searchBar = searchBar()
        waitForPageToLoad()
        return searchBar.getText()
    }

    fun selectToAlwaysOpenDownloadedFileWithApp(appName: String) {
        Log.i(TAG, "selectToAlwaysOpenDownloadedFileWithApp: Trying to click $appName from the \"Open with\" prompt")
        itemWithResIdContainingText("android:id/text1", appName).click()
        Log.i(TAG, "selectToAlwaysOpenDownloadedFileWithApp: Clicked $appName from the \"Open with\" prompt")
        Log.i(TAG, "selectToAlwaysOpenDownloadedFileWithApp: Trying to click the \"Always\" button from the \"Open with\" prompt")
        itemWithResId("android:id/button_always").click()
        Log.i(TAG, "selectToAlwaysOpenDownloadedFileWithApp: Clicked the \"Always\" button from the \"Open with\" prompt")
    }

    fun verifyWebCompatReporterViewItems(composeTestRule: ComposeTestRule, websiteURL: String) {
        Log.i(TAG, "verifyWebCompatReporterViewItems: Trying to verify that the report broken site description is displayed")
        composeTestRule.onNodeWithContentDescription(
            getStringResource(R.string.webcompat_reporter_description_2, appName) + " " + getStringResource(R.string.a11y_links_available),
        ).assertIsDisplayed()
        Log.i(TAG, "verifyWebCompatReporterViewItems: Verified that the report broken site description is displayed")
        Log.i(TAG, "verifyWebCompatReporterViewItems: Trying to verify that the \"URL\" header is displayed")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_label_url)).assertIsDisplayed()
        Log.i(TAG, "verifyWebCompatReporterViewItems: Verified that the \"What’s broken?\" header is displayed")
        Log.i(TAG, "verifyWebCompatReporterViewItems: Trying to verify that the $websiteURL url is displayed")
        composeTestRule.onNodeWithText(websiteURL).assertIsDisplayed()
        Log.i(TAG, "verifyWebCompatReporterViewItems: Verified that the $websiteURL url is displayed")
        Log.i(TAG, "verifyWebCompatReporterViewItems: Trying to verify that the \"What’s broken?\" header is displayed")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_label_whats_broken_2)).assertIsDisplayed()
        Log.i(TAG, "verifyWebCompatReporterViewItems: Verified that the \"What’s broken?\" header is displayed")
        Log.i(TAG, "verifyWebCompatReporterViewItems: Trying to verify that the \"Choose a reason\" field is displayed")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_choose_reason_2)).assertIsDisplayed()
        Log.i(TAG, "verifyWebCompatReporterViewItems: Verified that the \"Choose a reason\" field is displayed")
        Log.i(TAG, "verifyWebCompatReporterViewItems: Trying to verify that the \"Please choose a reason\" error message is displayed")
        composeTestRule.onNodeWithTag(BROKEN_SITE_REPORTER_CHOOSE_REASON_BUTTON).assertIsDisplayed()
        Log.i(TAG, "verifyWebCompatReporterViewItems: Verified that the \"Please choose a reason\" error message is displayed")
        Log.i(TAG, "verifyWebCompatReporterViewItems: Trying to verify that the \"Describe the problem (optional)\" field is displayed")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_label_description)).assertIsDisplayed()
        Log.i(TAG, "verifyWebCompatReporterViewItems: Verified that the \"Describe the problem (optional)\" field is displayed")
        if (appContext.components.core.engine.version.releaseChannel !== EngineReleaseChannel.RELEASE) {
            Log.i(
                TAG, "Release channel is ${appContext.components.core.engine.version.releaseChannel}",
            )
            Log.i(
                TAG,
                "verifyWebCompatReporterViewItems: Trying to verify that the \"Send more info\" link is displayed",
            )
            composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_send_more_info))
                .assertIsDisplayed()
            Log.i(
                TAG,
                "verifyWebCompatReporterViewItems: Verified that the \"Send more info\" link is displayed",
            )
        }
        Log.i(TAG, "verifyWebCompatReporterViewItems: Trying to verify that the \"Cancel\" button is displayed")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_cancel))
        Log.i(TAG, "verifyWebCompatReporterViewItems: Verified that the \"Cancel \" button is displayed")
        Log.i(TAG, "verifyWebCompatReporterViewItems: Trying to verify that the \"Send\" button is displayed")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_send)).assertIsDisplayed()
        Log.i(TAG, "verifyWebCompatReporterViewItems: Verified that the \"Send \" button is displayed")
    }

    fun verifyWhatIsBrokenField(composeTestRule: ComposeTestRule) {
        Log.i(TAG, "verifyWhatIsBrokenField: Trying to verify that the \"What’s broken?\" header is displayed")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_label_whats_broken_2)).assertIsDisplayed()
        Log.i(TAG, "verifyWhatIsBrokenField: Verified that the \"What’s broken?\" header is displayed")
        Log.i(TAG, "verifyWhatIsBrokenField: Trying to verify that the \"Choose a reason\" field is displayed")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_choose_reason_2)).assertIsDisplayed()
        Log.i(TAG, "verifyWhatIsBrokenField: Verified that the \"Choose a reason\" field is displayed")
        Log.i(TAG, "verifyWhatIsBrokenField: Trying to verify that the \"Please choose a reason\" error message is displayed")
        composeTestRule.onNodeWithTag(BROKEN_SITE_REPORTER_CHOOSE_REASON_BUTTON).assertIsDisplayed()
        Log.i(TAG, "verifyWhatIsBrokenField: Verified that the \"Please choose a reason\" error message is displayed")
    }

    fun verifyChooseReasonErrorMessageIsNotDisplayed(composeTestRule: ComposeTestRule) {
        Log.i(TAG, "verifyChooseReasonErrorMessageIsNotDisplayed: Trying to verify that the \"Please choose a reason\" error message is not displayed")
        composeTestRule.onNodeWithTag(BROKEN_SITE_REPORTER_CHOOSE_REASON_BUTTON).assertIsNotDisplayed()
        Log.i(TAG, "verifyChooseReasonErrorMessageIsNotDisplayed: Verified that the \"Please choose a reason\" error message is not displayed")
    }

    fun clickChooseReasonField(composeTestRule: ComposeTestRule) {
        Log.i(TAG, "clickChooseReasonField: Trying to click the \"Choose a reason\" field")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_choose_reason_2))
            .performClick()
        Log.i(TAG, "clickChooseReasonField: Trying to clicked the \"Choose a reason\" field")
    }

    fun clickSiteDoesNotLoadReason(composeTestRule: ComposeTestRule) {
        Log.i(TAG, "clickSiteDoesNotLoadReason: Trying to click the \"Site doesn’t load\" reason option")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_reason_load))
            .performClick()
        Log.i(TAG, "clickSiteDoesNotLoadReason: Clicked the \"Site doesn’t load\" reason option")
    }

    fun clickBrokenSiteFormCancelButton(composeTestRule: ComposeTestRule) {
        Log.i(TAG, "clickBrokenSiteFormCancelButton: Trying to click the \"Cancel\" button")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_cancel))
            .performClick()
        Log.i(TAG, "clickBrokenSiteFormCancelButton: Clicked the \"Cancel\" button")
    }

    fun clickBrokenSiteFormSendButton(composeTestRule: ComposeTestRule) {
        Log.i(TAG, "clickBrokenSiteFormSendButton: Trying to click the \"Cancel\" button")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_send))
            .performClick()
        Log.i(TAG, "clickBrokenSiteFormSendButton: Clicked the \"Cancel\" button")
    }

    fun describeBrokenSiteProblem(composeTestRule: ComposeTestRule, problemDescription: String) {
        Log.i(TAG, "describeBrokenSiteProblem: Trying to click the \"Describe the problem (optional)\" field")
        composeTestRule.onNodeWithText(getStringResource(R.string.webcompat_reporter_label_description)).performClick()
        Log.i(TAG, "describeBrokenSiteProblem: Clicked the \"Describe the problem (optional)\" field")
        Log.i(TAG, "describeBrokenSiteProblem: Trying to set the text of the \"Describe the problem (optional)\" field to $problemDescription")
        composeTestRule.onNode(hasText(getStringResource(R.string.webcompat_reporter_label_description))).performTextInput(problemDescription)
        Log.i(TAG, "describeBrokenSiteProblem: Set the text of the \"Describe the problem (optional)\" field to $problemDescription")
    }

    fun verifyBrokenSiteProblem(composeTestRule: ComposeTestRule, problemDescription: String, isDisplayed: Boolean) {
        if (isDisplayed) {
            Log.i(TAG, "verifyBrokenSiteProblem: Trying to verify that the $problemDescription broken site problem is displayed")
            composeTestRule.onNodeWithText(problemDescription).assertIsDisplayed()
            Log.i(TAG, "verifyBrokenSiteProblem: Verified that the $problemDescription broken site problem is displayed")
        } else {
            Log.i(TAG, "verifyBrokenSiteProblem: Trying to verify that the $problemDescription broken site problem is not displayed")
            composeTestRule.onNodeWithText(problemDescription).assertIsNotDisplayed()
            Log.i(TAG, "verifyBrokenSiteProblem: Verified that the $problemDescription broken site problem is not displayed")
        }
    }

    fun verifySendButtonIsEnabled(composeTestRule: ComposeTestRule, isEnabled: Boolean) {
        if (isEnabled) {
            Log.i(TAG, "verifySendButtonIsEnabled: Trying to verify that the the \"Send\" button is enabled")
            composeTestRule.onNodeWithTag(BROKEN_SITE_REPORTER_SEND_BUTTON).assertIsEnabled()
            Log.i(TAG, "verifySendButtonIsEnabled: Verified that the the \"Send\" button is enabled")
        } else {
            Log.i(TAG, "verifySendButtonIsEnabled: Trying to verify that the the \"Send\" button is not enabled")
            composeTestRule.onNodeWithTag(BROKEN_SITE_REPORTER_SEND_BUTTON).assertIsNotEnabled()
            Log.i(TAG, "verifySendButtonIsEnabled: Verified that the the \"Send\" button is not enabled")
        }
    }

    fun verifyToolsMenuDoesNotExist() {
        assertUIObjectIsGone(itemWithDescription(getStringResource(R.string.browser_tools_menu_handlebar_content_description)))
    }

    fun verifySaveMenuDoesNotExist() {
        assertUIObjectIsGone(itemWithDescription(getStringResource(R.string.browser_save_menu_handlebar_content_description)))
    }

    fun verifyExtensionsMenuDoesNotExist() {
        assertUIObjectIsGone(itemWithDescription(getStringResource(R.string.browser_extensions_menu_handlebar_content_description)))
    }

    fun verifyExtensionsPromotionBannerLearnMoreLinkURL() {
        try {
            verifyUrl("support.mozilla.org/en-US/kb/find-and-install-add-ons-firefox-android")
        } catch (e: AssertionError) {
            Log.i(TAG, "verifyExtensionsPromotionBannerLearnMoreLinkURL: AssertionError caught, checking redirect URL")
            verifyUrl(
                SupportUtils.getSumoURLForTopic(appContext, SupportUtils.SumoTopic.FIND_INSTALL_ADDONS).replace("https://", ""),
            )
        }
    }

    fun verifyWebCompatPageItemExists(itemText: String, isSmartBlockFixesItem: Boolean = false) {
        for (i in 1..RETRY_COUNT) {
            try {
                Log.i(TAG, "verifyWebCompatPageContent: Started try #$i")
                assertUIObjectExists(itemContainingText(itemText))

                break
            } catch (e: AssertionError) {
                Log.i(TAG, "verifyWebCompatPageContent: verifyWebCompatPageContent caught, executing fallback methods")
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    browserScreen {
                    }.openThreeDotMenu {
                    }.refreshPage {
                        waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
                    }
                    if (isSmartBlockFixesItem) {
                        clickWebCompatPageItem("SmartBlock Fixes")
                    }
                }
            }
        }
    }

    fun clickWebCompatPageItem(itemText: String) {
        clickPageObject(itemContainingText(itemText))
    }

    class Transition {
        fun openThreeDotMenu(interact: ThreeDotMenuMainRobot.() -> Unit): ThreeDotMenuMainRobot.Transition {
            Log.i(TAG, "openThreeDotMenu: Waiting for device to be idle for $waitingTime ms")
            mDevice.waitForIdle(waitingTime)
            Log.i(TAG, "openThreeDotMenu: Device was idle for $waitingTime ms")
            Log.i(TAG, "openThreeDotMenu: Trying to click the main menu button")
            threeDotButton().perform(click())
            Log.i(TAG, "openThreeDotMenu: Clicked the main menu button")

            ThreeDotMenuMainRobot().interact()
            return ThreeDotMenuMainRobot.Transition()
        }

        fun openThreeDotMenu(composeTestRule: ComposeTestRule, interact: ThreeDotMenuMainRobotCompose.() -> Unit): ThreeDotMenuMainRobotCompose.Transition {
            Log.i(TAG, "openThreeDotMenu: Trying to click main menu button")
            itemWithDescription(getStringResource(R.string.content_description_menu)).click()
            Log.i(TAG, "openThreeDotMenu: Clicked main menu button")

            ThreeDotMenuMainRobotCompose(composeTestRule).interact()
            return ThreeDotMenuMainRobotCompose.Transition(composeTestRule)
        }

        fun openNavigationToolbar(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
            navURLBar().waitForExists(waitingTime)
            Log.i(TAG, "openNavigationToolbar: Trying to click the address bar.")
            navURLBar().click()
            Log.i(TAG, "openNavigationToolbar: Clicked the address bar.")
            Log.i(TAG, "openNavigationToolbar: Waiting for $waitingTime ms for for search bar to exist")
            searchBar().waitForExists(waitingTime)
            Log.i(TAG, "openNavigationToolbar: Waited for $waitingTime ms for for search bar to exist")

            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }

        fun openTabDrawer(composeTestRule: HomeActivityComposeTestRule, interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            for (i in 1..RETRY_COUNT) {
                try {
                    Log.i(TAG, "openTabDrawer: Started try #$i")
                    mDevice.waitForObjects(
                        mDevice.findObject(
                            UiSelector()
                                .resourceId("$packageName:id/mozac_browser_toolbar_browser_actions"),
                        ),
                        waitingTime,
                    )
                    Log.i(TAG, "openTabDrawer: Trying to click the tab counter button")
                    tabsCounter().click()
                    Log.i(TAG, "openTabDrawer: Clicked the tab counter button")
                    Log.i(TAG, "openTabDrawer: Trying to verify the tabs tray exists")
                    composeTestRule.onNodeWithTag(TabsTrayTestTag.TABS_TRAY).assertExists()
                    Log.i(TAG, "openTabDrawer: Verified the tabs tray exists")

                    break
                } catch (e: AssertionError) {
                    Log.i(TAG, "openTabDrawer: AssertionError caught, executing fallback methods")
                    if (i == RETRY_COUNT) {
                        throw e
                    } else {
                        Log.i(TAG, "openTabDrawer: Waiting for device to be idle")
                        mDevice.waitForIdle()
                        Log.i(TAG, "openTabDrawer: Waited for device to be idle")
                    }
                }
            }
            Log.i(TAG, "openTabDrawer: Trying to verify the tabs tray new tab FAB button exists")
            composeTestRule.onNodeWithTag(TabsTrayTestTag.FAB).assertExists()
            Log.i(TAG, "openTabDrawer: Verified the tabs tray new tab FAB button exists")

            TabDrawerRobot(composeTestRule).interact()
            return TabDrawerRobot.Transition(composeTestRule)
        }

        fun openTabDrawerFromRedesignedToolbar(composeTestRule: HomeActivityComposeTestRule, interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            for (i in 1..RETRY_COUNT) {
                try {
                    Log.i(TAG, "openTabDrawerFromRedesignedToolbar: Started try #$i")
                    assertUIObjectExists(tabsCounterFromRedesignedToolbar())
                    Log.i(TAG, "openTabDrawerFromRedesignedToolbar: Trying to click the tab counter button")
                    tabsCounter().click()
                    Log.i(TAG, "openTabDrawerFromRedesignedToolbar: Clicked the tab counter button")
                    Log.i(TAG, "openTabDrawerFromRedesignedToolbar: Trying to verify the tabs tray exists")
                    composeTestRule.onNodeWithTag(TabsTrayTestTag.TABS_TRAY).assertExists()
                    Log.i(TAG, "openTabDrawer: Verified the tabs tray exists")

                    break
                } catch (e: AssertionError) {
                    Log.i(TAG, "openTabDrawerFromRedesignedToolbar: AssertionError caught, executing fallback methods")
                    if (i == RETRY_COUNT) {
                        throw e
                    } else {
                        Log.i(TAG, "openTabDrawerFromRedesignedToolbar: Waiting for device to be idle")
                        mDevice.waitForIdle()
                        Log.i(TAG, "openTabDrawerFromRedesignedToolbar: Waited for device to be idle")
                    }
                }
            }
            Log.i(TAG, "openTabDrawerFromRedesignedToolbar: Trying to verify the tabs tray new tab FAB button exists")
            composeTestRule.onNodeWithTag(TabsTrayTestTag.FAB).assertExists()
            Log.i(TAG, "openTabDrawerFromRedesignedToolbar: Verified the tabs tray new tab FAB button exists")

            TabDrawerRobot(composeTestRule).interact()
            return TabDrawerRobot.Transition(composeTestRule)
        }

        fun openNotificationShade(interact: NotificationRobot.() -> Unit): NotificationRobot.Transition {
            Log.i(TAG, "openNotificationShade: Trying to open the notification tray")
            mDevice.openNotification()
            Log.i(TAG, "openNotificationShade: Opened the notification tray")

            NotificationRobot().interact()
            return NotificationRobot.Transition()
        }

        @OptIn(ExperimentalTestApi::class)
        fun goToHomescreen(composeTestRule: ComposeTestRule, interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            Log.i(TAG, "goToHomescreen: Trying to click the go to home screen button.")
            itemWithResId("$packageName:id/mozac_browser_toolbar_navigation_actions").click()
            Log.i(TAG, "goToHomescreen: Clicked the go to home screen button.")
            Log.i(TAG, "goToHomescreen: Waiting for home screen to exist")
            composeTestRule.waitUntilAtLeastOneExists(hasTestTag(HOMEPAGE))
            Log.i(TAG, "goToHomescreen: Waited for home screen to exist")

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            Log.i(TAG, "goBack: Trying to click device back button")
            mDevice.pressBack()
            Log.i(TAG, "goBack: Clicked device back button")

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickTabCrashedCloseButton(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            clickPageObject(itemWithText("Close tab"))
            Log.i(TAG, "clickTabCrashedCloseButton: Waiting for device to be idle")
            mDevice.waitForIdle()
            Log.i(TAG, "clickTabCrashedCloseButton: Waited for device to be idle")

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickShareSelectedText(interact: ShareOverlayRobot.() -> Unit): ShareOverlayRobot.Transition {
            clickContextMenuItem("Share")

            ShareOverlayRobot().interact()
            return ShareOverlayRobot.Transition()
        }

        fun clickDownloadLink(title: String, interact: DownloadRobot.() -> Unit): DownloadRobot.Transition {
            clickPageObject(itemContainingText(title))

            DownloadRobot().interact()
            return DownloadRobot.Transition()
        }

        fun clickStartCameraButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Open camera"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickStartMicrophoneButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Open microphone"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickStartAudioVideoButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Camera & Microphone"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickOpenNotificationButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Open notifications dialogue"))
            mDevice.waitForObjects(mDevice.findObject(UiSelector().textContains("Allow to send notifications?")))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickGetLocationButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Test page used for testing permissions located at https://mozilla-mobile.github.io/testapp/permissions
            clickPageObject(itemWithText("Get Location"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickRequestStorageAccessButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            for (i in 1..RETRY_COUNT) {
                Log.i(TAG, "clickRequestStorageAccessButton: Started try #$i")
                try {
                    // Clicks the "request storage access" button from the "cross-site-cookies.html" local asset
                    clickPageObject(itemContainingText("requestStorageAccess()"))
                    assertUIObjectExists(
                        itemWithResId("$packageName:id/deny_button"),
                        itemWithResId("$packageName:id/allow_button"),
                    )

                    break
                } catch (e: AssertionError) {
                    Log.i(TAG, "clickRequestStorageAccessButton: AssertionError caught, executing fallback methods")
                    if (i == RETRY_COUNT) {
                        throw e
                    }
                }
            }

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickRequestPersistentStorageAccessButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Clicks the "Persistent storage" button from "https://mozilla-mobile.github.io/testapp/permissions"
            clickPageObject(itemWithResId("persistentStorageButton"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun clickRequestDRMControlledContentAccessButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            // Clicks the "DRM-controlled content" button from "https://mozilla-mobile.github.io/testapp/permissions"
            clickPageObject(itemWithResId("drmPermissionButton"))

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }

        fun openSiteSecuritySheet(interact: SiteSecurityRobot.() -> Unit): SiteSecurityRobot.Transition {
            Log.i(TAG, "openSiteSecuritySheet: Waiting for $waitingTime ms for site security toolbar button to exist")
            siteInfoToolbarButton().waitForExists(waitingTime)
            Log.i(TAG, "openSiteSecuritySheet: Waited for $waitingTime ms for site security toolbar button to exist")
            Log.i(TAG, "openSiteSecuritySheet: Trying to click the site security toolbar button and wait for $waitingTime ms for a new window")
            siteInfoToolbarButton().clickAndWaitForNewWindow(waitingTime)
            Log.i(TAG, "openSiteSecuritySheet: Clicked the site security toolbar button and waited for $waitingTime ms for a new window")

            SiteSecurityRobot().interact()
            return SiteSecurityRobot.Transition()
        }

        fun clickManageAddressButton(interact: SettingsSubMenuAutofillRobot.() -> Unit): SettingsSubMenuAutofillRobot.Transition {
            Log.i(TAG, "clickManageAddressButton: Trying to click the manage address button and wait for $waitingTime ms for a new window")
            itemWithResId("$packageName:id/manage_addresses")
                .clickAndWaitForNewWindow(waitingTime)
            Log.i(TAG, "clickManageAddressButton: Clicked the manage address button and waited for $waitingTime ms for a new window")

            SettingsSubMenuAutofillRobot().interact()
            return SettingsSubMenuAutofillRobot.Transition()
        }

        fun clickManageCreditCardsButton(interact: SettingsSubMenuAutofillRobot.() -> Unit): SettingsSubMenuAutofillRobot.Transition {
            Log.i(TAG, "clickManageCreditCardsButton: Trying to click the manage credit cards button and wait for $waitingTime ms for a new window")
            itemWithResId("$packageName:id/manage_credit_cards")
                .clickAndWaitForNewWindow(waitingTime)
            Log.i(TAG, "clickManageCreditCardsButton: Clicked the manage credit cards button and waited for $waitingTime ms for a new window")

            SettingsSubMenuAutofillRobot().interact()
            return SettingsSubMenuAutofillRobot.Transition()
        }

        fun clickOpenLinksInAppsGoToSettingsCFRButton(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            Log.i(TAG, "clickOpenLinksInAppsGoToSettingsCFRButton: Trying to click the \"Go to settings\" open links in apps CFR button and wait for $waitingTime ms for a new window")
            itemWithResIdContainingText(
                "$packageName:id/action",
                getStringResource(R.string.open_in_app_cfr_positive_button_text),
            ).clickAndWaitForNewWindow(waitingTime)
            Log.i(TAG, "clickOpenLinksInAppsGoToSettingsCFRButton: Clicked the \"Go to settings\" open links in apps CFR button and waited for $waitingTime ms for a new window")

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun clickDownloadPDFButton(interact: DownloadRobot.() -> Unit): DownloadRobot.Transition {
            Log.i(TAG, "clickDownloadPDFButton: Trying to click the download PDF button")
            itemWithResIdContainingText(
                "download",
                "Download",
            ).click()
            Log.i(TAG, "clickDownloadPDFButton: Clicked the download PDF button")

            DownloadRobot().interact()
            return DownloadRobot.Transition()
        }

        fun clickShareButtonFromRedesignedToolbar(interact: ShareOverlayRobot.() -> Unit): ShareOverlayRobot.Transition {
            Log.i(TAG, "clickShareButtonFromRedesignedToolbar: Trying to click the \"Share\" button")
            itemWithDescription(getStringResource(R.string.share_button_content_description)).click()
            Log.i(TAG, "clickShareButtonFromRedesignedToolbar: Clicked the \"Share\" button")
            mDevice.waitNotNull(Until.findObject(By.text("ALL ACTIONS")), waitingTime)

            ShareOverlayRobot().interact()
            return ShareOverlayRobot.Transition()
        }

        fun closeWebCompatReporter(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            Log.i(TAG, "clickShareButtonFromRedesignedToolbar: Trying to click the \"Navigate back\" button")
            itemWithDescription("Navigate back").click()
            Log.i(TAG, "clickShareButtonFromRedesignedToolbar: Clicked the \"Navigate back\" button")

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

fun browserScreen(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
    BrowserRobot().interact()
    return BrowserRobot.Transition()
}

private fun navURLBar() = itemWithResId("$packageName:id/toolbar")

private fun searchBar() = itemWithResId("$packageName:id/mozac_browser_toolbar_url_view")

private fun threeDotButton() = onView(withContentDescription("Menu"))

private fun tabsCounter() =
    mDevice.findObject(By.res("$packageName:id/counter_root"))

private fun tabsCounterFromRedesignedToolbar() = itemWithResId("$packageName:id/counter_box")

private fun progressBar() =
    itemWithResId("$packageName:id/mozac_browser_toolbar_progress")

private fun suggestedLogins() = itemWithResId("$packageName:id/loginSelectBar")
private fun selectAddressButton() = itemWithResId("$packageName:id/select_address_header")
private fun selectCreditCardButton() = itemWithResId("$packageName:id/select_credit_card_header")

private fun siteInfoToolbarButton() =
    itemWithResId("$packageName:id/mozac_browser_toolbar_site_info_indicator")

fun clickPageObject(item: UiObject) {
    for (i in 1..RETRY_COUNT) {
        try {
            Log.i(TAG, "clickPageObject: Started try #$i")
            Log.i(TAG, "clickPageObject: Waiting for $waitingTime ms for ${item.selector} to exist")
            item.waitForExists(waitingTime)
            Log.i(TAG, "clickPageObject: Waited for $waitingTime ms for ${item.selector} to exist")
            Log.i(TAG, "clickPageObject: Trying to click ${item.selector}")
            item.click()
            Log.i(TAG, "clickPageObject: Clicked ${item.selector}")

            break
        } catch (e: UiObjectNotFoundException) {
            Log.i(TAG, "clickPageObject: UiObjectNotFoundException caught, executing fallback methods")
            if (i == RETRY_COUNT) {
                throw e
            } else {
                browserScreen {
                }.openThreeDotMenu {
                }.refreshPage {
                    waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
                }
            }
        }
    }
}

fun longClickPageObject(item: UiObject) {
    for (i in 1..RETRY_COUNT) {
        try {
            Log.i(TAG, "longClickPageObject: Started try #$i")
            Log.i(TAG, "longClickPageObject: Waiting for $waitingTime ms for ${item.selector} to exist")
            item.waitForExists(waitingTime)
            Log.i(TAG, "longClickPageObject: Waited for $waitingTime ms for ${item.selector} to exist")
            Log.i(TAG, "longClickPageObject: Trying to long click ${item.selector}")
            item.longClick()
            Log.i(TAG, "longClickPageObject: Long clicked ${item.selector}")

            break
        } catch (e: UiObjectNotFoundException) {
            Log.i(TAG, "longClickPageObject: UiObjectNotFoundException caught, executing fallback methods")
            if (i == RETRY_COUNT) {
                throw e
            } else {
                browserScreen {
                }.openThreeDotMenu {
                }.refreshPage {
                    waitForPageToLoad()
                }
            }
        }
    }
}

fun clickContextMenuItem(item: String) {
    mDevice.waitNotNull(
        Until.findObject(text(item)),
        waitingTimeShort,
    )
    Log.i(TAG, "clickContextMenuItem: Trying to click context menu item: $item")
    mDevice.findObject(text(item)).click()
    Log.i(TAG, "clickContextMenuItem: Clicked context menu item: $item")
    Log.i(TAG, "clickContextMenuItem: Waiting for $waitingTimeShort ms for $packageName window to be updated")
    mDevice.waitForWindowUpdate(packageName, waitingTimeShort)
    Log.i(TAG, "clickContextMenuItem: Waiting for $waitingTimeShort ms for $packageName window to be updated")
}

fun setPageObjectText(webPageItem: UiObject, text: String) {
    for (i in 1..RETRY_COUNT) {
        Log.i(TAG, "setPageObjectText: Started try #$i")
        try {
            webPageItem.also {
                Log.i(TAG, "setPageObjectText: Waiting for $waitingTime ms for ${webPageItem.selector} to exist")
                it.waitForExists(waitingTime)
                Log.i(TAG, "setPageObjectText: Waited for $waitingTime ms for ${webPageItem.selector} to exist")
                Log.i(TAG, "setPageObjectText: Trying to clear ${webPageItem.selector} text field")
                it.clearTextField()
                Log.i(TAG, "setPageObjectText: Cleared ${webPageItem.selector} text field")
                Log.i(TAG, "setPageObjectText: Trying to set ${webPageItem.selector} text to $text")
                it.setText(text)
                Log.i(TAG, "setPageObjectText: ${webPageItem.selector} text was set to $text")
            }

            break
        } catch (e: UiObjectNotFoundException) {
            Log.i(TAG, "setPageObjectText: UiObjectNotFoundException caught, executing fallback methods")
            if (i == RETRY_COUNT) {
                throw e
            } else {
                browserScreen {
                }.openThreeDotMenu {
                }.refreshPage {
                    waitForPageToLoad(pageLoadWaitingTime = waitingTimeLong)
                }
            }
        }
    }
}

fun clearTextFieldItem(item: UiObject) {
    Log.i(TAG, "clearTextFieldItem: Waiting for $waitingTime ms for ${item.selector} to exist")
    item.waitForExists(waitingTime)
    Log.i(TAG, "clearTextFieldItem: Waited for $waitingTime ms for ${item.selector} to exist")
    Log.i(TAG, "clearTextFieldItem: Trying to clear ${item.selector} text field")
    item.clearTextField()
    Log.i(TAG, "clearTextFieldItem: Cleared ${item.selector} text field")
}

// Context menu items
// Link URL
private fun contextMenuLinkUrl(linkUrl: String) =
    itemContainingText(linkUrl)

// Open link in new tab option
private fun contextMenuOpenLinkInNewTab() =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_open_link_in_new_tab))

// Open link in private tab option
private fun contextMenuOpenLinkInPrivateTab() =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_open_link_in_private_tab))

// Copy link option
private fun contextMenuCopyLink() =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_copy_link))

// Download link option
private fun contextMenuDownloadLink() =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_download_link))

// Share link option
private fun contextMenuShareLink() =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_share_link))

// Open in external app option
private fun contextMenuOpenInExternalApp() =
    itemContainingText(getStringResource(R.string.mozac_feature_contextmenu_open_link_in_external_app))
