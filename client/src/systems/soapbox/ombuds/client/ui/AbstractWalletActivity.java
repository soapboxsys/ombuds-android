/*
 * Copyright 2011-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package systems.soapbox.ombuds.client.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.soapbox.ombuds.client.WalletApplication;
import systems.soapbox.ombuds.client_test.R;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractWalletActivity extends AppCompatActivity
{
    private WalletApplication application;

    protected static final Logger log = LoggerFactory.getLogger(AbstractWalletActivity.class);

    private boolean animateOnPause = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        application = (WalletApplication) getApplication();

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        if(animateOnPause) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
        super.onPause();
    }

    protected WalletApplication getWalletApplication()
    {
        return application;
    }

    protected void setAnimateOnPause(boolean animate) {
        this.animateOnPause = animate;
    }
}
