package com.udacity.project4.util

import android.view.WindowManager
import androidx.test.espresso.Root
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 * Match toast messages to better facilitate testing toast messages
 * with Espresso. This is based off the logic found at
 * https://stackoverflow.com/a/55511526
 */
class ToastMatcher : TypeSafeMatcher<Root>() {
    override fun describeTo(description: Description) {
    }

    override fun matchesSafely(root: Root?): Boolean {
        val type = root?.windowLayoutParams?.get()?.type;
        if ((type == WindowManager.LayoutParams.TYPE_TOAST)) {
            val windowToken = root.decorView.windowToken;
            val appToken = root.decorView.applicationWindowToken;
            if (windowToken == appToken) {
                return true;
            }
        }
        return false;
    }
}