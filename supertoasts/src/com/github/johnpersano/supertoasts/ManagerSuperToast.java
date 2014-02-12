/**
 *  Copyright 2013 John Persano
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 *
 */

package com.github.johnpersano.supertoasts;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *  Manages the life of a SuperToast. Copied from the Crouton library.
 */
public class ManagerSuperToast extends Handler {

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "ManagerSuperToast";

    /* Potential messages for the handler to send **/
    private static final class Messages {

        /* Hexadecimal numbers that represent acronyms for the operation. **/
        private static final int DISPLAY_SUPERTOAST = 0x445354;
        private static final int ADD_SUPERTOAST = 0x415354;
        private static final int REMOVE_SUPERTOAST = 0x525354;

    }

    private static ManagerSuperToast mManagerSuperToast;

    private final Queue<SuperToast> mQueue;

    /* Private method to create a new list if the manager is being initialized */
    private ManagerSuperToast() {

        mQueue = new LinkedBlockingQueue<SuperToast>();

    }

    /**
     * Singleton method to ensure all SuperToasts are passed through the same manager.
     */
    protected static synchronized ManagerSuperToast getInstance() {

        if (mManagerSuperToast != null) {

            return mManagerSuperToast;

        } else {

            mManagerSuperToast = new ManagerSuperToast();

            return mManagerSuperToast;

        }

    }

    /**
     * Add a SuperToast to the list. Will show immediately if no other SuperActivityToasts
     * are in the list.
     */
    protected void add(SuperToast superToast) {

        mQueue.add(superToast);
        this.showNextSuperToast();

    }

    /**
     * Shows the next SuperToast in the list. Called by add() and when the dismiss animation
     * of a previously showing SuperToast ends.
     */
    private void showNextSuperToast() {

        if (mQueue.isEmpty()) {

            return;

        }

        final SuperToast superToast = mQueue.peek();

        if (!superToast.isShowing()) {

            final Message message = obtainMessage(Messages.ADD_SUPERTOAST);
            message.obj = superToast;
            sendMessage(message);

        } else {

            sendMessageDelayed(superToast,
                    Messages.DISPLAY_SUPERTOAST,
                    getDuration(superToast));

        }

    }

    private void sendMessageDelayed(SuperToast superToast,
                                    final int messageId, final long delay) {

        Message message = obtainMessage(messageId);
        message.obj = superToast;
        sendMessageDelayed(message, delay);

    }

    private long getDuration(SuperToast superToast) {

        long duration = superToast.getDuration();
        duration += 1000;

        return duration;

    }

    @Override
    public void handleMessage(Message message) {

        final SuperToast superToast = (SuperToast)
                message.obj;

        switch (message.what) {

            case Messages.DISPLAY_SUPERTOAST:

                showNextSuperToast();

                break;

            case Messages.ADD_SUPERTOAST:

                displaySuperToast(superToast);

                break;

            case Messages.REMOVE_SUPERTOAST:

                removeSuperToast(superToast);

                break;

            default: {

                super.handleMessage(message);

                break;

            }

        }

    }

    /**
     * Displays a SuperToast.
     */
    private void displaySuperToast(SuperToast superToast) {

        if (superToast.isShowing()) {

            return;

        }

        final WindowManager windowManager = superToast
                .getWindowManager();

        final View toastView = superToast.getView();

        final WindowManager.LayoutParams params = superToast
                .getWindowManagerParams();

        if(windowManager != null) {

            windowManager.addView(toastView, params);

        }

        sendMessageDelayed(superToast, Messages.REMOVE_SUPERTOAST,
                superToast.getDuration() + 500);

    }

    /**
     *  Hide and remove the SuperToast
     */
    protected void removeSuperToast(SuperToast superToast) {

        final WindowManager windowManager = superToast
                .getWindowManager();

        final View toastView = superToast.getView();

        if (windowManager != null) {

            mQueue.poll();

            windowManager.removeView(toastView);

            sendMessageDelayed(superToast,
                    Messages.DISPLAY_SUPERTOAST, 500);

            if(superToast.getOnDismissListener() != null) {

                superToast.getOnDismissListener().onDismiss(superToast.getView());

            }

        }

    }

    protected void cancelAllSuperToasts() {

        removeMessages(Messages.ADD_SUPERTOAST);
        removeMessages(Messages.DISPLAY_SUPERTOAST);
        removeMessages(Messages.REMOVE_SUPERTOAST);

        for (SuperToast superToast : mQueue) {

            if (superToast.isShowing()) {

                superToast.getWindowManager().removeView(
                        superToast.getView());

            }

        }

        mQueue.clear();

    }

}
