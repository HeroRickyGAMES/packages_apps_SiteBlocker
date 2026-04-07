/*
 * Copyright (C) 2024 Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.siteblocker;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Stub ContentProvider — reserved for future IPC from the Settings fragment
 * that links to this app. Currently not used for data storage.
 */
public class SiteBlockerProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
