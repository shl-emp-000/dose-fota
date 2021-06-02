package com.example.fotaapplication;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

public class CommonUtils {

    /**
     * Used for replacing the main content of the view with provided fragment.
     * The fragment is not added to the activity stack.
     * @param activity
     * @param newFragment
     * @param newFragmentTag
     */
    public static void replaceFragment(FragmentActivity activity, Fragment newFragment, String newFragmentTag) {
        replaceFragment(activity, newFragment, newFragmentTag, false);
    }

    /**
     * Used for replacing the main content of the view with provided fragment
     * @param activity
     * @param newFragment
     * @param newFragmentTag
     * @param addToBackStack if the new fragment should be added to the activity stack
     */
    public static void replaceFragment(FragmentActivity activity, Fragment newFragment, String newFragmentTag, boolean addToBackStack) {
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, newFragment, newFragmentTag);
        if (addToBackStack) {
            transaction.addToBackStack(null); // null because we don't care about the name
        }
        transaction.commit();
    }
}
