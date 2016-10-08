package com.pseudonymous.appmea.network;

/**
 * Created by David Smerkous on 9/28/16.
 *
 */

public interface ResponseListener {
    void on_complete(CommonResponse req);
    void on_fail(CommonResponse req);
}
