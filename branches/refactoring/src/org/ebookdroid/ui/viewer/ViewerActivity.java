package org.ebookdroid.ui.viewer;

import org.ebookdroid.R;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.touch.TouchManagerView;
import org.ebookdroid.ui.viewer.dialogs.GoToPageDialog;
import org.ebookdroid.ui.viewer.views.PageViewZoomControls;
import org.ebookdroid.ui.viewer.views.ViewEffects;

import android.app.Dialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicLong;

import org.emdev.ui.AbstractActionActivity;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.ActionMethod;
import org.emdev.ui.actions.IActionController;
import org.emdev.ui.fullscreen.IFullScreenManager;
import org.emdev.utils.LengthUtils;

public class ViewerActivity extends AbstractActionActivity {

    private static final int DIALOG_GOTO = 0;

    public static final DisplayMetrics DM = new DisplayMetrics();

    private static final AtomicLong SEQ = new AtomicLong();

    final LogContext LCTX;

    IView view;

    private Toast pageNumberToast;

    private PageViewZoomControls zoomControls;

    private FrameLayout frameLayout;

    private TouchManagerView touchView;

    private boolean menuClosedCalled;

    private ViewerActivityController controller;

    /**
     * Instantiates a new base viewer activity.
     */
    public ViewerActivity() {
        super();
        LCTX = LogContext.ROOT.lctx(this.getClass().getSimpleName(), true).lctx("" + SEQ.getAndIncrement(), true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.emdev.ui.AbstractActionActivity#createController()
     */
    @Override
    protected IActionController<? extends AbstractActionActivity> createController() {
        if (controller == null) {
            controller = new ViewerActivityController(this);
        }
        return controller;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        LCTX.d("onCreate()");

        Object last = this.getLastNonConfigurationInstance();
        if (last instanceof ViewerActivityController) {
            this.controller = (ViewerActivityController) last;
        } else {
            this.controller = new ViewerActivityController(this);
        }

        this.controller.beforeCreate(this);

        super.onCreate(savedInstanceState);

        getWindowManager().getDefaultDisplay().getMetrics(DM);
        LCTX.i("XDPI=" + DM.xdpi + ", YDPI=" + DM.ydpi);

        frameLayout = createMainContainer();
        view = createView();
        view.getView().setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        frameLayout.addView(view.getView());
        frameLayout.addView(getZoomControls());
        frameLayout.addView(getTouchView());

        this.controller.afterCreate();

        setContentView(frameLayout);
    }

    @Override
    protected void onResume() {
        LCTX.d("onResume()");

        this.controller.beforeResume();

        super.onResume();
        IFullScreenManager.instance.onResume(getWindow());

        this.controller.afterResume();
    }

    @Override
    protected void onPause() {
        LCTX.d("onPause()");

        this.controller.beforePause();

        super.onPause();
        IFullScreenManager.instance.onPause(getWindow());

        this.controller.afterPause();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return controller;
    }

    @Override
    protected void onDestroy() {
        LCTX.d("onDestroy()");

        controller.beforeDestroy();
        super.onDestroy();
        controller.afterDestroy();
    }

    protected IView createView() {
        return SettingsManager.getAppSettings().getDocumentViewType().create(controller);
    }

    public TouchManagerView getTouchView() {
        if (touchView == null) {
            touchView = new TouchManagerView(controller);
        }
        return touchView;
    }

    public void askPassword(final String fileName) {
        setContentView(R.layout.password);
        final Button ok = (Button) findViewById(R.id.pass_ok);

        ok.setOnClickListener(getController().getOrCreateAction(R.id.actions_redecodingWithPassord).putValue(
                "fileName", fileName));

        final Button cancel = (Button) findViewById(R.id.pass_cancel);
        cancel.setOnClickListener(getController().getOrCreateAction(R.id.mainmenu_close));
    }

    public void showErrorDlg(final String msg) {
        setContentView(R.layout.error);
        final TextView errortext = (TextView) findViewById(R.id.error_text);
        if (msg != null && msg.length() > 0) {
            errortext.setText(msg);
        } else {
            errortext.setText("Unexpected error occured!");
        }
        final Button cancel = (Button) findViewById(R.id.error_close);
        cancel.setOnClickListener(controller.getOrCreateAction(R.id.mainmenu_close));
    }

    public void decodingProgressChanged(final int currentlyDecoding) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    setProgressBarIndeterminateVisibility(true);
                    getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                            currentlyDecoding == 0 ? 10000 : currentlyDecoding);
                } catch (final Throwable e) {
                }
            }
        });
    }

    public void currentPageChanged(final String pageText, final String currentFilename) {
        String prefix = "";
        if (LengthUtils.isNotEmpty(pageText)) {
            if (SettingsManager.getAppSettings().getPageInTitle()) {
                prefix = "(" + pageText + ") ";
            } else {
                if (pageNumberToast != null) {
                    pageNumberToast.setText(pageText);
                } else {
                    pageNumberToast = Toast.makeText(this, pageText, 300);
                }
                pageNumberToast.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
                pageNumberToast.show();
            }
        }
        getWindow().setTitle(prefix + currentFilename);
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        controller.beforePostCreate();
        super.onPostCreate(savedInstanceState);
        controller.afterPostCreate();
    }

    public PageViewZoomControls getZoomControls() {
        if (zoomControls == null) {
            zoomControls = new PageViewZoomControls(this, controller.getZoomModel());
            zoomControls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        }
        return zoomControls;
    }

    private FrameLayout createMainContainer() {
        return new FrameLayout(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onMenuOpened(int, android.view.Menu)
     */
    @Override
    public boolean onMenuOpened(final int featureId, final Menu menu) {
        view.changeLayoutLock(true);
        IFullScreenManager.instance.onMenuOpened(getWindow());
        return super.onMenuOpened(featureId, menu);
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onPanelClosed(int, android.view.Menu)
     */
    @Override
    public void onPanelClosed(final int featureId, final Menu menu) {
        menuClosedCalled = false;
        super.onPanelClosed(featureId, menu);
        if (!menuClosedCalled) {
            onOptionsMenuClosed(menu);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Activity#onOptionsMenuClosed(android.view.Menu)
     */
    @Override
    public void onOptionsMenuClosed(final Menu menu) {
        menuClosedCalled = true;
        IFullScreenManager.instance.onMenuClosed(getWindow());
        view.changeLayoutLock(false);
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        switch (id) {
            case DIALOG_GOTO:
                return new GoToPageDialog(controller);
        }
        return null;
    }

    @ActionMethod(ids = { R.id.mainmenu_zoom, R.id.actions_toggleTouchManagerView })
    public void toggleControls(final ActionEx action) {
        final View view = action.getParameter("view");
        ViewEffects.toggleControls(view);
    }

    @Override
    public final boolean dispatchKeyEvent(final KeyEvent event) {
        if (controller.dispatchKeyEvent(event)) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    public void showToastText(int duration, int resId, Object... args) {
        Toast.makeText(getApplicationContext(), getResources().getString(resId, args), duration).show();
    }

}
