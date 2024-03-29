/*
 * Copyright 2014-2015 the original author or authors.
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

package systems.soapbox.ombuds.client.ui.send;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.annotation.Nullable;

import systems.soapbox.ombuds.client.Constants;
import systems.soapbox.ombuds.client.btc.data.PaymentIntent;
import systems.soapbox.ombuds.client.ui.InputParser;
import systems.soapbox.ombuds.client.util.Bluetooth;
import systems.soapbox.ombuds.client_test.R;

/**
 * @author Andreas Schildbach
 */
public abstract class RequestPaymentRequestTask
{
    private final Handler backgroundHandler;
    private final Handler callbackHandler;
    private final ResultCallback resultCallback;

    private static final Logger log = LoggerFactory.getLogger(RequestPaymentRequestTask.class);

    public interface ResultCallback
    {
        void onPaymentIntent(PaymentIntent paymentIntent);

        void onFail(int messageResId, Object... messageArgs);
    }

    public RequestPaymentRequestTask(final Handler backgroundHandler, final ResultCallback resultCallback)
    {
        this.backgroundHandler = backgroundHandler;
        this.callbackHandler = new Handler(Looper.myLooper());
        this.resultCallback = resultCallback;
    }

    public final static class HttpRequestTask extends RequestPaymentRequestTask
    {
        @Nullable
        private final String userAgent;

        public HttpRequestTask(final Handler backgroundHandler, final ResultCallback resultCallback, @Nullable final String userAgent)
        {
            super(backgroundHandler, resultCallback);

            this.userAgent = userAgent;
        }

        @Override
        public void requestPaymentRequest(final String url)
        {
            super.backgroundHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    log.info("trying to request payment request from {}", url);

                    HttpURLConnection connection = null;
                    InputStream is = null;

                    try
                    {
                        connection = (HttpURLConnection) new URL(url).openConnection();

                        connection.setInstanceFollowRedirects(false);
                        connection.setConnectTimeout(Constants.HTTP_TIMEOUT_MS);
                        connection.setReadTimeout(Constants.HTTP_TIMEOUT_MS);
                        connection.setUseCaches(false);
                        connection.setDoInput(true);
                        connection.setDoOutput(false);

                        connection.setRequestMethod("GET");
                        connection.setRequestProperty("Accept", PaymentProtocol.MIMETYPE_PAYMENTREQUEST);
                        if (userAgent != null)
                            connection.addRequestProperty("User-Agent", userAgent);
                        connection.connect();

                        final int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK)
                        {
                            is = connection.getInputStream();

                            new InputParser.StreamInputParser(connection.getContentType(), is)
                            {
                                @Override
                                protected void handlePaymentIntent(final PaymentIntent paymentIntent)
                                {
                                    log.info("received {} via http", paymentIntent);

                                    onPaymentIntent(paymentIntent);
                                }

                                @Override
                                protected void error(final int messageResId, final Object... messageArgs)
                                {
                                    onFail(messageResId, messageArgs);
                                }
                            }.parse();
                        }
                        else
                        {
                            final String responseMessage = connection.getResponseMessage();

                            log.info("got http error {}: {}", responseCode, responseMessage);

                            onFail(R.string.error_http, responseCode, responseMessage);
                        }
                    }
                    catch (final IOException x)
                    {
                        log.info("problem sending", x);

                        onFail(R.string.error_io, x.getMessage());
                    }
                    finally
                    {
                        if (is != null)
                        {
                            try
                            {
                                is.close();
                            }
                            catch (final IOException x)
                            {
                                // swallow
                            }
                        }

                        if (connection != null)
                            connection.disconnect();
                    }
                }
            });
        }
    }

    public final static class BluetoothRequestTask extends RequestPaymentRequestTask
    {
        private final BluetoothAdapter bluetoothAdapter;

        public BluetoothRequestTask(final Handler backgroundHandler, final ResultCallback resultCallback, final BluetoothAdapter bluetoothAdapter)
        {
            super(backgroundHandler, resultCallback);

            this.bluetoothAdapter = bluetoothAdapter;
        }

        @Override
        public void requestPaymentRequest(final String url)
        {
            super.backgroundHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    log.info("trying to request payment request from {}", url);

                    final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(Bluetooth.decompressMac(Bluetooth.getBluetoothMac(url)));

                    BluetoothSocket socket = null;
                    OutputStream os = null;
                    InputStream is = null;

                    try
                    {
                        socket = device.createInsecureRfcommSocketToServiceRecord(Bluetooth.PAYMENT_REQUESTS_UUID);
                        socket.connect();

                        log.info("connected to {}", url);

                        is = socket.getInputStream();
                        os = socket.getOutputStream();

                        final CodedInputStream cis = CodedInputStream.newInstance(is);
                        final CodedOutputStream cos = CodedOutputStream.newInstance(os);

                        cos.writeInt32NoTag(0);
                        cos.writeStringNoTag(Bluetooth.getBluetoothQuery(url));
                        cos.flush();

                        final int responseCode = cis.readInt32();

                        if (responseCode == 200)
                        {
                            new InputParser.BinaryInputParser(PaymentProtocol.MIMETYPE_PAYMENTREQUEST, cis.readBytes().toByteArray())
                            {
                                @Override
                                protected void handlePaymentIntent(final PaymentIntent paymentIntent)
                                {
                                    log.info("received {} via bluetooth", paymentIntent);

                                    onPaymentIntent(paymentIntent);
                                }

                                @Override
                                protected void error(final int messageResId, final Object... messageArgs)
                                {
                                    onFail(messageResId, messageArgs);
                                }
                            }.parse();
                        }
                        else
                        {
                            log.info("got bluetooth error {}", responseCode);

                            onFail(R.string.error_bluetooth, responseCode);
                        }
                    }
                    catch (final IOException x)
                    {
                        log.info("problem sending", x);

                        onFail(R.string.error_io, x.getMessage());
                    }
                    finally
                    {
                        if (os != null)
                        {
                            try
                            {
                                os.close();
                            }
                            catch (final IOException x)
                            {
                                // swallow
                            }
                        }

                        if (is != null)
                        {
                            try
                            {
                                is.close();
                            }
                            catch (final IOException x)
                            {
                                // swallow
                            }
                        }

                        if (socket != null)
                        {
                            try
                            {
                                socket.close();
                            }
                            catch (final IOException x)
                            {
                                // swallow
                            }
                        }
                    }
                }
            });
        }
    }

    public abstract void requestPaymentRequest(String url);

    protected void onPaymentIntent(final PaymentIntent paymentIntent)
    {
        callbackHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                resultCallback.onPaymentIntent(paymentIntent);
            }
        });
    }

    protected void onFail(final int messageResId, final Object... messageArgs)
    {
        callbackHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                resultCallback.onFail(messageResId, messageArgs);
            }
        });
    }
}
