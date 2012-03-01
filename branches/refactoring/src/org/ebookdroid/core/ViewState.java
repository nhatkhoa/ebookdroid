package org.ebookdroid.core;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.ui.viewer.IViewController;

import android.graphics.RectF;
import android.util.SparseArray;

public class ViewState {

    public final AppSettings app;
    public final BookSettings book;
    public final IViewController ctrl;
    public final DocumentModel model;

    public final RectF viewRect;
    public final float zoom;
    public final PageAlign pageAlign;
    public final boolean nightMode;
    public final PagePaint paint;

    public final Pages pages;

    public final SparseArray<RectF> cachedBounds = new SparseArray<RectF>();

    public ViewState(final PageTreeNode node) {
        this(node.page.base.getDocumentController());
    }

    public ViewState(final IViewController dc) {
        this(dc, dc.getBase().getZoomModel().getZoom());
    }

    public ViewState(final IViewController dc, final float zoom) {
        this.app = SettingsManager.getAppSettings();
        this.book = SettingsManager.getBookSettings();
        this.ctrl = dc;
        this.model = dc.getBase().getDocumentModel();

        this.viewRect = new RectF(ctrl.getView().getViewRect());
        this.zoom = zoom;
        this.pageAlign = DocumentViewMode.getPageAlign(book);
        this.nightMode = app.getNightMode();
        this.paint = this.nightMode ? PagePaint.NIGHT : PagePaint.DAY;

        this.pages = new Pages();
    }

    public ViewState(final ViewState oldState, final IViewController dc) {
        this.app = SettingsManager.getAppSettings();
        this.book = SettingsManager.getBookSettings();
        this.ctrl = dc;
        this.model = dc.getBase().getDocumentModel();

        this.viewRect = oldState.viewRect;
        this.zoom = oldState.zoom;
        this.pageAlign = oldState.pageAlign;
        this.nightMode = oldState.nightMode;
        this.paint = oldState.paint;

        this.pages = new Pages();
    }

    public ViewState(final ViewState oldState, final int firstVisiblePage, final int lastVisiblePage) {
        this.app = SettingsManager.getAppSettings();
        this.book = SettingsManager.getBookSettings();
        this.ctrl = oldState.ctrl;
        this.model = oldState.model;

        this.viewRect = oldState.viewRect;
        this.zoom = oldState.zoom;
        this.pageAlign = oldState.pageAlign;
        this.nightMode = oldState.nightMode;
        this.paint = oldState.paint;

        this.pages = new Pages(firstVisiblePage, lastVisiblePage);
    }

    public RectF getBounds(final Page page) {
//        RectF bounds = cachedBounds.get(page.index.viewIndex);
//        if (bounds == null) {
//            bounds = page.getBounds(zoom);
//            cachedBounds.append(page.index.viewIndex, bounds);
//        }
//        return bounds;
        return page.getBounds(zoom);
    }

    public final boolean isPageKeptInMemory(final Page page) {
        return pages.firstCached <= page.index.viewIndex && page.index.viewIndex <= pages.lastCached;
    }

    public final boolean isPageVisible(final Page page) {
        return pages.firstVisible <= page.index.viewIndex && page.index.viewIndex <= pages.lastVisible;
    }

    public final boolean isNodeKeptInMemory(final PageTreeNode node, final RectF pageBounds) {
        if (this.zoom < 1.5) {
            return this.isPageKeptInMemory(node.page) || this.isPageVisible(node.page);
        }
        if (this.zoom < 2.5) {
            return this.isPageKeptInMemory(node.page) || this.isNodeVisible(node, pageBounds);
        }
        return this.isNodeVisible(node, pageBounds);
    }

    public final boolean isNodeVisible(final PageTreeNode node, final RectF pageBounds) {
        final RectF tr = node.getTargetRect(this, pageBounds);
        return isNodeVisible(tr);
    }

    public final boolean isNodeVisible(final RectF tr) {
        return RectF.intersects(viewRect, tr);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        pages.toString(buf).append(" ").append("zoom: ").append(zoom);

        return buf.toString();
    }

    public class Pages {

        public final int currentIndex;
        public final int firstVisible;
        public final int lastVisible;

        public final int firstCached;
        public final int lastCached;

        public Pages() {
            this.firstVisible = ctrl.getFirstVisiblePage();
            this.lastVisible = ctrl.getLastVisiblePage();

            if (model != null) {
                this.currentIndex = ctrl.calculateCurrentPage(ViewState.this, firstVisible, lastVisible);

                final int inMemory = (int) Math.ceil(SettingsManager.getAppSettings().getPagesInMemory() / 2.0);
                this.firstCached = Math.max(0, this.currentIndex - inMemory);
                this.lastCached = Math.min(this.currentIndex + inMemory, model.getPageCount());
            } else {
                this.currentIndex = firstVisible;
                this.firstCached = firstVisible;
                this.lastCached = lastVisible;
            }
        }

        public Pages(final int firstVisible, final int lastVisible) {
            this.firstVisible = firstVisible;
            this.lastVisible = lastVisible;

            if (model != null) {
                this.currentIndex = ctrl.calculateCurrentPage(ViewState.this, firstVisible, lastVisible);

                final int inMemory = (int) Math.ceil(SettingsManager.getAppSettings().getPagesInMemory() / 2.0);
                this.firstCached = Math.max(0, this.currentIndex - inMemory);
                this.lastCached = Math.min(this.currentIndex + inMemory, model.getPageCount());
            } else {
                this.currentIndex = firstVisible;
                this.firstCached = firstVisible;
                this.lastCached = lastVisible;
            }
        }

        public Iterable<Page> getVisiblePages() {
            return firstVisible != -1 ? model.getPages(firstVisible, lastVisible + 1) : model.getPages(0);
        }

        public Page getCurrentPage() {
            return model.getPageObject(currentIndex);
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
            buf.append("[");
            toString(buf);
            buf.append("]");
            return buf.toString();
        }

        StringBuilder toString(final StringBuilder buf) {
            buf.append("visible: ").append("[");
            buf.append(firstVisible).append(", ").append(currentIndex).append(", ").append(lastVisible);
            buf.append("]");
            buf.append(" ");
            buf.append("cached: ").append("[");
            buf.append(firstCached).append(", ").append(lastCached);
            buf.append("]");

            return buf;
        }
    }
}
