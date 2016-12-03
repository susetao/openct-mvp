package cc.metapro.openct.libsearch;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.Map;

import cc.metapro.openct.data.BookInfo;
import cc.metapro.openct.data.source.Loader;
import cc.metapro.openct.data.source.RequestType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jeffrey on 11/29/16.
 */

public class LibSearchPresnter implements LibSearchContract.Presenter {

    public final static String PAGE_INDEX = "page_index", TYPE = "type", CONTENT = "content";

    private final static int RESULT_FAIL = 1, RESULT_OK = 2, MORE_FAIL = 3, MORE_OK = 4;

    private static int mNextPageIndex;

    private static Map<String, String> mLastSearchKvs;

    private final LibSearchContract.View mLibSearchView;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case RESULT_OK:
                    List<BookInfo> list = (List<BookInfo>) message.obj;
                    mLibSearchView.showOnSearchResultOk(list);
                    break;
                case RESULT_FAIL:
                    mLibSearchView.showOnSearchResultFail();
                    break;
                case MORE_OK:
                    List<BookInfo> more = (List<BookInfo>) message.obj;
                    mLibSearchView.showOnLoadMoreOk(more);
                    break;
                case MORE_FAIL:
                    mLibSearchView.showOnLoadMoreFail();
                    break;
            }
            return false;
        }
    });

    private Loader mSearchLibLoader =
            new Loader(RequestType.SEARCH_LIB, new Loader.CallBack() {
                @Override
                public void onResultOk(Object results) {
                    List<BookInfo> infos = (List<BookInfo>) results;
                    Message message = new Message();
                    message.what = RESULT_OK;
                    message.obj = infos;
                    mHandler.sendMessage(message);
                }

                @Override
                public void onResultFail() {
                    Message message = new Message();
                    message.what = RESULT_FAIL;
                    mHandler.sendMessage(message);
                }
            });

    private Loader mGetNextPageLoader =
            new Loader(RequestType.GET_LIB_RESULT_PAGE, new Loader.CallBack() {
                @Override
                public void onResultOk(Object results) {
                    List<BookInfo> bookInfos = (List<BookInfo>) results;
                    Message message = new Message();
                    message.what = MORE_OK;
                    message.obj = bookInfos;
                    mHandler.sendMessage(message);
                }

                @Override
                public void onResultFail() {
                    if (mNextPageIndex > 1) {
                        mNextPageIndex--;
                    } else {
                        mNextPageIndex = 2;
                    }
                    Message message = new Message();
                    message.what = MORE_FAIL;
                    mHandler.sendMessage(message);
                }
            });

    public LibSearchPresnter(@NonNull LibSearchContract.View libSearchView) {
        mLibSearchView = checkNotNull(libSearchView, "libSearchView can't be null");

        mLibSearchView.setPresenter(this);
    }

    @Override
    public void search(Map<String, String> kvs) {
        try {
            mLibSearchView.showOnSearching();
            mNextPageIndex = 2;
            mLastSearchKvs = kvs;
            mSearchLibLoader.loadFromRemote(mLastSearchKvs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getNextPage() {
        try {
            mLibSearchView.showOnSearching();
            if (mLastSearchKvs == null) throw new Exception("you haven't search yet");
            mLastSearchKvs.put(PAGE_INDEX, mNextPageIndex + "");
            mGetNextPageLoader.loadFromRemote(mLastSearchKvs);
            mNextPageIndex++;
        } catch (Exception e) {
            e.printStackTrace();
            mLibSearchView.showOnLoadMoreFail();
        }
    }

    @Override
    public void start() {

    }
}