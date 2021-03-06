package cn.rongcloud.sealmicandroid.ui.main;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jcodecraeer.xrecyclerview.ArrowRefreshHeader;
import com.jcodecraeer.xrecyclerview.ProgressStyle;
import com.jcodecraeer.xrecyclerview.XRecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.rongcloud.sealmicandroid.BuildConfig;
import cn.rongcloud.sealmicandroid.DebugActivity;
import cn.rongcloud.sealmicandroid.R;
import cn.rongcloud.sealmicandroid.bean.repo.RoomDetailRepo;
import cn.rongcloud.sealmicandroid.bean.repo.RoomListRepo;
import cn.rongcloud.sealmicandroid.bean.repo.VersionCheckRepo;
import cn.rongcloud.sealmicandroid.common.Event;
import cn.rongcloud.sealmicandroid.common.NetStateLiveData;
import cn.rongcloud.sealmicandroid.common.constant.MainLoadData;
import cn.rongcloud.sealmicandroid.common.factory.CommonViewModelFactory;
import cn.rongcloud.sealmicandroid.common.lifecycle.MainObserver;
import cn.rongcloud.sealmicandroid.common.listener.RoomListItemOnClickListener;
import cn.rongcloud.sealmicandroid.databinding.MainFragmentBinding;
import cn.rongcloud.sealmicandroid.manager.CacheManager;
import cn.rongcloud.sealmicandroid.manager.NavOptionsRouterManager;
import cn.rongcloud.sealmicandroid.ui.login.LoginViewModel;
import cn.rongcloud.sealmicandroid.ui.room.ChatRoomViewModel;
import cn.rongcloud.sealmicandroid.ui.room.adapter.ChatRoomRefreshAdapter;
import cn.rongcloud.sealmicandroid.ui.widget.CustomTitleBar;
import cn.rongcloud.sealmicandroid.ui.widget.PromptDialog;
import cn.rongcloud.sealmicandroid.util.ToastUtil;
import cn.rongcloud.sealmicandroid.util.log.SLog;

/**
 * ?????????
 */
public class MainFragment extends Fragment {

    private MainViewModel mainViewModel;
    private ChatRoomViewModel chatRoomViewModel;
    private LoginViewModel loginViewModel;
    private MainFragmentBinding mainFragmentBinding;
    /**
     * ????????????
     */
    private int pageSize = 12;

    /**
     * ???????????????0?????????
     */
    String startIndex = "";
    private ChatRoomRefreshAdapter chatRoomRefreshAdapter;
    private XRecyclerView mainRoomRecyclerView;

    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    /**
     * 1?????????????????????????????????2???????????????????????????
     */
    private int clickWhere;
    private RoomListRepo.RoomsBean roomInfo;
    private AppVersionViewModel appVersionViewModel;

    /**
     * ???????????????????????????
     */
    private boolean isShowUpgrade;

    private List<RoomListRepo.RoomsBean> roomsBeanList = new ArrayList<>();

    private GridLayoutManager mainListLayoutManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        getLifecycle().addObserver(new MainObserver(MainFragment.class.getSimpleName()));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        appVersionViewModel = new ViewModelProvider(this, new CommonViewModelFactory()).get(AppVersionViewModel.class);
        chatRoomViewModel = new ViewModelProvider(this,
                new CommonViewModelFactory()).get(ChatRoomViewModel.class);
        loginViewModel = new ViewModelProvider(this, new CommonViewModelFactory()).get(LoginViewModel.class);
        mainFragmentBinding =
                DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false);
        mainFragmentBinding.setMainViewModel(mainViewModel);
        mainFragmentBinding.setLifecycleOwner(this);
        return mainFragmentBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        //??????????????????
        checkAppVersion();
        startIndex = "";
        //?????????????????????
        int position = mainViewModel.getMainListPosition();
        if (position != -1) {
            if (position > pageSize - 1) {
                pageSize = position + 1;
            }
        }
        loadData(startIndex, pageSize, MainLoadData.LOAD_DATA.getValue());
    }

    /**
     * ??????????????????
     */
    private void checkAppVersion() {
        NetStateLiveData<VersionCheckRepo> versionCheckRepoNetStateLiveData = appVersionViewModel.checkVersion();
        versionCheckRepoNetStateLiveData.observe(getViewLifecycleOwner(), new Observer<VersionCheckRepo>() {
            @Override
            public void onChanged(final VersionCheckRepo versionCheckRepo) {
                if (versionCheckRepo == null) {
                    return;
                }
                //????????????
                if (Long.parseLong(versionCheckRepo.getVersionCode()) > BuildConfig.VERSION_CODE) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("????????????");
                    builder.setMessage(versionCheckRepo.getReleaseNote());
                    if (versionCheckRepo.isForceUpgrade()) {
                        builder.setCancelable(false);
                        builder.setNegativeButton("", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                    } else {
                        builder.setCancelable(true);
                        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                builder.create().cancel();
                            }
                        });
                    }
                    builder.setPositiveButton("????????????", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String downloadUrl = versionCheckRepo.getDownloadUrl();
                            if (URLUtil.isValidUrl(downloadUrl)) {
                                Uri uri = Uri.parse(downloadUrl);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                requireContext().startActivity(intent);
                            } else {
                                ToastUtil.showToast(R.string.invalid_url);
                            }
                        }
                    });
                    builder.show();
                }
            }
        });
    }

    private void initView() {
        mainRoomRecyclerView = mainFragmentBinding.mainChatroomRv;
        chatRoomRefreshAdapter = new ChatRoomRefreshAdapter();
        chatRoomRefreshAdapter.setItemOnClickListener(new RoomListItemOnClickListener() {

            @Override
            public void onClick(final View view, final RoomListRepo.RoomsBean roomsBean, int position) {
                mainViewModel.setMainListPosition(position);
                clickWhere = 1;
                roomInfo = roomsBean;
                if (checkPermissions()) {
                    mainFragmentBinding.mainProgressBar.setVisibility(View.VISIBLE);
                    //????????????????????????????????????????????????????????????????????????
                    chatRoomViewModel.roomDetail(roomsBean.getRoomId());
                    chatRoomViewModel.getRoomDetailRepoMutableLiveData().observe(getViewLifecycleOwner(), new Observer<RoomDetailRepo>() {
                        @Override
                        public void onChanged(RoomDetailRepo roomDetailRepo) {
                            if (roomDetailRepo == null) {
                                //???????????????
                                Iterator<RoomListRepo.RoomsBean> roomsBeanIterator = roomsBeanList.iterator();
                                roomsBeanIterator.remove();
                                chatRoomRefreshAdapter.setData(roomsBeanList);
                            } else {
                                //??????????????????????????????????????????
                                //?????????????????????????????????????????????????????????
                                //????????????????????????????????????
                                if (roomsBean.isAllowedJoinRoom()) {
                                    chatRoomViewModel.gotoChatRoomFragment(view,
                                            roomsBean.getRoomId(),
                                            roomsBean.getRoomName(),
                                            roomsBean.getThemePictureUrl());
                                } else {
                                    ToastUtil.showToast(getResources().getString(R.string.room_is_lock));
                                }
                            }
                            mainFragmentBinding.mainProgressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    ToastUtil.showToast(R.string.toast_error_need_app_permission);
                }
            }

            @Override
            public void onClickCreateRoom() {
                clickWhere = 2;
                if (CacheManager.getInstance().getIsLogin()) {
                    if (checkPermissions()) {
                        NavOptionsRouterManager.getInstance().gotoCreateRoomFragment(getView());
                    } else {
                        ToastUtil.showToast(R.string.toast_error_need_app_permission);
                    }
                } else {
                    NavOptionsRouterManager.getInstance().gotoLoginFragmentFromMain(getView());
                }
            }
        });
        mainListLayoutManager = new GridLayoutManager(requireActivity(), 2);
        mainRoomRecyclerView.setLayoutManager(mainListLayoutManager);
        mainRoomRecyclerView.setAdapter(chatRoomRefreshAdapter);
        mainRoomRecyclerView.setLoadingMoreEnabled(true);
        mainRoomRecyclerView.setRefreshProgressStyle(ProgressStyle.BallSpinFadeLoader);
        mainRoomRecyclerView.setLoadingMoreProgressStyle(ProgressStyle.SquareSpin);
        View refreshHeader = LayoutInflater.from(requireContext()).inflate(R.layout.list_refresh_header, null);
        TextView currentVersionNameTextView = refreshHeader.findViewById(R.id.current_version_name);
        String versionName = String.format(getResources().getString(R.string.current_version), BuildConfig.VERSION_NAME);
        currentVersionNameTextView.setText(versionName);
        ArrowRefreshHeader arrowRefreshHeader = mainRoomRecyclerView.getDefaultRefreshHeaderView();
        RelativeLayout headerContentLayout = arrowRefreshHeader.findViewById(R.id.listview_header_content);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.header_refresh_time_container);
        refreshHeader.setLayoutParams(layoutParams);
        headerContentLayout.addView(refreshHeader);
        mainRoomRecyclerView.setRefreshHeader(arrowRefreshHeader);
        arrowRefreshHeader.setRefreshTimeVisible(true);

        mainRoomRecyclerView.setPullRefreshEnabled(true);
        mainRoomRecyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                startIndex = "";
                loadData(startIndex, pageSize, MainLoadData.PULL_REFRESH.getValue());
            }

            @Override
            public void onLoadMore() {
                loadData(startIndex, pageSize, MainLoadData.LOAD_MORE.getValue());
            }
        });
        mainFragmentBinding.mainTitle.setTitleClickListener(new CustomTitleBar.TitleClickListener() {
            @Override
            public void onLeftClick() {

            }

            @Override
            public void onRightClick() {
                if (CacheManager.getInstance().getIsLogin()) {
                    return;
                }
                mainViewModel.gotoLoginFragment(getView());
            }

            @Override
            public void onTitleLongClick() {
                showDebugActivity();
            }
        });
        if (CacheManager.getInstance().getIsLogin()) {
            mainFragmentBinding.mainTitle.setRightUrl(CacheManager.getInstance().getUserPortrait());
            mainFragmentBinding.mainTitle.hideRightText();
            mainFragmentBinding.mainTitle.setTitleClickListener(new CustomTitleBar.TitleClickListener() {
                @Override
                public void onLeftClick() {

                }

                @Override
                public void onRightClick() {
                    if (CacheManager.getInstance().getIsLogin()) {
                        return;
                    }
                    mainViewModel.gotoLoginFragment(getView());
                }

                @Override
                public void onTitleLongClick() {
                    showDebugActivity();
                }
            });
        } else {
            mainFragmentBinding.mainTitle.setRightTitle(getString(R.string.login_commit));
            mainFragmentBinding.mainTitle.showRightText();
        }
    }

    /**
     * ????????????
     *
     * @param page  ????????????????????????????????????????????????????????????????????????????????????????????? roomId
     * @param size  ????????????????????????
     * @param value ??????????????????????????????????????????????????????
     */
    public void loadData(final String page, final int size, final int value) {
        //????????????????????????
        final NetStateLiveData<RoomListRepo> result = chatRoomViewModel.roomList(page, size);
        result.observe(getViewLifecycleOwner(), new Observer<RoomListRepo>() {
            @Override
            public void onChanged(RoomListRepo roomListRepo) {
                if (roomListRepo == null) {
                    return;
                }
                List<RoomListRepo.RoomsBean> rooms = roomListRepo.getRooms();
                if (MainLoadData.PULL_REFRESH.getValue() == value) {
                    //????????????????????????
                    if (rooms == null) {
                        mainRoomRecyclerView.refreshComplete();
                        return;
                    }
                    chatRoomRefreshAdapter.setData(rooms);
                    roomsBeanList.addAll(rooms);
                    mainRoomRecyclerView.refreshComplete();
                }
                if (MainLoadData.LOAD_MORE.getValue() == value) {
                    //????????????????????????
                    if (rooms == null) {
                        mainRoomRecyclerView.loadMoreComplete();
                        ToastUtil.showToast("?????????????????????");
                        return;
                    }
                    chatRoomRefreshAdapter.addData(rooms);
                    roomsBeanList.addAll(rooms);
                    mainRoomRecyclerView.loadMoreComplete();
                }
                if (rooms.size() > 0 && !CacheManager.getInstance().getLastCreateRoomId().isEmpty()
                        && MainLoadData.LOCATE_CREATE.getValue() != value) {
                    boolean isHaveLastCreateRoom = false;
                    int createTarget = 0;
                    for (int i = 0; i < rooms.size(); i++) {
                        RoomListRepo.RoomsBean roomsBean = rooms.get(i);
                        if (roomsBean.getRoomId().equals(CacheManager.getInstance().getLastCreateRoomId())) {
                            isHaveLastCreateRoom = true;
                            createTarget = i;
                            break;
                        }
                    }
                    if (isHaveLastCreateRoom) {
                        mainViewModel.setMainListPosition(createTarget);
                        CacheManager.getInstance().cacheLastCreateRoomId("");
                        loadData(startIndex, createTarget + 1, MainLoadData.LOCATE_CREATE.getValue());
                    } else {
                        //??????????????????
                        loadData(startIndex, roomListRepo.getTotalCount() + 1, MainLoadData.LOCATE_CREATE.getValue());
                    }
                    return;
                }
                if (MainLoadData.LOCATE_CREATE.getValue() == value) {
                    chatRoomRefreshAdapter.addData(rooms);
                    roomsBeanList.addAll(rooms);
                    mainRoomRecyclerView.loadMoreComplete();
                    if (!CacheManager.getInstance().getLastCreateRoomId().isEmpty()) {
                        //?????????????????????????????????????????????????????????????????????????????????????????????
                        for (int i = 0; i < rooms.size(); i++) {
                            RoomListRepo.RoomsBean roomsBean = rooms.get(i);
                            if (roomsBean.getRoomId().equals(CacheManager.getInstance().getLastCreateRoomId())) {
                                mainViewModel.setMainListPosition(i);
                                CacheManager.getInstance().cacheLastCreateRoomId("");
                                break;
                            }
                        }
                    }

                }
                if (MainLoadData.LOAD_DATA.getValue() == value) {
                    pageSize = 12;
                    chatRoomRefreshAdapter.addData(rooms);
                    roomsBeanList.addAll(rooms);
                    mainRoomRecyclerView.loadMoreComplete();
                }
                RoomListRepo.RoomsBean room = rooms.get(rooms.size() - 1);
                startIndex = room.getRoomId();
                //?????????????????????
                int position = mainViewModel.getMainListPosition();
                if (position != -1) {
                    mainListLayoutManager.scrollToPositionWithOffset(position, 0);
                    mainViewModel.setMainListPosition(-1);
                }
            }
        });
    }

    private boolean checkPermissions() {
        List<String> unGrantedPermissions = new ArrayList<>();
        for (String permission : MANDATORY_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                unGrantedPermissions.add(permission);
            }
        }
        if (unGrantedPermissions.size() == 0) {
            //???????????????????????????
            return true;
        } else {
            //????????????????????????????????????????????????
            String[] array = new String[unGrantedPermissions.size()];
            requestPermissions(unGrantedPermissions.toArray(array), 0);
            ToastUtil.showToast(R.string.toast_error_need_app_permission);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions.length == grantResults.length) {
            //?????????????????????????????????
            if (clickWhere == 1) {
                if (roomInfo.isAllowedJoinRoom()) {
                    chatRoomViewModel.gotoChatRoomFragment(getView(),
                            roomInfo.getRoomId(),
                            roomInfo.getRoomName(),
                            roomInfo.getThemePictureUrl());
                } else {
                    ToastUtil.showToast(getResources().getString(R.string.room_is_lock));
                }
            } else if (clickWhere == 2) {
                if (CacheManager.getInstance().getIsLogin()) {
                    if (checkPermissions()) {
                        NavOptionsRouterManager.getInstance().gotoCreateRoomFragment(getView());
                    } else {
                        ToastUtil.showToast(R.string.toast_error_need_app_permission);
                    }
                } else {
                    NavOptionsRouterManager.getInstance().gotoLoginFragmentFromMain(getView());
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUserGoOutBean(Event.UserGoOutBean userGoOutBean) {
        ToastUtil.showToast("??????????????????????????????");
        SLog.e(SLog.TAG_SEAL_MIC, "???????????????????????????");
        loginViewModel.visitorLogin();
        mainFragmentBinding.mainTitle.setRightTitle(getString(R.string.login_commit));
        mainFragmentBinding.mainTitle.showRightText();
        if (mainViewModel != null) {
            SLog.e(SLog.TAG_SEAL_MIC, "?????????????????????????????????????????????????????????");
            mainViewModel.gotoLoginFragment(getView());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mainRoomRecyclerView != null) {
            // this will totally release XR's memory
            mainRoomRecyclerView.destroy();
            mainRoomRecyclerView = null;
        }
        EventBus.getDefault().unregister(this);
    }

    private void showDebugActivity(){
//        if (!CacheManager.getInstance().getDebugMode()) {
            PromptDialog dialog = PromptDialog.newInstance(requireActivity(), "",
                    getResources().getString(R.string.about_opening_debug));
            dialog.setPromptButtonClickedListener(new PromptDialog.OnPromptButtonClickedListener() {
                @Override
                public void onPositiveButtonClicked() {
                    CacheManager.getInstance().cacheDebugMode(true);
                    Toast.makeText(requireActivity(), R.string.about_opened_debug,
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(requireActivity(), DebugActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onNegativeButtonClicked() {
                }
            }).show();
//        }
    }
}