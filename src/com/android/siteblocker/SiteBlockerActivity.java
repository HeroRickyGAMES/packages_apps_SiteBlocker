/*
 * Copyright (C) 2024 Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.siteblocker;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main UI for managing the site blocklist stored in /data/system/site_blocker_domains.
 *
 * Running as sharedUserId=android.uid.system (UID 1000) gives direct write
 * access to /data/system/ — no ContentProvider or root trick needed.
 */
public class SiteBlockerActivity extends Activity {

    /** Path readable by all processes (world-readable), writable only by system (UID 1000). */
    static final String BLOCKLIST_PATH = "/data/system/site_blocker_domains";

    private List<String> mDomains = new ArrayList<>();
    private DomainAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_blocker);

        mAdapter = new DomainAdapter();
        ListView list = findViewById(R.id.domain_list);
        list.setAdapter(mAdapter);
        list.setEmptyView(findViewById(R.id.empty_view));

        loadDomains();

        findViewById(R.id.fab_add).setOnClickListener(v -> showAddDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, R.string.menu_clear_all);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_clear_title)
                    .setMessage(R.string.confirm_clear_message)
                    .setPositiveButton(android.R.string.ok, (d, w) -> clearAll())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadDomains() {
        mDomains.clear();
        File f = new File(BLOCKLIST_PATH);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        mDomains.add(line);
                    }
                }
            } catch (IOException e) {
                Toast.makeText(this, R.string.error_read, Toast.LENGTH_SHORT).show();
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void saveDomains() {
        File f = new File(BLOCKLIST_PATH);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            for (String domain : mDomains) {
                bw.write(domain);
                bw.newLine();
            }
            // Make file world-readable so libcore can read it from any process.
            f.setReadable(true, false);
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_write, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_domain, null);
        EditText input = dialogView.findViewById(R.id.domain_input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_add_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_add, (d, w) -> {
                    String domain = input.getText().toString().trim().toLowerCase();
                    if (TextUtils.isEmpty(domain)) return;
                    // Basic validation: must have at least one dot
                    if (!domain.contains(".")) {
                        Toast.makeText(this, R.string.error_invalid_domain, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (mDomains.contains(domain)) {
                        Toast.makeText(this, R.string.error_already_exists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mDomains.add(domain);
                    mAdapter.notifyDataSetChanged();
                    saveDomains();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void removeDomain(int position) {
        mDomains.remove(position);
        mAdapter.notifyDataSetChanged();
        saveDomains();
    }

    private void clearAll() {
        mDomains.clear();
        mAdapter.notifyDataSetChanged();
        new File(BLOCKLIST_PATH).delete();
    }

    private class DomainAdapter extends ArrayAdapter<String> {
        DomainAdapter() {
            super(SiteBlockerActivity.this, R.layout.item_domain, mDomains);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_domain, parent, false);
            }
            TextView tv = convertView.findViewById(R.id.domain_text);
            ImageButton btn = convertView.findViewById(R.id.btn_remove);
            tv.setText(mDomains.get(position));
            btn.setOnClickListener(v -> removeDomain(position));
            return convertView;
        }
    }
}
