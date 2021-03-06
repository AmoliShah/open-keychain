/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Senecaso
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.UploadResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.UploadKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.keyimport.ParcelableHkpKeyserver;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.ArrayList;

/**
 * Sends the selected public key to a keyserver
 */
public class UploadKeyActivity extends BaseActivity
        implements CryptoOperationHelper.Callback<UploadKeyringParcel, UploadResult> {
    private View mUploadButton;
    private Spinner mKeyServerSpinner;

    private Uri mDataUri;

    // CryptoOperationHelper.Callback vars
    private ParcelableHkpKeyserver mKeyserver;
    private CryptoOperationHelper<UploadKeyringParcel, UploadResult> mUploadOpHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUploadButton = findViewById(R.id.upload_key_action_upload);
        mKeyServerSpinner = (Spinner) findViewById(R.id.upload_key_keyserver);

        MultiUserIdsFragment mMultiUserIdsFragment = (MultiUserIdsFragment)
                getSupportFragmentManager().findFragmentById(R.id.multi_user_ids_fragment);
        mMultiUserIdsFragment.setCheckboxVisibility(false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, getKeyserversArray()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKeyServerSpinner.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            mKeyServerSpinner.setSelection(0);
        } else {
            mUploadButton.setEnabled(false);
        }

        mUploadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadKey();
            }
        });

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of key!");
            finish();
            return;
        }

    }

    private String[] getKeyserversArray() {
        ArrayList<ParcelableHkpKeyserver> keyservers = Preferences.getPreferences(this)
                .getKeyServers();
        String[] keyserversArray = new String[keyservers.size()];
        int i = 0;
        for (ParcelableHkpKeyserver k : keyservers) {
            keyserversArray[i] = k.getUrl();
            i++;
        }
        return keyserversArray;
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.upload_key_activity);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mUploadOpHelper != null) {
            mUploadOpHelper.handleActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadKey() {
        mKeyserver = (ParcelableHkpKeyserver) mKeyServerSpinner.getSelectedItem();

        mUploadOpHelper = new CryptoOperationHelper<>(1, this, this, R.string.progress_uploading);
        mUploadOpHelper.cryptoOperation();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Intent viewIntent = NavUtils.getParentActivityIntent(this);
                viewIntent.setData(KeychainContract.KeyRings.buildGenericKeyRingUri(mDataUri));
                NavUtils.navigateUpTo(this, viewIntent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public UploadKeyringParcel createOperationInput() {
        long[] masterKeyIds = getIntent().getLongArrayExtra(MultiUserIdsFragment.EXTRA_KEY_IDS);

        return new UploadKeyringParcel(mKeyserver, masterKeyIds[0]);
    }

    @Override
    public void onCryptoOperationSuccess(UploadResult result) {
        result.createNotify(this).show();
    }

    @Override
    public void onCryptoOperationCancelled() {

    }

    @Override
    public void onCryptoOperationError(UploadResult result) {
        result.createNotify(this).show();
    }

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }
}
