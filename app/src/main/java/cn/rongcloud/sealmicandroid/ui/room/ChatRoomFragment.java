package cn.rongcloud.sealmicandroid.ui.room;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.rongcloud.rtc.api.report.StatusBean;
import cn.rongcloud.rtc.api.report.StatusReport;
import cn.rongcloud.sealmicandroid.R;
import cn.rongcloud.sealmicandroid.SealMicApp;
import cn.rongcloud.sealmicandroid.bean.SendSuperGiftBean;
import cn.rongcloud.sealmicandroid.bean.kv.AppliedMicListBean;
import cn.rongcloud.sealmicandroid.bean.kv.KvExtraBean;
import cn.rongcloud.sealmicandroid.bean.kv.MicBean;
import cn.rongcloud.sealmicandroid.bean.kv.SpeakBean;
import cn.rongcloud.sealmicandroid.bean.repo.NetResult;
import cn.rongcloud.sealmicandroid.bean.repo.RoomDetailRepo;
import cn.rongcloud.sealmicandroid.bean.repo.RoomMemberRepo;
import cn.rongcloud.sealmicandroid.common.Event;
import cn.rongcloud.sealmicandroid.common.MicState;
import cn.rongcloud.sealmicandroid.common.NetStateLiveData;
import cn.rongcloud.sealmicandroid.common.SealMicResultCallback;
import cn.rongcloud.sealmicandroid.common.adapter.ExtensionClickListenerAdapter;
import cn.rongcloud.sealmicandroid.common.adapter.SendMessageAdapter;
import cn.rongcloud.sealmicandroid.common.constant.ErrorCode;
import cn.rongcloud.sealmicandroid.common.constant.RoomMemberStatus;
import cn.rongcloud.sealmicandroid.common.constant.SealMicConstant;
import cn.rongcloud.sealmicandroid.common.constant.UserRoleType;
import cn.rongcloud.sealmicandroid.common.factory.CommonViewModelFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.BgBaseAudioDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.ChangeBaseAudioDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.ClickMessageDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.GiftDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.HandOverHostDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.MicAudienceFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.MicConnectDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.MicConnectTakeOverDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.MicDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.MicEnqueueDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.MicSettingDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.RoomMemberManagerDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.RoomNoticeDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.RoomSettingDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.SelectedGiftDialogFactory;
import cn.rongcloud.sealmicandroid.common.factory.dialog.TakeOverHostDialogFactory;
import cn.rongcloud.sealmicandroid.common.lifecycle.RoomObserver;
import cn.rongcloud.sealmicandroid.common.listener.OnChatRoomTopBarClickListener;
import cn.rongcloud.sealmicandroid.common.listener.OnDialogButtonListClickListener;
import cn.rongcloud.sealmicandroid.common.listener.OnHandOverHostDialogClickListener;
import cn.rongcloud.sealmicandroid.common.listener.OnTakeOverHostDialogClickListener;
import cn.rongcloud.sealmicandroid.databinding.FragmentChatRoomBinding;
import cn.rongcloud.sealmicandroid.im.IMClient;
import cn.rongcloud.sealmicandroid.im.message.HandOverHostMessage;
import cn.rongcloud.sealmicandroid.im.message.KickMemberMessage;
import cn.rongcloud.sealmicandroid.im.message.SendGiftTag;
import cn.rongcloud.sealmicandroid.im.message.TakeOverHostMessage;
import cn.rongcloud.sealmicandroid.manager.CacheManager;
import cn.rongcloud.sealmicandroid.manager.GlideManager;
import cn.rongcloud.sealmicandroid.manager.NavOptionsRouterManager;
import cn.rongcloud.sealmicandroid.manager.RoomManager;
import cn.rongcloud.sealmicandroid.manager.ThreadManager;
import cn.rongcloud.sealmicandroid.rtc.DebugInfoAdapter;
import cn.rongcloud.sealmicandroid.rtc.RTCClient;
import cn.rongcloud.sealmicandroid.ui.login.LoginViewModel;
import cn.rongcloud.sealmicandroid.ui.room.adapter.RoomChatMessageListAdapter;
import cn.rongcloud.sealmicandroid.ui.room.member.RoomMemberViewModel;
import cn.rongcloud.sealmicandroid.ui.widget.CustomDynamicAvatar;
import cn.rongcloud.sealmicandroid.ui.widget.LoadDialog;
import cn.rongcloud.sealmicandroid.ui.widget.MicTextLayout;
import cn.rongcloud.sealmicandroid.util.ButtonDelayUtil;
import cn.rongcloud.sealmicandroid.util.KeyBoardUtil;
import cn.rongcloud.sealmicandroid.util.SystemUtil;
import cn.rongcloud.sealmicandroid.util.ToastUtil;
import cn.rongcloud.sealmicandroid.util.log.SLog;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.chatroom.message.ChatRoomKVNotiMessage;
import io.rong.imlib.model.ChatRoomInfo;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.TextMessage;

/**
 * ?????????
 */
public class ChatRoomFragment extends Fragment {

    private static final String TAG = ChatRoomFragment.class.getSimpleName();

    /**
     * ???????????????????????????????????????????????????
     */
    private int inputLevel;

    /**
     * ?????????????????????
     */
    private boolean isShowKey = false;

    private FragmentChatRoomBinding fragmentChatRoomBinding;
    private ChatRoomViewModel chatRoomViewModel;
    private RoomMemberViewModel roomMemberViewModel;
    private LoginViewModel loginViewModel;
    private String roomId;
    private String roomName;
    private String roomTheme;
    private UserRoleType userRoleType;
    private ClickProxy clickProxy;
    private Gson gson;
    private List<CustomDynamicAvatar> dynamicAvatarViewList;
    private List<MicTextLayout> micTextLayoutList;
    private List<String> userIdList;
    boolean isAudienceJoin = false;
    boolean isAudienceFreeMic = false;
    private String name;
    /**
     * ????????????????????????????????????dialog
     */
    private boolean isAlertSettingDialog = false;

    /**
     * ?????????????????????
     */
    private String micUserName;
    private RoomChatMessageListAdapter roomChatMessageListAdapter;

    /**
     * ??????????????????kv??????
     */
    private Map<Integer, MicBean> localMicBeanMap = new HashMap<>();

    /**
     * debug?????????RTC????????????
     */
    private DebugInfoAdapter debugInfoAdapter;

    /**
     * ????????????
     */
    private PowerManager.WakeLock wakeLock;

    public ChatRoomFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getActivity().getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getLifecycle().addObserver(new RoomObserver());
        gson = new Gson();
        if (getArguments() != null) {
            roomId = getArguments().getString(SealMicConstant.ROOM_ID);
            CacheManager.getInstance().cacheRoomId(roomId);
            roomName = getArguments().getString(SealMicConstant.ROOM_NAME);
            roomTheme = getArguments().getString(SealMicConstant.ROOM_THEME);
            userRoleType = (UserRoleType) getArguments().getSerializable(SealMicConstant.ROOM_USER_ROLE);
            SLog.d(TAG, TextUtils.isEmpty(roomId) ? "" : roomId);
            SLog.d(TAG, TextUtils.isEmpty(roomName) ? "" : roomName);
        }
        CacheManager.getInstance().cacheMicBean(null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        fragmentChatRoomBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_chat_room, container, false);
        chatRoomViewModel = new ViewModelProvider(this, new CommonViewModelFactory()).get(ChatRoomViewModel.class);
        roomMemberViewModel = new ViewModelProvider(this, new CommonViewModelFactory()).get(RoomMemberViewModel.class);
        loginViewModel = new ViewModelProvider(this, new CommonViewModelFactory()).get(LoginViewModel.class);
        fragmentChatRoomBinding.setLifecycleOwner(this);
        fragmentChatRoomBinding.setChatRoomViewModel(chatRoomViewModel);
        clickProxy = new ClickProxy();
        fragmentChatRoomBinding.setClick(clickProxy);
        dynamicAvatarViewList = new ArrayList<>();
        micTextLayoutList = new ArrayList<>();
        EventBus.getDefault().register(this);
        return fragmentChatRoomBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initRoom();
        initView();
    }

    @Override
    public void onResume() {
        super.onResume();
        PowerManager powerManager = ((PowerManager) requireActivity().getSystemService(Context.POWER_SERVICE));
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
            if (wakeLock != null) {
                wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (wakeLock != null) {
            wakeLock.release();
        }
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        if (CacheManager.getInstance().getUserRoleType() == UserRoleType.HOST.getValue()
                || CacheManager.getInstance().getUserRoleType() == UserRoleType.CONNECT_MIC.getValue()) {
            NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micQuit();
            result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                @Override
                public void onChanged(Integer integer) {
                    SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????");
                }
            });
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SLog.d(RoomObserver.class.getSimpleName(), "FragmentId: " + this.getId() + " ON_DESTROY");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUserGoOutBean(Event.UserGoOutBean userGoOutBean) {
        ToastUtil.showToast("??????????????????????????????");
        SLog.e(SLog.TAG_SEAL_MIC, "????????????????????????");
        loginViewModel.visitorLogin();
        NavOptionsRouterManager.getInstance().gotoLoginFragmentFromChatRoom(getView());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventSendSuperGift(Event.EventSendSuperGift eventSendSuperGift) {
        String userId = CacheManager.getInstance().getUserId();
        String userName = CacheManager.getInstance().getUserName();
        String userPortrait = CacheManager.getInstance().getUserPortrait();
        SendSuperGiftBean.UserBean userInfo = new SendSuperGiftBean.UserBean(userName, userPortrait, userId);
        SendSuperGiftBean sendSuperGiftBean = IMClient.getInstance().getSendSuperGiftBean(roomName, "RCMic:broadcastGift", userInfo);
        String json = new Gson().toJson(sendSuperGiftBean);
        chatRoomViewModel.messageBroad(json);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBroadcastGiftMessage(Event.EventBroadcastGiftMessage eventBroadcastGiftMessage) {
        TextView textView = fragmentChatRoomBinding.chatroomBroadcastGiftmessage;
        String strMsg = "<font color=\"#F8E71C\">"
                + eventBroadcastGiftMessage.getEventBroadcastGiftMessage().getUserInfo().getName()
                + "</font> ??? " + "<font color=\"#F8E71C\">"
                + eventBroadcastGiftMessage.getEventBroadcastGiftMessage().getRoomName()
                + "</font> ??????????????????!!!";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(Html.fromHtml(strMsg, Html.FROM_HTML_MODE_COMPACT));
        } else {
            textView.setText(Html.fromHtml(strMsg));
        }
        ObjectAnimator animator;
        textView.setTranslationX(SystemUtil.dp2px(requireActivity(), -1));
        animator = ObjectAnimator.ofFloat(textView, "translationX",
                -2000);
        animator.setDuration(8000);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //??????????????????????????????????????????????????????????????????????????????
                //?????????????????????????????????????????????????????????????????????????????????
                //image.setTranslationX(dp2px(-1));
            }
        });
        animator.start();
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventFilterBankUser(Event.EventUserLineStatusChange.MicUserStatus micUserStatus) {
        List<RoomMemberRepo.MemberBean> memberBeanList = micUserStatus.getMemberBeanList();
        if (memberBeanList.size() <= 0) {
            return;
        }
        Iterator<RoomMemberRepo.MemberBean> beanIterator = memberBeanList.iterator();
        while (beanIterator.hasNext()) {
            RoomMemberRepo.MemberBean memberBean = beanIterator.next();
            for (int i = 0; i < localMicBeanMap.size(); i++) {
                if (localMicBeanMap.get(i).getUserId() != null
                        && !localMicBeanMap.get(i).getUserId().isEmpty()) {
                    if (memberBean.getUserId()
                            .equals(localMicBeanMap.get(i).getUserId())) {
                        beanIterator.remove();
                    }
                }
            }
        }
        //??????????????????????????????????????????????????????
        EventBus.getDefault().post(new Event.EventUserLineStatusChange.MicUserFilterBankAndMic(memberBeanList));
    }

//    /**
//     * ????????????????????????
//     *
//     * @param eventAudioInputLevel
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventAudioInputLevel(Event.EventAudioInputLevel eventAudioInputLevel) {
//        int position = eventAudioInputLevel.getPosition();
//        inputLevel = eventAudioInputLevel.getInputLevel();
//        // > 0??????????????????
//        SLog.e("AudioInput", "????????????" + position + "??????????????????:" + (inputLevel > 0));
//        if (inputLevel > 0) {
//            //??????????????????????????????????????????????????????????????????????????????????????????????????????
//            MicBean micBean = localMicBeanMap.get(position);
//            if (micBean == null || micBean.getUserId().isEmpty() ||
//                    micBean.getState() == MicState.LOCK.getState() ||
//                    micBean.getState() == MicState.CLOSE.getState()) {
////                dynamicAvatarViewList.get(position).stopSpeak();
//            } else {
////                dynamicAvatarViewList.get(position).startSpeak();
//            }
//        } else {
////            dynamicAvatarViewList.get(position).stopSpeak();
//        }
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventKvList(Event.EventKvMessage eventKvMessage) {
        //???????????????????????????????????????????????????????????????????????????RTC???????????????
        if (CacheManager.getInstance().getUserRoleType() != UserRoleType.AUDIENCE.getValue()) {
            return;
        }
        String key = eventKvMessage.getChatRoomKVNotiMessage().getKey();
        int po = Integer.parseInt(key.substring(key.lastIndexOf("_") + 1));
        final SpeakBean speakBean = gson.fromJson(eventKvMessage.getChatRoomKVNotiMessage().getValue(), SpeakBean.class);
        Log.e(SLog.TAG_SEAL_MIC, speakBean.getPosition() + "speak" + speakBean.getSpeaking());

        if (speakBean.getSpeaking() > 0) {
            dynamicAvatarViewList.get(po).startSpeak();
        } else {
            dynamicAvatarViewList.get(po).stopSpeak();
        }
    }

    /**
     * ???????????????????????????
     *
     * @param eventGiftMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Event.EventGiftMessage eventGiftMessage) {

        //??????????????????????????????
        if (eventGiftMessage.getMessage() != null) {
            roomChatMessageListAdapter.addMessages(eventGiftMessage.getMessage());
            //???????????????????????????
            fragmentChatRoomBinding.chatroomListChat.smoothScrollToPosition(roomChatMessageListAdapter.getCount());
            fragmentChatRoomBinding.chatroomListChat.setSelection(roomChatMessageListAdapter.getCount());
        }
        //??????????????????
        SelectedGiftDialogFactory selectedGiftDialogFactory = new SelectedGiftDialogFactory();
        final Dialog giftDialog = selectedGiftDialogFactory.setSelectedGift(ContextCompat.getDrawable(SealMicApp.getApplication(),
                SendGiftTag.getGiftType(eventGiftMessage.getTag())))
                .buildDialog(requireActivity());
        giftDialog.show();
        selectedGiftDialogFactory.setGiftContent(eventGiftMessage.getContent());
        selectedGiftDialogFactory.setGiftTitle(eventGiftMessage.getMessage().getContent().getUserInfo().getName());

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMemberChange(Event.EventMemberChangeMessage eventMemberChangeMessage) {
        KickMemberMessage roomMemberChangeMessage = eventMemberChangeMessage.getRoomMemberChangeMessage();
        if (roomMemberChangeMessage.getType() == 0) {
            ToastUtil.showToast("??????????????????");
            NavOptionsRouterManager.getInstance().backUp(getView());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Event.EventBroadcastRecallMessage recallMessage) {
        Message recallNtfMessage = recallMessage.getEventBroadRecallMessage();
        roomChatMessageListAdapter.removeMessage(recallNtfMessage.getMessageId());
    }

    /**
     * ???????????????????????????
     *
     * @param eventMicKVMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMicKVMessage(Event.EventMicKVMessage eventMicKVMessage) {
        ChatRoomKVNotiMessage chatRoomKVNotiMessage = eventMicKVMessage.getChatRoomKVNotiMessage();
        String json = chatRoomKVNotiMessage.getValue();
        String key = chatRoomKVNotiMessage.getKey();
        String extra = chatRoomKVNotiMessage.getExtra();
        //KV??????????????????????????????
        KvExtraBean kvExtraBean = new Gson().fromJson(extra, KvExtraBean.class);

        //????????????id
        String roomId = CacheManager.getInstance().getRoomId();
        //????????????id
        String userId = CacheManager.getInstance().getUserId();
        //?????????????????????????????????????????????????????????????????????????????????????????????
        MicBean currentMicBean = CacheManager.getInstance().getMicBean();
        //?????????????????????
        int currentUserType = CacheManager.getInstance().getUserRoleType();

        SpeakBean newSpeakBean = null;
        AppliedMicListBean appliedMicListBean = null;

        final MicBean newMicBean;
        //?????????KV?????????4???KV??????????????????
        if (key.contains(SealMicConstant.KV_MIC_POSITION_PREFIX)) {
            //?????? KV ???????????????????????????????????????
            newMicBean = new Gson().fromJson(json, MicBean.class);
            //??????kv?????????????????????map
            //????????????hashMap??????K???V????????????
            if (localMicBeanMap.containsKey(newMicBean.getPosition())) {
                if (!newMicBean.getUserId().equals(userId)) {
                    localMicBeanMap.put(newMicBean.getPosition(), newMicBean);
                }
            }
            if (newMicBean != null) {
                //1. ?????????????????????KV??????UI
                if (newMicBean.getState() == MicState.NORMAL.getState() || newMicBean.getState() == MicState.CLOSE.getState()) {
                    if (newMicBean.getUserId() == null || newMicBean.getUserId().isEmpty()) {
                        //????????????,????????????
                        dynamicAvatarViewList.get(newMicBean.getPosition()).micDelUser();
                        //?????????
                        if (UserRoleType.HOST.isHost(CacheManager.getInstance().getUserRoleType())) {
                            if (newMicBean.getUserId().equals(CacheManager.getInstance().getUserId())) {
                                micTextLayoutList.get(newMicBean.getPosition()).HasMic("?????????");
                            } else {
                                micTextLayoutList.get(newMicBean.getPosition()).NullMic("?????????");
                            }
                        }
                        //?????????
                        if (UserRoleType.CONNECT_MIC.isConnectMic(CacheManager.getInstance().getUserRoleType()) ||
                                UserRoleType.AUDIENCE.isAudience(CacheManager.getInstance().getUserRoleType())) {
                            micTextLayoutList.get(newMicBean.getPosition()).NullMic("?????????");
                        }
                    }

                    List<String> ids = new ArrayList<>();
                    ids.add(newMicBean.getUserId());
                    chatRoomViewModel.userBatch(ids);
                    final MicBean finalNewMicBean = newMicBean;
                    chatRoomViewModel.getUserinfolistRepoLiveData().observe(getViewLifecycleOwner(), new Observer<NetResult<List<RoomMemberRepo.MemberBean>>>() {
                        @Override
                        public void onChanged(NetResult<List<RoomMemberRepo.MemberBean>> listNetResult) {
                            List<RoomMemberRepo.MemberBean> memberBeanList = listNetResult.getData();
                            if (memberBeanList != null && memberBeanList.size() != 0) {
                                RoomMemberRepo.MemberBean memberBean = memberBeanList.get(0);
                                if ("".equals(memberBean.getUserName())) {
                                    micTextLayoutList.get(finalNewMicBean.getPosition()).HasMic(finalNewMicBean.getPosition() + "??????");
                                } else {
                                    micTextLayoutList.get(finalNewMicBean.getPosition()).HasMic(memberBean.getUserName());
                                }
                                GlideManager.getInstance().setUrlImage(getView(),
                                        memberBean.getPortrait(),
                                        dynamicAvatarViewList.get(finalNewMicBean.getPosition()).getUserImg());

                                if (newMicBean.getState() == MicState.CLOSE.getState()) {
                                    //??????
                                    dynamicAvatarViewList.get(newMicBean.getPosition()).bankMic();
                                } else {
                                    dynamicAvatarViewList.get(newMicBean.getPosition()).unBankMic();
                                }
                            }
                        }
                    });
                } else if (newMicBean.getState() == MicState.LOCK.getState()) {
                    //????????????
                    dynamicAvatarViewList.get(newMicBean.getPosition()).lockMic();
                } else if (newMicBean.getState() == MicState.CLOSE.getState()) {
//                    ??????
                    dynamicAvatarViewList.get(newMicBean.getPosition()).bankMic();
                }

                //2. ?????????????????????
                if (newMicBean.getUserId().equals(userId)) {
                    if (UserRoleType.AUDIENCE.isAudience(currentUserType)) {
                        SLog.e(SLog.TAG_SEAL_MIC, "??????????????????");
                        //????????????
                        chatRoomViewModel.switchMic(roomId, CacheManager.getInstance().getUserRoleType(),
                                newMicBean.getPosition() == 0
                                        ? UserRoleType.HOST.getValue()
                                        : UserRoleType.CONNECT_MIC.getValue(),
                                new SealMicResultCallback<String>() {
                                    @Override
                                    public void onSuccess(String roomId) {
                                        //?????????????????????????????????????????????????????????
                                        ThreadManager.getInstance().runOnUIThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                SLog.e(SLog.TAG_SEAL_MIC, "??????????????????");
                                                //??????????????????????????????????????????????????????????????????V???????????????
                                                EventBus.getDefault().post(
                                                        newMicBean.getPosition() == 0
                                                                ? new Event.EventUserRoleType(UserRoleType.HOST, true)
                                                                : new Event.EventUserRoleType(UserRoleType.CONNECT_MIC, true));
                                                //???????????????????????????????????????
                                                fragmentChatRoomBinding.chatroomVoiceIn.setSelected(false);
                                                RTCClient.getInstance().setLocalMicEnable(true);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFail(int errorCode) {
                                        //????????????????????????
                                        SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????" + errorCode);
                                    }
                                });
                    }
                    //?????????????????????????????????????????????????????????????????????????????????????????????
                    if (currentMicBean == null || currentMicBean.getState() != newMicBean.getState()) {
                        //???????????????????????????????????????????????????????????????????????????
                        //?????????????????????????????????????????????
                        //??????: ?????? ????????????  ??????: ?????? ????????????
                        if (newMicBean.getState() == MicState.NORMAL.getState()) {
                            fragmentChatRoomBinding.chatroomVoiceIn.setSelected(false);
                            RTCClient.getInstance().setLocalMicEnable(true);
                        } else if (newMicBean.getState() == MicState.CLOSE.getState()) {
                            fragmentChatRoomBinding.chatroomVoiceIn.setSelected(true);
                            RTCClient.getInstance().setLocalMicEnable(false);
                        }
                    }

                    //???????????????????????????????????????????????????????????????????????????????????? 0 ??????????????????KV???
                    SpeakBean speakBean = new SpeakBean(0, currentMicBean != null ? currentMicBean.getPosition() : 0);
                    String speakingValue = new Gson().toJson(speakBean);
                    IMClient.getInstance().setChatRoomSpeakEntry(
                            roomId,
                            SealMicConstant.KV_SPEAK_POSITION_PREFIX + (currentMicBean != null ? currentMicBean.getPosition() : 0),
                            speakingValue);

                    SLog.e(SLog.TAG_SEAL_MIC, "Cache???????????????Position???" + newMicBean.getPosition());
                    //????????????????????????????????????????????????????????????????????????
                    CacheManager.getInstance().cacheMicBean(newMicBean);
                    //???????????????????????????map
                    localMicBeanMap.put(newMicBean.getPosition(), newMicBean);

                } else {
                    //?????? changeType ?????? ???4???5???6??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                    int changeType = kvExtraBean.getChangeType();
                    //456 4?????????????????????????????????
                    if (changeType != 4
                            && changeType != 5
                            && changeType != 6
                            && currentMicBean != null
                            && currentMicBean.getPosition() == newMicBean.getPosition()) {
                        //????????????
                        chatRoomViewModel.switchMic(roomId,
                                CacheManager.getInstance().getUserRoleType(),
                                UserRoleType.AUDIENCE.getValue(),
                                new SealMicResultCallback<String>() {
                                    @Override
                                    public void onSuccess(String stringStringMap) {
                                        //?????????????????????????????????????????????????????????
                                        ThreadManager.getInstance().runOnUIThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                SLog.e(SLog.TAG_SEAL_MIC, "??????????????????");
                                                //????????????????????????????????????????????????V???????????????
                                                EventBus.getDefault().post(new Event.EventUserRoleType(UserRoleType.AUDIENCE, true));
                                                boolean outSelected = fragmentChatRoomBinding.chatroomVoiceOut.isSelected();
                                                RTCClient.getInstance().setSpeakerEnable(!outSelected);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFail(int errorCode) {
                                        SLog.e(SLog.TAG_SEAL_MIC, "?????????????????? " + errorCode);
                                    }
                                });

                        //???????????????????????????????????????????????????????????????????????????????????? 0 ??????
                        //?????????KV???
                        SpeakBean speakBean = new SpeakBean(0, currentMicBean.getPosition());
                        String speakingValue = new Gson().toJson(speakBean);
                        IMClient.getInstance().setChatRoomSpeakEntry(
                                roomId,
                                SealMicConstant.KV_SPEAK_POSITION_PREFIX + currentMicBean.getPosition(),
                                speakingValue);

                        //????????????kv??????
                        currentMicBean.setUserId("");
                        //???????????????????????????map
//                        localMicBeanMap.put(currentMicBean.getPosition(), currentMicBean);
                        SLog.e(SLog.TAG_SEAL_MIC, "Cache?????????nullMicBean????????????");
                        //????????????????????????????????????????????????????????????????????????
                        CacheManager.getInstance().cacheMicBean(null);
                    }
                }
            }
        }
        if (key.contains(SealMicConstant.KV_SPEAK_POSITION_PREFIX)) {
            //?????????????????????????????????
            //????????????????????????KV??????????????????
            newSpeakBean = new Gson().fromJson(json, SpeakBean.class);
            CustomDynamicAvatar customDynamicAvatar = dynamicAvatarViewList.get(newSpeakBean.getPosition());
            //1: ????????????  0: ????????????
//            if (newSpeakBean.getSpeaking() == 1 && inputLevel > 0) {
//                customDynamicAvatar.startSpeak();
//            } else {
//                customDynamicAvatar.stopSpeak();
//            }

            //?????????????????????????????????1???????????????????????????


        }
        if (key.contains(SealMicConstant.KV_APPLIED_MIC_PREFIX)) {
            //???????????????????????????????????????
            //0 ????????????????????????1 ?????????????????????
            if ("0".equals(json)) {
                fragmentChatRoomBinding.chatRoomTopBar.hideRedDot();
            } else if ("1".equals(json)) {
                fragmentChatRoomBinding.chatRoomTopBar.showRedDot();
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Event.EventHandOverHostMessage eventHandOverHostMessage) {
        HandOverHostMessage handOverHostMessage = eventHandOverHostMessage.getHandOverHostMessage();
        String currentUserId = CacheManager.getInstance().getUserId();
        //???????????????id?????????????????????????????????id??????????????????????????????????????????
        boolean isShowDialog = currentUserId.equals(handOverHostMessage.getTargetUserId())
                && CacheManager.getInstance().getUserRoleType() != UserRoleType.HOST.getValue();
        if (isShowDialog) {
            if (handOverHostMessage.getCmd() == 0) {
                //??????????????????????????????????????????
                HandOverHostDialogFactory handOverHostDialogFactory = new HandOverHostDialogFactory();
                Dialog dialog = handOverHostDialogFactory.buildDialog(requireActivity(), handOverHostMessage);
                handOverHostDialogFactory.setOnHandOverHostClickListener(new OnHandOverHostDialogClickListener() {
                    @Override
                    public void onAgree(HandOverHostMessage takeOverHostMessage) {
                        final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micTransferHostAccept();
                        result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                            @Override
                            public void onChanged(Integer integer) {
                                if (result.isSuccess()) {
                                    //????????????????????????????????????????????????????????????????????????????????????????????????KV??????
                                    ThreadManager.getInstance().runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SLog.e(SLog.TAG_SEAL_MIC, "????????????");

                                        }
                                    });
                                }
                            }
                        });
                    }

                    @Override
                    public void onRefuse(HandOverHostMessage takeOverHostMessage) {
                        final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micTransferHostReject();
                        result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                            @Override
                            public void onChanged(Integer integer) {
                                if (result.isSuccess()) {
                                    //????????????????????????????????????????????????????????????????????????????????????????????????KV??????
                                    ThreadManager.getInstance().runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SLog.e(SLog.TAG_SEAL_MIC, "????????????");
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
                dialog.show();
            }
            if (handOverHostMessage.getCmd() == 1) {
                //?????????????????????????????????
                //???????????????????????????????????????????????????????????????
                ToastUtil.showToast(getResources().getString(R.string.hand_over_refuse));
            }
            if (handOverHostMessage.getCmd() == 2) {
                //?????????????????????????????????
                //???????????????????????????????????????????????????????????????
                ToastUtil.showToast(getResources().getString(R.string.hand_over_agree));
            }
        }
    }

    /**
     * ???????????????????????????
     *
     * @param eventTakeOverHostMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Event.EventTakeOverHostMessage eventTakeOverHostMessage) {
        TakeOverHostMessage takeOverHostMessage = eventTakeOverHostMessage.getTakeOverHostMessage();
        String currentUserId = CacheManager.getInstance().getUserId();
        //???????????????id?????????????????????????????????id??????????????????????????????????????????
        boolean isShowDialog = currentUserId.equals(takeOverHostMessage.getTargetUserId())
                && UserRoleType.HOST.getValue() == UserRoleType.HOST.getValue();
        if (isShowDialog) {
            if (takeOverHostMessage.getCmd() == 0) {
                //??????
                TakeOverHostDialogFactory takeOverHostDialogFactory = new TakeOverHostDialogFactory();
                Dialog dialog = takeOverHostDialogFactory.buildDialog(requireActivity(), takeOverHostMessage);
                takeOverHostDialogFactory.setOnTakeOverHostClickListener(new OnTakeOverHostDialogClickListener() {
                    @Override
                    public void onAgree(TakeOverHostMessage takeOverHostMessage) {
                        final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micTakeOverHostAccept(takeOverHostMessage.getOperatorId());
                        result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                            @Override
                            public void onChanged(Integer integer) {
                                if (result.isSuccess()) {
                                    //????????????????????????????????????????????????????????????????????????????????????????????????KV??????
                                    ThreadManager.getInstance().runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ToastUtil.showToast("????????????");
                                        }
                                    });
                                }
                            }
                        });
                    }

                    @Override
                    public void onRefuse(TakeOverHostMessage takeOverHostMessage) {
                        final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micTakeOverHostReject(takeOverHostMessage.getOperatorId());
                        result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                            @Override
                            public void onChanged(Integer integer) {
                                if (result.isSuccess()) {
                                    //????????????????????????????????????????????????????????????????????????????????????????????????KV??????
                                    ThreadManager.getInstance().runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ToastUtil.showToast("????????????");
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
                dialog.show();
            }
            if (takeOverHostMessage.getCmd() == 1) {
                //?????????????????????????????????
                ToastUtil.showToast(getResources().getString(R.string.take_over_refuse));
            }
            if (takeOverHostMessage.getCmd() == 2) {
                //?????????????????????????????????
                ToastUtil.showToast(getResources().getString(R.string.take_over_agree));
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUserRoleType(Event.EventUserRoleType eventUserRoleType) {
        if (UserRoleType.AUDIENCE.isAudience(eventUserRoleType.getUserRoleType().getValue())) {
            SLog.e(SLog.TAG_SEAL_MIC, "???????????????????????????");
            fragmentChatRoomBinding.chatroomVoiceIn.setVisibility(View.GONE);
            fragmentChatRoomBinding.chatroomVoice.setVisibility(View.GONE);
        }
        if (UserRoleType.HOST.isHost(eventUserRoleType.getUserRoleType().getValue())
                || UserRoleType.CONNECT_MIC.isConnectMic(eventUserRoleType.getUserRoleType().getValue())) {
            SLog.e(SLog.TAG_SEAL_MIC, "???????????????????????????");
            fragmentChatRoomBinding.chatroomVoiceIn.setVisibility(View.VISIBLE);
            fragmentChatRoomBinding.chatroomVoice.setVisibility(View.VISIBLE);
        }
//        fragmentChatRoomBinding.chatroomVoiceChanger.setSelected(!eventUserRoleType.isMicOpen());
    }

    /**
     * ??????????????????
     *
     * @param eventImList
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventImList(Event.EventImList eventImList) {
        Message message = eventImList.getMessage();
        if (message != null) {
            roomChatMessageListAdapter.addMessages(message);
            fragmentChatRoomBinding.chatroomListChat.smoothScrollToPosition(roomChatMessageListAdapter.getCount());
            fragmentChatRoomBinding.chatroomListChat.setSelection(roomChatMessageListAdapter.getCount());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        debugInfoAdapter = new DebugInfoAdapter(requireContext());
        fragmentChatRoomBinding.debugLayout.debugInfoList.setAdapter(debugInfoAdapter);

        //??????????????????????????????????????????????????????????????????
        fragmentChatRoomBinding.chatroomVoiceOut.setSelected(false);
        RTCClient.getInstance().setSpeakerEnable(true);
        //????????????????????????true??????
        boolean isSpeakerphoneOn = RTCClient.getInstance().isSpeakerphoneOn(requireContext());
        SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????: " + isSpeakerphoneOn);

        //??????????????????????????????????????????????????????????????????
        fragmentChatRoomBinding.chatroomVoiceIn.setSelected(false);
        RTCClient.getInstance().setLocalMicEnable(true);

        //??????roommanager???????????????
        final MicTextLayout chatroomRoomManagerTv = fragmentChatRoomBinding.chatroomRoomManagerTv;
        if (chatroomRoomManagerTv != null) {
            chatroomRoomManagerTv.getTextView().setTextSize(14);
            chatroomRoomManagerTv.getImageView().setImageResource(R.drawable.bg_item_user_target);
        }

        //?????????????????????
        fragmentChatRoomBinding.rcExtension.setConversation(Conversation.ConversationType.CHATROOM, roomId);
        fragmentChatRoomBinding.rcExtension.setExtensionClickListener(new ExtensionClickListenerAdapter() {
            @Override
            public void onSendToggleClick(View v, String text) {
                if (TextUtils.isEmpty(text)) {
                    return;
                }
                RoomManager.getInstance().sendMessage(roomId, text, new SendMessageAdapter() {
                    @Override
                    public void onSuccess(Message message) {
                        if (message.getContent() instanceof TextMessage) {
                            roomChatMessageListAdapter.addMessages(message);
                            fragmentChatRoomBinding.chatroomListChat.smoothScrollToPosition(roomChatMessageListAdapter.getCount());
                            fragmentChatRoomBinding.chatroomListChat.setSelection(roomChatMessageListAdapter.getCount());
                            super.onSuccess(message);
                        }

                    }

                    @Override
                    public void onError(Message message, IRongCoreEnum.CoreErrorCode errorCode) {
                        super.onError(message, errorCode);
                        //????????????????????????????????????
                        if (errorCode.getValue() == ErrorCode.FORBIDDEN_IN_CHATROOM.getCode()) {
                            ToastUtil.showToast(getResources().getString(R.string.cant_speak));
                        }
                    }
                });
                fragmentChatRoomBinding.rcExtension.setVisibility(View.GONE);
                KeyBoardUtil.closeKeyBoard(requireActivity(), requireActivity().getCurrentFocus());
                fragmentChatRoomBinding.chatroomFunction.setVisibility(View.VISIBLE);
            }
        });
        chatRoomViewModel.roomDetail(roomId);
        chatRoomViewModel.getRoomDetailRepoMutableLiveData().observe(getViewLifecycleOwner(), new Observer<RoomDetailRepo>() {
            @Override
            public void onChanged(RoomDetailRepo roomDetailRepo) {
                isAudienceJoin = roomDetailRepo.isAllowedJoinRoom();
                isAudienceFreeMic = roomDetailRepo.isAllowedFreeJoinMic();
                fragmentChatRoomBinding.chatRoomTopBar.getRoomName().setText(roomDetailRepo.getRoomName());
                GlideManager.getInstance().setUrlImage(fragmentChatRoomBinding.getRoot(),
                        roomTheme,
                        fragmentChatRoomBinding.chatRoomTopBar.getRoomPortrait());
                //????????????????????????
                chatRoomViewModel.saveRoomDetail(roomDetailRepo);
                //???????????????????????????????????????dialog
                if (isAlertSettingDialog) {
                    //???????????????dialog???
                    clickProxy.alertDialog();
                }
            }
        });
        fragmentChatRoomBinding.chatRoomTopBar.setOnChatRoomTopBarClickListener(new OnChatRoomTopBarClickListener() {
            @Override
            public void back(View view) {
                NavOptionsRouterManager.getInstance().backUp(view);
            }

            @Override
            public void noticeDialog() {
                clickProxy.showRoomNoticeDialog();
            }

            @Override
            public void lineUpDialog() {
                clickProxy.showRoomMemberManagerDialog();
            }

            @Override
            public void settingRoomDialog() {
                clickProxy.showRoomSettingDialog();
            }
        });
        roomChatMessageListAdapter = new RoomChatMessageListAdapter(SealMicApp.getApplication());
        List<Message> messageList = new ArrayList<>();

        fragmentChatRoomBinding.chatroomListChat.setAdapter(roomChatMessageListAdapter);
        roomChatMessageListAdapter.setMessages(messageList);
        //??????????????????????????????????????????????????????
        TextMessage currentUserTextMessage = new TextMessage(getResources().getString(R.string.welcome_join_room));
        currentUserTextMessage.setUserInfo(new UserInfo(CacheManager.getInstance().getUserId(),
                CacheManager.getInstance().getUserName(),
                Uri.parse(CacheManager.getInstance().getUserPortrait())));

        RongCoreClient.getInstance().sendMessage(Conversation.ConversationType.CHATROOM, roomId,
                currentUserTextMessage, null, null, new IRongCoreCallback.ISendMessageCallback() {
                    @Override
                    public void onAttached(Message message) {

                    }

                    @Override
                    public void onSuccess(Message message) {
                        roomChatMessageListAdapter.addMessages(message);
                        //???????????????????????????
                        fragmentChatRoomBinding.chatroomListChat.setSelection(roomChatMessageListAdapter.getCount());
                    }

                    @Override
                    public void onError(Message message, IRongCoreEnum.CoreErrorCode coreErrorCode) {

                    }

                });
        if (roomChatMessageListAdapter.getCount() > 0) {
            //???????????????????????????
            fragmentChatRoomBinding.chatroomListChat.setSelection(roomChatMessageListAdapter.getCount());
        }

        dynamicAvatarViewList.add(fragmentChatRoomBinding.chatroomRoomManager);
        micTextLayoutList.add(fragmentChatRoomBinding.chatroomRoomManagerTv);
        for (int i = 0; i < fragmentChatRoomBinding.chatroomMiclist.getChildCount(); i++) {
            View v = fragmentChatRoomBinding.chatroomMiclist.getChildAt(i);
            if (v instanceof CustomDynamicAvatar) {
                CustomDynamicAvatar d = (CustomDynamicAvatar) v;
                dynamicAvatarViewList.add(d);
            }
            if (v instanceof MicTextLayout) {
                MicTextLayout m = (MicTextLayout) v;
                micTextLayoutList.add(m);
            }
        }

        //???????????????OnClick??????
        roomChatMessageListAdapter.setCallClick(new RoomChatMessageListAdapter.CallClick() {
            @Override
            public void onClick(int position, final Message message) {
                final String currentUserId = CacheManager.getInstance().getUserId();
                int userRoleType = CacheManager.getInstance().getUserRoleType();
                if (UserRoleType.HOST.isHost(userRoleType)) {
                    //?????????????????????????????????
                    ThreadManager.getInstance().runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            String userId = message.getContent().getUserInfo().getUserId();
                            //?????????????????????????????????
                            ClickMessageDialogFactory clickMessageDialogFactory = new ClickMessageDialogFactory();
                            if (currentUserId.equals(userId)) {
                                clickMessageDialogFactory.setClickType(true);
                            } else {
                                clickMessageDialogFactory.setClickType(false);
                            }
                            final Dialog dialog = clickMessageDialogFactory.buildDialog(requireActivity());
                            if (currentUserId.equals(userId)) {
                                name = CacheManager.getInstance().getUserName();
                                clickMessageDialogFactory.setPortrait(CacheManager.getInstance().getUserPortrait());
                                clickMessageDialogFactory.setUserName(CacheManager.getInstance().getUserName());
                                clickMessageDialogFactory.setMicPosition("?????????");
                            } else {
                                //????????????map??????????????????????????????????????????????????????
                                for (int i = 0; i < localMicBeanMap.size(); i++) {
                                    MicBean micBean = localMicBeanMap.get(i);
                                    if (micBean.getUserId() != null && !micBean.getUserId().isEmpty()) {
                                        if (localMicBeanMap.get(i).getUserId().equals(userId)) {
                                            //?????????????????????????????????,????????????????????????
                                            clickMessageDialogFactory.setMicPositionIsGong();
                                            break;
                                        }
                                    }
                                }
                                name = message.getContent().getUserInfo().getName();
                                clickMessageDialogFactory.setPortrait(message.getContent().getUserInfo().getPortraitUri().toString());
                                clickMessageDialogFactory.setUserName(message.getContent().getUserInfo().getName());
                                clickMessageDialogFactory.setMicPosition("??????");
                            }

                            clickMessageDialogFactory.setOnDialogButtonListClickListener(new OnDialogButtonListClickListener() {
                                @Override
                                public void onClick(String content) {

                                    //????????????
                                    if (getResources().getString(R.string.connect_speak).equals(content)) {
                                        final NetStateLiveData<NetResult<Void>> result = roomMemberViewModel.micInvite(message.getContent().getUserInfo().getUserId());
                                        result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                            @Override
                                            public void onChanged(Integer integer) {
                                                if (result.isSuccess()) {
                                                    SLog.e(SLog.TAG_SEAL_MIC, "????????????????????????");
                                                    dialog.cancel();
                                                }
                                            }
                                        });
                                    }

                                    //????????????????????????????????????????????????
                                    if (getResources().getString(R.string.send_message).equals(content)) {
                                        sendMessage(dialog);
                                    }

                                    //??????
                                    if (getResources().getString(R.string.lock_wheet_speak).equals(content)) {
                                        List<String> userIds = new ArrayList<>();
                                        userIds.add(message.getContent().getUserInfo().getUserId());
                                        final NetStateLiveData<NetResult<Void>> result = roomMemberViewModel.banMember("add", userIds);
                                        result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                            @Override
                                            public void onChanged(Integer integer) {
                                                if (result.isSuccess()) {
                                                    ToastUtil.showToast("????????????");
                                                    dialog.cancel();
                                                }
                                            }
                                        });
                                    }

                                    //??????????????????
                                    if (getResources().getString(R.string.go_out_room).equals(content)) {
                                        List<String> userIds = new ArrayList<>();
                                        userIds.add(message.getContent().getUserInfo().getUserId());
                                        final NetStateLiveData<NetResult<Void>> result = roomMemberViewModel.kickMember(userIds);
                                        result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                            @Override
                                            public void onChanged(Integer integer) {
                                                if (result.isSuccess()) {
                                                    ToastUtil.showToast("????????????");
                                                    dialog.cancel();
                                                }
                                            }
                                        });
                                    }

                                    //????????????
                                    if (getResources().getString(R.string.delete_message).equals(content)) {
                                        recallMessage(message, "");
                                        dialog.cancel();
                                    }


                                }
                            });
                            dialog.show();
                        }
                    });
                }
            }
        });

        fragmentChatRoomBinding.chatroomVoiceOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //???????????????????????????
                fragmentChatRoomBinding.chatroomVoiceOut.setSelected(!fragmentChatRoomBinding.chatroomVoiceOut.isSelected());
                RTCClient.getInstance().setSpeakerEnable(!fragmentChatRoomBinding.chatroomVoiceOut.isSelected());
            }
        });
        fragmentChatRoomBinding.chatroomVoiceIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ButtonDelayUtil.isNormalClick()) {
                    //??????????????????????????????
                    MicBean micBean = CacheManager.getInstance().getMicBean();
                    boolean isSelectedChatRoomVoiceIn = fragmentChatRoomBinding.chatroomVoiceIn.isSelected();
                    if (isSelectedChatRoomVoiceIn) {
                        //??????????????????????????????
                        chatRoomViewModel.setLocalMicEnable(
                                true,
                                micBean.getPosition(),
                                new SealMicResultCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean aBoolean) {
                                        fragmentChatRoomBinding.chatroomVoiceIn.setSelected(!aBoolean);
                                    }

                                    @Override
                                    public void onFail(int errorCode) {

                                    }
                                });
                    } else {
                        //??????????????????????????????
                        chatRoomViewModel.setLocalMicEnable(
                                false,
                                micBean.getPosition(),
                                new SealMicResultCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean aBoolean) {
                                        fragmentChatRoomBinding.chatroomVoiceIn.setSelected(!aBoolean);
                                    }

                                    @Override
                                    public void onFail(int errorCode) {

                                    }
                                }
                        );
                    }
                }
            }
        });

        //??????recyclerview??????
        fragmentChatRoomBinding.chatroomListChat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //???????????????????????????
                if (isShowKey) {
                    clickProxy.hide();
                    fragmentChatRoomBinding.chatroomListChat.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Event.EventMicStatusReport eventMicStatusReport) {
        StatusReport statusReport = eventMicStatusReport.getStatusReport();
        String delayMs = String.format(getString(R.string.delay_ms), String.valueOf(statusReport.rtt));
        fragmentChatRoomBinding.chatRoomTopBar.getRtt().setText(delayMs);
        updateDebugInfo(statusReport);
    }

    private void updateDebugInfo(StatusReport statusReport) {
        fragmentChatRoomBinding.debugLayout.debugInfoBitrateSend.setText(String.valueOf(statusReport.bitRateSend));
        fragmentChatRoomBinding.debugLayout.debugInfoBitrateRcv.setText(String.valueOf(statusReport.bitRateRcv));
        fragmentChatRoomBinding.debugLayout.debugInfoRttSend.setText(String.valueOf(statusReport.rtt));
        List<StatusBean> statusBeans = RTCClient.getInstance().parseToDebugInfoList(statusReport);
        debugInfoAdapter.setStatusBeanList(statusBeans);
    }

    /**
     * ???????????????????????????????????????????????????????????????-??????????????????rtc?????????????????????????????????
     * ?????????????????????????????????????????????
     *
     * @param eventRemoteAudioChange
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEventRemoteAudioChange(Event.EventRemoteAudioChange eventRemoteAudioChange) {
        final HashMap<String, String> audioLevel = eventRemoteAudioChange.getSpeakerMap();
        if (audioLevel != null && audioLevel.size() > 0) {
            try {
                ThreadManager.getInstance().runOnWorkThread(new Runnable() {
                    @Override
                    public void run() {
                        Collection<MicBean> values = localMicBeanMap.values();
                        for (MicBean next : values) {
                            if (next == null || next.getUserId() == null || next.getUserId().isEmpty()) {
                                continue;
                            }
                            if (audioLevel.containsKey(next.getUserId())) {
                                int anInt = Integer.parseInt(audioLevel.get(next.getUserId()));
                                //??????????????????????????????UI
                                if (anInt > 0) {
                                    SLog.i(SLog.TAG_SEAL_MIC_AUDIO, "??? " + next.getPosition() + " ?????????????????????");
                                    dynamicAvatarViewList.get(next.getPosition()).startSpeak();
                                } else {
                                    dynamicAvatarViewList.get(next.getPosition()).stopSpeak();
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????-??????????????????rtc?????????????????????????????????
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventLocalAudioChange(Event.EventLocalAudioChange eventLocalAudioChange) {
        try {
            if (eventLocalAudioChange != null) {

                Collection<MicBean> values = localMicBeanMap.values();
                for (MicBean next : values) {
                    if (next.getUserId().equals(CacheManager.getInstance().getUserId())) {
                        int anInt = Integer.parseInt(eventLocalAudioChange.getAudioLevel());
                        if (anInt > 0) {
                            dynamicAvatarViewList.get(next.getPosition()).startSpeak();
                        } else {
                            dynamicAvatarViewList.get(next.getPosition()).stopSpeak();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void initData() {
        chatRoomViewModel.onlineNumber(roomId, new SealMicResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                if (aBoolean) {
                    IMClient.getInstance().getChatRoomInfo(roomId, new IRongCoreCallback.ResultCallback<ChatRoomInfo>() {
                        @Override
                        public void onSuccess(ChatRoomInfo chatRoomInfo) {
                            int onlineNumber = chatRoomInfo.getTotalMemberCount();
                            String onlineNumberString = SealMicApp.getApplication().getResources().getString(R.string.online_number);
                            fragmentChatRoomBinding.chatRoomTopBar.getOnlineNumber().setText(String.format(onlineNumberString, onlineNumber + ""));
                        }

                        @Override
                        public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {

                        }
                    });
                }
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
    }

    public void initRoom() {
        LoadDialog.show(requireContext());
        //?????????????????????????????????????????????????????????????????????????????????????????????????????????UI?????????
        if (UserRoleType.AUDIENCE.isAudience(userRoleType.getValue())) {
            fragmentChatRoomBinding.chatroomVoiceIn.setVisibility(View.GONE);
            fragmentChatRoomBinding.chatroomVoice.setVisibility(View.GONE);
            RoomManager.getInstance().audienceJoinRoom(roomId, new SealMicResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean b) {
                    SLog.e(SLog.TAG_SEAL_MIC, "??????????????????????????????");
//                    initMic();
//                    initSpeak();
                    //????????????????????????????????????
                    //??????????????????????????????
                    //???????????????????????????????????????????????????
                    boolean speakerphoneOn = RTCClient.getInstance().isSpeakerphoneOn(SealMicApp.getApplication());
                    fragmentChatRoomBinding.chatroomVoiceOut.setSelected(!speakerphoneOn);
                    RTCClient.getInstance().setSpeakerEnable(speakerphoneOn);
                    LoadDialog.dismiss(requireContext());
                }

                @Override
                public void onFail(int errorCode) {
                    SLog.e(SLog.TAG_SEAL_MIC, "??????????????????????????????: " + errorCode);
                }
            });
        }
        if (UserRoleType.HOST.isHost(userRoleType.getValue())
                || UserRoleType.CONNECT_MIC.isConnectMic(userRoleType.getValue())) {
            SLog.e(SLog.TAG_SEAL_MIC, "???????????????????????????Mic?????????");
            fragmentChatRoomBinding.chatroomVoiceIn.setVisibility(View.VISIBLE);
            fragmentChatRoomBinding.chatroomVoice.setVisibility(View.VISIBLE);
            RoomManager.getInstance().micJoinRoom(roomId, new IRongCoreCallback.ResultCallback<String>() {
                @Override
                public void onSuccess(String s) {
                    SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????????????????");
                    //????????????????????????????????????????????????????????????
                    CacheManager.getInstance().cacheUserRoleType(UserRoleType.HOST.getValue());
                    KeyBoardUtil.closeKeyBoard(requireActivity(), getView());
//                    initMic();
//                    initSpeak();
                    //????????????????????????????????????
                    //??????????????????????????????
                    boolean speakerphoneOn = RTCClient.getInstance().isSpeakerphoneOn(SealMicApp.getApplication());
                    fragmentChatRoomBinding.chatroomVoiceOut.setSelected(!speakerphoneOn);
                    RTCClient.getInstance().setSpeakerEnable(speakerphoneOn);
                    LoadDialog.dismiss(requireContext());
                }

                @Override
                public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                    SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????????????????" + coreErrorCode);
                }
            });
        }
        initData();
    }

    /**
     * ?????????????????????
     */
    public void recallMessage(final Message message, String pushMessage) {
//        if (message.getMessageId())
        SLog.i("asdff", message.getMessageId() + "");
        IMClient.getInstance().recallMessage(message, pushMessage, new IRongCoreCallback.ResultCallback<RecallNotificationMessage>() {
            /**
             * ???????????????????????????
             * @param recallNotificationMessage
             */
            @Override
            public void onSuccess(RecallNotificationMessage recallNotificationMessage) {
                SLog.e(SLog.TAG_SEAL_MIC, "??????????????????");
                roomChatMessageListAdapter.removeMessage(message.getMessageId());
            }

            /**
             * ???????????????????????????
             * @param coreErrorCode
             */
            @Override
            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                SLog.e(SLog.TAG_SEAL_MIC, "??????????????????: " + coreErrorCode.getValue());
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void initSpeakSuccess(Event.ChatRoomKVSyncSpeakingSuccessEvent chatRoomKVSyncSpeakingSuccessEvent) {
        Map<String, String> stringStringMap = chatRoomKVSyncSpeakingSuccessEvent.getStringStringMap();
        for (String key : stringStringMap.keySet()) {
            final SpeakBean speakBean = gson.fromJson(stringStringMap.get(key), SpeakBean.class);
            ThreadManager.getInstance().runOnUIThread(new Runnable() {
                @Override
                public void run() {
//                            if (1 == speakBean.getSpeaking()) {
//                                dynamicAvatarViewList.get(speakBean.getPosition()).startSpeak();
//                            } else {
//                                dynamicAvatarViewList.get(speakBean.getPosition()).stopSpeak();
//                            }
                }
            });

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void initSpeakError(Event.ChatRoomKVSyncSpeakingErrorEvent chatRoomKVSyncSpeakingErrorEvent) {
        IRongCoreEnum.CoreErrorCode coreErrorCode = chatRoomKVSyncSpeakingErrorEvent.getCoreErrorCode();
        SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????KV???????????????????????????: " + coreErrorCode);
//                ToastUtil.showToast("?????????????????????KV???????????????????????????: " + errorCode);
//        NavOptionsRouterManager.getInstance().backUp(getView());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void initMicSuccess(Event.ChatRoomKVSyncMicSuccessEvent chatRoomKVSyncSuccessEvent) {
        //??????KV???????????????????????????????????????????????????????????????????????????
        Map<String, String> stringStringMap = chatRoomKVSyncSuccessEvent.getStringStringMap();
        RoomManager.getInstance().transMicBean(stringStringMap, new SealMicResultCallback<MicBean>() {
            @Override
            public void onSuccess(final MicBean micBean) {
                //??????????????????????????????????????????map
                if (micBean == null) {
                    return;
                }
                localMicBeanMap.put(micBean.getPosition(), micBean);
                userIdList = new ArrayList<>();
                userIdList.add(micBean.getUserId());
                chatRoomViewModel.userBatch(userIdList);
                //??????????????????V???
                //Can't access the Fragment View's LifecycleOwner when getView() is null i.e., before onCreateView() or after onDestroyView()
                if (getView() != null) {
                    chatRoomViewModel.getUserinfolistRepoLiveData().observe(getViewLifecycleOwner(), new Observer<NetResult<List<RoomMemberRepo.MemberBean>>>() {
                        @Override
                        public void onChanged(NetResult<List<RoomMemberRepo.MemberBean>> listNetResult) {
                            if (listNetResult != null && listNetResult.getData().size() != 0) {
//                                        dynamicAvatarViewList.get(micBean.getPosition()).stopSpeak();
                                GlideManager.getInstance().setUrlImage(fragmentChatRoomBinding.getRoot(),
                                        listNetResult.getData().get(0).getPortrait(),
                                        dynamicAvatarViewList.get(micBean.getPosition()).getUserImg());
                                micTextLayoutList.get(micBean.getPosition()).HasMic(listNetResult.getData().get(0).getUserName());
                            }
                            if (micBean.getState() == MicState.NORMAL.getState()) {
                                dynamicAvatarViewList.get(micBean.getPosition()).unBankMic();
                            } else if (micBean.getState() == MicState.CLOSE.getState()) {
                                dynamicAvatarViewList.get(micBean.getPosition()).bankMic();
                            } else if (micBean.getState() == MicState.LOCK.getState()) {
                                dynamicAvatarViewList.get(micBean.getPosition()).lockMic();
                            }
                        }
                    });
                }
                EventBus.getDefault().postSticky(new Event.EventMicBean(micBean));
                //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                if (CacheManager.getInstance().getUserId().equals(micBean.getUserId())) {
                    SLog.e(SLog.TAG_SEAL_MIC, "initMic??????????????????????????????????????????");
                    chatRoomViewModel.switchMic(roomId,
                            CacheManager.getInstance().getUserRoleType(),
                            micBean.getPosition() == 0
                                    ? UserRoleType.HOST.getValue()
                                    : UserRoleType.CONNECT_MIC.getValue(),
                            new SealMicResultCallback<String>() {
                                @Override
                                public void onSuccess(String stringStringMap) {
                                    SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????????????????????????????????????????????????????????????????");
                                    EventBus.getDefault().post(
                                            micBean.getPosition() == 0
                                                    ? new Event.EventUserRoleType(UserRoleType.HOST, true)
                                                    : new Event.EventUserRoleType(UserRoleType.CONNECT_MIC, true));
                                    //???????????????????????????????????????
                                    if (micBean.getState() == MicState.CLOSE.getState()) {
                                        fragmentChatRoomBinding.chatroomVoiceIn.setSelected(true);
                                        RTCClient.getInstance().setLocalMicEnable(false);
                                    }
                                    if (micBean.getState() == MicState.NORMAL.getState()) {
                                        fragmentChatRoomBinding.chatroomVoiceIn.setSelected(false);
                                        RTCClient.getInstance().setLocalMicEnable(true);
                                    }
                                    //??????micBean??????
                                    CacheManager.getInstance().cacheMicBean(micBean);
                                    //?????????????????????????????????
                                    localMicBeanMap.put(micBean.getPosition(), micBean);
                                }

                                @Override
                                public void onFail(int errorCode) {
                                    SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????????????????????????????????????????????????????????????????");
                                }
                            });
                }
            }

            @Override
            public void onFail(int errorCode) {
                SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????????????????: " + errorCode);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void initMicError(Event.ChatRoomKVSyncMicErrorEvent chatRoomKVSyncErrorEvent) {
        IRongCoreEnum.CoreErrorCode coreErrorCode = chatRoomKVSyncErrorEvent.getCoreErrorCode();
        SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????KV???????????????????????????: " + coreErrorCode);
//                ToastUtil.showToast("?????????????????????KV???????????????????????????: " + errorCode);
//        NavOptionsRouterManager.getInstance().backUp(getView());
    }

    public void clickMic(final int position) {
        try {
            //???????????????????????????
            MicBean clickMicBean = localMicBeanMap.get(position);
            if (clickMicBean != null) {
                if (TextUtils.isEmpty(clickMicBean.getUserId())) {
                    //?????????
                    if (UserRoleType.HOST.getValue() == CacheManager.getInstance().getUserRoleType()) {
                        //?????????
                        micAbsentHost(clickMicBean);
                    } else if (UserRoleType.CONNECT_MIC.getValue() == CacheManager.getInstance().getUserRoleType()) {
                        //?????????
                        micAbsentConnect(clickMicBean);
                    } else if (UserRoleType.AUDIENCE.getValue() == CacheManager.getInstance().getUserRoleType()) {
                        //??????
                        micAbsentAudience(clickMicBean);
                    }
                } else {
                    //???????????????
                    if (UserRoleType.HOST.getValue() == CacheManager.getInstance().getUserRoleType()) {
                        //?????????
                        micPresentHost(clickMicBean);
                    } else if (UserRoleType.CONNECT_MIC.getValue() == CacheManager.getInstance().getUserRoleType()) {
                        //?????????
                        micPresentConnect(clickMicBean);
                    } else if (UserRoleType.AUDIENCE.getValue() == CacheManager.getInstance().getUserRoleType()) {
                        //??????
                        micPresentAudience(clickMicBean);
                    }
                }
            } else {
                SLog.e(SLog.TAG_SEAL_MIC, "????????????MicBean???Null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????????????????????????????????????????
     */
    public void micAbsentHost(final MicBean micBean) {
        //???????????????
        ThreadManager.getInstance().runOnUIThread(new Runnable() {
            @Override
            public void run() {
                MicSettingDialogFactory micSettingDialogFactory = new MicSettingDialogFactory();
                final Dialog dialog = micSettingDialogFactory.buildDialog(requireActivity(), micBean);
                micSettingDialogFactory.setOnDialogButtonListClickListener(new OnDialogButtonListClickListener() {
                    @Override
                    public void onClick(String content) {
                        if (getResources().getString(R.string.invite_mic).equals(content)) {
                            //?????????????????????????????????
                            if (micBean.getState() == MicState.LOCK.getState()) {
                                ToastUtil.showToast(getResources().getString(R.string.current_mic_lock));
                                dialog.cancel();
                                return;
                            }
                            //??????????????????
                            new RoomMemberManagerDialogFactory().buildDialog(requireActivity(), RoomMemberStatus.ONLINE.getStatus()).show();
                            dialog.cancel();
                        }
                        if (getResources().getString(R.string.lock_mic).equals(content)) {
                            //????????????????????????
                            if (micBean.getState() == MicState.NORMAL.getState()) {
                                //?????????????????????????????????
                                final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micState(MicState.LOCK.getState(), micBean.getPosition());
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        if (result.isSuccess()) {
                                            ToastUtil.showToast("????????????");
                                            dialog.cancel();
                                        }
                                    }
                                });
                            }
                        }


                        if (getResources().getString(R.string.unlock_all_mic).equals(content)) {
                            if (micBean.getState() == MicState.LOCK.getState()) {
                                //?????????????????????????????????
                                final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micState(MicState.NORMAL.getState(), micBean.getPosition());
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        if (result.isSuccess()) {
                                            ToastUtil.showToast("????????????");
                                            dialog.cancel();
                                        }
                                    }
                                });
                            }
                        }
//                        }
                    }
                });
                dialog.show();
                micSettingDialogFactory.setWheetContent("????????????-" + micBean.getPosition() + "??????");
            }
        });
    }

    /**
     * ????????????????????????????????????????????????????????????
     */
    public void micPresentHost(final MicBean micBean) {

        //???????????????
        //????????????????????????
        if (micBean.getPosition() == 0 && CacheManager.getInstance().getUserId().equals(micBean.getUserId())) {
            ThreadManager.getInstance().runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    final MicConnectDialogFactory micConnectFactory = new MicConnectDialogFactory();
                    final Dialog dialog = micConnectFactory.buildDialog(requireActivity());
                    micConnectFactory.setCurrentUser(true);
                    micConnectFactory.setOnDialogButtonListClickListener(new OnDialogButtonListClickListener() {
                        @Override
                        public void onClick(String content) {
                            NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micQuit();
                            result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                @Override
                                public void onChanged(Integer integer) {
                                    SLog.e(SLog.TAG_SEAL_MIC, "???????????????");
                                    //???????????????????????????????????????????????????
                                    if (fragmentChatRoomBinding.chatroomVoiceIn.getVisibility() == View.VISIBLE) {
                                        fragmentChatRoomBinding.chatroomVoiceIn.setVisibility(View.GONE);
                                        fragmentChatRoomBinding.chatroomVoice.setVisibility(View.GONE);
                                    }
                                    dialog.cancel();
                                }
                            });
                        }
                    });
                    List<String> ids = new ArrayList<>();
                    ids.add(micBean.getUserId());
                    chatRoomViewModel.userBatch(ids);
                    chatRoomViewModel.getUserinfolistRepoLiveData().observe(getViewLifecycleOwner(), new Observer<NetResult<List<RoomMemberRepo.MemberBean>>>() {
                        @Override
                        public void onChanged(NetResult<List<RoomMemberRepo.MemberBean>> listNetResult) {
                            List<RoomMemberRepo.MemberBean> result = listNetResult.getData();
                            if (result == null || result.size() == 0) {
                                return;
                            }
                            RoomMemberRepo.MemberBean memberBean = result.get(0);
                            micConnectFactory.setUserName(memberBean.getUserName());
                        }
                    });
                    micConnectFactory.setMicPosition("?????????");
                    dialog.show();
                }
            });
        }

        //???????????????????????????
        if (micBean.getPosition() != 0 && !"".equals(micBean.getUserId())) {
            micUserName = "";
            ThreadManager.getInstance().runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    final MicDialogFactory micDialogFactory = new MicDialogFactory();
                    final Dialog micDialog = micDialogFactory.buildDialog(requireActivity(), micBean);
                    List<String> ids = new ArrayList<>();
                    ids.add(micBean.getUserId());
                    chatRoomViewModel.userBatch(ids);
                    chatRoomViewModel.getUserinfolistRepoLiveData().observe(getViewLifecycleOwner(), new Observer<NetResult<List<RoomMemberRepo.MemberBean>>>() {
                        @Override
                        public void onChanged(NetResult<List<RoomMemberRepo.MemberBean>> listNetResult) {
                            RoomMemberRepo.MemberBean memberBean = listNetResult.getData().get(0);
                            name = memberBean.getUserName();
                            //?????????????????????name
                            micUserName = memberBean.getUserName();
                            micDialogFactory.setUserName(memberBean.getUserName());
                            micDialogFactory.setPortrait(memberBean.getPortrait());
                        }
                    });
                    micDialogFactory.setMicPosition(String.valueOf(micBean.getPosition()) + "??????");
                    micDialogFactory.setOnDialogButtonListClickListener(new OnDialogButtonListClickListener() {
                        @Override
                        public void onClick(String content) {
                            if (getResources().getString(R.string.close_mic).equals(content)) {
                                //????????????
                                final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micState(MicState.CLOSE.getState(), micBean.getPosition());
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        if (result.isSuccess()) {
                                            ToastUtil.showToast("????????????");
                                            micDialog.cancel();
                                        }
                                    }
                                });
                            }
                            if (getResources().getString(R.string.open_mic).equals(content)) {
                                //????????????
                                final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micState(MicState.NORMAL.getState(), micBean.getPosition());
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        if (result.isSuccess()) {
                                            ToastUtil.showToast("????????????");
                                            micDialog.cancel();
                                        }
                                    }
                                });
                            }
                            if (getResources().getString(R.string.down_mic).equals(content)) {
                                //????????????
                                final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micKick(micBean.getUserId());
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        if (result.isSuccess()) {
                                            ToastUtil.showToast("????????????");
                                            micDialog.cancel();
                                        }
                                    }
                                });
                            }
                            if (getResources().getString(R.string.hand_over_host).equals(content)) {
                                //?????????????????????
                                final NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micTransferHost(micBean.getUserId());
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        if (result.isSuccess()) {
                                            ToastUtil.showToast("???????????????");
                                            micDialog.cancel();
                                        }
                                    }
                                });
                            }
                            if (getResources().getString(R.string.send_message).equals(content)) {
                                //????????????????????????????????????????????????
                                sendMessage(micDialog);
                            }
                            if (getResources().getString(R.string.send_gift_item).equals(content)) {
                                //??????????????????????????????????????????
                                ThreadManager.getInstance().runOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        GiftDialogFactory giftDialogFactory = new GiftDialogFactory();
                                        giftDialogFactory.buildDialog(requireActivity(), micUserName).show();
                                        micDialog.cancel();
                                        giftDialogFactory.setCallSendGiftMessage(new GiftDialogFactory.CallSendGiftMessage() {
                                            @Override
                                            public void callMessage(Message message) {
                                                //??????????????????????????????
                                                roomChatMessageListAdapter.addMessages(message);
                                                fragmentChatRoomBinding.chatroomListChat.smoothScrollToPosition(roomChatMessageListAdapter.getCount());
                                                fragmentChatRoomBinding.chatroomListChat.setSelection(roomChatMessageListAdapter.getCount());
                                            }
                                        });
                                    }
                                });
                            }
                            if (getResources().getString(R.string.go_out_room).equals(content)) {
                                //??????????????????
                                String userId = micBean.getUserId();
                                List<String> userIds = new ArrayList<>();
                                userIds.add(userId);
                                final NetStateLiveData<NetResult<Void>> result = roomMemberViewModel.kickMember(userIds);
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        if (result.isSuccess()) {
                                            ToastUtil.showToast("????????????");
                                            micDialog.cancel();
                                        }
                                    }
                                });
                            }
                        }
                    });
                    micDialog.show();
                }
            });
        }

    }

    /**
     * ??????????????????????????????????????????????????????
     */
    public void micAbsentAudience(final MicBean micBean) {
        if (micBean.getPosition() == 0) {
            //????????????????????????????????????????????????????????????????????????????????????
            ThreadManager.getInstance().runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    final MicConnectTakeOverDialogFactory micConnectTakeOverDialogFactory = new MicConnectTakeOverDialogFactory();
                    micConnectTakeOverDialogFactory.setShowMessageButton(false);
                    final Dialog dialog = micConnectTakeOverDialogFactory.buildDialog(requireActivity());
                    micConnectTakeOverDialogFactory.setCurrentType(false);
                    micConnectTakeOverDialogFactory.setUserName(getResources().getString(R.string.host_location));
                    micConnectTakeOverDialogFactory.setOnDialogButtonListClickListener(new OnDialogButtonListClickListener() {
                        @Override
                        public void onClick(String content) {
                            if (getResources().getString(R.string.send_message).equals(content)) {
                                //???????????????
                                //????????????????????????????????????????????????????????????
                            }
                            if (getResources().getString(R.string.take_over_host).equals(content)) {
                                //????????????
                                NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micTakeOverHost();
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        ToastUtil.showToast("????????????");
//                                        if (fragmentChatRoomBinding.chatroomVoiceIn.getVisibility() == View.GONE) {
//                                            //????????????????????????????????????
//                                            fragmentChatRoomBinding.chatroomVoiceIn.setVisibility(View.VISIBLE);
//                                            //??????????????????
//                                            fragmentChatRoomBinding.chatroomVoice.setVisibility(View.VISIBLE);
//                                        }
                                        dialog.cancel();
                                    }
                                });
                            }
                        }
                    });
                    dialog.show();
                }
            });
        } else {
            //??????????????????????????????????????????????????????????????????????????????
            ThreadManager.getInstance().runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    MicEnqueueDialogFactory micEnqueueDialogFactory = new MicEnqueueDialogFactory();
                    final Dialog dialog = micEnqueueDialogFactory.buildDialog(requireActivity(), micBean);
                    micEnqueueDialogFactory.setCallClick(new MicEnqueueDialogFactory.CallClick() {
                        @Override
                        public void onClick(String content) {
                            if (ButtonDelayUtil.isNormalClick()) {
//                                SLog.i("asdff", content);
                                if (getResources().getString(R.string.enqueue_mic).equals(content)) {
                                    //??????????????????????????????????????????
                                    if (micBean.getState() == MicState.NORMAL.getState() || micBean.getState() == MicState.CLOSE.getState()) {
                                        //?????????????????????????????????
                                        for (int i = 0; i < localMicBeanMap.size(); i++) {
                                            MicBean bean = localMicBeanMap.get(i);
                                            if (bean == null) {
                                                continue;
                                            }
                                            if (bean.getUserId().equals(CacheManager.getInstance().getUserId())) {
                                                dialog.cancel();
                                                return;
                                            }
                                        }
                                        //????????????????????????
                                        final NetStateLiveData<NetResult<Void>> result = roomMemberViewModel.micApply();
                                        result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                            @Override
                                            public void onChanged(Integer integer) {
                                                if (result.isSuccess()) {
                                                    SLog.e(SLog.TAG_SEAL_MIC, "????????????????????????????????????");
//                                                    ToastUtil.showToast("??????????????????");
//                                                    if (fragmentChatRoomBinding.chatroomVoiceIn.getVisibility() == View.GONE) {
////                                                        //????????????????????????????????????
////                                                        fragmentChatRoomBinding.chatroomVoiceIn.setVisibility(View.VISIBLE);
////                                                        //??????????????????
////                                                        fragmentChatRoomBinding.chatroomVoice.setVisibility(View.VISIBLE);
////                                                    }
                                                    dialog.cancel();
                                                }
                                            }
                                        });
                                    } else {
                                        ToastUtil.showToast(getResources().getString(R.string.already_lock_mic));
                                        dialog.cancel();
                                    }
                                }
                            }
                            dialog.cancel();
                        }
                    });
                    dialog.show();
                }
            });
        }
    }

    /**
     * ?????????????????????????????????????????????????????????
     */
    public void micPresentAudience(final MicBean micBean) {
        ThreadManager.getInstance().runOnUIThread(new Runnable() {
            @Override
            public void run() {
                final MicAudienceFactory micAudienceFactory = new MicAudienceFactory();
                final Dialog dialog = micAudienceFactory.buildDialog(requireActivity());
                if (micBean.getPosition() == 0) {
                    micAudienceFactory.setMicPosition("?????????");
                } else {
                    micAudienceFactory.setMicPosition(micBean.getPosition() + "??????");
                }
                List<String> ids = new ArrayList<>();
                ids.add(micBean.getUserId());
                chatRoomViewModel.userBatch(ids);
                chatRoomViewModel.getUserinfolistRepoLiveData().observe(getViewLifecycleOwner(), new Observer<NetResult<List<RoomMemberRepo.MemberBean>>>() {
                    @Override
                    public void onChanged(NetResult<List<RoomMemberRepo.MemberBean>> listNetResult) {
                        if (listNetResult.getData() == null || listNetResult.getData().size() <= 0) {
                            return;
                        }
                        RoomMemberRepo.MemberBean memberBean = listNetResult.getData().get(0);
                        name = memberBean.getUserName();
                        micUserName = memberBean.getUserName();
                        micAudienceFactory.setUserName(memberBean.getUserName());
                        micAudienceFactory.setPortrait(memberBean.getPortrait());

                    }
                });
                micAudienceFactory.setOnDialogButtonListClickListener(new OnDialogButtonListClickListener() {
                    @Override
                    public void onClick(String content) {
                        if (getResources().getString(R.string.send_message).equals(content)) {
                            //?????????
                            sendMessage(dialog);
                        }

                        if (getResources().getString(R.string.send_gift_item).equals(content)) {
                            //??????
                            ThreadManager.getInstance().runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    GiftDialogFactory giftDialogFactory = new GiftDialogFactory();
                                    giftDialogFactory.buildDialog(requireActivity(), micUserName).show();
                                    dialog.cancel();
                                    giftDialogFactory.setCallSendGiftMessage(new GiftDialogFactory.CallSendGiftMessage() {
                                        @Override
                                        public void callMessage(Message message) {
                                            roomChatMessageListAdapter.addMessages(message);
                                            fragmentChatRoomBinding.chatroomListChat.smoothScrollToPosition(roomChatMessageListAdapter.getCount());
                                            fragmentChatRoomBinding.chatroomListChat.setSelection(roomChatMessageListAdapter.getCount());
                                        }
                                    });
                                }
                            });
                        }
//                        if (getResources().getString(R.string.mic_apply).equals(content)) {
//                            //????????????
//                            if (micBean.getState() == MicState.NORMAL.getState() || micBean.getState() == MicState.LOCK.getState()) {
//                                final NetStateLiveData<NetResult<Void>> result = roomMemberViewModel.micApply();
//                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
//                                    @Override
//                                    public void onChanged(Integer integer) {
//                                        if (result.isSuccess()) {
//                                            ToastUtil.showToast("??????????????????");
//                                            dialog.cancel();
//                                        }
//                                    }
//                                });
//                            }
//                        }
                    }
                });
                dialog.show();
            }
        });
    }

    private void sendMessage(Dialog dialog) {
        dialog.cancel();
        clickProxy.popupEditText();
        String str = "@" + name + " ";
        fragmentChatRoomBinding.rcExtension.getInputEditText().setText(str);
        fragmentChatRoomBinding.rcExtension.getInputEditText().setSelection(str.length());
        fragmentChatRoomBinding.rcExtension.showSoftInput();
    }

    /**
     * ????????????????????????????????????????????????????????????
     */
    public void micPresentConnect(final MicBean micBean) {
        if (micBean.getPosition() == 0 && !"".equals(micBean.getUserId())) {
            //???????????????????????????????????????????????????dialog
            ThreadManager.getInstance().runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    final MicConnectTakeOverDialogFactory micConnectTakeOverDialogFactory = new MicConnectTakeOverDialogFactory();
                    final Dialog dialog = micConnectTakeOverDialogFactory.buildDialog(requireActivity());
                    micConnectTakeOverDialogFactory.setCurrentType(true);
                    micConnectTakeOverDialogFactory.setOnDialogButtonListClickListener(new OnDialogButtonListClickListener() {
                        @Override
                        public void onClick(String content) {
                            if (getResources().getString(R.string.send_message).equals(content)) {
                                //???????????????
                                sendMessage(dialog);
                            }
                            if (getResources().getString(R.string.take_over_host).equals(content)) {
                                //????????????
                                NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micTakeOverHost();
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        ToastUtil.showToast("????????????");
//                                        if (fragmentChatRoomBinding.chatroomVoiceIn.getVisibility() == View.GONE) {
//                                            fragmentChatRoomBinding.chatroomVoiceIn.setVisibility(View.VISIBLE);
//                                            fragmentChatRoomBinding.chatroomVoice.setVisibility(View.VISIBLE);
//                                        }
                                        dialog.cancel();
                                    }
                                });
                            }
                        }
                    });
                    List<String> ids = new ArrayList<>();
                    ids.add(micBean.getUserId());
                    chatRoomViewModel.userBatch(ids);
                    chatRoomViewModel.getUserinfolistRepoLiveData().observe(getViewLifecycleOwner(), new Observer<NetResult<List<RoomMemberRepo.MemberBean>>>() {
                        @Override
                        public void onChanged(NetResult<List<RoomMemberRepo.MemberBean>> listNetResult) {
                            RoomMemberRepo.MemberBean memberBean = listNetResult.getData().get(0);
                            name = memberBean.getUserName();
                            micConnectTakeOverDialogFactory.setPortrait(memberBean.getPortrait());
                        }
                    });
                    micConnectTakeOverDialogFactory.setUserName("???????????????");
                    dialog.show();

                }
            });
        }
        if (micBean.getPosition() != 0 && micBean.getUserId().equals(CacheManager.getInstance().getUserId())) {
            //???????????????????????????????????????????????????dialog
            ThreadManager.getInstance().runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    final MicConnectDialogFactory micConnectFactory = new MicConnectDialogFactory();
                    final Dialog dialog = micConnectFactory.buildDialog(requireActivity());
                    micConnectFactory.setCurrentUser(false);
                    micConnectFactory.setMicPosition(micBean.getPosition() + "??????");
                    micConnectFactory.setOnDialogButtonListClickListener(new OnDialogButtonListClickListener() {
                        @Override
                        public void onClick(String content) {
                            NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micQuit();
                            result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                @Override
                                public void onChanged(Integer integer) {
                                    ToastUtil.showToast("???????????????");
                                    //???????????????????????????????????????????????????
                                    if (fragmentChatRoomBinding.chatroomVoiceIn.getVisibility() == View.VISIBLE) {
                                        fragmentChatRoomBinding.chatroomVoiceIn.setVisibility(View.GONE);
                                        fragmentChatRoomBinding.chatroomVoice.setVisibility(View.GONE);
                                    }
                                    dialog.cancel();
                                }
                            });
                        }
                    });
                    List<String> ids = new ArrayList<>();
                    ids.add(micBean.getUserId());
                    chatRoomViewModel.userBatch(ids);
                    chatRoomViewModel.getUserinfolistRepoLiveData().observe(getViewLifecycleOwner(), new Observer<NetResult<List<RoomMemberRepo.MemberBean>>>() {
                        @Override
                        public void onChanged(NetResult<List<RoomMemberRepo.MemberBean>> listNetResult) {
                            if (listNetResult == null || listNetResult.getData() == null || listNetResult.getData().size() <= 0) {
                                return;
                            }
                            RoomMemberRepo.MemberBean memberBean = listNetResult.getData().get(0);
                            micConnectFactory.setUserName(memberBean.getUserName());
                            micConnectFactory.setPortrait(memberBean.getPortrait());
                        }
                    });
                    dialog.show();
                }
            });
        }
        if (micBean.getPosition() != 0 && !micBean.getUserId().equals(CacheManager.getInstance().getUserId())) {
            //??????????????????????????????????????????????????????????????????????????????
            micPresentAudience(micBean);
        }
    }

    public void micAbsentConnect(final MicBean micBean) {
        //???????????????????????????????????????????????????????????????????????????????????????
        if (micBean.getPosition() == 0) {
            ThreadManager.getInstance().runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    final MicConnectTakeOverDialogFactory micConnectTakeOverDialogFactory = new MicConnectTakeOverDialogFactory();
                    micConnectTakeOverDialogFactory.setShowMessageButton(false);
                    final Dialog dialog = micConnectTakeOverDialogFactory.buildDialog(requireActivity());
                    micConnectTakeOverDialogFactory.setCurrentType(false);
                    micConnectTakeOverDialogFactory.setOnDialogButtonListClickListener(new OnDialogButtonListClickListener() {
                        @Override
                        public void onClick(String content) {
                            if (getResources().getString(R.string.send_message).equals(content)) {
                                //???????????????
                                //????????????????????????????????????????????????????????????
                            }
                            if (getResources().getString(R.string.take_over_host).equals(content)) {
                                //????????????
                                NetStateLiveData<NetResult<Void>> result = chatRoomViewModel.micTakeOverHost();
                                result.getNetStateMutableLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
                                    @Override
                                    public void onChanged(Integer integer) {
                                        ToastUtil.showToast("????????????");
                                        dialog.cancel();
                                    }
                                });
                            }
                        }
                    });
                    micConnectTakeOverDialogFactory.setUserName("???????????????");
                    dialog.show();
                }
            });
        }
    }

    public class ClickProxy {

        public void micManager() {
            clickMic(0);
        }

        public void mic1() {
            clickMic(1);
        }

        public void mic2() {
            clickMic(2);
        }

        public void mic3() {
            clickMic(3);
        }

        public void mic4() {
            clickMic(4);
        }

        public void mic5() {
            clickMic(5);
        }

        public void mic6() {
            clickMic(6);
        }

        public void mic7() {
            clickMic(7);
        }

        public void mic8() {
            clickMic(8);
        }


        public void popupEditText() {
            isShowKey = true;
            fragmentChatRoomBinding.rcExtension.setVisibility(View.VISIBLE);
            fragmentChatRoomBinding.rcExtension.showSoftInput();
            fragmentChatRoomBinding.chatroomFunction.setVisibility(View.GONE);
        }

        public void hide() {
            isShowKey = false;
            fragmentChatRoomBinding.rcExtension.setVisibility(View.GONE);
            fragmentChatRoomBinding.rcExtension.collapseExtension();
            fragmentChatRoomBinding.chatroomFunction.setVisibility(View.VISIBLE);
        }

        public void showRoomMemberManagerDialog() {
            if (ButtonDelayUtil.isNormalClick()) {
                new RoomMemberManagerDialogFactory().buildDialog(requireActivity(), RoomMemberStatus.ENQUEUE_MIC.getStatus()).show();
            }
        }

        public void showRoomSettingDialog() {
            //??????????????????????????????true?????????????????????????????????
            isAlertSettingDialog = true;
            chatRoomViewModel.roomDetail(roomId);
        }

        private void alertDialog() {
            //??????????????????false
            isAlertSettingDialog = false;
            final RoomSettingDialogFactory roomSettingDialogFactory = new RoomSettingDialogFactory();
            roomSettingDialogFactory.setOnRoomSettingDialogAction(new RoomSettingDialogFactory.OnRoomSettingDialogAction() {
                @Override
                public void audienceJoin(boolean isChecked) {
                    isAudienceJoin = isChecked;
                    //??????????????????????????????????????????????????????????????????????????????
                    if (UserRoleType.HOST.isHost(CacheManager.getInstance().getUserRoleType())) {
                        chatRoomViewModel.roomSetting(SealMicApp.getApplication(), roomId, isChecked, isAudienceFreeMic);
                    } else {
                        ToastUtil.showToast(getResources().getString(R.string.no_permission_update));
                    }

                }

                @Override
                public void audienceFreeMic(boolean isChecked) {
                    isAudienceFreeMic = isChecked;
                    //??????????????????????????????????????????????????????????????????????????????
                    if (UserRoleType.HOST.isHost(CacheManager.getInstance().getUserRoleType())) {
                        chatRoomViewModel.roomSetting(SealMicApp.getApplication(), roomId, isAudienceJoin, isChecked);
                    } else {
                        ToastUtil.showToast(getResources().getString(R.string.no_permission_update));
                    }
                }

                @Override
                public void useTelephoneReceiver(boolean isChecked) {
                    ToastUtil.showToast(getResources().getString(R.string.room_setting_success));
                    //???????????????????????????
                    RTCClient.getInstance().setSpeakerEnable(!isChecked);
                    //????????????
                    fragmentChatRoomBinding.chatroomVoiceOut.setSelected(isChecked);
                }

                @Override
                public void openDebug(boolean isChecked) {
                    if (isChecked) {
                        fragmentChatRoomBinding.debugLayout.debugInfo.setVisibility(View.VISIBLE);
                    } else {
                        fragmentChatRoomBinding.debugLayout.debugInfo.setVisibility(View.GONE);
                    }
                    CacheManager.getInstance().cacheIsOpenDebug(isChecked);
                }
            });
            Dialog dialog = roomSettingDialogFactory.buildDialog(requireActivity());
            dialog.show();
        }

        public void showChangeAudioDialog() {
            new ChangeBaseAudioDialogFactory().buildDialog(requireActivity()).show();
        }

        public void showBgAudioDialog() {
            new BgBaseAudioDialogFactory().buildDialog(requireActivity()).show();
        }

        public void showRoomNoticeDialog() {
            new RoomNoticeDialogFactory().buildDialog(requireActivity()).show();
        }

        public void showGiftDialog() {
            GiftDialogFactory giftDialogFactory = new GiftDialogFactory();
            giftDialogFactory.buildDialog(requireActivity(), "")
                    .show();
            giftDialogFactory.setCallSendGiftMessage(new GiftDialogFactory.CallSendGiftMessage() {
                @Override
                public void callMessage(Message message) {
                    roomChatMessageListAdapter.addMessages(message);
                    fragmentChatRoomBinding.chatroomListChat.smoothScrollToPosition(roomChatMessageListAdapter.getCount());
                    fragmentChatRoomBinding.chatroomListChat.setSelection(roomChatMessageListAdapter.getCount());

                }
            });
        }

    }
}
