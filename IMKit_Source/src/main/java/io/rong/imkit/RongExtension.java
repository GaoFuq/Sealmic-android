package io.rong.imkit;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.fragment.app.Fragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.emoticon.AndroidEmoji;
import io.rong.imkit.emoticon.EmoticonTabAdapter;
import io.rong.imkit.emoticon.IEmoticonClickListener;
import io.rong.imkit.emoticon.IEmoticonSettingClickListener;
import io.rong.imkit.emoticon.IEmoticonTab;
import io.rong.imkit.utilities.ExtensionHistoryUtil;
import io.rong.imkit.utilities.KitCommonDefine;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Conversation;

public class RongExtension extends LinearLayout implements View.OnClickListener, View.OnTouchListener {
    private final static String TAG = "RongExtension";

    private LinearLayout mMainBar;
    private ViewGroup mExtensionBar;
    private ViewGroup mSwitchLayout;
    private ViewGroup mContainerLayout;
    private ViewGroup mPluginLayout;
    private ViewGroup mMenuContainer;

    private View mEditTextLayout;
    private EditText mEditText;
    private Button mVoiceInputToggle;
    private EmoticonTabAdapter mEmotionTabAdapter;
    private FrameLayout mSendToggle;
    private ImageView mEmoticonToggle;
    private ImageView mPluginToggle;
    private ImageView mVoiceToggle;
    private boolean isRobotFirst = false;
    private IRongExtensionState mFireState;
    private IRongExtensionState mNormalState;

    private Fragment mFragment;

    private IExtensionClickListener mExtensionClickListener;

    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private List<IExtensionModule> mExtensionModuleList;
    private InputBar.Style mStyle;
    private VisibilityState lastState = VisibilityState.EXTENSION_VISIBLE; //????????????????????????
    private boolean hasEverDrawn = false;  // edit text ???????????????
    private String mUserId;
    private boolean isBurnMode;

    public static final int TRIGGER_MODE_SYSTEM = 1;//???????????????????????????????????????
    public static final int TRIGGER_MODE_TOUCH = 2;//??????????????????????????????????????????

    @IntDef({TRIGGER_MODE_SYSTEM, TRIGGER_MODE_TOUCH})
    @Retention(RetentionPolicy.SOURCE)
    @interface TRIGGERMODE {
    }

    private @TRIGGERMODE
    int triggerMode = TRIGGER_MODE_SYSTEM;

    /**
     * RongExtension ????????????.
     *
     * @param context ?????????
     */
    public RongExtension(Context context) {
        super(context);
        initView();
        initData();
    }

    /**
     * RongExtension ????????????.
     *
     * @param context ?????????
     * @param attrs   View ???????????????
     */
    public RongExtension(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RongExtension);
        int attr = a.getInt(R.styleable.RongExtension_RCStyle, 0x123);
        a.recycle();
        initView();
        initData();
        mStyle = InputBar.Style.getStyle(attr);
        if (mStyle != null) {
            setInputBarStyle(mStyle);
        }
    }

    /**
     * ????????????Activity???Fragment???????????????onDestroy??????????????????
     */
    public void onDestroy() {
        RLog.d(TAG, "onDestroy");
        for (IExtensionModule module : mExtensionModuleList) {
            module.onDetachedFromExtension();
        }
        mExtensionClickListener = null;
        hideInputKeyBoard();
    }

    /**
     * ?????? extension ??????
     */
    public void collapseExtension() {
        hideEmoticonBoard();
        hideInputKeyBoard();
    }

    /**
     * ???????????????
     */
    public void showSoftInput() {
        showInputKeyBoard();
        mContainerLayout.setSelected(true);
    }

    /**
     * ?????? ExtensionBar ??????.
     *
     * @param style ???????????? 5 ??????????????????: {@link InputBar.Style}
     */
    public void setInputBarStyle(InputBar.Style style) {
        switch (style) {
            case STYLE_SWITCH_CONTAINER_EXTENSION:
                setSCE();
                break;
            case STYLE_CONTAINER:
                setC();
                break;
            case STYLE_CONTAINER_EXTENSION:
                setCE();
                break;
            case STYLE_EXTENSION_CONTAINER:
                setEC();
                break;
            case STYLE_SWITCH_CONTAINER:
                setSC();
                break;
        }
    }

    /**
     * ??? Extension ???????????????????????????????????????????????? Extension ?????????????????????
     *
     * @param conversationType ????????????
     * @param targetId         ?????? id
     */
    public void setConversation(Conversation.ConversationType conversationType, String targetId) {
        // ???????????? setConversation ???????????? module ??? attach ??????
        if (mConversationType == null && mTargetId == null) {
            mConversationType = conversationType;
            mTargetId = targetId;
            for (IExtensionModule module : mExtensionModuleList) {
                module.onAttachedToExtension(this);
            }

            initEmoticons();
            initPanelStyle();
        }
        mConversationType = conversationType;
        mTargetId = targetId;
        SharedPreferences sp = getContext().getSharedPreferences(KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
        boolean isBurn = sp.getBoolean(KitCommonDefine.BURN_PREFIX + getTargetId(), false);
        if (isBurn) {
            enterBurnMode();
        }
    }


    private void initEmoticons() {
        IExtensionProxy proxy = RongExtensionManager.getExtensionProxy();
        for (IExtensionModule module : mExtensionModuleList) {
            IExtensionModule handledResult;
            if (proxy != null) {
                handledResult = proxy.onPreLoadEmoticons(mConversationType, mTargetId, module);
                if (handledResult != null) {
                    List<IEmoticonTab> tabs = module.getEmoticonTabs();
                    mEmotionTabAdapter.initTabs(tabs, module.getClass().getCanonicalName());
                }
            } else {
                List<IEmoticonTab> tabs = module.getEmoticonTabs();
                mEmotionTabAdapter.initTabs(tabs, module.getClass().getCanonicalName());
            }
        }
    }

    private void setExtensionBarVisibility(int visibility) {
        if (visibility == GONE) {
            hideEmoticonBoard();
            hideInputKeyBoard();
        }
        mExtensionBar.setVisibility(visibility);
    }


    /**
     * ????????????
     */
    enum VisibilityState {
        EXTENSION_VISIBLE,
        MENUCONTAINER_VISIBLE
    }

    /**
     * ???????????????????????????
     *
     * @return ?????????????????????
     */
    public EditText getInputEditText() {
        return mEditText;
    }

    /**
     * ????????? Emoticon tab ???????????? tab icon ??????????????????????????????????????????????????????????????????
     *
     * @param tab  ??????????????? tab
     * @param icon tab ????????? icon
     */
    public void refreshEmoticonTabIcon(IEmoticonTab tab, Drawable icon) {
        if (icon != null && mEmotionTabAdapter != null && tab != null) {
            mEmotionTabAdapter.refreshTabIcon(tab, icon);
        }
    }

    /**
     * ??????????????????????????? tab ??????????????? tab ??????????????????????????????????????????????????? tab???
     * ?????????????????????????????????????????????????????????????????????????????????????????? index ?????? 0 ???????????????????????? tab ????????????????????????????????????
     * ????????????????????? {@link #getEmoticonTabs(String)} ???????????? tag ????????? tab ?????????
     * <p>
     * ????????????????????????????????????????????? tag????????????IExtensionModule's CanonicalName {@link DefaultExtensionModule}
     *
     * @param index tab ????????????
     * @param tab   ????????? tab ???
     * @param tag   ??????????????????????????????????????? {@link IExtensionModule} Canonical Name.
     */
    public boolean addEmoticonTab(int index, IEmoticonTab tab, String tag) {
        if (mEmotionTabAdapter != null && tab != null && !TextUtils.isEmpty(tag)) {
            return mEmotionTabAdapter.addTab(index, tab, tag);
        }
        RLog.e(TAG, "addEmoticonTab Failure");
        return false;
    }

    /**
     * ????????????????????????????????? tab ??????????????? tab ??????????????????????????????????????????????????? tab???
     * ?????????????????????????????????????????????????????????????????????????????????????????? index ?????? 0 ???????????????????????? tab ????????????????????????????????????
     * ????????????????????? {@link #getEmoticonTabs(String)} ???????????? tag ????????? tab ?????????
     * <p>
     * ????????????????????????????????????????????? tag????????????IExtensionModule's CanonicalName {@link DefaultExtensionModule}
     *
     * @param tab ????????? tab ???
     * @param tag ??????????????????????????????????????? {@link IExtensionModule} Canonical Name.
     */
    public void addEmoticonTab(IEmoticonTab tab, String tag) {
        if (mEmotionTabAdapter != null && tab != null && !TextUtils.isEmpty(tag)) {
            mEmotionTabAdapter.addTab(tab, tag);
        }
    }

    /**
     * get the tab list as a tag.
     *
     * @param tag the unique tag, must not be null.
     * @return the list of the mapping with the specified tag, or {@code null}
     * if no mapping for the specified key is found.
     */
    public List<IEmoticonTab> getEmoticonTabs(String tag) {
        if (mEmotionTabAdapter != null && !TextUtils.isEmpty(tag)) {
            return mEmotionTabAdapter.getTagTabs(tag);
        }
        return null;
    }

    /**
     * get the tab index as a tag.
     *
     * @param tag the unique tag, must not be null.
     * @return the index of the mapping with the specified tag, or -1
     * if no mapping for the specified tag is found.
     */
    public int getEmoticonTabIndex(String tag) {
        if (mEmotionTabAdapter != null && !TextUtils.isEmpty(tag)) {
            return mEmotionTabAdapter.getTagTabIndex(tag);
        }
        return -1;
    }

    /**
     * remove a tab as the tag.
     *
     * @param tab the tab will be removed, must not be null.
     * @param tag the unique tag, must not be null.
     * @return true if this tab was modified by this operation, false
     * otherwise.
     */
    public boolean removeEmoticonTab(IEmoticonTab tab, String tag) {
        boolean result = false;
        if (mEmotionTabAdapter != null && tab != null && !TextUtils.isEmpty(tag)) {
            result = mEmotionTabAdapter.removeTab(tab, tag);
        }
        return result;
    }

    /**
     * set current selected tab.
     *
     * @param tab the tab, must not be null.
     * @param tag the unique tag, must not be null.
     */
    public void setCurrentEmoticonTab(IEmoticonTab tab, String tag) {
        if (mEmotionTabAdapter != null && tab != null && !TextUtils.isEmpty(tag)) {
            mEmotionTabAdapter.setCurrentTab(tab, tag);
        }
    }

    /**
     * ??????????????????????????? table bar
     *
     * @param enable ????????????
     */
    public void setEmoticonTabBarEnable(boolean enable) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setTabViewEnable(enable);
        }
    }

    /**
     * ???????????? tab bar ???+????????????????????????
     *
     * @param enable ????????????
     */
    public void setEmoticonTabBarAddEnable(boolean enable) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setAddEnable(enable);
        }
    }

    public void setEmoticonTabBarAddClickListener(IEmoticonClickListener listener) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setOnEmoticonClickListener(listener);
        }
    }

    public void setEmoticonTabBarSettingEnable(boolean enable) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setSettingEnable(enable);
        }
    }

    public void setEmoticonTabBarSettingClickListener(IEmoticonSettingClickListener listener) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.setOnEmoticonSettingClickListener(listener);
        }
    }

    /**
     * ???????????? tab bar + ??????????????????????????????????????? tab bar ??????????????? icon
     * ????????? icon ??????????????? "+" ????????????????????? tab ??????
     *
     * @param drawable      ????????? icon
     * @param clickListener ????????? icon ?????????????????????
     */
    public void addEmoticonExtraTab(Context context, Drawable drawable, OnClickListener clickListener) {
        if (mEmotionTabAdapter != null) {
            mEmotionTabAdapter.addExtraTab(context, drawable, clickListener);
        }
    }

    /**
     * ?????? Extension ????????? Fragment???
     *
     * @param fragment ?????? Fragment
     */
    public void setFragment(Fragment fragment) {
        this.mFragment = fragment;
    }

    public Fragment getFragment() {
        return this.mFragment;
    }

    /**
     * ???????????? Extension ??????????????????????????????
     *
     * @return ???????????????
     */
    public Conversation.ConversationType getConversationType() {
        return mConversationType;
    }

    /**
     * ??????????????????????????? targetId???
     *
     * @return ?????? id???
     */
    public String getTargetId() {
        return mTargetId;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param clickListener ??????
     */
    public void setExtensionClickListener(IExtensionClickListener clickListener) {
        this.mExtensionClickListener = clickListener;
    }


    private void initData() {
        mExtensionModuleList = RongExtensionManager.getInstance().getExtensionModules();
        mEmotionTabAdapter = new EmoticonTabAdapter();
        mUserId = RongCoreClient.getInstance().getCurrentUserId();
        try {
            boolean enable = getResources().getBoolean(getResources().getIdentifier("rc_extension_history", "bool", getContext().getPackageName()));
            ExtensionHistoryUtil.setEnableHistory(enable);
            ExtensionHistoryUtil.addExceptConversationType(Conversation.ConversationType.CUSTOMER_SERVICE);
        } catch (Resources.NotFoundException e) {
            RLog.i(TAG, "rc_extension_history not configure in rc_configuration.xml");
        }

    }

    private void initView() {
        setOrientation(VERTICAL);
        setBackgroundColor(getContext().getResources().getColor(R.color.rc_extension_normal));
        mExtensionBar = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.rc_ext_extension_bar, null);
        mMainBar = mExtensionBar.findViewById(R.id.ext_main_bar);
        mSwitchLayout = mExtensionBar.findViewById(R.id.rc_switch_layout);
        mContainerLayout = mExtensionBar.findViewById(R.id.rc_container_layout);
        mPluginLayout = mExtensionBar.findViewById(R.id.rc_plugin_layout);
        mEditTextLayout = LayoutInflater.from(getContext()).inflate(R.layout.rc_ext_input_edit_text, null);
        mEditTextLayout.setVisibility(VISIBLE);
        mContainerLayout.addView(mEditTextLayout);
        LayoutInflater.from(getContext()).inflate(R.layout.rc_ext_voice_input, mContainerLayout, true);
        mVoiceInputToggle = mContainerLayout.findViewById(R.id.rc_audio_input_toggle);
        mVoiceInputToggle.setVisibility(GONE);
        mEditText = mExtensionBar.findViewById(R.id.rc_edit_text);
        mSendToggle = mExtensionBar.findViewById(R.id.rc_send_toggle);
        mPluginToggle = mExtensionBar.findViewById(R.id.rc_plugin_toggle);

        mEditText.setOnTouchListener(this);

        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !TextUtils.isEmpty(mEditText.getText()) && mEditTextLayout.getVisibility() == VISIBLE) {
                    mSendToggle.setVisibility(VISIBLE);
                    mPluginLayout.setVisibility(GONE);
                }
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            private int start;
            private int count;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (mExtensionClickListener != null) {
                    mExtensionClickListener.beforeTextChanged(s, start, count, after);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                this.start = start;
                this.count = count;
                if (mExtensionClickListener != null) {
                    mExtensionClickListener.onTextChanged(s, start, before, count);
                }
                if (mVoiceInputToggle.getVisibility() == VISIBLE) {
                    mSendToggle.setVisibility(GONE);
                    mPluginLayout.setVisibility(VISIBLE);
                } else {
                    if (s == null || s.length() == 0) {
                        mSendToggle.setVisibility(GONE);
                        mPluginLayout.setVisibility(VISIBLE);
                    } else {
                        mSendToggle.setVisibility(VISIBLE);
                        mPluginLayout.setVisibility(GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (AndroidEmoji.isEmoji(s.subSequence(start, start + count).toString())) {
                    mEditText.removeTextChangedListener(this);
                    String resultStr = AndroidEmoji.replaceEmojiWithText(s.toString());
                    mEditText.setText(AndroidEmoji.ensure(resultStr), TextView.BufferType.SPANNABLE);
                    mEditText.setSelection(mEditText.getText().length());
                    mEditText.addTextChangedListener(this);
                }

                if (mExtensionClickListener != null) {
                    mExtensionClickListener.afterTextChanged(s);
                }
            }
        });

        mEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return mExtensionClickListener != null && mExtensionClickListener.onKey(mEditText, keyCode, event);
            }
        });

        mEditText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mEditText.getText().length() > 0 && mEditText.isFocused() && !hasEverDrawn) {
                    Rect rect = new Rect();
                    mEditText.getWindowVisibleDisplayFrame(rect);
                    int keypadHeight = mEditText.getRootView().getHeight() - rect.bottom;
                    int inputbarHeight = (int) mEditText.getContext().getResources().getDimension(R.dimen.rc_extension_bar_min_height);

                    if (keypadHeight > inputbarHeight * 2) { // ??????????????????????????????????????????, ??????????????????????????????
                        hasEverDrawn = true;
                    }
                    if (mExtensionClickListener != null)
                        mExtensionClickListener.onEditTextClick(mEditText);
                    showInputKeyBoard();
                    mContainerLayout.setSelected(true);

                    hideEmoticonBoard();
                }
            }
        });

        mVoiceToggle = mExtensionBar.findViewById(R.id.rc_voice_toggle);
        mVoiceToggle.setOnClickListener(this);

        mVoiceInputToggle.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mExtensionClickListener != null)
                    mExtensionClickListener.onVoiceInputToggleTouch(v, event);
                return false;
            }
        });

        mSendToggle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = mEditText.getText().toString();
                mEditText.setText("");
                if (mExtensionClickListener != null) {
                    mExtensionClickListener.onSendToggleClick(v, text);
                }
            }
        });

        mPluginToggle.setOnClickListener(this);


        mEmoticonToggle = mExtensionBar.findViewById(R.id.rc_emoticon_toggle);
        mEmoticonToggle.setOnClickListener(this);

        addView(mExtensionBar);
    }

    void hideEmoticonBoard() {
        getRongExtensionState().hideEmoticonBoard(mEmoticonToggle, mEmotionTabAdapter);
    }

    void setEmoticonBoard() {
        if (mEmotionTabAdapter.isInitialized()) {
            if (mEmotionTabAdapter.getVisibility() == VISIBLE) {
                mEmotionTabAdapter.setVisibility(GONE);
                mEmoticonToggle.setSelected(false);
                mEmoticonToggle.setImageResource(R.drawable.rc_emotion_toggle_selector);
                showInputKeyBoard();
            } else {
                mEmotionTabAdapter.setVisibility(VISIBLE);
                mContainerLayout.setSelected(true);
                mEmoticonToggle.setSelected(true);
                mEmoticonToggle.setImageResource(R.drawable.rc_keyboard_selector);
            }
        } else {
            mEmotionTabAdapter.bindView(this);
            mEmotionTabAdapter.setVisibility(VISIBLE);
            mContainerLayout.setSelected(true);
            mEmoticonToggle.setSelected(true);
            mEmoticonToggle.setImageResource(R.drawable.rc_keyboard_selector);
        }
        if (!TextUtils.isEmpty(mEditText.getText())) {
            mSendToggle.setVisibility(VISIBLE);
            mPluginLayout.setVisibility(GONE);
        }
    }

    private boolean isKeyBoardActive = false;

    boolean isKeyBoardActive() {
        return isKeyBoardActive;
    }

    void setKeyBoardActive(boolean pIsKeyBoardActive) {
        isKeyBoardActive = pIsKeyBoardActive;
    }

    void hideInputKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        }
        mEditText.clearFocus();
        isKeyBoardActive = false;
    }

    void showInputKeyBoard() {
        mEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(mEditText, 0);
        }
        mEmoticonToggle.setSelected(false);
        isKeyBoardActive = true;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????
     */
    public int getTriggerMode() {
        return triggerMode;
    }

    private void setSCE() {
        mSwitchLayout.setVisibility(VISIBLE);
        if (mSendToggle.getVisibility() == VISIBLE) {
            mPluginLayout.setVisibility(GONE);
        } else {
            mPluginLayout.setVisibility(VISIBLE);
        }
        mMainBar.removeAllViews();
        mMainBar.addView(mSwitchLayout);
        mMainBar.addView(mContainerLayout);
        mMainBar.addView(mPluginLayout);
    }

    private void setSC() {
        mSwitchLayout.setVisibility(VISIBLE);
        mMainBar.removeAllViews();
        mMainBar.addView(mSwitchLayout);
        mMainBar.addView(mContainerLayout);
    }

    private void setCE() {
        if (mSendToggle.getVisibility() == VISIBLE) {
            mPluginLayout.setVisibility(GONE);
        } else {
            mPluginLayout.setVisibility(VISIBLE);
        }
        mMainBar.removeAllViews();
        mMainBar.addView(mContainerLayout);
        mMainBar.addView(mPluginLayout);
    }

    private void setEC() {
        if (mSendToggle.getVisibility() == VISIBLE) {
            mPluginLayout.setVisibility(GONE);
        } else {
            mPluginLayout.setVisibility(VISIBLE);
        }
        mMainBar.removeAllViews();
        mMainBar.addView(mPluginLayout);
        mMainBar.addView(mContainerLayout);
    }

    private void setC() {
        mMainBar.removeAllViews();
        mMainBar.addView(mContainerLayout);
    }

    private void initPanelStyle() {
        //String saveId = DeviceUtils.ShortMD5(Base64.DEFAULT, mUserId, mTargetId, mConversationType.getName());
        ExtensionHistoryUtil.ExtensionBarState state = ExtensionHistoryUtil.getExtensionBarState(getContext(), "1", mConversationType);
        if (state == ExtensionHistoryUtil.ExtensionBarState.NORMAL) {
            mVoiceToggle.setImageResource(R.drawable.rc_voice_toggle_selector);
            mEditTextLayout.setVisibility(VISIBLE);
            mVoiceInputToggle.setVisibility(GONE);
        } else {
            mVoiceToggle.setImageResource(R.drawable.rc_keyboard_selector);
            mEditTextLayout.setVisibility(GONE);
            mVoiceInputToggle.setVisibility(VISIBLE);
            mSendToggle.setVisibility(GONE);
            mPluginLayout.setVisibility(VISIBLE);
        }
    }

    boolean collapsed = true;
    int originalTop = 0;
    int originalBottom = 0;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (originalTop != 0) {
            if (originalTop > t) {
                if (originalBottom > b && mExtensionClickListener != null && collapsed) {
                    collapsed = false;
                    mExtensionClickListener.onExtensionExpanded(originalBottom - t);
                } else if (collapsed && mExtensionClickListener != null) {
                    collapsed = false;
                    mExtensionClickListener.onExtensionExpanded(b - t);
                }
            } else {
                if (!collapsed && mExtensionClickListener != null) {
                    collapsed = true;
                    mExtensionClickListener.onExtensionCollapsed();
                }
            }
        }
        if (originalTop == 0) {
            originalTop = t;
            originalBottom = b;
        }
    }


    public void enterBurnMode() {
        isBurnMode = true;
        SharedPreferences sp = getContext().getSharedPreferences(KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KitCommonDefine.BURN_PREFIX + getTargetId(), true).apply();
        refreshBurnMode();
    }

    public void exitBurnMode() {
        SharedPreferences sp = getContext().getSharedPreferences(KitCommonDefine.RONG_KIT_SP_CONFIG, Context.MODE_PRIVATE);
        sp.edit().remove(KitCommonDefine.BURN_PREFIX + getTargetId()).apply();
        isBurnMode = false;
        refreshBurnMode();
    }

    private void refreshBurnMode() {
        getRongExtensionState().changeView(this);
    }

    public IRongExtensionState getRongExtensionState() {

        if (mNormalState == null) {
            mNormalState = new NormalState();
        }
        return mNormalState;
    }

    ImageView getVoiceToggle() {
        return mVoiceToggle;
    }

    ImageView getPluginToggle() {
        return mPluginToggle;
    }

    ImageView getEmoticonToggle() {
        return mEmoticonToggle;
    }

    boolean isRobotFirst() {
        return isRobotFirst;
    }

    ViewGroup getContainerLayout() {
        return mContainerLayout;
    }

    IExtensionClickListener getExtensionClickListener() {
        return mExtensionClickListener;
    }

    View getEditTextLayout() {
        return mEditTextLayout;
    }

    FrameLayout getSendToggle() {
        return mSendToggle;
    }

    ViewGroup getPluginLayout() {
        return mPluginLayout;
    }

    EditText getEditText() {
        return mEditText;
    }

    Button getVoiceInputToggle() {
        return mVoiceInputToggle;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rc_plugin_toggle ||
                id == R.id.rc_emoticon_toggle ||
                id == R.id.rc_voice_toggle) {
            getRongExtensionState().onClick(RongExtension.this, v);
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (triggerMode != TRIGGER_MODE_TOUCH) {
            triggerMode = TRIGGER_MODE_TOUCH;
        }
        return getRongExtensionState().onEditTextTouch(this, v, event);
    }

    public boolean isFireStatus() {
        return isBurnMode;
    }

    void clickVoice(boolean pRobotFirst, RongExtension pExtension, View pV, @DrawableRes int emotionDrawable) {
        if (pExtension.getExtensionClickListener() != null)
            pExtension.getExtensionClickListener().onSwitchToggleClick(pV, pExtension.getContainerLayout());
        //?????????CUSTOM_SERVICE_MODE_ROBOT_FIRST??????,???????????????????????????
        if (pRobotFirst)
            return;
        if (pExtension.getVoiceInputToggle().getVisibility() == GONE) {
            pExtension.getEditTextLayout().setVisibility(GONE);
            pExtension.getSendToggle().setVisibility(GONE);
            pExtension.getPluginLayout().setVisibility(VISIBLE);
            pExtension.hideInputKeyBoard();
            pExtension.getContainerLayout().setClickable(true);
            pExtension.getContainerLayout().setSelected(false);
        } else {
            pExtension.getEditTextLayout().setVisibility(VISIBLE);
            pExtension.getEmoticonToggle().setImageResource(emotionDrawable);
            if (pExtension.getEditText().getText().length() > 0) {
                pExtension.getSendToggle().setVisibility(VISIBLE);
                pExtension.getPluginLayout().setVisibility(GONE);
            } else {
                pExtension.getSendToggle().setVisibility(GONE);
                pExtension.getPluginLayout().setVisibility(VISIBLE);
            }
            pExtension.showInputKeyBoard();
            pExtension.getContainerLayout().setSelected(true);
        }
        pExtension.hideEmoticonBoard();
    }
}