/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;

import lombok.Getter;

/**
 * {@link ViewModel} Wrapper for {@link FRUser}
 */
public abstract class FRViewModel<T> extends ViewModel {

    @Getter
    private MutableLiveData<SingleLiveEvent<Node>> nodeLiveData = new MutableLiveData<>();
    @Getter
    private MutableLiveData<T> resultLiveData = new MutableLiveData<>();
    @Getter
    private MutableLiveData<Exception> exceptionLiveData = new MutableLiveData<>();

    private NodeListener nodeListener;

    public FRViewModel() {
        nodeListener = new NodeListener<T>() {
            @Override
            public void onCallbackReceived(Node node) {
                nodeLiveData.postValue(new SingleLiveEvent<>(node));
            }

            @Override
            public void onSuccess(T result) {
                resultLiveData.postValue(result);
            }

            @Override
            public void onException(Exception e) {
                exceptionLiveData.postValue(e);
            }
        };
    }

    public abstract void login(Context context);

    public abstract void register(Context context);

    public void next(Context context, Node node) {
        node.next(context, nodeListener);
    }

}
