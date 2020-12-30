package com.hcl.kandy.cpass;

import android.app.Application;
import android.content.Context;

import com.hcl.kandy.cpass.activities.HomeActivity;
import com.hcl.kandy.cpass.utils.CpassSubscribe;
import com.rbbn.cpaas.mobile.CPaaS;
import com.rbbn.cpaas.mobile.utilities.Configuration;
import com.rbbn.cpaas.mobile.utilities.Globals;
import com.rbbn.cpaas.mobile.utilities.exception.MobileException;
import com.rbbn.cpaas.mobile.utilities.logging.LogLevel;

public class App extends Application {

    private CPaaS mCpaas;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void setCpass(String baseUrl, String mAccessToken, String idToken, HomeActivity.CpassListner cpassListner) {
        Context context = getApplicationContext();

        Configuration.getInstance().setRestServerUrl(baseUrl);
        Configuration.getInstance().setLogLevel(LogLevel.TRACE);
        ConfigurationHelper.setConfigurations(baseUrl);
        Globals.setApplicationContext(context);

        mCpaas = CpassSubscribe.initKandyService(mAccessToken, idToken, cpassListner);

    }

    public CPaaS getCpass() {
        return mCpaas;
    }
}
