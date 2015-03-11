/**
 * MloInfoViewController.java
 * (C) 2014, Hitachi Solutions, Ltd.
 */
package org.o3project.mlo.client.impl.control;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.o3project.mlo.client.control.ClientConfigConstants;
import org.o3project.mlo.client.control.MloClientException;
import org.o3project.mlo.client.control.MloDialogController;
import org.o3project.mlo.client.control.MloViewController;
import org.o3project.mlo.client.control.SliceTask;
import org.o3project.mlo.client.control.SliceTaskFactory;
import org.o3project.mlo.client.view.FlowPanel;
import org.o3project.mlo.client.view.MloCommonTable;
import org.o3project.mlo.server.dto.FlowDto;
import org.o3project.mlo.server.dto.SliceDto;

/**
 * スライス情報表示画面のコントローラクラスです。
 */
public class MloInfoViewController implements MloViewController, Initializable {
	private static final Log LOG = LogFactory.getLog(MloInfoViewController.class);
	
	private static final String SLICE_PARAM_KEY_ID = "Id";
	private static final String SLICE_PARAM_KEY_NAME = "Name";
	
	private static final double PROP_TABLE_WIDTH = 520d;
	private static final double PROP_TABLE_HEIGHT = 85d;
	
	@FXML
	private AnchorPane anchorPane;
	
	@FXML
	private Label headerLabel;
	
	@FXML
	private ScrollPane scrollPane;
	
	@FXML
	private Pane propPanel;
	
	@FXML
	private ListView<String> sliceListView;

	@FXML
	private Button deleteSliceBt;
	
	@FXML
	private Button editSliceBt;
	
	@FXML
	private WebView topologyWebView;
	
	@FXML
	private Label copyrightLabel;
	
	private MloCommonTable slicePropsDispTable;
	private final VBox sliceFlowsDispVBox = new VBox();
	
	private SliceDto targetSlice;
	private LinkedHashMap<String, Integer> sliceMap;
	private String selectSliceName;

	private WebEngine topologyWebEngine;
	
	private String topologyViewUrl;
	
	/* (non-Javadoc)
	 * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		LOG.info("Start initializing");
		
		headerLabel.setText(ClientConfigConstants.APP_NAME);
		copyrightLabel.setText(ClientConfigConstants.APP_COPYRIGHT);
		
		MultipleSelectionModel<String>  model = sliceListView.getSelectionModel();
		ObservableList<String> selectedItems = model.getSelectedItems();
		selectedItems.addListener(new ListChangeListener<String>() {
			@Override
			public void onChanged(Change<? extends String> change) {
				selectSliceName = change.getList().get(0);
				
				doReadSliceTask(MloClient.getInstance());
			}
		});
		
		scrollPane.setContent(sliceFlowsDispVBox);
		
		setUpView();
		
		LOG.info("End initializing");
	}

	/*
	 * (non-Javadoc)
	 * @see org.o3project.mlo.client.control.MloViewController#getRoot()
	 */
	@Override
	public Parent getRoot() {
		return anchorPane;
	}
	
	/**
	 * ビュー要素のセットアップを行います。
	 */
	public void setUpView() {
		targetSlice = null; // 表示した瞬間はスライス情報は空。
		
		setButtonDisable(true);
		
		setUpSlicePropsView(targetSlice);
		setUpSliceFlowsView(targetSlice);
		
		// スライス一覧取得処理
		doGetSlicesTask(MloClient.getInstance());
		
		topologyWebEngine = topologyWebView.getEngine();
	}

	/**
	 * 引数で指定されたスライスのプロパティを表示する。
	 * スライスが null の場合は空表示にする。
	 * @param sliceDto 表示するスライス DTO
	 */
	private void setUpSlicePropsView(SliceDto sliceDto) {
		if (slicePropsDispTable != null) {
			propPanel.getChildren().removeAll(slicePropsDispTable);
			slicePropsDispTable.dispose();
			slicePropsDispTable = null;
		}
		
		if (sliceDto != null) {
			// 選択したフロー情報を画面に表示する
			LinkedHashMap<String, String> sliceInfoMap = new LinkedHashMap<String, String>();
			sliceInfoMap.put(SLICE_PARAM_KEY_ID, String.valueOf(sliceDto.id));
			sliceInfoMap.put(SLICE_PARAM_KEY_NAME, sliceDto.name);
			slicePropsDispTable = new MloCommonTable(sliceInfoMap, PROP_TABLE_WIDTH, false);
			slicePropsDispTable.setPrefSize(PROP_TABLE_WIDTH, PROP_TABLE_HEIGHT);
			propPanel.getChildren().add(slicePropsDispTable);
		}
	}

	/**
	 * 引数で指定されたスライスのフローを表示する。
	 * スライスが null の場合は空表示にする。
	 * @param sliceDto 表示するスライス DTO
	 */
	private void setUpSliceFlowsView(SliceDto sliceDto) {
		FlowPanel[] fps = sliceFlowsDispVBox.getChildren().toArray(new FlowPanel[0]);
		sliceFlowsDispVBox.getChildren().removeAll(fps);
		for (FlowPanel fp : fps) {
			fp.dispose();
		}
		
		if (sliceDto != null) {
			int flowCnt = 1;
			for(FlowDto flowDto : sliceDto.flows){
				FlowPanel flowPanel = new FlowPanel("Flow-"+flowCnt, flowDto, false, null);
				sliceFlowsDispVBox.getChildren().add(flowPanel);
				flowCnt += 1;
			}
		}
	}
	
	/**
	 * スライス一覧を設定する。
	 * @param dataMap スライス情報マップ
	 */
	private void setSliceList(LinkedHashMap<String, Integer> dataMap){
		sliceMap = dataMap;
		List<String> sliceList = new ArrayList<String>();
		
		for(Map.Entry<String, Integer> entry : dataMap.entrySet()) {
			String key = (String) entry.getKey();
			sliceList.add(key);
		}
		ObservableList<String> data = FXCollections.observableArrayList(sliceList);
		sliceListView.setItems(data);
	}
	
	/**
	 *  スライス情報を設定する
	 * @param sliceDto スライス情報
	 */
	private void setData(SliceDto sliceDto){
		targetSlice = sliceDto;
		setUpSlicePropsView(targetSlice);
		setUpSliceFlowsView(targetSlice);
	}

	/**
	 * 削除用スライスDTOを作成します。
	 * @return スライスDTO
	 */
	public SliceDto getDeleteSliceData(){
		SliceDto sliceDto= new SliceDto();
		sliceDto.id = targetSlice.id;
		return sliceDto;
	}
	
	/**
	 * 取得用スライスDTOを作成します。
	 * @return スライスDTO
	 */
	public SliceDto getReadSliceDto(){
		SliceDto sliceDto= new SliceDto();
		sliceDto.id = sliceMap.get(selectSliceName);
		return sliceDto;
		
	}
	
	/**
	 * スライス情報表示画面内のボタンのアクションハンドラです。
	 * FXML からのハンドラ指定用です。
	 * @param event アクションイベント
	 */
	@FXML
	void handleButtonAction(ActionEvent event) {
		
		Button bt = (Button) event.getSource();

		if("addSliceBt".equals(bt.getId())){
			MloClient.getInstance().changeCreateView();
		}
		
		if("editSliceBt".equals(bt.getId())){
			MloClient.getInstance().changeUpdateView();
		}
		
		if("deleteSliceBt".equals(bt.getId())){
			MloDialogController mloDialog = new MloDialogController();
			mloDialog.setMsgLabl(SliceTaskFactory.DELETE_TASK_LB);
			mloDialog.showDaialog();
		}
	}
	
	/**
	 * Editボタン及びDeleteボタン、SimpleViewボタンを非活性にするか否かを設定する。
	 */
	private void setButtonDisable(boolean isDisable){
		deleteSliceBt.setDisable(isDisable);
		editSliceBt.setDisable(isDisable);
	}
	
	/**
	 * スライス一覧取得をタスク実行する。
	 * @param mloClient {@link MloClient} インスタンス
	 */
	private void doGetSlicesTask(final MloClient mloClient) {
		String taskKey = SliceTaskFactory.LIST_TASK_LB;
		final SliceTask task = mloClient.getSliceTaskFactory().createTask(taskKey);
		
		task.setOnScheduled(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent evt) {
				mloClient.getIndicatorStage().show();
			}
		});
		
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent evt) {
				List<SliceDto> sliceDtos = task.getSliceDtoList();
				MloClientException err = task.getError();

				// 取得したスライスリストをデータとして設定
				LinkedHashMap<String, Integer> newSliceMap = new LinkedHashMap<String, Integer>();
				if (sliceDtos != null) {
					for (SliceDto sliceDto : sliceDtos) {
						newSliceMap.put(sliceDto.name, sliceDto.id);
					}
				}
				setSliceList(newSliceMap);
				
				clearTopologyViewFlowList();
				
				// インジケータを閉じる
				mloClient.getIndicatorStage().hide();

				// エラーがあれば結果ダイアログに表示
				if (err != null) {
					mloClient.showResultAndGoBackToInfoView(err);
				}
			}
		});
		
		mloClient.getExecutorService().submit(task);
	}

	/**
	 * スライス読み込み処理タスクを実行する。
	 * @param mloClient {@link MloClient} インスタンス
	 */
	private void doReadSliceTask(final MloClient mloClient) {
		final String taskKey = SliceTaskFactory.READ_TASK_LB;
		final SliceTask task = mloClient.getSliceTaskFactory().createTask(taskKey);
		
		task.setOnScheduled(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent arg0) {
				mloClient.getIndicatorStage().show();
			}
		});
		
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent arg0) {
				List<SliceDto> sliceDtos = task.getSliceDtoList();
				MloClientException err = task.getError();
				
				// 取得したスライスをデータとして設定
				SliceDto sliceDto = null;
				if (sliceDtos != null && sliceDtos.size() > 0) {
					sliceDto = task.getSliceDtoList().get(0);
					setData(sliceDto);
					mloClient.setSelectSliceDto(sliceDto);
					setButtonDisable(false);
				}
				
				setTopologyViewFlowList(sliceDto);
				
				// インジケータを閉じる
				mloClient.getIndicatorStage().hide();
				
				// エラーがあれば結果ダイアログに表示
				if (err != null) {
					mloClient.showResultAndGoBackToInfoView(err);
				}
			}
		});
		
		mloClient.getExecutorService().submit(task);
	}
	
	private void setTopologyViewFlowList(SliceDto sliceDto) {
		String scriptArgSliceName = null;
		String scriptArgFlowListItems = null;
		
		if (sliceDto != null && sliceDto.name != null) {
			scriptArgSliceName = "'"+ sliceDto.name + "'";
		} else {
			scriptArgSliceName = "null";
		}
		
		if (sliceDto != null && sliceDto.flows != null) {
			StringBuilder sb = new StringBuilder();
			String ele = null;
			int idx = 1;
			for (FlowDto flowDto : sliceDto.flows) {
				String flowName = flowDto.name;
				String flowTypeName = flowDto.flowTypeName;
				if (flowName == null) {
					flowName = "Flow-" + idx;
				}
				if (flowTypeName == null) {
					flowTypeName = "== UNKNOWN ==";
				}
				ele = String.format("{flowName:\"%s\", flowTypeName:\"%s\"}", 
						flowName, flowTypeName);
				String prefix = "";
				if (sb.length() > 0) {
					prefix = ",";
				}
				sb.append(prefix + ele);
				idx += 1;
			}
//			sb.append("{flowName:'flow-1', flowTypeName:'osaka11slow'}");
//			sb.append(",");
//			sb.append("{flowName:'flow-2', flowTypeName:'akashi23cutthrough'}");
			scriptArgFlowListItems = "[" + sb.toString() + "]";
		} else {
			scriptArgFlowListItems = "null";
		}
		
		String scriptFmt = "window.APP.api.setFlowListItems(%s, %s);";
		String script = String.format(scriptFmt, scriptArgSliceName, scriptArgFlowListItems);
		try {
			LOG.info("executeScript(" + script + ")");
			topologyWebEngine.executeScript(script);
		} catch (RuntimeException e) {
			LOG.warn("Failed to execute script.", e);
		}
	}
	
	private void clearTopologyViewFlowList() {
		setTopologyViewFlowList(null);
	}
	
	/**
	 * @param topologyViewUrl the topologyViewUrl to set
	 */
	public void setTopologyViewUrl(String topologyViewUrl) {
		this.topologyViewUrl = topologyViewUrl;
		reloadTopologyUrl();
	}
	
	public void reloadTopologyUrl() {
		if (this.topologyViewUrl != null) {
			topologyWebEngine.load(topologyViewUrl);
		}
	}
}
