/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.Context;
import androidx.lifecycle.*;
import lombok.Getter;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;

/**
 * {@link ViewModel} Wrapper for {@link FRUser}
 */
public class FRUserViewModel extends FRViewModel<FRUser> {

    private NodeListener<FRUser> nodeListener;

    public FRUserViewModel() {
        nodeListener = new NodeListener<FRUser>() {
            @Override
            public void onCallbackReceived(Node node) {
                getNodeLiveData().postValue(new SingleLiveEvent<>(node));
            }

            @Override
            public void onSuccess(FRUser result) {
                getResultLiveData().postValue(result);
            }

            @Override
            public void onException(Exception e) {
                getExceptionLiveData().postValue(e);
            }
        };
    }

    public void login(Context context) {
        FRUser.login(context, nodeListener);
    }

    public void register(Context context) {
        FRUser.register(context, nodeListener);
    }

    public void next(Context context, Node node) {
        node.next(context, nodeListener);
    }

}
